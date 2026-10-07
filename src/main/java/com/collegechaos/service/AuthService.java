package com.collegechaos.service;

import com.collegechaos.dao.UserDAO;
import com.collegechaos.model.User;
import com.collegechaos.util.*;
import java.sql.*;
import java.util.logging.Logger;

public class AuthService {
  private static final Logger LOG = Logger.getLogger(AuthService.class.getName());
  private final UserDAO users;
  private static final String DUMMY_HASH = PasswordUtil.hash("dummy-password-unused");

  public AuthService(UserDAO users) {
    this.users = users;
  }

  public void register(String name, String username, String email, String password, String confirm)
      throws SQLException {
    name = ValidationUtil.clean(name);
    username = ValidationUtil.clean(username).toLowerCase(java.util.Locale.ROOT);
    email = ValidationUtil.clean(email).toLowerCase(java.util.Locale.ROOT);
    ValidationUtil.registration(name, username, email, password, confirm);
    try {
      users.create(name, username, email, PasswordUtil.hash(password));
    } catch (SQLException e) {
      if (e.getErrorCode() == 1062)
        throw new IllegalArgumentException("That username or email is already registered.");
      throw e;
    }
    LOG.info("User registered: username=" + username);
  }

  public User login(String username, String password) throws SQLException {
    ValidationUtil.password(password);
    User user = users.byUsername(ValidationUtil.clean(username));
    boolean valid = PasswordUtil.verify(password, user == null ? DUMMY_HASH : user.passwordHash());
    ValidationUtil.require(user != null && valid, "Username or password is incorrect.");
    LOG.info("Login success: userId=" + user.id());
    return user;
  }
}
