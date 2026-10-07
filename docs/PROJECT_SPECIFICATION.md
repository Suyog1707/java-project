# College Survival Simulator

A fun, full-stack Java Servlet mini-project built for a college practical using **Apache Tomcat, Java Servlets, HTML/CSS/JavaScript, JDBC, and MySQL**.

The application turns common college situations into an interactive decision-based survival game. A player registers, logs in, starts a run, faces random college scenarios, makes decisions, gains or loses stats, and receives a final survival result. Scores are stored so players can compete on a leaderboard.

The project should feel like a polished entertainment website rather than an academic CRUD application, while still demonstrating the Java web-development concepts required for the practical.

---

## 1. Project Goal

Build a browser-based game called **College Survival Simulator** where the player tries to survive a fictional college journey by making choices in funny situations.

Example:

> **Scenario:** Professor asks for yesterday's assignment.
>
> A. Blame the internet  
> B. Blame your teammate  
> C. Say the file is ready but your laptop is updating  
> D. Accept your fate

Every decision modifies one or more game stats. The run continues until the player completes the configured number of scenarios or reaches a game-over condition.

The result should be humorous, replayable, and easy to demonstrate to a class.

---

# 2. Product Scope

## Core Features

1. User registration
2. User login/logout
3. Session-based authentication
4. Player profile
5. Start a new game
6. Random college scenarios
7. Multiple choices for every scenario
8. Choice-dependent stat changes
9. Score calculation
10. Survival/progress state during a run
11. Game-over conditions
12. Final result screen
13. Save completed run to MySQL
14. Global leaderboard
15. Personal game history
16. Replay/new-game functionality
17. Responsive UI
18. End-to-end validation and testing

## Optional Features

Add these only after the core application is working:

- Daily challenge
- Achievement badges
- Funny player titles
- Difficulty modes
- Scenario categories
- Streaks
- Shareable result card
- Admin scenario management
- Theme switcher
- Animated scenario transitions

Do **not** let optional features delay the core game.

---

# 3. Technology Stack

## Backend

- Java
- Jakarta Servlet API
- Apache Tomcat 10.1.x
- Maven
- JDBC
- MySQL
- BCrypt password hashing

## Frontend

- HTML5
- CSS3
- Vanilla JavaScript
- Fetch API where useful
- No frontend framework required

## Architecture Style

Use a simple **MVC-style layered monolithic WAR application**.

Do not use Spring Boot, Spring MVC, Hibernate, or another large backend framework unless explicitly requested later. The purpose is to demonstrate Java Servlet development directly.

---

# 4. High-Level Architecture

```text
                    BROWSER
                       |
                       | HTTP / HTTPS
                       v
              +-------------------+
              |   Apache Tomcat   |
              |                   |
              |  Servlet Layer    |
              |  Controller Layer |
              +---------+---------+
                        |
                        v
              +-------------------+
              |    Service Layer  |
              |                   |
              | AuthService       |
              | GameService       |
              | ScoreService      |
              +---------+---------+
                        |
                        v
              +-------------------+
              |       DAO Layer   |
              |                   |
              | UserDAO           |
              | GameDAO           |
              | ScenarioDAO       |
              | ScoreDAO          |
              +---------+---------+
                        |
                        v
              +-------------------+
              |    JDBC Layer     |
              +---------+---------+
                        |
                        v
              +-------------------+
              |      MySQL DB     |
              +-------------------+
```

## Request Flow

```text
User action
   -> HTTP request
   -> Servlet
   -> Service
   -> DAO
   -> JDBC/MySQL
   -> Service result
   -> Servlet response
   -> HTML/JSON
   -> Browser UI update
```

---

# 5. Application Architecture

Use the following logical layers.

## Presentation Layer

Responsible for:

- Pages
- Forms
- Buttons
- Navigation
- Game cards
- Stats bars
- Result screens
- Client-side validation
- Rendering JSON responses when APIs are used

Location:

```text
src/main/webapp/
```

## Controller Layer

Java Servlets handle HTTP requests and responses.

Location:

```text
src/main/java/.../controller/
```

## Service Layer

Contains business logic and game rules.

Location:

```text
src/main/java/.../service/
```

## DAO Layer

Contains database access code.

Location:

```text
src/main/java/.../dao/
```

## Model Layer

POJOs/entities representing application data.

Location:

```text
src/main/java/.../model/
```

## Utility Layer

Contains shared utilities such as:

- Database connection management
- Password hashing
- Session helpers
- JSON utility if needed
- Input validation
- Configuration loading

Location:

```text
src/main/java/.../util/
```

