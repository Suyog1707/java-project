package com.collegechaos;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.collegechaos.dao.*;
import com.collegechaos.model.*;
import com.collegechaos.service.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;

class GameServiceTest {
  ScenarioDAO dao;
  GameResultDAO results;
  GameService service;
  ScenarioOption good =
      new ScenarioOption(1, "Study", 0, -5, 5, -10, 10, 4, 550, "Learning unlocked.");

  Scenario scenario(long id) {
    return new Scenario(
        id,
        "Exam",
        "Exam tomorrow",
        "Choose wisely",
        List.of(
            good, new ScenarioOption(2, "Panic", -60, 60, -50, -2000, -90, -90, 100, "Panic!")));
  }

  @BeforeEach
  void setup() {
    dao = mock(ScenarioDAO.class);
    results = mock(GameResultDAO.class);
    service = new GameService(dao, results);
  }

  @Test
  void initialStats() {
    GameStats s = new GameStats();
    assertEquals(100, s.health);
    assertEquals(10, s.stress);
    assertEquals(80, s.attendance);
    assertEquals(1000, s.money);
    assertEquals(40, s.knowledge);
    assertEquals(50, s.social);
  }

  @Test
  void appliesAndClamps() {
    GameStats s = new GameStats();
    s.apply(new ScenarioOption(3, "X", 100, -200, 100, -2000, -200, 200, 0, "X"));
    assertEquals(100, s.health);
    assertEquals(0, s.stress);
    assertEquals(100, s.attendance);
    assertEquals(0, s.money);
    assertEquals(0, s.knowledge);
    assertEquals(100, s.social);
  }

  @Test
  void upperMoneyBound() {
    GameStats s = new GameStats();
    s.money = 1000000;
    s.clamp();
    assertEquals(100000, s.money);
  }

  @Test
  void shuffledUniqueScenarios() throws Exception {
    List<Scenario> pool = new ArrayList<>();
    for (int i = 1; i <= 30; i++) pool.add(scenario(i));
    when(dao.active()).thenReturn(pool);
    GameState g = service.start(1);
    assertEquals(10, g.scenarios.size());
    assertEquals(10, g.scenarios.stream().map(Scenario::id).distinct().count());
    assertEquals(30, pool.size());
  }

  @Test
  void insufficientScenarios() throws Exception {
    when(dao.active()).thenReturn(List.of(scenario(1)));
    assertThrows(IllegalArgumentException.class, () -> service.start(1));
  }

  @Test
  void correctChoiceAndOriginalUnchanged() throws Exception {
    GameState g = new GameState(List.of(scenario(1), scenario(2)));
    GameState next = service.choose(1, g, g.runId, 1, 1);
    assertEquals(550, next.score);
    assertEquals(5, next.stats.stress);
    assertEquals(50, next.stats.knowledge);
    assertEquals(1, next.completed);
    assertEquals(2, next.current().id());
    assertEquals(0, g.completed);
    assertEquals(10, g.stats.stress);
    verifyNoInteractions(results);
  }

  @Test
  void invalidChoiceLeavesRunUntouched() {
    GameState g = new GameState(List.of(scenario(1)));
    assertThrows(IllegalArgumentException.class, () -> service.choose(1, g, g.runId, 1, 99));
    assertEquals(0, g.completed);
    verifyNoInteractions(results);
  }

  @Test
  void staleScenarioRejected() {
    GameState g = new GameState(List.of(scenario(1)));
    assertThrows(IllegalArgumentException.class, () -> service.choose(1, g, g.runId, 99, 1));
  }

  @Test
  void staleRunRejected() {
    GameState g = new GameState(List.of(scenario(1)));
    assertThrows(IllegalArgumentException.class, () -> service.choose(1, g, "old", 1, 1));
  }

  @Test
  void missingGameRejected() {
    assertThrows(IllegalArgumentException.class, () -> service.choose(1, null, "x", 1, 1));
  }

  @Test
  void completionPersistsOnce() throws Exception {
    GameState g = new GameState(List.of(scenario(1)));
    GameState done = service.choose(1, g, g.runId, 1, 1);
    assertEquals("COMPLETED", done.status);
    assertNull(done.current());
    assertEquals(550 + (100 + 85 + 50 + 54) * 5 + 1500 - 50, done.score);
    assertEquals(ScoreService.tier(done.score), done.resultTitle);
    verify(results).save(1, done);
    assertThrows(IllegalArgumentException.class, () -> service.choose(1, done, done.runId, 1, 1));
  }

  @Test
  void persistenceFailureAllowsSafeRetry() throws Exception {
    GameState g = new GameState(List.of(scenario(1)));
    doThrow(new SQLException("offline")).when(results).save(anyLong(), any());
    assertThrows(SQLException.class, () -> service.choose(1, g, g.runId, 1, 1));
    assertEquals(0, g.completed);
    assertEquals("IN_PROGRESS", g.status);
    assertEquals(0, g.score);
  }

  @Test
  void stressGameOver() {
    GameState g = new GameState(List.of(scenario(1)));
    g.stats.stress = 100;
    assertEquals("STRESS_OVERLOAD", GameService.gameOver(g));
  }

  @Test
  void healthGameOver() {
    GameState g = new GameState(List.of(scenario(1)));
    g.stats.health = 0;
    assertEquals("HEALTH_COLLAPSE", GameService.gameOver(g));
  }

  @Test
  void attendanceGameOver() {
    GameState g = new GameState(List.of(scenario(1)));
    g.stats.attendance = 0;
    assertEquals("ATTENDANCE_DISASTER", GameService.gameOver(g));
  }

  @Test
  void finalChoiceFailureIsNotCompletion() {
    GameState g = new GameState(List.of(scenario(1)));
    g.completed = 1;
    g.stats.health = 0;
    assertEquals("HEALTH_COLLAPSE", GameService.gameOver(g));
  }

  @Test
  void quitPersists() throws Exception {
    GameState g = new GameState(List.of(scenario(1)));
    GameState done = service.quit(1, g);
    assertEquals("QUIT", done.status);
    verify(results).save(1, done);
    assertEquals("IN_PROGRESS", g.status);
  }

  @Test
  void publicStateHidesRules() {
    GameState g = new GameState(List.of(scenario(1)));
    String json = new com.google.gson.Gson().toJson(g.publicState());
    assertFalse(json.contains("Learning unlocked"));
    assertFalse(json.contains("health_change"));
    assertTrue(json.contains("Study"));
  }

  @Test
  void resultBoundaries() {
    int[] values = {0, 999, 1000, 2999, 3000, 4999, 5000, 6999, 7000, 8999, 9000};
    String[] titles = {
      "ACADEMICALLY DECEASED",
      "ACADEMICALLY DECEASED",
      "ACADEMICALLY CONFUSED",
      "ACADEMICALLY CONFUSED",
      "RUNNING ON LUCK",
      "RUNNING ON LUCK",
      "SURVIVED SOMEHOW",
      "SURVIVED SOMEHOW",
      "COLLEGE MASTER",
      "COLLEGE MASTER",
      "LEGENDARY SURVIVOR"
    };
    for (int i = 0; i < values.length; i++) assertEquals(titles[i], ScoreService.tier(values[i]));
  }

  @Test
  void scoreNeverNegative() {
    GameStats s = new GameStats();
    s.health = s.attendance = s.knowledge = s.social = 0;
    s.stress = 100;
    assertEquals(0, ScoreService.finalScore(0, s, false));
  }
}
