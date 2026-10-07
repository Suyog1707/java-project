package com.collegechaos.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import org.junit.jupiter.api.*;

class ControllerTest {
  HttpServletRequest req;
  HttpServletResponse res;
  HttpSession session;
  StringWriter body;
  ApiServlet servlet;

  @BeforeEach
  void setup() throws Exception {
    req = mock(HttpServletRequest.class);
    res = mock(HttpServletResponse.class);
    session = mock(HttpSession.class);
    body = new StringWriter();
    when(res.getWriter()).thenReturn(new PrintWriter(body));
    when(req.getSession(true)).thenReturn(session);
    when(session.getAttribute("csrf")).thenReturn("test-token");
    when(req.getMethod()).thenReturn("GET");
    servlet = new ApiServlet();
  }

  void call() throws Exception {
    servlet.service((ServletRequest) req, (ServletResponse) res);
  }

  @Test
  void protectedApi401() throws Exception {
    when(req.getPathInfo()).thenReturn("/dashboard");
    call();
    verify(res).setStatus(401);
    assertTrue(body.toString().contains("Please log in"));
  }

  @Test
  void unknownEndpoint404() throws Exception {
    when(req.getPathInfo()).thenReturn("/missing");
    call();
    verify(res).setStatus(404);
  }

  @Test
  void unsupportedMethod405() throws Exception {
    when(req.getPathInfo()).thenReturn("/game/new");
    call();
    verify(res).setStatus(405);
    verify(res).setHeader("Allow", "POST");
  }

  @Test
  void missingCsrf403() throws Exception {
    when(req.getPathInfo()).thenReturn("/login");
    when(req.getMethod()).thenReturn("POST");
    call();
    verify(res).setStatus(403);
  }

  @Test
  void malformedJson400() throws Exception {
    when(req.getPathInfo()).thenReturn("/register");
    when(req.getMethod()).thenReturn("POST");
    when(req.getHeader("X-CSRF-Token")).thenReturn("test-token");
    when(req.getContentType()).thenReturn("application/json");
    when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{bad")));
    call();
    verify(res).setStatus(400);
    assertTrue(body.toString().contains("Malformed JSON"));
  }

  @Test
  void logoutInvalidates() throws Exception {
    when(req.getPathInfo()).thenReturn("/logout");
    when(req.getMethod()).thenReturn("POST");
    when(req.getHeader("X-CSRF-Token")).thenReturn("test-token");
    when(session.getAttribute("userId")).thenReturn(1L);
    call();
    verify(session).invalidate();
    verify(res).setStatus(200);
  }

  @Test
  void invalidHistoryPage400() throws Exception {
    when(req.getPathInfo()).thenReturn("/history");
    when(req.getParameter("page")).thenReturn("bad");
    when(session.getAttribute("userId")).thenReturn(1L);
    call();
    verify(res).setStatus(400);
  }

  @Test
  void anonymousSessionPublic() throws Exception {
    when(req.getPathInfo()).thenReturn("/session");
    call();
    verify(res).setStatus(200);
    assertTrue(body.toString().contains("\"authenticated\":false"));
  }

  @Test
  void pageFilterRedirectsAndNoCache() throws Exception {
    when(req.getRequestURI()).thenReturn("/college-survival/history.html");
    when(req.getContextPath()).thenReturn("/college-survival");
    FilterChain chain = mock(FilterChain.class);
    new SecurityFilter().doFilter(req, res, chain);
    verify(res).sendRedirect("/college-survival/login.html");
    verify(res).setHeader("Cache-Control", "no-store");
    verifyNoInteractions(chain);
  }

  @Test
  void authenticatedPageAllowed() throws Exception {
    when(req.getRequestURI()).thenReturn("/college-survival/game.html");
    when(req.getContextPath()).thenReturn("/college-survival");
    when(req.getSession(false)).thenReturn(session);
    when(session.getAttribute("userId")).thenReturn(1L);
    FilterChain chain = mock(FilterChain.class);
    new SecurityFilter().doFilter(req, res, chain);
    verify(chain).doFilter(req, res);
  }
}
