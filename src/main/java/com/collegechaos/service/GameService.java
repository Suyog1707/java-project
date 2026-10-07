package com.collegechaos.service;

import com.collegechaos.dao.*;
import com.collegechaos.model.*;
import com.collegechaos.util.*;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class GameService {
  private final ScenarioDAO scenarios;
  private final GameResultDAO results;
  private static final Logger LOG = Logger.getLogger(GameService.class.getName());

  public GameService(ScenarioDAO scenarios, GameResultDAO results) {
    this.scenarios = scenarios;
    this.results = results;
  }

  public GameState start(long userId) throws SQLException {
    int length = Config.number("GAME_LENGTH", 10);
    List<Scenario> pool = new ArrayList<>(scenarios.active());
    ValidationUtil.require(
        length > 0 && pool.size() >= length, "Not enough scenarios available. Please try later.");
    Collections.shuffle(pool);
    LOG.info("Game started: userId=" + userId);
    return new GameState(pool.subList(0, length));
  }

  public GameState choose(
      long userId, GameState current, String runId, long scenarioId, long optionId)
      throws SQLException {
    ValidationUtil.require(current != null && current.current() != null, "Start a new game first.");
    ValidationUtil.require(
        current.runId.equals(runId) && current.current().id() == scenarioId,
        "This scenario has already changed. Refresh your game.");
    ScenarioOption choice =
        current.current().options().stream()
            .filter(o -> o.id() == optionId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid game choice."));
    GameState next = new GameState(current);
    next.stats.apply(choice);
    next.score += choice.score();
    next.completed++;
    next.outcome = choice.outcome();
    String reason = gameOver(next);
    if (reason != null) finish(userId, next, reason);
    return next;
  }

  public static String gameOver(GameState g) {
    if (g.stats.stress >= 100) return "STRESS_OVERLOAD";
    if (g.stats.health <= 0) return "HEALTH_COLLAPSE";
    if (g.stats.attendance <= 0) return "ATTENDANCE_DISASTER";
    if (g.completed >= g.scenarios.size()) return "COMPLETED";
    return null;
  }

  private void finish(long userId, GameState next, String reason) throws SQLException {
    next.status = reason;
    next.score = ScoreService.finalScore(next.score, next.stats, reason.equals("COMPLETED"));
    next.resultTitle = ScoreService.tier(next.score);
    next.summary =
        switch (reason) {
          case "COMPLETED" -> "You survived college without understanding how. Respect.";
          case "STRESS_OVERLOAD" ->
              "Your brain opened 47 tabs. Every single one was playing music.";
          case "HEALTH_COLLAPSE" -> "Your body has submitted a leave application.";
          case "ATTENDANCE_DISASTER" -> "The register thinks you are a fictional character.";
          default -> "You chose peace. The semester will remember this.";
        };
    results.save(userId, next);
    LOG.info("Game completed: userId=" + userId + ", score=" + next.score);
  }

  public GameState quit(long userId, GameState current) throws SQLException {
    ValidationUtil.require(current != null && current.current() != null, "No active game to quit.");
    GameState next = new GameState(current);
    finish(userId, next, "QUIT");
    return next;
  }
}
