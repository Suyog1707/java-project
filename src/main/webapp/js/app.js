"use strict";
const main = document.querySelector("#main"),
  page = document.body.dataset.page;
let csrf = "",
  activeGame = null,
  historyPage = 0;
const escapeHtml = (s) =>
  String(s ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const fmt = (n) => Number(n).toLocaleString("en-IN");
const date = (s) =>
  new Date(String(s).replace(" ", "T") + "Z").toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
function toast(text) {
  const el = document.querySelector("#toast");
  el.textContent = text;
  el.hidden = false;
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => (el.hidden = true), 5000);
}
async function api(path, data) {
  const response = await fetch("api/" + path, {
    method: data === undefined ? "GET" : "POST",
    credentials: "same-origin",
    headers:
      data === undefined
        ? {}
        : { "Content-Type": "application/json", "X-CSRF-Token": csrf },
    body: data === undefined ? undefined : JSON.stringify(data),
  });
  let body;
  try {
    body = await response.json();
  } catch {
    throw new Error("Campus server unavailable. Please try again.");
  }
  if (!response.ok || !body.success) {
    if (response.status === 401) {
      location.replace("login.html");
    }
    throw new Error(body.message || "Something went wrong.");
  }
  return body.data;
}
function heading(label, title, subtitle, action = "") {
  return `<section class="page-heading"><div><div class="eyebrow">${label}</div><h1>${title}</h1><p>${subtitle}</p></div>${action}</section>`;
}
function table(rows, mode) {
  if (!rows.length)
    return `<section class="panel empty"><h3>No runs on the board. Yet.</h3><p>A campus legend has to start somewhere.</p><a class="button primary" href="dashboard.html">START SURVIVING ↗</a></section>`;
  return `<div class="table-wrap"><table><thead><tr>${(mode === "leaderboard" ? ["RANK", "PLAYER", "SCORE", "RESULT", "DATE"] : ["DATE", "SCORE", "RESULT", "SCENARIOS", "DURATION"]).map((s) => `<th scope="col">${s}</th>`).join("")}</tr></thead><tbody>${rows.map((r, i) => `<tr>${mode === "leaderboard" ? `<td class="rank">${String(i + 1).padStart(2, "0")}</td><td>${escapeHtml(r.username)}</td>` : `<td>${escapeHtml(date(r.completed_at))}</td>`}<td><strong>${fmt(r.score)}</strong></td><td>${escapeHtml(r.result_title)}${r.status === "QUIT" ? " · QUIT" : ""}</td>${mode === "leaderboard" ? `<td>${escapeHtml(date(r.completed_at))}</td>` : `<td>${r.scenarios_completed}</td><td>${Math.floor(r.duration / 60)}m ${r.duration % 60}s</td>`}</tr>`).join("")}</tbody></table></div>`;
}
function stats(s) {
  return `<section class="stats" aria-label="Player statistics">${[
    ["health", "♥", "Health"],
    ["stress", "ϟ", "Stress"],
    ["attendance", "✓", "Attendance"],
    ["knowledge", "▤", "Knowledge"],
    ["social", "☺", "Social"],
    ["money", "₹", "Money"],
  ]
    .map(
      ([key, icon, label]) =>
        `<div class="stat ${key}"><div class="stat-label"><span>${icon} ${label}</span><b>${key === "money" ? "₹" + fmt(s[key]) : s[key] + "%"}</b></div>${key === "money" ? '<div class="small-note">Canteen survival fund</div>' : `<meter min="0" max="100" value="${s[key]}" aria-label="${label}">${s[key]}%</meter>`}</div>`,
    )
    .join("")}</section>`;
}
async function newGame(button) {
  button.disabled = true;
  try {
    await api("game/new", {});
    location.href = "game.html";
  } catch (e) {
    toast(e.message);
    button.disabled = false;
  }
}
function metrics(s) {
  return `<section class="metric-grid"><article class="panel"><div class="metric-label">PERSONAL BEST</div><div class="metric">${fmt(s.bestScore)}</div><span class="muted">${escapeHtml(s.bestResult)}</span></article><article class="panel"><div class="metric-label">RUNS PLAYED</div><div class="metric">${s.gamesPlayed}</div><span class="muted">Practice makes probably perfect.</span></article><article class="panel"><div class="metric-label">BEST SURVIVAL</div><div class="metric">${s.highestSurvival} <span class="muted">situations</span></div><span class="muted">Personal leaderboard rank: #${s.rank}</span></article></section>`;
}
async function dashboard() {
  const d = await api("dashboard");
  let current = false;
  try {
    await api("game/current");
    current = true;
  } catch (e) {
    if (!e.message.startsWith("No active game")) throw e;
  }
  main.innerHTML =
    heading(
      "PLAYER HQ / YOUR NEXT BAD IDEA AWAITS",
      `Hey, ${escapeHtml(d.player.name.split(" ")[0])}.`,
      "Another day. Another chance to survive the semester.",
    ) +
    metrics(d.stats) +
    `<section class="run-card"><div><span class="tag">${current ? "YOUR RUN IS STILL ALIVE" : "TEN DECISIONS. SIX STATS. ONE YOU."}</span><h2>${current ? "Finish what you started." : "Ready for the campus chaos?"}</h2><p>${current ? "Your progress is saved in this session." : "Keep your health up, your stress down, and your attendance believable."}</p></div>${current ? '<a class="button primary" href="game.html">RESUME RUN ↗</a>' : '<button class="primary" id="start-game">START NEW GAME ↗</button>'}</section><div class="section-label"><h2>Recent survival attempts</h2><a class="text-link" href="history.html">All runs ↗</a></div>` +
    table(d.recent.slice(0, 5), "history");
  document
    .querySelector("#start-game")
    ?.addEventListener("click", (e) => newGame(e.currentTarget));
}
function gameRender(g) {
  activeGame = g;
  main.innerHTML =
    heading(
      "SURVIVAL RUN / KEEP IT TOGETHER",
      "Campus chaos.",
      `Your choices. Your consequences. <span class="lime">SCORE ${fmt(g.score)}</span>`,
      '<button class="ghost danger" id="quit-game">Quit run</button>',
    ) +
    stats(g.stats) +
    `<section class="game-progress"><div><span>SCENARIO ${g.scenarioNumber} / ${g.total}</span><span>${g.completed} SURVIVED</span></div><progress value="${g.completed}" max="${g.total}" aria-label="Run progress"></progress></section><section class="panel scenario-card"><span class="tag">${escapeHtml(g.scenario.category)}</span><h2>${escapeHtml(g.scenario.title)}</h2><p>${escapeHtml(g.scenario.description)}</p><div class="choices">${g.scenario.options.map((o, i) => `<button class="choice" data-choice="${o.id}"><span class="choice-key">${"ABCD"[i]}</span>${escapeHtml(o.text)}<span class="choice-arrow">↗</span></button>`).join("")}</div><div class="small-note">Choose wisely. Or make a really good story.</div></section>`;
  document
    .querySelectorAll("[data-choice]")
    .forEach((b) =>
      b.addEventListener("click", () => choose(b.dataset.choice)),
    );
  document.querySelector("#quit-game").addEventListener("click", quitPrompt);
}
async function choose(id) {
  document
    .querySelectorAll(".choice,#quit-game")
    .forEach((b) => (b.disabled = true));
  const before = activeGame;
  try {
    const g = await api("game/choose", {
      runId: before.runId,
      scenarioId: String(before.scenario.id),
      optionId: id,
    });
    activeGame = g;
    const deltas = Object.entries(g.stats)
      .filter(([k, v]) => v !== before.stats[k])
      .map(
        ([k, v]) =>
          `<span class="delta">${escapeHtml(k)} ${v - before.stats[k] > 0 ? "+" : ""}${v - before.stats[k]}</span>`,
      )
      .join("");
    document.querySelector(".scenario-card").outerHTML =
      `<section class="panel outcome-card"><div class="outcome-icon">✦</div><span class="tag">DECISION MADE. CONSEQUENCES DELIVERED.</span><h2>${escapeHtml(g.outcome)}</h2><div class="delta-list">${deltas}<span class="delta">Score +${fmt(g.score - before.score)}</span></div><button class="primary" id="continue-game">${g.status === "IN_PROGRESS" ? "NEXT SITUATION" : "SEE YOUR RESULT"} ↗</button></section>`;
    document.querySelector(".stats").outerHTML = stats(g.stats);
    document
      .querySelector("#continue-game")
      .addEventListener("click", () =>
        g.status === "IN_PROGRESS"
          ? gameRender(g)
          : location.assign("result.html"),
      );
    document.querySelector("#quit-game").disabled = g.status !== "IN_PROGRESS";
  } catch (e) {
    toast(e.message);
    document
      .querySelectorAll(".choice,#quit-game")
      .forEach((b) => (b.disabled = false));
  }
}
function quitPrompt() {
  const card = document.querySelector(".scenario-card,.outcome-card");
  card.innerHTML =
    '<span class="tag">TAKING AN EARLY EXIT?</span><h2>Quit this run?</h2><p>Your current attempt will be saved as a quit. It won’t enter the leaderboard.</p><div class="actions"><button class="danger" id="confirm-quit">Yes, save and quit</button><button class="secondary" id="cancel-quit">Keep surviving</button></div>';
  document
    .querySelector("#cancel-quit")
    .addEventListener("click", () => gameRender(activeGame));
  document
    .querySelector("#confirm-quit")
    .addEventListener("click", async (e) => {
      e.currentTarget.disabled = true;
      try {
        await api("game/quit", { runId: activeGame.runId });
        location.href = "result.html";
      } catch (err) {
        toast(err.message);
        gameRender(activeGame);
      }
    });
}
async function result() {
  const g = await api("game/result");
  main.innerHTML =
    `<section class="result-header"><div class="eyebrow">${g.status === "COMPLETED" ? "SEMESTER SURVIVED" : "THE SEMESTER FOUGHT BACK"} / ${g.completed} SITUATIONS FACED</div><div class="result-icon">${g.status === "COMPLETED" ? "🏆" : "☕"}</div><h1>${escapeHtml(g.resultTitle)}</h1><div class="result-score">${fmt(g.score)}</div><span class="badge">FINAL SCORE · SAVED TO YOUR HISTORY</span><p class="small-note">${escapeHtml(g.summary)}</p></section>` +
    stats(g.stats) +
    `<div class="actions result-actions"><button id="start-game" class="primary">PLAY AGAIN ↗</button><a class="button secondary" href="leaderboard.html">VIEW LEADERBOARD</a><a class="text-link" href="dashboard.html">Player HQ ↗</a></div>`;
  document
    .querySelector("#start-game")
    .addEventListener("click", (e) => newGame(e.currentTarget));
}
async function history() {
  const d = await api("history?page=" + historyPage);
  main.innerHTML =
    heading(
      "THE ARCHIVE / EVERY RUN HAS A STORY",
      "Your survival record.",
      "The good, the bad, and the academically confused.",
    ) +
    table(d.rows, "history") +
    `<div class="pager"><button id="prev" class="ghost" ${historyPage === 0 ? "disabled" : ""}>← Previous</button><span class="muted">Page ${historyPage + 1}</span><button id="next" class="ghost" ${d.rows.length < 20 ? "disabled" : ""}>Next →</button></div>`;
  document.querySelector("#prev").addEventListener("click", () => {
    historyPage--;
    history().catch(showError);
  });
  document.querySelector("#next").addEventListener("click", () => {
    historyPage++;
    history().catch(showError);
  });
}
async function leaderboard() {
  const rows = await api("leaderboard");
  main.innerHTML =
    heading(
      "HALL OF SURVIVORS / BRAGGING RIGHTS LIVE HERE",
      "Campus legends.",
      "Each player’s best run. Ranked by score. Powered by sheer luck.",
      '<a class="button primary" href="dashboard.html">TAKE YOUR SHOT ↗</a>',
    ) + table(rows, "leaderboard");
}
async function profile() {
  const d = await api("profile");
  main.innerHTML =
    heading(
      "PLAYER PROFILE / YOUR CAMPUS IDENTITY",
      "Meet the survivor.",
      "A degree in making it through.",
    ) +
    `<section class="panel profile-card"><div class="avatar">${escapeHtml(d.player.name[0].toUpperCase())}</div><div><h2>${escapeHtml(d.player.name)}</h2><div class="muted">@${escapeHtml(d.player.username)} · Joined ${escapeHtml(date(d.player.createdAt))}</div></div></section>` +
    metrics(d.stats);
}
function showError(e) {
  main.innerHTML = `<section class="panel empty"><span class="tag">CAMPUS DETOUR</span><h2>${escapeHtml(e.message)}</h2><div class="actions result-actions"><button class="primary" id="retry">TRY AGAIN</button><a class="button secondary" href="dashboard.html">PLAYER HQ</a></div></section>`;
  document
    .querySelector("#retry")
    .addEventListener("click", () => location.reload());
}
async function init() {
  const s = await api("session");
  csrf = s.csrf;
  document
    .querySelectorAll(".private")
    .forEach((el) => (el.hidden = !s.authenticated));
  document.querySelector(".nav-login").hidden = s.authenticated;
  document.querySelector(".logout").addEventListener("click", async (e) => {
    e.currentTarget.disabled = true;
    try {
      await api("logout", {});
      location.replace("index.html");
    } catch (err) {
      toast(err.message);
      e.currentTarget.disabled = false;
    }
  });
  if (page === "index" && s.authenticated) {
    document.querySelector(".hero .primary").href = "dashboard.html";
    document.querySelector(".hero .primary").firstChild.textContent =
      "BACK TO CAMPUS ";
  }
  const form = document.querySelector("form");
  if (form)
    form.addEventListener("submit", async (e) => {
      e.preventDefault();
      const b = form.querySelector("button");
      b.disabled = true;
      const error = form.querySelector(".form-error");
      error.textContent = "";
      const values = Object.fromEntries(new FormData(form));
      try {
        if (page === "register" && values.password !== values.confirmPassword)
          throw new Error("Passwords do not match.");
        await api(page, values);
        if (page === "register") {
          location.href = "login.html?registered=1";
        } else location.href = "dashboard.html";
      } catch (err) {
        error.textContent = err.message;
        b.disabled = false;
      }
    });
  if (
    page === "login" &&
    new URLSearchParams(location.search).has("registered")
  )
    toast("Account created. Log in to enter campus.");
  switch (page) {
    case "dashboard":
      await dashboard();
      break;
    case "game":
      gameRender(await api("game/current"));
      break;
    case "result":
      await result();
      break;
    case "history":
      await history();
      break;
    case "leaderboard":
      await leaderboard();
      break;
    case "profile":
      await profile();
      break;
  }
}
window.addEventListener("pageshow", (e) => {
  if (e.persisted) location.reload();
});
init().catch(showError);
