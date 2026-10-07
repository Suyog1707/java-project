package com.collegechaos.dao;

import com.collegechaos.model.User;
import com.collegechaos.util.DatabaseConnection;
import java.sql.*;

public class UserDAO {
  private User row(ResultSet r) throws SQLException {
    return new User(
        r.getLong("id"),
        r.getString("name"),
        r.getString("username"),
        r.getString("email"),
        r.getString("password_hash"),
        r.getString("created_at"));
  }

  public User byUsername(String username) throws SQLException {
    try (Connection c = DatabaseConnection.open();
        PreparedStatement p = c.prepareStatement("SELECT * FROM users WHERE username=?")) {
      p.setString(1, username);
      try (ResultSet r = p.executeQuery()) {
        return r.next() ? row(r) : null;
      }
    }
  }

  public User byId(long id) throws SQLException {
    try (Connection c = DatabaseConnection.open();
        PreparedStatement p = c.prepareStatement("SELECT * FROM users WHERE id=?")) {
      p.setLong(1, id);
      try (ResultSet r = p.executeQuery()) {
        return r.next() ? row(r) : null;
      }
    }
  }

  public void create(String name, String username, String email, String hash) throws SQLException {
    try (Connection c = DatabaseConnection.open();
        PreparedStatement p =
            c.prepareStatement(
                "INSERT INTO users(name,username,email,password_hash) VALUES(?,?,?,?)")) {
      p.setString(1, name);
      p.setString(2, username);
      p.setString(3, email);
      p.setString(4, hash);
      p.executeUpdate();
    }
  }
}
