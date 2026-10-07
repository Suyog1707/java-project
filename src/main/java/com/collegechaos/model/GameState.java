package com.collegechaos.model;

import java.util.*;

public final class GameState {
  public final String runId;
  public final long startedAt;
  public final List<Scenario> scenarios;
  public GameStats stats;
  public int completed, score;
  public String status = "IN_PROGRESS", outcome = "", resultTitle = "", summary = "";

  public GameState(List<Scenario> scenarios) {
    this.runId = UUID.randomUUID().toString();
    this.startedAt = System.currentTimeMillis();
    this.scenarios = List.copyOf(scenarios);
    this.stats = new GameStats();
  }

  public GameState(GameState other) {
    runId = other.runId;
    startedAt = other.startedAt;
    scenarios = other.scenarios;
    stats = new GameStats(other.stats);
    completed = other.completed;
    score = other.score;
    status = other.status;
    outcome = other.outcome;
    resultTitle = other.resultTitle;
    summary = other.summary;
  }

  public Scenario current() {
    return completed < scenarios.size() && status.equals("IN_PROGRESS")
        ? scenarios.get(completed)
        : null;
  }

  public Object publicState() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("runId", runId);
    m.put("stats", stats);
    m.put("score", score);
    m.put("completed", completed);
    m.put("total", scenarios.size());
    m.put("scenarioNumber", Math.min(completed + 1, scenarios.size()));
    m.put("status", status);
    m.put("outcome", outcome);
    m.put("resultTitle", resultTitle);
    m.put("summary", summary);
    m.put("scenario", current() == null ? null : current().publicScenario());
    return m;
  }
}
