package com.collegechaos.model;

public record ScenarioOption(
    long id,
    String text,
    int health,
    int stress,
    int attendance,
    int money,
    int knowledge,
    int social,
    int score,
    String outcome) {
  public Object publicChoice() {
    return java.util.Map.of("id", id, "text", text);
  }
}
