package com.collegechaos.model;

public record User(
    long id, String name, String username, String email, String passwordHash, String createdAt) {
  public Object publicProfile() {
    return java.util.Map.of("id", id, "name", name, "username", username, "createdAt", createdAt);
  }
}
