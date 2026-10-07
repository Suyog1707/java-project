package com.collegechaos.dao;

import com.collegechaos.model.*;
import com.collegechaos.util.DatabaseConnection;
import java.sql.*;
import java.util.*;

public class GameResultDAO {
  public void save(long userId, GameState g) throws SQLException {
    try (Connection c = DatabaseConnection.open();
        PreparedStatement p =
            c.prepareStatement(
                "INSERT INTO"
                    + " game_results(run_id,user_id,score,result_title,status,health,stress,attendance,money,knowledge,social,scenarios_completed,started_at)"
                    + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE run_id=run_id")) {
      p.setString(1, g.runId);
      p.setLong(2, userId);
      p.setInt(3, g.score);
      p.setString(4, g.resultTitle);
      p.setString(5, g.status);
      p.setInt(6, g.stats.health);
      p.setInt(7, g.stats.stress);
      p.setInt(8, g.stats.attendance);
      p.setInt(9, g.stats.money);
      p.setInt(10, g.stats.knowledge);
      p.setInt(11, g.stats.social);
      p.setInt(12, g.completed);
      p.setTimestamp(13, new Timestamp(g.startedAt));
      p.executeUpdate();
    }
  }

  private List<Map<String, Object>> rows(PreparedStatement p) throws SQLException {
    try (ResultSet r = p.executeQuery()) {
      List<Map<String, Object>> rows = new ArrayList<>();
      ResultSetMetaData meta = r.getMetaData();
      while (r.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
          Object v = r.getObject(i);
          row.put(meta.getColumnLabel(i), v instanceof Timestamp ? v.toString() : v);
        }
        rows.add(row);
      }
      return rows;
    }
  }

  public List<Map<String, Object>> history(long id, int page) throws SQLException {
    try (Connection c = DatabaseConnection.open();
        PreparedStatement p =
            c.prepareStatement(
                "SELECT"
                    + " id,score,result_title,status,scenarios_completed,health,stress,attendance,money,knowledge,social,completed_at,TIMESTAMPDIFF(SECOND,started_at,completed_at)"
                    + " AS duration FROM game_results WHERE user_id=? ORDER BY completed_at DESC,id"
                    + " DESC LIMIT 20 OFFSET ?")) {
      p.setLong(1, id);
      p.setInt(2, page * 20);
      return rows(p);
    }
  }

  public List<Map<String, Object>> leaderboard() throws SQLException {
    try (Connection c = DatabaseConnection.open();
        PreparedStatement p =
            c.prepareStatement(
                "SELECT username,score,result_title,completed_at FROM (SELECT"
                    + " u.username,g.score,g.result_title,g.completed_at,ROW_NUMBER()"
                    + " OVER(PARTITION BY g.user_id ORDER BY g.score DESC,g.completed_at ASC,g.id"
                    + " ASC) AS row_num FROM game_results g JOIN users u ON u.id=g.user_id WHERE"
                    + " g.status<>'QUIT') ranked WHERE row_num=1 ORDER BY score DESC,completed_at"
                    + " ASC,username ASC LIMIT 50")) {
      return rows(p);
    }
  }

  public Map<String, Object> statistics(long id) throws SQLException {
    try (Connection c = DatabaseConnection.open();
        PreparedStatement p =
            c.prepareStatement(
                "SELECT COUNT(*) AS gamesPlayed,COALESCE(MAX(CASE WHEN status<>'QUIT' THEN score"
                    + " END),0) AS bestScore,COALESCE(MAX(scenarios_completed),0) AS"
                    + " highestSurvival FROM game_results WHERE user_id=?")) {
      p.setLong(1, id);
      Map<String, Object> m = rows(p).getFirst();
      int best = ((Number) m.get("bestScore")).intValue();
      m.put("bestResult", com.collegechaos.service.ScoreService.tier(best));
      try (PreparedStatement rank =
          c.prepareStatement(
              "SELECT COUNT(*)+1 FROM (SELECT user_id,MAX(score) AS best FROM game_results WHERE"
                  + " status<>'QUIT' GROUP BY user_id) scores WHERE best>?")) {
        rank.setInt(1, best);
        try (ResultSet r = rank.executeQuery()) {
          r.next();
          m.put("rank", r.getInt(1));
        }
      }
      return m;
    }
  }
}
