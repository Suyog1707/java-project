package com.collegechaos;

import static org.junit.jupiter.api.Assertions.*;

import com.collegechaos.dao.*;
import com.collegechaos.model.*;
import com.collegechaos.service.*;
import com.collegechaos.util.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class DatabaseIntegrationTest {
  long id;
  UserDAO users = new UserDAO();
  GameResultDAO results = new GameResultDAO();

  @AfterEach
  void cleanup() throws Exception {
    if (id != 0)
      try (Connection c = DatabaseConnection.open();
          PreparedStatement p = c.prepareStatement("DELETE FROM users WHERE id=?")) {
        p.setLong(1, id);
        p.executeUpdate();
      }
  }

  @Test
  void realMysqlUserScenariosRunAndQueries() throws Exception {
    String username = "junit_" + UUID.randomUUID().toString().replace("-", "");
    new AuthService(users)
        .register(
            "Integration Player",
            username,
            username + "@example.org",
            "Test-pass-123",
            "Test-pass-123");
    User u = users.byUsername(username);
    assertNotNull(u);
    id = u.id();
    assertEquals(id, users.byId(id).id());
    assertTrue(PasswordUtil.verify("Test-pass-123", u.passwordHash()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AuthService(users)
                .register(
                    "Duplicate",
                    username,
                    username + "@example.org",
                    "Test-pass-123",
                    "Test-pass-123"));
    List<Scenario> pool = new ScenarioDAO().active();
    assertTrue(pool.size() >= 25);
    assertTrue(pool.stream().allMatch(s -> s.options().size() == 4));
    GameService game = new GameService(new ScenarioDAO(), results);
    GameState g = game.start(id);
    while (g.current() != null) {
      Scenario s = g.current();
      g = game.choose(id, g, g.runId, s.id(), s.options().getLast().id());
    }
    assertEquals("COMPLETED", g.status);
    results.save(id, g);
    var history = results.history(id, 0);
    assertEquals(1, history.size());
    assertEquals(g.score, ((Number) history.getFirst().get("score")).intValue());
    assertEquals(g.resultTitle, history.getFirst().get("result_title"));
    assertEquals(1, ((Number) results.statistics(id).get("gamesPlayed")).intValue());
    assertTrue(results.leaderboard().stream().anyMatch(r -> r.get("username").equals(username)));
    assertTrue(results.history(id, 1).isEmpty());
  }
}