---

# 6. Complete User Journey

## First Visit

```text
Landing Page
    |
    +---- Register
    |
    +---- Login
    |
    +---- Leaderboard
```

The landing page should explain the game in a fun way.

Example copy:

> **College Survival Simulator**  
> Survive lectures, assignments, surprise tests, attendance disasters, canteen emergencies, and group-project chaos.

Primary CTA:

> START SURVIVING

---

# 7. Registration Flow

## Registration Page

Fields:

- Full name
- Username
- Email
- Password
- Confirm password

## Validation

Client-side and server-side validation must be performed.

Rules:

- Name cannot be empty
- Username must be unique
- Email must be valid
- Password must satisfy minimum length
- Password and confirmation must match
- Trim unnecessary whitespace
- Reject malformed input

## Registration Request

```text
POST /register
```

Servlet responsibilities:

1. Read form values
2. Validate input
3. Check whether username/email already exists
4. Hash password using BCrypt
5. Insert user into MySQL
6. Redirect to login page
7. Show friendly success/error message

Never store plain-text passwords.

---

# 8. Login Flow

```text
Login Form
   |
   v
POST /login
   |
   v
LoginServlet
   |
   v
UserDAO
   |
   v
Verify BCrypt password
   |
   +---- failure -> error message
   |
   +---- success
          |
          v
      HttpSession
          |
          v
       /dashboard
```

Store only the minimum identity information in the session, for example:

```text
userId
username
```

Use the session cookie for authentication. Do not depend on localStorage for login state.

Logout:

```text
POST /logout
```

Invalidate the session and redirect to the login/landing page.

---

# 9. Dashboard Flow

After login, show:

- Player name
- Best score
- Games played
- Highest survival level
- Recent results
- START NEW GAME button
- LEADERBOARD button
- PROFILE button
- LOGOUT button

Example:

```text
-----------------------------------------------
        WELCOME BACK, SUYOG
-----------------------------------------------

Best Score       Games Played       Best Result
   8420               7              LEGEND

        [ START NEW GAME ]

        [ LEADERBOARD ]
```

---

# 10. Game Flow

## Start Game

```text
GET /game/new
```

The server creates a new game state for the authenticated player.

A game should contain:

- Current scenario index
- Score
- Health/stamina
- Stress
- Attendance
- Money
- Knowledge
- Friendship/social score
- Status
- Start time

A game is primarily stored in the session while it is active.

Completed games are persisted in MySQL.

---

# 11. Game State

Recommended initial state:

```text
Health       = 100
Stress       = 10
Attendance   = 80
Money        = 1000
Knowledge    = 40
Social       = 50
Score        = 0
Scenario     = 1
Status       = IN_PROGRESS
```

Keep these values configurable rather than hardcoding them throughout the code.

Clamp values to sensible ranges where appropriate.

Example:

```text
0 <= Health <= 100
0 <= Stress <= 100
0 <= Attendance <= 100
0 <= Knowledge <= 100
0 <= Social <= 100
```

Money can have its own range rules.

---

# 12. Scenario System

Every scenario should contain:

```text
id
category
title
description
optionA
optionB
optionC
optionD
outcome/rule metadata
```

A cleaner implementation is to store each choice separately in a dedicated table or represent the scenario and choice data as JSON-like objects in Java, depending on project complexity.

Recommended database design uses separate `scenarios` and `scenario_options` tables.

Example scenario:

```text
Category: Assignment
Title: The Missing Assignment

Professor says:
"Where is your assignment?"

A. Blame the Wi-Fi
B. Blame your teammate
C. Say your laptop is updating
D. Confess
```

Each choice has effects:

```text
A -> Stress +10, Score +100
B -> Social -10, Score +60
C -> Stress +15, Score +120
D -> Knowledge +5, Score +250
```

The actual game should display funny outcome text after the player chooses.

---

# 13. Scenario Categories

Create an initial pool of at least **25-40 scenarios**.

Suggested categories:

- Assignments
- Attendance
- Lectures
- Viva
- Exams
- Group Projects
- Canteen
- Friends
- Sleep
- Technology
- College Events
- Hostel/Travel
- Surprise Situations

Avoid using real student/professor names in built-in scenarios.

---

# 14. Example Scenarios

## Scenario 1: Surprise Viva

**Situation:**

> Professor suddenly asks you to explain a topic you studied 6 months ago.

Choices:

- Pretend to remember everything
- Ask a friend for help
- Give a confident but questionable explanation
- Honestly say you forgot

Possible effects:

