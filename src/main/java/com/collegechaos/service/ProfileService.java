package com.collegechaos.service;

import com.collegechaos.dao.*;
import com.collegechaos.model.User;
import java.sql.*;
import java.util.*;

public class ProfileService {
  private final UserDAO users;
  private final GameResultDAO results;

  public ProfileService(UserDAO users, GameResultDAO results) {
    this.users = users;
    this.results = results;
  }

  public Object profile(long id) throws SQLException {
    User user = users.byId(id);
    if (user == null) throw new IllegalArgumentException("Player no longer exists.");
    return Map.of(
        "player",
        user.publicProfile(),
        "stats",
        results.statistics(id),
        "recent",
        results.history(id, 0));
  }
}
