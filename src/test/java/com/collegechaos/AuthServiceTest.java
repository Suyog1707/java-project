package com.collegechaos;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.collegechaos.dao.*;
import com.collegechaos.model.*;
import com.collegechaos.service.*;
import com.collegechaos.util.*;
import java.sql.*;
import org.junit.jupiter.api.*;

class AuthServiceTest {
  @Test
  void validRegistration() {
    assertDoesNotThrow(
        () ->
            ValidationUtil.registration(
                "Student", "student_1", "test@example.org", "abcdefgh", "abcdefgh"));
  }

  @Test
  void invalidRegistration() {
    String[][] inputs = {
      {"", "valid", "a@b.co", "abcdefgh", "abcdefgh"},
      {"Name", "x", "a@b.co", "abcdefgh", "abcdefgh"},
      {"Name", "bad name", "a@b.co", "abcdefgh", "abcdefgh"},
      {"Name", "valid", "bademail", "abcdefgh", "abcdefgh"},
      {"Name", "valid", "a b@c.co", "abcdefgh", "abcdefgh"},
      {"Name", "valid", "a@b.co", "short", "short"},
      {"Name", "valid", "a@b.co", "abcdefgh", "different"}
    };
    for (String[] s : inputs)
      assertThrows(
          IllegalArgumentException.class,
          () -> ValidationUtil.registration(s[0], s[1], s[2], s[3], s[4]));
  }

  @Test
  void bcryptLengthLimit() {
    assertThrows(IllegalArgumentException.class, () -> ValidationUtil.password("é".repeat(37)));
  }

  @Test
  void bcryptHashing() {
    String hash = PasswordUtil.hash("survival-password");
    assertNotEquals("survival-password", hash);
    assertTrue(hash.startsWith("$2a$"));
    assertTrue(PasswordUtil.verify("survival-password", hash));
    assertFalse(PasswordUtil.verify("wrong", hash));
  }

  @Test
  void normalizedRegistration() throws Exception {
    UserDAO dao = mock(UserDAO.class);
    new AuthService(dao)
        .register(" Student ", " PLAYER ", " STUDENT@EXAMPLE.ORG ", "abcdefgh", "abcdefgh");
    verify(dao)
        .create(
            eq("Student"),
            eq("player"),
            eq("student@example.org"),
            argThat(v -> v.startsWith("$2a$")));
  }

  @Test
  void duplicateRegistrationFriendly() throws Exception {
    UserDAO dao = mock(UserDAO.class);
    doThrow(new SQLException("duplicate", "23000", 1062))
        .when(dao)
        .create(anyString(), anyString(), anyString(), anyString());
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new AuthService(dao)
                    .register("Student", "player", "student@example.org", "abcdefgh", "abcdefgh"));
    assertTrue(e.getMessage().contains("already registered"));
  }

  @Test
  void loginSuccess() throws Exception {
    UserDAO dao = mock(UserDAO.class);
    User u =
        new User(
            1, "Student", "student", "e@example.org", PasswordUtil.hash("abcdefgh"), "2026-01-01");
    when(dao.byUsername("student")).thenReturn(u);
    assertEquals(u, new AuthService(dao).login("student", "abcdefgh"));
  }

  @Test
  void loginWrongOrUnknown() throws Exception {
    UserDAO dao = mock(UserDAO.class);
    User u =
        new User(
            1, "Student", "student", "e@example.org", PasswordUtil.hash("abcdefgh"), "2026-01-01");
    when(dao.byUsername("student")).thenReturn(u);
    AuthService s = new AuthService(dao);
    assertThrows(IllegalArgumentException.class, () -> s.login("student", "incorrect"));
    assertThrows(IllegalArgumentException.class, () -> s.login("unknown", "abcdefgh"));
  }

  @Test
  void privateFieldsNotReturned() {
    User u = new User(1, "Student", "player", "secret@example.org", "password-hash", "date");
    String json = new com.google.gson.Gson().toJson(u.publicProfile());
    assertFalse(json.contains("secret@example.org"));
    assertFalse(json.contains("password-hash"));
  }
}