```text
A: Stress +15, Score +100
B: Social -5, Score +50
C: Knowledge -5, Score +200
D: Stress -5, Knowledge +5, Score +150
```

## Scenario 2: Attendance Crisis

> Attendance is 74.8%. You need 75%.

Choices:

- Ask politely
- Blame the timetable
- Submit an extremely creative explanation
- Accept destiny

## Scenario 3: Group Project

> Three hours before submission, your teammate says: "Bro, did you make the project?"

Choices can change social, stress, score, and money.

## Scenario 4: Exam Tomorrow

> You have an exam tomorrow and have studied one chapter.

Possible outcomes should create funny but understandable stat changes.

---

# 15. Decision Processing

When a user selects an option:

```text
POST /game/choose
```

Request should include the selected option identifier.

The server must:

1. Verify authentication
2. Verify an active game exists
3. Verify the requested option belongs to the current scenario
4. Apply game effects
5. Update score
6. Increment scenario number
7. Determine whether the run continues
8. Return the outcome/result
9. Persist only when the game finishes

Do not trust score/stat values sent from the browser.

The browser sends a choice. The server calculates all effects.

---

# 16. Game-Over Conditions

At minimum, support these conditions:

### Normal Completion

Player completes the configured number of scenarios, e.g. 10.

### Stress Overload

```text
Stress >= 100
```

### Health Collapse

```text
Health <= 0
```

### Attendance Disaster

```text
Attendance <= 0
```

Other conditions may be added later.

The final result should depend on overall stats and score.

---

# 17. Result System

Create fun result tiers.

Example:

```text
9000+  -> LEGENDARY SURVIVOR
7000+  -> COLLEGE MASTER
5000+  -> SURVIVED SOMEHOW
3000+  -> RUNNING ON LUCK
1000+  -> ACADEMICALLY CONFUSED
<1000  -> ACADEMICALLY DECEASED
```

The result page should show:

- Final title
- Final score
- Stats
- Scenarios completed
- Funny summary
- PLAY AGAIN button
- VIEW LEADERBOARD button

Example:

```text
========================================
       🏆 LEGENDARY SURVIVOR
========================================

Score:       8,420
Attendance:  78%
Stress:      64%
Knowledge:   71%
Social:      88%
Money:       ₹420

"You survived college without
understanding how. Respect."

[ PLAY AGAIN ]  [ LEADERBOARD ]
```

---

# 18. Leaderboard

Route:

```text
GET /leaderboard
```

Show top players sorted by highest score.

Suggested columns:

```text
Rank | Player | Score | Result | Date
```

Do not expose private information such as email addresses.

Also provide the logged-in user's best score and rank when practical.

---

# 19. Game History

Route:

```text
GET /history
```

Show the authenticated user's previously completed games.

Columns:

```text
Date | Score | Result | Scenarios | Duration
```

Add pagination if history becomes large.

---

# 20. Profile

Route:

```text
GET /profile
```

Display:

- Name
- Username
- Games played
- Best score
- Best result
- Join date

Optional later feature:

- Update display name

Do not include password information in the UI.

---

# 21. Page Structure

Recommended pages:

```text
/
/index.html
/register.html
/login.html
/dashboard.html
/game.html
/result.html
/leaderboard.html
/history.html
/profile.html
/error.html
```

Static frontend pages can communicate with servlet endpoints using Fetch API, or traditional form submissions may be used where appropriate.

For a cleaner game UI, prefer Fetch API for gameplay requests and JSON responses.

---

# 22. Suggested URL/Servlet Map

Use a clear servlet mapping strategy.

```text
GET  /
GET  /login
POST /login
GET  /register
POST /register
POST /logout
GET  /dashboard
GET  /profile
GET  /history
GET  /leaderboard
POST /game/new
GET  /game/current
POST /game/choose
POST /game/quit
```

Alternative naming such as `/api/auth/login` is acceptable, but remain consistent.

---

# 23. Recommended Servlet Classes

```text
AuthLoginServlet
AuthRegisterServlet
LogoutServlet
DashboardServlet
ProfileServlet
HistoryServlet
LeaderboardServlet
NewGameServlet
CurrentGameServlet
ChooseOptionServlet
QuitGameServlet
```

Keep each Servlet thin. Do not put all business logic inside `doGet()` or `doPost()`.

---

# 24. Recommended Service Classes

```text
AuthService
GameService
LeaderboardService
ProfileService
HistoryService
```

Example responsibilities:

### AuthService

- Register user
- Authenticate user
- Validate credentials

### GameService

- Start game
- Get current scenario
- Process choice
- Apply effects
- Determine game over
- Calculate final score
- Produce final result

