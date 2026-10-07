# Executed test report

Verified on **8 October 2026**, Fedora Linux x86_64, Temurin JDK 21.0.12.1, Maven 3.9.11, Tomcat 10.1.60, MySQL 8.4, and the installed Chrome browser through Playwright 1.55.1.

## Build and Java tests

`mvn clean test` and `mvn clean package` were executed during development. The final clean package/start run executed **40 tests, zero failures, zero errors, zero skipped**:

| Suite | Tests | Executed coverage |
|---|---:|---|
| AuthServiceTest | 9 | Valid/invalid registration, BCrypt verification/byte limit, normalized input, duplicate account handling, valid/invalid/unknown login, private fields |
| GameServiceTest | 20 | Initial stats, clamping, money range, unique selection, insufficient pool, effects, stale/missing/invalid choices, scoring/tier boundaries, all game-over conditions, quit, hidden rules, persistence failure preserving the original state |
| ControllerTest | 10 | Protected API/page behavior, logout, CSRF, malformed JSON, invalid pagination, unknown route, method rejection, session endpoint, authenticated filter passage |
| DatabaseIntegrationTest | 1 | Real MySQL account creation/lookups/duplicates, scenario retrieval, full ten-choice game, result insertion/idempotency, history, statistics, leaderboard, pagination |

`target/college-survival.war` was created and deployed to ordinary Apache Tomcat 10.1, then the same version in a persistent Docker service. The final `scripts/start.sh` clean build, deployment and readiness check completed successfully.

## Mandatory HTTP/database journey

All fifteen required cases passed on the final deployment:

| Case | Result | Verification |
|---|---|---|
| E2E-01 Registration | PASS | New account exists in MySQL; stored password is BCrypt |
| E2E-02 Duplicate registration | PASS | Duplicate username/email rejected; no extra row |
| E2E-03 Login | PASS | Session and CSRF rotate; dashboard accessible |
| E2E-04 Invalid login | PASS | Wrong password rejected; session remains anonymous |
| E2E-05 Protected dashboard | PASS | API 401 and HTML login redirect |
| E2E-06 Start game | PASS | Exact initial stats, ten scenarios configured |
| E2E-07 Choice processing | PASS | Effects, score, progress and outcome checked against seeded rules; browser-submitted stats ignored |
| E2E-08 Invalid choice | PASS | Invalid option rejected; state unchanged |
| E2E-09 Completion | PASS | Ten distinct scenarios; exact final formula and MySQL score/title/all stats; one result row |
| E2E-10 History | PASS | Own completed run found; pagination checked |
| E2E-11 Leaderboard | PASS | Player's best result found; ordering and privacy checked |
| E2E-12 Logout | PASS | Session invalidated; protected APIs inaccessible |
| E2E-13 Refresh | PASS | Current game state unchanged |
| E2E-14 Back navigation | PASS | Protected pages still redirect after logout; no-store response policy |
| E2E-15 Invalid/expired session | PASS | Invalidated session cookie and anonymous session rejected safely |

Session expiry is represented by an invalidated server session and stale cookie; the suite does not wait thirty minutes for the idle timeout. Additional HTTP checks passed for missing runs, preventing active-run replacement, replayed/stale choices, malformed JSON/array/oversized input, missing CSRF, private histories, quit persistence and a real early game-over.

Recorded output: [test-output/http-e2e.txt](test-output/http-e2e.txt).

## Browser and layout verification

The real Chrome/Playwright suite passed: landing, protected-page redirect, registration, invalid-login feedback, successful login, dashboard, refresh preserving the current scenario, ten choice/outcome transitions, result, history, leaderboard, profile, logout and browser back protection. The visible result score/title were compared to MySQL, including the completed-scenario count. No JavaScript exceptions or HTTP 5xx responses occurred during this successful browser journey.

Desktop viewport: **1440 × 1080**. Mobile viewport: **390 × 844**. Landing, gameplay and result were checked for horizontal overflow. Desktop/mobile landing, gameplay and result screenshots were captured and visually inspected. Additional dashboard/history/leaderboard/profile captures are available in [screenshots/](screenshots/).

Recorded output: [test-output/browser-e2e.txt](test-output/browser-e2e.txt).

## Fresh setup and failure recovery

`verify-schema.py` imported both SQL files into a fresh temporary MySQL database, confirmed thirty scenarios and 120 choices, verified Unicode punctuation, repeated the imports without duplication, then removed only the temporary database.

`database-outage-test.py` temporarily stopped the project MySQL service. The leaderboard returned a friendly HTTP 503 without exposing exception details or credentials. The script restarted MySQL, waited for health and query readiness, and confirmed the API returned HTTP 200 again. Recorded output: [test-output/database-outage.txt](test-output/database-outage.txt). The expected induced database exception remains in Tomcat's development logs as evidence of this negative test.

The final application was left running with healthy MySQL and Tomcat services. Test accounts/results were removed by the test suites. No demo credentials or fake player scores remain in the application database.

## Limits

Windows deployment guidance was not executed. Tests target the default ten-scenario configuration and modern Chrome; other browser engines, screen readers, production load and HTTPS hosting were not verified. No optional features were claimed. Active sessions/runs are intentionally reset by Tomcat restart; completed data is persistent.
