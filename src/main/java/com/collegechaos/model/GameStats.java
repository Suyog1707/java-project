package com.collegechaos.model;

import com.collegechaos.util.Config;

public final class GameStats {
  public int health, stress, attendance, money, knowledge, social;

  public GameStats() {
    health = Config.number("INITIAL_HEALTH", 100);
    stress = Config.number("INITIAL_STRESS", 10);
    attendance = Config.number("INITIAL_ATTENDANCE", 80);
    money = Config.number("INITIAL_MONEY", 1000);
    knowledge = Config.number("INITIAL_KNOWLEDGE", 40);
    social = Config.number("INITIAL_SOCIAL", 50);
    clamp();
  }

  public GameStats(GameStats other) {
    health = other.health;
    stress = other.stress;
    attendance = other.attendance;
    money = other.money;
    knowledge = other.knowledge;
    social = other.social;
  }

  private static int bounded(int n) {
    return Math.max(0, Math.min(100, n));
  }

  public void clamp() {
    health = bounded(health);
    stress = bounded(stress);
    attendance = bounded(attendance);
    knowledge = bounded(knowledge);
    social = bounded(social);
    money = Math.max(0, Math.min(100000, money));
  }

  public void apply(ScenarioOption o) {
    health += o.health();
    stress += o.stress();
    attendance += o.attendance();
    money += o.money();
    knowledge += o.knowledge();
    social += o.social();
    clamp();
  }
}