### LeaderboardService

- Fetch top scores
- Fetch user's best result
- Calculate rank if required

---

# 25. Recommended DAO Classes

```text
UserDAO
ScenarioDAO
GameDAO
GameResultDAO
```

Use `PreparedStatement` for SQL queries.

Never construct SQL using raw user input concatenation.

---

# 26. Suggested Java Package Structure

```text
src/main/java/com/collegechaos/
│
├── controller/
│   ├── AuthLoginServlet.java
│   ├── AuthRegisterServlet.java
│   ├── LogoutServlet.java
│   ├── DashboardServlet.java
│   ├── ProfileServlet.java
│   ├── HistoryServlet.java
│   ├── LeaderboardServlet.java
│   ├── NewGameServlet.java
│   ├── CurrentGameServlet.java
│   ├── ChooseOptionServlet.java
│   └── QuitGameServlet.java
│
├── service/
│   ├── AuthService.java
│   ├── GameService.java
│   ├── LeaderboardService.java
│   ├── ProfileService.java
│   └── HistoryService.java
│
├── dao/
│   ├── UserDAO.java
│   ├── ScenarioDAO.java
│   ├── GameDAO.java
│   └── GameResultDAO.java
│
├── model/
│   ├── User.java
│   ├── Scenario.java
│   ├── ScenarioOption.java
│   ├── GameState.java
│   ├── GameResult.java
│   └── GameStats.java
│
└── util/
    ├── DatabaseConnection.java
    ├── PasswordUtil.java
    ├── SessionUtil.java
    ├── ValidationUtil.java
    └── JsonUtil.java
```

---

# 27. Frontend Structure

```text
src/main/webapp/
│
├── index.html
├── login.html
├── register.html
├── dashboard.html
├── game.html
├── result.html
├── leaderboard.html
├── history.html
├── profile.html
├── error.html
│
├── css/
│   ├── global.css
│   ├── auth.css
│   ├── dashboard.css
│   ├── game.css
│   └── leaderboard.css
│
└── js/
    ├── auth.js
    ├── dashboard.js
    ├── game.js
    ├── leaderboard.js
    ├── history.js
    └── profile.js
```

Images/icons may be kept under:

```text
src/main/webapp/assets/
```

---

# 28. UI/UX Design Direction

The website should look like a modern game dashboard rather than a college management portal.

## Visual Style

Use:

- Dark or dark-purple background
- Bright accent colors
- Rounded cards
- Large typography
- Progress bars
- Emoji/icons where appropriate
- Subtle animations
- Clear CTA buttons
- Mobile responsive layout

Avoid making the design look childish. It should feel like a polished indie web game.

## Landing Page

Hero:

> **SURVIVE COLLEGE.**
>
> Every decision matters. Probably.

CTA:

> START THE CHAOS

Supporting cards:

```text
🎓 10+ College Situations
😂 Unpredictable Outcomes
🏆 Global Leaderboard
🔁 Unlimited Replays
```

---

# 29. Game Screen Design

Layout:

```text
--------------------------------------------------
 PLAYER: SUYOG                 SCORE: 4200
--------------------------------------------------

Health       █████████░ 90%
Stress       ██████░░░░ 60%
Attendance   ████████░░ 80%
Knowledge    █████░░░░░ 50%
Social       ███████░░░ 70%

--------------------------------------------------

         📚 SURPRISE ASSIGNMENT

Professor just announced that the assignment
is due RIGHT NOW.

[ BLAME THE WIFI ]
[ BLAME YOUR TEAMMATE ]
[ MAKE UP AN EXCUSE ]
[ ACCEPT YOUR FATE ]

--------------------------------------------------
Scenario 4 / 10
--------------------------------------------------
```

After selection, display an animated outcome card before loading the next scenario.

---

# 30. Database Design

Use MySQL.

Recommended database:

```text
college_survival
```

## users

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## scenarios

```sql
CREATE TABLE scenarios (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## scenario_options

```sql
CREATE TABLE scenario_options (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    option_text VARCHAR(255) NOT NULL,
    health_change INT DEFAULT 0,
    stress_change INT DEFAULT 0,
    attendance_change INT DEFAULT 0,
    money_change INT DEFAULT 0,
    knowledge_change INT DEFAULT 0,
    social_change INT DEFAULT 0,
    score_change INT DEFAULT 0,
    outcome_text TEXT NOT NULL,
    FOREIGN KEY (scenario_id) REFERENCES scenarios(id) ON DELETE CASCADE
);
```

## game_results

```sql
CREATE TABLE game_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    score INT NOT NULL,
    result_title VARCHAR(100) NOT NULL,
    health INT NOT NULL,
    stress INT NOT NULL,
    attendance INT NOT NULL,
    money INT NOT NULL,
    knowledge INT NOT NULL,
    social INT NOT NULL,
    scenarios_completed INT NOT NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

