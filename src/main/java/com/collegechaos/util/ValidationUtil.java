package com.collegechaos.util;

import java.nio.charset.StandardCharsets;

public final class ValidationUtil {
  private ValidationUtil() {}

  public static String clean(String value) {
    return value == null ? "" : value.strip();
  }

  public static void require(boolean condition, String message) {
    if (!condition) throw new IllegalArgumentException(message);
  }

  public static void password(String value) {
    require(
        value != null && value.length() >= 8 && value.getBytes(StandardCharsets.UTF_8).length <= 72,
        "Password must be at least 8 characters and at most 72 UTF-8 bytes.");
  }

  public static void registration(
      String name, String username, String email, String password, String confirmation) {
    require(
        !name.isBlank() && name.length() <= 100 && name.chars().noneMatch(Character::isISOControl),
        "Enter a name of up to 100 characters.");
    require(
        username.matches("[a-zA-Z0-9_]{3,50}"),
        "Username must be 3–50 letters, numbers or underscores.");
    require(
        email.length() <= 150 && email.matches("[^\\s@]+@[^\\s@]+[.][^\\s@]+"),
        "Enter a valid email address.");
    password(password);
    require(password.equals(confirmation), "Passwords do not match.");
  }
}
