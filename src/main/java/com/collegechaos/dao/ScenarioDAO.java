package com.collegechaos.dao;

import com.collegechaos.model.*;
import com.collegechaos.util.DatabaseConnection;
import java.sql.*;
import java.util.*;

public class ScenarioDAO {
  public List<Scenario> active() throws SQLException {
    List<Scenario> all = new ArrayList<>();
    try (Connection c = DatabaseConnection.open();
        PreparedStatement p =
            c.prepareStatement("SELECT * FROM scenarios WHERE is_active=TRUE ORDER BY id");
        ResultSet r = p.executeQuery();
        PreparedStatement choices =
            c.prepareStatement("SELECT * FROM scenario_options WHERE scenario_id=? ORDER BY id")) {
      while (r.next()) {
        List<ScenarioOption> options = new ArrayList<>();
        choices.setLong(1, r.getLong("id"));
        try (ResultSet o = choices.executeQuery()) {
          while (o.next())
            options.add(
                new ScenarioOption(
                    o.getLong("id"),
                    o.getString("option_text"),
                    o.getInt("health_change"),
                    o.getInt("stress_change"),
                    o.getInt("attendance_change"),
                    o.getInt("money_change"),
                    o.getInt("knowledge_change"),
                    o.getInt("social_change"),
                    o.getInt("score_change"),
                    o.getString("outcome_text")));
        }
        if (options.size() != 4) throw new SQLException("Each active scenario needs four options");
        all.add(
            new Scenario(
                r.getLong("id"),
                r.getString("category"),
                r.getString("title"),
                r.getString("description"),
                List.copyOf(options)));
      }
    }
    return all;
  }
}
