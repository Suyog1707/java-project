package com.collegechaos.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Set;

@WebFilter("/*")
public class SecurityFilter implements Filter {
  private static final Set<String> PROTECTED =
      Set.of("dashboard", "profile", "history", "game", "result");

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    req.setCharacterEncoding("UTF-8");
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("X-Frame-Options", "DENY");
    res.setHeader("Referrer-Policy", "same-origin");
    res.setHeader(
        "Content-Security-Policy",
        "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src"
            + " 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
    res.setHeader("Cache-Control", "no-store");
    String path = req.getRequestURI().substring(req.getContextPath().length());
    String page = path.replaceFirst("^/", "").replaceFirst("[.]html$", "");
    if (PROTECTED.contains(page)
        && (req.getSession(false) == null
            || req.getSession(false).getAttribute("userId") == null)) {
      res.sendRedirect(req.getContextPath() + "/login.html");
      return;
    }
    if (Set.of(
                "login",
                "register",
                "dashboard",
                "profile",
                "history",
                "game",
                "result",
                "leaderboard",
                "error")
            .contains(page)
        && !path.endsWith(".html")) {
      req.getRequestDispatcher("/" + page + ".html").forward(req, res);
      return;
    }
    chain.doFilter(request, response);
  }
}