Indexes should be added for frequently queried leaderboard/history fields.

Example:

```sql
CREATE INDEX idx_game_results_score ON game_results(score DESC);
CREATE INDEX idx_game_results_user ON game_results(user_id);
```

---

# 31. Database Initialization

Provide:

```text
database/schema.sql
 database/seed.sql
```

`schema.sql` creates the database/tables.

`seed.sql` inserts the initial scenario set.

The project should be runnable by a new developer after importing these files into MySQL.

Include at least 25-40 seed scenarios.

---

# 32. Configuration

Do not hardcode database credentials in Java source files.

Support environment variables or an external properties file.

Example environment values:

```text
DB_URL=jdbc:mysql://localhost:3306/college_survival
DB_USERNAME=root
DB_PASSWORD=your_password
```

For local development, document the setup clearly.

Do not commit real secrets.

Provide:

```text
.env.example
```

or a documented local properties template.

---

# 33. Maven Setup

Use a Maven WAR project.

Expected structure:

```text
pom.xml
src/main/java/
src/main/webapp/
src/test/java/
database/
README.md
.env.example
```

Configure the Servlet API with `provided` scope because Tomcat supplies it at runtime.

Use the Jakarta Servlet namespace:

```java
import jakarta.servlet.*;
import jakarta.servlet.http.*;
```

Do not mix `javax.servlet.*` and `jakarta.servlet.*`.

---

# 34. WAR Deployment

Build:

```bash
mvn clean package
```

Expected artifact:

```text
target/college-survival.war
```

Copy the WAR into:

```text
<TOMCAT_HOME>/webapps/
```

Start Tomcat using its normal startup command for the operating system.

Then open:

```text
http://localhost:8080/college-survival/
```

Document Linux and Windows startup commands in the final project README after the implementation is verified.

---

# 35. Authentication and Security Requirements

For a college project, implement practical security basics.

## Passwords

- Never store plain-text passwords
- Hash using BCrypt
- Never return password hashes to frontend

## SQL Injection

Use `PreparedStatement` everywhere.

## Session Security

- Use `HttpSession`
- Check authentication on protected endpoints
- Invalidate session on logout
- Prevent access to dashboard/game/history/profile without login

## Input Validation

Validate on the server even when client-side validation exists.

## Authorization

Every user should only be able to view their own profile/history.

Leaderboard may be public.

## Error Handling

Do not expose Java stack traces, SQL errors, passwords, or database credentials to users.

Log technical errors server-side and show a friendly error page/message.

---

# 36. API Response Convention

For Fetch-based requests, prefer JSON.

Example success response:

```json
{
  "success": true,
  "message": "Choice processed",
  "data": {
    "scenarioNumber": 4,
    "score": 4200,
    "stats": {
      "health": 90,
      "stress": 60,
      "attendance": 80,
      "knowledge": 50,
      "social": 70,
      "money": 420
    },
    "outcome": "Your excuse somehow worked. Nobody knows why."
  }
}
```

Example error:

```json
{
  "success": false,
  "message": "Invalid game choice"
}
```

Keep response structures consistent.

---

# 37. Game Session Model

Use a Java object such as `GameState` inside the `HttpSession` for the active run.

Conceptually:

```text
HttpSession
   |
   +-- userId
   +-- username
   +-- currentGame -> GameState
```

`GameState` contains the current temporary run.

When the game finishes:

```text
GameState
   -> GameService calculates final result
   -> GameResultDAO saves to MySQL
   -> currentGame removed from session
```

This avoids writing to the database after every click while still saving completed games.

---

# 38. Random Scenario Selection

The initial version may randomly shuffle the active scenario list at game start and select the first N scenarios.

Important rules:

- Avoid duplicate scenarios in the same run where possible
- Never trust the scenario ID sent by the client without checking it against the active game state
- Server decides which scenario comes next

A simple approach:

```text
fetch active scenarios
shuffle server-side
select N
store selected IDs in GameState
```

---

# 39. Scoring System

The score should be more interesting than simply adding a fixed number.

Possible formula:

```text
base choice score
+ healthy-stat bonus
+ completion bonus
- severe stress penalty
```

Keep the formula understandable.

The browser must never calculate or submit the final score.

Server is authoritative.

---

# 40. Design System

