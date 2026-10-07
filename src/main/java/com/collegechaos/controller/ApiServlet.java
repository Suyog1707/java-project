package com.collegechaos.controller;

import com.collegechaos.dao.*;
import com.collegechaos.model.*;
import com.collegechaos.service.*;
import com.google.gson.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.*;

@WebServlet("/api/*")
public class ApiServlet extends HttpServlet {
  private static final Gson JSON = new Gson();
  private static final Logger LOG = Logger.getLogger(ApiServlet.class.getName());
  private final UserDAO users = new UserDAO();
  private final GameResultDAO results = new GameResultDAO();
  private final AuthService auth = new AuthService(users);
  private final GameService game = new GameService(new ScenarioDAO(), results);
  private final ProfileService profile = new ProfileService(users, results);

  private void send(
      HttpServletResponse res, int status, boolean success, String message, Object data)
      throws IOException {
    res.setStatus(status);
    res.setContentType("application/json;charset=UTF-8");
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("success", success);
    body.put("message", message);
    if (data != null) body.put("data", data);
    res.getWriter().write(JSON.toJson(body));
  }

  private String value(JsonObject body, String key) {
    if (!body.has(key)
        || !body.get(key).isJsonPrimitive()
        || !body.getAsJsonPrimitive(key).isString())
      throw new IllegalArgumentException("Missing or invalid " + key + ".");
    return body.get(key).getAsString();
  }

  private JsonObject body(HttpServletRequest req) throws IOException {
    if (req.getContentType() == null
        || !req.getContentType().toLowerCase(Locale.ROOT).startsWith("application/json"))
      throw new IllegalArgumentException("Send a JSON request.");
    char[] buf = new char[16385];
    int count = 0, n;
    Reader r = req.getReader();
    while (count < buf.length && (n = r.read(buf, count, buf.length - count)) != -1) count += n;
    if (count > 16384) throw new IllegalArgumentException("Request is too large.");
    JsonElement e = JsonParser.parseString(new String(buf, 0, count));
    if (!e.isJsonObject()) throw new IllegalArgumentException("Send a JSON object.");
    return e.getAsJsonObject();
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {
    String path = req.getPathInfo();
    if (path == null) path = "/";
    boolean get = req.getMethod().equals("GET"), post = req.getMethod().equals("POST");
    Set<String> reads =
        Set.of(
            "/session",
            "/leaderboard",
            "/dashboard",
            "/profile",
            "/history",
            "/game/current",
            "/game/result");
    Set<String> writes =
        Set.of("/register", "/login", "/logout", "/game/new", "/game/choose", "/game/quit");
    if (!reads.contains(path) && !writes.contains(path)) {
      send(res, 404, false, "Endpoint not found.", null);
      return;
    }
    if (!(get && reads.contains(path) || post && writes.contains(path))) {
      res.setHeader("Allow", reads.contains(path) ? "GET" : "POST");
      send(res, 405, false, "Method not allowed.", null);
      return;
    }
    try {
      HttpSession session = req.getSession(true);
      if (session.getAttribute("csrf") == null)
        session.setAttribute("csrf", UUID.randomUUID().toString());
      if (path.equals("/session")) {
        send(
            res,
            200,
            true,
            "Session ready.",
            Map.of(
                "csrf",
                session.getAttribute("csrf"),
                "authenticated",
                session.getAttribute("userId") != null));
        return;
      }
      if (post && !Objects.equals(session.getAttribute("csrf"), req.getHeader("X-CSRF-Token"))) {
        send(res, 403, false, "Session changed. Refresh the page and try again.", null);
        return;
      }
      boolean publicPath = Set.of("/register", "/login", "/leaderboard").contains(path);
      if (!publicPath && session.getAttribute("userId") == null) {
        send(res, 401, false, "Please log in to continue.", null);
        return;
      }
      long userId =
          session.getAttribute("userId") == null
              ? 0
              : ((Number) session.getAttribute("userId")).longValue();
      Object data = null;
      String message = "All good.";
      switch (path) {
        case "/register" -> {
          JsonObject b = body(req);
          auth.register(
              value(b, "name"),
              value(b, "username"),
              value(b, "email"),
              value(b, "password"),
              value(b, "confirmPassword"));
          message = "Account created. Time to log in.";
        }
        case "/login" -> {
          JsonObject b = body(req);
          User user = auth.login(value(b, "username"), value(b, "password"));
          session.invalidate();
          session = req.getSession(true);
          session.setAttribute("userId", user.id());
          session.setAttribute("username", user.username());
          session.setAttribute("csrf", UUID.randomUUID().toString());
          data = Map.of("csrf", session.getAttribute("csrf"), "player", user.publicProfile());
        }
        case "/logout" -> {
          session.invalidate();
          message = "Logged out. Go touch grass.";
        }
        case "/leaderboard" -> data = results.leaderboard();
        case "/dashboard", "/profile" -> data = profile.profile(userId);
        case "/history" -> {
          int page = 0;
          try {
            if (req.getParameter("page") != null) page = Integer.parseInt(req.getParameter("page"));
          } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid history page.");
          }
          if (page < 0 || page > 10000) throw new IllegalArgumentException("Invalid history page.");
          data = Map.of("page", page, "rows", results.history(userId, page));
        }
        case "/game/new" -> {
          body(req);
          synchronized (session) {
            if (session.getAttribute("currentGame") != null)
              throw new IllegalArgumentException("Finish or quit your active game first.");
            GameState g = game.start(userId);
            session.setAttribute("currentGame", g);
            session.removeAttribute("lastResult");
            data = g.publicState();
          }
        }
        case "/game/current" -> {
          synchronized (session) {
            GameState g = (GameState) session.getAttribute("currentGame");
            if (g == null) throw new IllegalArgumentException("No active game. Start a new run.");
            data = g.publicState();
          }
        }
        case "/game/result" -> {
          synchronized (session) {
            GameState g = (GameState) session.getAttribute("lastResult");
            if (g == null) throw new IllegalArgumentException("Finish a game to see your result.");
            data = g.publicState();
          }
        }
        case "/game/choose", "/game/quit" -> {
          JsonObject b = body(req);
          synchronized (session) {
            GameState current = (GameState) session.getAttribute("currentGame");
            GameState next;
            if (path.equals("/game/choose")) {
              next =
                  game.choose(
                      userId,
                      current,
                      value(b, "runId"),
                      Long.parseLong(value(b, "scenarioId")),
                      Long.parseLong(value(b, "optionId")));
            } else {
              if (current == null || !current.runId.equals(value(b, "runId")))
                throw new IllegalArgumentException("This run has changed. Refresh your game.");
              next = game.quit(userId, current);
            }
            if (next.status.equals("IN_PROGRESS")) session.setAttribute("currentGame", next);
            else {
              session.removeAttribute("currentGame");
              session.setAttribute("lastResult", next);
            }
            data = next.publicState();
          }
        }
      }
      send(res, 200, true, message, data);
    } catch (IllegalArgumentException | JsonParseException | IllegalStateException e) {
      send(
          res,
          400,
          false,
          e instanceof JsonParseException ? "Malformed JSON request." : e.getMessage(),
          null);
    } catch (SQLException e) {
      LOG.log(Level.SEVERE, "Database operation failed", e);
      send(res, 503, false, "The campus server is taking a break. Please try again shortly.", null);
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Unexpected request failure", e);
      send(res, 500, false, "Something went wrong. Please try again.", null);
    }
  }
}
