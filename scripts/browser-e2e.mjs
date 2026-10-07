import { chromium } from "playwright";
import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { mkdirSync } from "node:fs";
import { randomUUID } from "node:crypto";
const url = process.env.APP_URL || "http://127.0.0.1:20007/college-survival";
const username = "browser_" + randomUUID().replaceAll("-", "").slice(0, 16),
  password = "Browser-Test-123";
const db = (sql) =>
  execFileSync(
    "docker",
    [
      "exec",
      "-i",
      "college-survival-mysql",
      "sh",
      "-c",
      'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" -N -B college_survival',
    ],
    { input: sql, encoding: "utf8" },
  ).trim();
mkdirSync("docs/screenshots", { recursive: true });
const browser = await chromium.launch({
  executablePath: process.env.CHROME_PATH || "/opt/google/chrome/chrome",
  headless: true,
});
const context = await browser.newContext({
  viewport: { width: 1440, height: 1080 },
});
const page = await context.newPage(),
  errors = [],
  serverErrors = [];
page.on("pageerror", (e) => errors.push(e.message));
page.on("response", (r) => {
  if (r.status() >= 500) serverErrors.push(r.url() + ": " + r.status());
});
const pass = (name) => console.log("PASS " + name);
async function noOverflow() {
  const overflow = await page.evaluate(() => ({
    width: innerWidth,
    scroll: document.documentElement.scrollWidth,
    elements: [...document.querySelectorAll("body *")]
      .filter((e) => e.getBoundingClientRect().right > innerWidth + 1)
      .map((e) => ({
        tag: e.tagName,
        class: e.className,
        right: e.getBoundingClientRect().right,
      }))
      .slice(0, 8),
  }));
  assert(
    overflow.scroll <= overflow.width,
    "Horizontal overflow " + JSON.stringify(overflow),
  );
}
async function screenshot(name) {
  await page.screenshot({
    path: `docs/screenshots/${name}.png`,
    fullPage: true,
  });
}
try {
  await page.goto(url + "/");
  await page.locator(".hero .primary").waitFor();
  await noOverflow();
  await screenshot("landing-desktop");
  await page.setViewportSize({ width: 390, height: 844 });
  await noOverflow();
  await screenshot("landing-mobile");
  await page.setViewportSize({ width: 1440, height: 1080 });
  pass("landing desktop + mobile layout");
  await page.goto(url + "/dashboard.html");
  await page.waitForURL("**/login.html");
  pass("protected page redirects");
  await page.goto(url + "/register.html");
  for (const [key, value] of Object.entries({
    name: "Campus Test Player",
    username,
    email: username + "@example.org",
    password,
    confirmPassword: password,
  }))
    await page.locator(`[name="${key}"]`).fill(value);
  await page.locator('button[type="submit"]').click();
  await page.waitForURL("**/login.html?registered=1");
  pass("browser registration");
  await page.locator('[name="username"]').fill(username);
  await page.locator('[name="password"]').fill("wrong-password");
  await page.locator('button[type="submit"]').click();
  await page.locator(".form-error").filter({ hasText: "incorrect" }).waitFor();
  pass("invalid login feedback");
  await page.locator('[name="password"]').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForURL("**/dashboard.html");
  await page.locator("#start-game").waitFor();
  await screenshot("dashboard");
  pass("login and dashboard");
  await page.locator("#start-game").click();
  await page.waitForURL("**/game.html");
  await page.locator(".choice").last().waitFor();
  const firstTitle = await page.locator(".scenario-card h2").textContent();
  await page.reload();
  await page.locator(".choice").last().waitFor();
  assert.equal(
    await page.locator(".scenario-card h2").textContent(),
    firstTitle,
  );
  pass("refresh keeps scenario");
  await screenshot("game-desktop");
  await page.setViewportSize({ width: 390, height: 844 });
  await noOverflow();
  await screenshot("game-mobile");
  for (let i = 0; i < 10; i++) {
    await page.locator(".choice").last().click();
    await page.locator("#continue-game").waitFor();
    assert(await page.locator(".outcome-card h2").textContent());
    await page.locator("#continue-game").click();
  }
  await page.waitForURL("**/result.html");
  await page.locator(".result-score").waitFor();
  await noOverflow();
  await screenshot("result-mobile");
  await page.setViewportSize({ width: 1440, height: 1080 });
  await screenshot("result");
  const score = Number(
    (await page.locator(".result-score").textContent()).replaceAll(",", ""),
  );
  const title = await page.locator(".result-header h1").textContent();
  const saved = db(
    `SELECT g.score,g.result_title,g.scenarios_completed FROM game_results g JOIN users u ON g.user_id=u.id WHERE u.username='${username}';`,
  ).split("\t");
  assert.equal(Number(saved[0]), score);
  assert.equal(saved[1], title);
  assert.equal(Number(saved[2]), 10);
  pass("10 choices, outcomes, result + exact MySQL score/title");
  await page.goto(url + "/history.html");
  await page.locator("tbody tr").waitFor();
  assert(
    (await page.locator("tbody").textContent()).includes(
      score.toLocaleString("en-IN"),
    ),
  );
  await screenshot("history");
  pass("history");
  await page.goto(url + "/leaderboard.html");
  await page.getByRole("cell", { name: username, exact: true }).waitFor();
  await screenshot("leaderboard");
  pass("leaderboard");
  await page.goto(url + "/profile.html");
  await page.locator(".profile-card h2").waitFor();
  assert.equal(
    await page.locator(".profile-card h2").textContent(),
    "Campus Test Player",
  );
  await screenshot("profile");
  pass("profile");
  await page.goto(url + "/dashboard.html");
  await page.locator(".logout").click();
  await page.waitForURL("**/index.html");
  await page.goBack();
  await page.waitForURL("**/login.html");
  pass("logout and back navigation protection");
  assert.deepEqual(errors, []);
  assert.deepEqual(serverErrors, []);
  pass("no JavaScript exceptions or server errors");
  console.log(
    "All browser end-to-end checks passed. Screenshots: docs/screenshots/",
  );
} finally {
  await browser.close();
  db(`DELETE FROM users WHERE username='${username}';`);
}