Create reusable UI patterns:

### Buttons

- Primary
- Secondary
- Danger
- Ghost

### Cards

- Scenario card
- Stat card
- Leaderboard row
- Result card

### Feedback

- Success toast
- Error toast
- Loading state
- Empty state

### Responsive breakpoints

Design for:

- Desktop
- Tablet
- Mobile

The game should remain playable without horizontal scrolling.

---

# 41. Loading/Error/Empty States

Every network-based page must have clear states.

Examples:

```text
Loading...
Unable to load leaderboard.
No games played yet.
Session expired. Please log in again.
```

Do not leave a blank screen when an API fails.

---

# 42. End-to-End Testing Requirement

This is mandatory.

Codex must not consider the project complete merely because the code compiles.

The implementation must be tested **end-to-end from a clean setup through final game result and leaderboard verification**.

## Minimum E2E journey

```text
Open application
   ↓
Register a new account
   ↓
Verify registration success
   ↓
Login
   ↓
Verify dashboard
   ↓
Start game
   ↓
Load scenario
   ↓
Select option
   ↓
Verify outcome
   ↓
Verify stats changed correctly
   ↓
Continue through all scenarios
   ↓
Finish game
   ↓
Verify final score/result
   ↓
Verify game saved to DB
   ↓
Verify game appears in history
   ↓
Verify leaderboard contains score
   ↓
Logout
   ↓
Verify protected pages reject access
```

---

# 43. Testing Layers

## Unit Tests

Test pure business logic.

Minimum areas:

- Password validation
- Registration validation
- Score calculation
- Stat clamping
- Game-over conditions
- Result tier calculation
- Scenario selection logic

## DAO/Integration Tests

Where practical, verify:

- User creation
- User lookup
- Duplicate username/email handling
- Scenario retrieval
- Game-result insertion
- Leaderboard query
- History query

## Servlet/Controller Tests

Verify:

- Login success/failure
- Registration validation
- Protected endpoint behavior
- Logout
- Invalid game choice
- Missing active game

## Full E2E Tests

Use a browser automation framework only if practical in the environment. Playwright is preferred if Codex can install and execute it reliably.

If browser automation is not available, perform a scripted HTTP end-to-end suite plus manual browser verification, and document the limitation honestly.

---

# 44. Mandatory E2E Test Cases

### E2E-01: Registration

Given a new username/email:

- Submit valid registration
- Expect success
- Confirm user exists in DB

### E2E-02: Duplicate Registration

Attempt to reuse username/email.

Expected:

- Friendly error
- No duplicate DB row

### E2E-03: Login

Valid credentials must create a session and reach dashboard.

### E2E-04: Invalid Login

Invalid credentials must fail without creating an authenticated session.

### E2E-05: Protected Dashboard

Open dashboard while logged out.

Expected:

- Redirect/login response

### E2E-06: Start Game

Start a run.

Expected:

- Valid game state
- First scenario displayed
- Initial stats correct

### E2E-07: Choice Processing

Select a valid option.

Expected:

- Correct outcome
- Correct server-side stat updates
- Score update
- Next scenario loaded

### E2E-08: Invalid Choice

Send an option not belonging to the current scenario.

Expected:

- Rejected request
- Game remains valid

### E2E-09: Game Completion

Play until the required scenario count is completed.

Expected:

- Result page
- Correct final score/result
- DB record created

### E2E-10: History

After completing a run:

- Open history
- Verify completed game is present

### E2E-11: Leaderboard

After completing a high-scoring game:

- Verify score appears on leaderboard
- Verify ordering is correct

### E2E-12: Logout

Logout.

Expected:

- Session invalidated
- Dashboard/game/history/profile no longer accessible

### E2E-13: Refresh During Game

Refresh the game page during an active run.

Expected:

- Active game remains consistent through the session
- Current scenario does not unexpectedly reset

### E2E-14: Browser Back Navigation

Try navigating back into protected screens after logout.

Expected:

- Authentication check still applies

### E2E-15: Session Expiry/Invalid Session

Simulate an invalid or expired session.

Expected:

- Friendly authentication error/redirect
- No server crash

---

# 45. E2E Test Data

Create a predictable test account for local automated testing only, or have the test suite generate a unique account on every run.

Do not hardcode production passwords.

Seed deterministic scenarios for tests where needed so expected score/stat changes can be asserted.

Example test scenario:

```text
Scenario: TEST_SCENARIO_001
Option A: score +100, stress +10
```

The production random scenario pool and deterministic test data should be clearly separated.

---

# 46. Test Assertions

Testing must verify both UI behavior and server/database behavior.

