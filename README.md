# College Survival Simulator

A complete Java Servlet browser game: survive ten randomly selected college situations by balancing health, stress, attendance, money, knowledge and social life. Thirty scenarios and 120 original choices are included.

Built with Java 21, Jakarta Servlet 6, Apache Tomcat 10.1, Maven, JDBC, MySQL, BCrypt, HTML, CSS and vanilla JavaScript. The original supplied requirements are preserved in [docs/PROJECT_SPECIFICATION.md](docs/PROJECT_SPECIFICATION.md).

## Run on this PC

Java development tools and Maven are installed **inside this project**, under `.tools/`; the original system Java runtime is unchanged. MySQL and Tomcat use the existing Docker installation. Local credentials were generated into the ignored, private `.env` file.

```bash
cd "/home/spidyboy_1707/Documents/ChatGPT/java projrct"
./scripts/start.sh
```

Open **http://localhost:20007/college-survival/** and create your own account. No default player password is provided. `start.sh` starts MySQL, tests and builds the WAR, deploys it into Tomcat, and waits for the application to respond. Both services continue running after the terminal closes.

```bash
./scripts/stop.sh                  # Stop both services; retain database data
./mvnw clean test                  # Unit/controller tests + DB tests if .env exists
./mvnw clean package               # Creates target/college-survival.war
```

`mvnw` is a small project-local Maven launcher, not the Apache Maven Wrapper distribution. To use the normal Maven commands:

```bash
source scripts/env.sh
mvn clean test
mvn clean package
```

## Fresh Linux setup

Supported automatic setup: Linux x86_64 with Bash, curl, tar, Python 3, and Docker Engine with Compose. Docker must be running and your user must be permitted to access it. Docker and Node.js already existed on the development PC. Only browser tests need Node.js 20+ and Chrome.

```bash
./scripts/setup.sh
./scripts/start.sh
```

`setup.sh` reuses an available Java compiler and Maven, otherwise downloads Temurin JDK 21 and Maven 3.9.11 into `.tools/`. It also prepares Tomcat 10.1.60 for optional native deployment. It generates unique database passwords only when `.env` does not already exist. The first Maven build and Docker image pulls require internet access. Services bind to loopback: website `20007`, MySQL `20008`.

## Database and configuration

Docker Compose creates `college_survival`, an application database user, and a persistent named volume. On the first database initialization it imports `database/schema.sql` then `database/seed.sql`. `SET NAMES utf8mb4` preserves punctuation and Unicode scenario text. Initialization is repeatable; seeding does not remove players or scores.

Configuration lives in `.env`. See `.env.example` for all settings. Quote the JDBC URL in this shell-compatible file because it contains ampersands. Keep credentials out of Git. Compose uses `db:3306` for Tomcat's internal JDBC connection; native Tomcat and tests use the host URL in `.env`.

Inspect the seeded pool without exposing passwords:

```bash
docker exec college-survival-mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" -N college_survival -e "SELECT COUNT(*) FROM scenarios; SELECT COUNT(*) FROM scenario_options;"'
```

