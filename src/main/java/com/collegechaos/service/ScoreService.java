package com.collegechaos.service;

import com.collegechaos.model.GameStats;

public final class ScoreService {
  private ScoreService() {}

  public static int finalScore(int base, GameStats s, boolean completed) {
    return Math.max(
        0,
        base
            + (s.health + s.attendance + s.knowledge + s.social) * 5
            + (completed ? 1500 : 0)
            - s.stress * 10);
  }

  public static String tier(int n) {
    if (n >= 9000) return "LEGENDARY SURVIVOR";
    if (n >= 7000) return "COLLEGE MASTER";
    if (n >= 5000) return "SURVIVED SOMEHOW";
    if (n >= 3000) return "RUNNING ON LUCK";
    if (n >= 1000) return "ACADEMICALLY CONFUSED";
    return "ACADEMICALLY DECEASED";
  }
}