Examples:

```text
UI says score = 4200
AND
DB result score = 4200
```

```text
UI says result = LEGENDARY SURVIVOR
AND
DB result_title = LEGENDARY SURVIVOR
```

Do not rely only on screenshots or visual inspection.

---

# 47. Manual Demo Checklist

Before presenting to the professor:

```text
[ ] Tomcat starts without errors
[ ] MySQL starts
[ ] Database exists
[ ] Seed scenarios exist
[ ] Registration works
[ ] Login works
[ ] Dashboard works
[ ] Game starts
[ ] Scenario renders correctly
[ ] Choice changes stats
[ ] Multiple scenarios work
[ ] Game completion works
[ ] Result screen works
[ ] History works
[ ] Leaderboard works
[ ] Logout works
[ ] Protected routes are protected
[ ] Mobile layout is acceptable
[ ] No major console errors
[ ] No major server log errors
```

---

# 48. Logging

Add useful server-side logging for development.

Log examples:

```text
User registered: username=...
User login success: userId=...
Game started: userId=...
Game completed: userId=..., score=...
```

Do not log passwords, database credentials, session IDs, or sensitive information.

---

# 49. Error Handling Strategy

Use centralized or consistent error handling where practical.

HTTP examples:

```text
400 -> Invalid request
401 -> Not authenticated
403 -> Not authorized
404 -> Resource not found
500 -> Internal server error
```

Frontend should convert these into understandable messages.

Backend must not expose implementation details to the browser.

---

# 50. Development Milestones

## Milestone 1 — Project Bootstrap

- Maven WAR project
- Tomcat configuration
- Servlet API
- Base frontend
- Database connection
- Basic project structure

Deliverable:

Application successfully loads from Tomcat.

## Milestone 2 — Authentication

- Registration
- BCrypt hashing
- Login
- Session
- Logout
- Protected pages

Deliverable:

A user can register, login, access dashboard, and logout.

## Milestone 3 — Database and Scenarios

- Scenario tables
- Option tables
- Seed data
- Scenario DAO

Deliverable:

Application can retrieve real scenarios from MySQL.

## Milestone 4 — Game Engine

- GameState
- Start game
- Scenario selection
- Choice processing
- Stat updates
- Score calculation
- Game-over rules

Deliverable:

A complete playable game run works.

## Milestone 5 — Results and Social Features

- Save result
- History
- Leaderboard
- Profile statistics

Deliverable:

Users can see and compare completed runs.

## Milestone 6 — UI Polish

- Responsive design
- Animations
- Toasts
- Loading states
- Error states
- Accessibility improvements

Deliverable:

Polished, presentation-ready UI.

## Milestone 7 — Testing

- Unit tests
- Integration tests
- E2E tests
- Bug fixes
- Clean-build verification

Deliverable:

All mandatory test cases pass.

## Milestone 8 — Documentation

Prepare:

- README
- Setup guide
- Architecture diagram
- Database diagram
- API documentation
- Test report
- Screenshots
- Project report material

---

# 51. Suggested Git Workflow

Use branches such as:

```text
main
  |
  +-- feature/auth
  +-- feature/game-engine
  +-- feature/database
  +-- feature/leaderboard
  +-- feature/frontend
  +-- feature/testing
```

Keep commits meaningful.

Examples:

```text
feat: add registration servlet
feat: implement session authentication
feat: add scenario database seed
feat: implement game state engine
feat: add leaderboard
 test: add game service tests
 test: add end-to-end authentication flow
```

---

# 52. Definition of Done

The project is complete only when all of the following are true:

1. Maven build succeeds from a clean checkout.
2. WAR deploys successfully to Apache Tomcat.
3. MySQL schema imports successfully.
4. Seed scenarios load successfully.
5. A user can register.
6. A user can login.
7. Authentication uses server-side session state.
8. A user can start a game.
9. Scenarios are selected server-side.
10. Choices change server-side game state.
11. Invalid choices are rejected.
12. A full game can be completed.
13. Final results are persisted.
14. History displays completed games.
15. Leaderboard works.
16. Logout invalidates the session.
17. Protected routes are blocked when unauthenticated.
18. Passwords are hashed.
19. SQL queries use prepared statements.
20. Mandatory automated/integration/E2E tests pass.
21. The application has no blocking console/server errors.
22. The UI is responsive enough for the demo.
23. README setup instructions have been verified from scratch.

---

# 53. Codex Implementation Instructions

Codex should treat this README as the **source-of-truth implementation specification**.

## Important Instructions to Codex

### 1. Build, do not only explain