For an existing standalone MySQL installation, use a dedicated database account, import both SQL files, and set the host connection in `.env`:

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p college_survival < database/seed.sql
```

On an existing Docker database, changes to SQL files are **not** automatically reimported. Apply the seed safely without deleting data:

```bash
docker exec -i college-survival-mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql --default-character-set=utf8mb4 -u "$MYSQL_USER" college_survival' < database/seed.sql
```

## Native Tomcat deployment

The WAR also works in ordinary Tomcat 10.1 without Docker. Export `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` before starting Tomcat. The application explicitly loads Connector/J from the WAR. For native deployment on this PC, stop the container web service first:

```bash
docker compose stop web
./scripts/start-local-tomcat.sh
```

Native Tomcat uses port 8080 by default; the Docker website uses port 20007. The native script builds, copies the WAR to `.tools/tomcat/webapps`, and runs Tomcat in the foreground. Use Ctrl+C to stop it. With a different installation:

```bash
source scripts/env.sh
mvn clean package
cp target/college-survival.war "$CATALINA_HOME/webapps/"
"$CATALINA_HOME/bin/catalina.sh" run
```

On Windows, install JDK 21+, Maven, MySQL 8+ and Tomcat 10.1, import the SQL files using MySQL, and use Command Prompt:

```bat
set JAVA_HOME=C:\path\to\jdk-21
set PATH=%JAVA_HOME%\bin;C:\path\to\maven\bin;%PATH%
set DB_URL=jdbc:mysql://localhost:3306/college_survival?serverTimezone=UTC
set DB_USERNAME=college_app
set DB_PASSWORD=your-local-password
mvn clean test
mvn clean package
copy target\college-survival.war C:\path\to\tomcat\webapps\
C:\path\to\tomcat\bin\catalina.bat run
```

Windows commands are provided as deployment guidance; they were not executed on this Linux PC. MySQL application-user creation is handled by Compose in the automatic setup; create that user and grant rights to `college_survival` when using standalone MySQL.

## Play and rules

Register → log in → dashboard → start a run → choose and read each outcome → result → history/leaderboard → play again or log out. Refreshing an active run retains its state in the session. A new run cannot overwrite an unfinished run; resume or quit it first.

Initial stats: health 100, stress 10, attendance 80, money ₹1000, knowledge 40, social 50. The server shuffles active scenarios without repetition. Stats are clamped to 0–100; money to 0–100000. Stress 100, health 0 or attendance 0 ends the run early. Otherwise ten choices complete the run. Quit attempts are saved in history and excluded from the leaderboard.

Final score:

```text
sum of choice scores
+ 5 × (health + attendance + knowledge + social)
+ 1500 for normal completion
− 10 × stress
```

The minimum score is zero. Tiers: 9000+ Legendary Survivor, 7000+ College Master, 5000+ Survived Somehow, 3000+ Running on Luck, 1000+ Academically Confused, below 1000 Academically Deceased. The public leaderboard shows each player's best eligible run, with date-based tie ordering. Personal history is paginated, twenty rows per page.

## Tests

Start the application before the end-to-end suites:

```bash
./mvnw clean test
python3 scripts/http-e2e.py
npm ci
npm run test:e2e
python3 scripts/verify-schema.py
```

The HTTP suite covers the fifteen mandatory cases, malformed inputs, CSRF, stale choices, private histories, quit and early game-over. It checks exact score, title and stats against MySQL. It needs Docker access to inspect the project database. It assumes default game settings.

The Playwright suite uses the existing Chrome executable at `/opt/google/chrome/chrome`; override `CHROME_PATH` on another PC. It tests registration, failed and successful login, dashboard, refresh, ten choices, outcome screens, result, exact database score/title, history, leaderboard, profile, logout and back navigation. It checks desktop/mobile overflow and JavaScript exceptions. Screenshots are saved in `docs/screenshots/`. Both end-to-end suites generate unique accounts and remove their own database rows afterward.

`verify-schema.py` imports the schema and seed twice into a unique temporary database, checks counts/UTF-8, and removes that temporary database. `database-outage-test.py` intentionally stops only the project database, verifies a friendly 503, and restores it; run that check when nobody is playing.

Override `APP_URL` to test a different local deployment. Real DAO tests execute when `DB_PASSWORD` is configured; otherwise JUnit clearly reports them as skipped. See [docs/TEST_REPORT.md](docs/TEST_REPORT.md) for actual executed results.

## Structure

```text
pom.xml / mvnw                   Maven WAR build and local launcher
compose.yaml                     Persistent MySQL + Tomcat services
scripts/                         Setup, start/stop, HTTP/browser tests
.env.example                     Configuration template
.mvn/                            Project-local Maven dependency cache settings
database/schema.sql              Tables, foreign keys, indexes
database/seed.sql                30 scenarios / 120 options
src/main/java/com/collegechaos/
  controller/                    API servlet and authentication/security filter
  service/                       Authentication, game engine, scoring, profiles
  dao/                           Prepared-statement MySQL access
  model/                         Users, scenarios, options, game/session state
  util/                          Configuration, connection, validation, BCrypt
src/main/webapp/                  Ten pages, shared CSS and vanilla JS
src/test/java/                   Business, controller, filter and real DAO tests
docs/                            Specification, architecture, API, tests, screenshots
target/college-survival.war       Generated deployment artifact (ignored)
.tools/                          Local JDK, Maven, Tomcat and Maven cache (ignored)
```

## Security and operational limits

Passwords use BCrypt; hashes and email addresses are omitted from public profile/leaderboard responses. Database queries use prepared statements. Login rotates the session, logout invalidates it, and protected pages/APIs check server session identity. Cookies are HttpOnly and SameSite=Lax. Mutations require a session CSRF token; submitted stats and scores are ignored. Choices carry a run/scenario identifier to reject stale requests. Session locking serializes choices, and a unique run ID makes result insertion idempotent. A database failure leaves the original game state untouched so the player can retry.

This is a local college practical application. Active runs last only for the current 30-minute idle session and do not survive a Tomcat restart. Completed results persist in MySQL. Native Tomcat uses a simple connection per DAO operation; there is no connection pool. Public hosting would need HTTPS with Secure cookies, deployment-specific secrets, backups, and authentication rate limiting. Daily challenges, achievements, admin management and difficulty modes are optional future work. Node.js is used only for browser tests/formatting; it is not an application server.

Troubleshooting: `docker compose logs web db`; native Tomcat logs are under `.tools/tomcat/logs/`. If port 20007/20008 is occupied, change the Compose binding and test URL/JDBC host URL together. Do not delete the database volume to resolve a startup problem.
