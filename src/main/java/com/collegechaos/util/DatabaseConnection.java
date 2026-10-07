package com.collegechaos.util;

import java.sql.*;

public final class DatabaseConnection {
  private DatabaseConnection() {}

  public static Connection open() throws SQLException {
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      throw new SQLException("MySQL JDBC driver unavailable", e);
    }
    return DriverManager.getConnection(
        Config.get("DB_URL", "jdbc:mysql://127.0.0.1:3307/college_survival?serverTimezone=UTC"),
        Config.get("DB_USERNAME", "college_app"),
        Config.get("DB_PASSWORD", ""));
  }
}
