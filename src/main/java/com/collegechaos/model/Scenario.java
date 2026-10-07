package com.collegechaos.model;

import java.util.List;

public record Scenario(
    long id, String category, String title, String description, List<ScenarioOption> options) {
  public Object publicScenario() {
    return java.util.Map.of(
        "id",
        id,
        "category",
        category,
        "title",
        title,
        "description",
        description,
        "options",
        options.stream().map(ScenarioOption::publicChoice).toList());
  }
}
