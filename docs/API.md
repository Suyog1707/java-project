# API reference

Base: `/college-survival/api`. JSON responses use `{ "success": true, "message": "…", "data": … }` or `{ "success": false, "message": "…" }`.

First GET `/session`, retain its `data.csrf`, and send it as `X-CSRF-Token` on every POST, along with `Content-Type: application/json` and the session cookie. Successful login rotates both session and token; use the new `data.csrf`. Browser Fetch requests include cookies automatically with `credentials: same-origin`.

| Method | Route | Access | Request / response data |
|---|---|---|---|
| GET | `/session` | Public | `csrf`, `authenticated` |
| POST | `/register` | Public + CSRF | `name`, `username`, `email`, `password`, `confirmPassword` |
| POST | `/login` | Public + CSRF | `username`, `password`; returns public player and new CSRF token |
| POST | `/logout` | Auth + CSRF | `{}`; invalidates session |
| GET | `/dashboard` | Auth | Public player, aggregate stats, recent results |
| GET | `/profile` | Auth | Same own-player summary |
| GET | `/history?page=0` | Auth | Twenty own results and page number |
| GET | `/leaderboard` | Public | Top fifty personal-best non-quit results |
| POST | `/game/new` | Auth + CSRF | `{}`; creates a run, rejects unfinished existing run |
| GET | `/game/current` | Auth | Active run and current public scenario |
| POST | `/game/choose` | Auth + CSRF | String fields `runId`, `scenarioId`, `optionId`; updated run/outcome |
| POST | `/game/quit` | Auth + CSRF | String `runId`; saves quit result |
| GET | `/game/result` | Auth | Most recently finished run in this session |

Scenario responses expose only option identifiers and text. Effects/outcomes stay server-side until a choice is processed. Game responses include `runId`, `score`, `stats`, `completed`, `total`, `scenarioNumber`, `status`, `outcome`, `resultTitle`, `summary`, `scenario`. Completed runs return `scenario: null`.

Errors: 400 invalid input or stale gameplay, 401 authentication required, 403 invalid CSRF token, 404 unknown endpoint, 405 unsupported method, 503 database unavailable, 500 unexpected failure. SQL exceptions and stack traces appear only in server logs. Usernames are case-insensitive in MySQL; registration stores a lowercase username/email. Passwords are preserved exactly and must have at least eight characters with a maximum of 72 UTF-8 bytes for BCrypt.

HTML routes include `/`, `/index.html`, `/login.html`, `/register.html`, `/dashboard.html`, `/game.html`, `/result.html`, `/leaderboard.html`, `/history.html`, `/profile.html`, `/error.html`. Extensionless page aliases are also available. Gameplay APIs use the consistent `/api` prefix; `/game/new` itself is not a state-changing GET route.