Create the complete working application in the repository.

Do not stop at pseudocode, a plan, or partial files.

### 2. Preserve the requested stack

Use:

```text
Java + Jakarta Servlets + Apache Tomcat + Maven + JDBC + MySQL + HTML/CSS/JavaScript
```

Do not replace the stack with Spring Boot or another framework.

### 3. Keep the architecture clean

Servlets should handle HTTP concerns.

Services should handle game/business logic.

DAOs should handle persistence.

Models should represent data.

Utilities should contain shared infrastructure code.

### 4. Make the app runnable

After implementation, verify:

```bash
mvn clean test
mvn clean package
```

Then deploy the WAR to Tomcat and verify the application actually starts.

### 5. Test end-to-end

Do not mark the task complete until the full user journey has been tested.

At minimum verify:

```text
Register -> Login -> Dashboard -> Start Game
-> Play all scenarios -> Result -> DB Save
-> History -> Leaderboard -> Logout
```

### 6. Test failure paths

Test invalid login, duplicate registration, invalid game choice, unauthenticated access, malformed input, and server/database error handling.

### 7. Fix problems found during testing

When a test fails:

1. Diagnose the root cause.
2. Fix the implementation.
3. Re-run the failed test.
4. Re-run the relevant full test suite.
5. Continue until the required suite passes or a genuine environment limitation is documented.

### 8. Verify from clean setup

Where practical, remove generated build artifacts and verify that the project can be rebuilt from source.

### 9. Do not fake test results

Only report tests as passed when they were actually executed.

If a browser E2E framework is unavailable, state exactly what was tested and what could not be tested.

### 10. Finish with developer documentation

Update this README with actual:

- Prerequisites
- Setup commands
- Database setup
- Tomcat deployment instructions
- Test commands
- Known limitations
- Final project structure

The final README should match the implementation rather than merely repeating the original specification.

---

# 54. Suggested Testing Command Set

The final project should provide a straightforward command sequence similar to:

```bash
mvn clean test
mvn clean package
```

Then, after starting MySQL and Tomcat:

```text
Open http://localhost:8080/college-survival/
```

If Playwright or another browser test system is added, document its exact commands in the final README.

---

# 55. Suggested Deliverables

Final repository should contain:

```text
college-survival/
│
├── pom.xml
├── README.md
├── .gitignore
├── .env.example
│
├── database/
│   ├── schema.sql
│   └── seed.sql
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── webapp/
│   └── test/
│       └── java/
│
└── target/
    └── college-survival.war
```

Do not commit generated `target/` contents to Git unless specifically required by the college submission process.

---

# 56. Future Enhancements

After the base version is stable, possible additions include:

## Achievement System

Examples:

- Assignment Dodger
- Attendance Survivor
- Viva Legend
- Last-Minute King
- Canteen Royalty

## Difficulty Modes

```text
Easy
Normal
Chaos
Nightmare
```

## Daily Challenge

Everyone gets the same scenario sequence for the day.

## Shareable Results

Generate a card such as:

```text
I survived college.
Score: 8420
Title: LEGENDARY SURVIVOR
```

## Admin Scenario Management

An admin can create, update, disable, and delete scenarios through a protected admin interface.

These are optional and should not be implemented until the core system is stable.

---

# 57. Final Product Vision

The final application should feel like a small, polished browser game that happens to be implemented using Java Servlets.

The user should be able to go from:

```text
Visitor
  ↓
Register
  ↓
Login
  ↓
Dashboard
  ↓
Start Survival Run
  ↓
Make funny decisions
  ↓
Watch stats change
  ↓
Survive or fail
  ↓
Receive a funny final title
  ↓
Save score
  ↓
Check leaderboard
  ↓
Play again
```

The most important principle is:

> **Make it fun for the player, simple enough to explain in a Java practical, and structured enough to demonstrate real full-stack engineering.**

---

# 58. Report Topics This Project Can Support

The implementation should make it easy to write the college practical/project report with these sections:

1. Title
2. Abstract
3. Problem Statement
4. Objectives
5. Scope
6. Technologies Used
7. System Requirements
8. System Architecture
9. User Flow
10. Database Design
11. ER Diagram
12. Module Description
13. Servlet Architecture
14. Game Logic
15. UI Design
16. Security Considerations
17. Testing Strategy
18. Test Cases
19. Screenshots
20. Results
21. Limitations
22. Future Scope
23. Conclusion

---

## Project Name

**College Survival Simulator**

## Suggested Formal Title

**College Survival Simulator: A Java Servlet Based Interactive Decision-Making Web Game**
