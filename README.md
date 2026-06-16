# GuessTheBest — Quiz Game

A Hungarian quiz **board game** for 1–10 players sharing one device. A 5×5 grid of
cards each shows a topic and difficulty; pick one, confirm, and answer the revealed
question (four options, one correct). All questions and answers are in **Hungarian**.

- **Backend:** Kotlin + [Ktor](https://ktor.io/), backed by SQLite (plain JDBC). Stateless —
  it serves the question pool; the browser holds the game state.
- **Frontend:** [Vue 3](https://vuejs.org/) + [Vite](https://vitejs.dev/), served by nginx in Docker.

The fastest way to run it is [with Docker](#running-with-docker-recommended).

See [Gameplay](#gameplay) for the full rules.

---

## Structure

```
guessthebest/
├── backend/                 # Ktor REST API + SQLite (stateless)
│   ├── src/main/kotlin/io/adroit/guessthebest/
│   │   ├── Application.kt        # entry point + routes
│   │   ├── Database.kt           # SQLite schema + seed loading
│   │   ├── QuestionRepository.kt # queries (meta, draw, answer check)
│   │   └── Models.kt             # API DTOs
│   ├── src/main/resources/
│   │   ├── questions.json        # seed set loaded at startup (generated, see tools/)
│   │   └── logback.xml
│   └── Dockerfile           # multi-stage build (JDK build -> JRE runtime)
├── frontend/                # Vue 3 + Vite client
│   ├── src/
│   │   ├── App.vue               # game orchestration + state
│   │   ├── api.js                # backend calls
│   │   ├── game.js               # rules: points, board build, standings
│   │   ├── style.css             # styling
│   │   └── components/           # PlayerSetup, QuizGrid, ConfirmModal,
│   │                             #   QuestionModal, Leaderboard, RankingBar, ResultScreen
│   ├── nginx.conf           # serves the SPA + reverse-proxies /api -> backend
│   └── Dockerfile           # multi-stage build (Vite build -> nginx)
├── tools/
│   ├── questions-source.json     # hand-authored Hungarian questions (the source)
│   └── build-questions.mjs       # validates + shuffles -> questions.json
├── docker-compose.yml       # backend + frontend services
└── README.md
```

On first launch, the questions are loaded from `questions.json` into a SQLite file
named `guessthebest.db` (created inside the `backend/` folder). If the table already
contains questions, seeding is skipped — so the "init once" behavior is automatic.

---

## Running with Docker (recommended)

Requires **Docker** with the Compose plugin. From the repo root:

```bash
docker compose up --build
```

Then open **http://localhost:8080**.

- The **frontend** (nginx) is published on port `8080` and serves the app.
- It reverse-proxies `/api/*` to the **backend** container, so everything runs from a
  single origin (no CORS, no separate ports to juggle).
- The backend seeds its SQLite database on first start; the database lives in a named
  volume (`quizdata`), so it survives restarts.
- Compose waits for the backend's healthcheck before starting the frontend.

Useful commands:

```bash
docker compose up --build -d     # run in the background
docker compose logs -f backend   # follow backend logs
docker compose down              # stop (keeps the quizdata volume)
docker compose down -v           # stop and delete the seeded database
```

> If port `8080` is already taken (e.g. by a local dev backend), stop that first or
> change the published port in `docker-compose.yml` (`"8080:80"` -> `"<host>:80"`).

---

## Running locally (without Docker)

### Prerequisites

- **JDK 17+** (the build uses the Java 17 toolchain)
- **Node.js 18+** and **npm**

The Gradle wrapper (`gradlew`) downloads the correct Gradle version automatically — a
separate Gradle installation is not required.

Run the backend and the frontend in two separate terminals.

### 1) Backend (Ktor API — `http://localhost:8080`)

```powershell
cd backend
./gradlew run
```

The first run downloads dependencies and seeds the questions. On a successful start
you'll see `Seeded 294 questions into SQLite.` in the log (only the first time).

### 2) Frontend (Vite dev server — `http://localhost:5173`)

```powershell
cd frontend
npm install   # first time only
npm run dev
```

Then open **http://localhost:5173** in your browser.

Vite proxies `/api` calls to the backend automatically (see `frontend/vite.config.js`),
so no extra CORS configuration is needed during development.

---

## API endpoints

| Method | Path | Description |
| ------ | ---- | ----------- |
| `GET`  | `/api/health` | Health check (`{"status":"ok"}`). |
| `GET`  | `/api/meta` | Topic+difficulty combos (and category/difficulty lists) used to build the board. |
| `GET`  | `/api/question?category=&difficulty=&exclude=1,2,3` | Draw one question for a cell, skipping used ids. Does **not** include the correct answer. Falls back to difficulty-only (then any) if a pool is exhausted. |
| `POST` | `/api/questions/{id}/answer` | Check an answer. Body: `{ "selectedIndex": 0-3 }`. Response: `{ "correct": true/false, "correctIndex": 0-3 }`. |
| `GET`  | `/api/questions/categories` | List of available categories. |

> The correct answer is never sent to the client alongside the questions — validation
> always happens server-side via the `POST .../answer` endpoint.

### Example

```bash
curl "http://localhost:8080/api/meta"
curl "http://localhost:8080/api/question?category=Sport&difficulty=könnyű"
curl -X POST http://localhost:8080/api/questions/1/answer \
  -H "Content-Type: application/json" \
  -d '{"selectedIndex":1}'
```

---

## Questions data

The questions are **hand-authored** and genuinely Hungary-focused (Hungarian history,
geography, literature, culture, science & inventors, sport, and general knowledge).

The source of truth is `tools/questions-source.json`. In that file every question
lists the **correct answer first** (`correctIndex: 0`) — that's the only convention
you need to follow when adding questions:

```json
{
  "category": "Magyarország földrajza",
  "difficulty": "könnyű",
  "text": "Mi Magyarország fővárosa?",
  "answers": ["Budapest", "Debrecen", "Szeged", "Pécs"],
  "correctIndex": 0
}
```

- `category` / `difficulty` (`könnyű`, `közepes`, `nehéz`) / `text` are required.
- `answers`: exactly **4** items, the correct one first.
- `correctIndex`: keep it `0` in the source — the build step shuffles for you.

### Building `questions.json`

```powershell
# from the repo root
node tools/build-questions.mjs
```

This validates every entry (4 distinct answers, no duplicate questions, valid fields),
deterministically shuffles each question's answers so the correct one lands in a varied
position, recomputes `correctIndex`, and writes
`backend/src/main/resources/questions.json`. It prints a per-category / per-difficulty
breakdown and fails loudly if any question is malformed.

Seeding only runs when the table is empty. To reload questions after rebuilding,
delete `backend/guessthebest.db` and restart the backend.

---

## Gameplay

A hot-seat board game for **1–10 players** on one device.

1. **Setup** — add players and name them.
2. **The board** — a 5×5 grid (25 cards). Each card shows only a **topic** and a
   **difficulty** (and its point value). The difficulty is colour-coded on the card's
   top edge: green = könnyű, amber = közepes, red = nehéz.
3. **A turn** — the current player picks any open card. A **confirmation** appears with
   the topic, difficulty and points; on accept, the question is revealed (four options).
4. **Scoring** — a correct answer awards points by difficulty (**könnyű 1, közepes 2,
   nehéz 3**) and **checks** that card (it's claimed by the player, shown with their name).
5. **Wrong answer** — the correct option is shown, then the card is **re-randomized**
   with a brand-new topic & difficulty and stays open for someone to try later.
6. **Turns rotate** after every question, regardless of the result.
7. **Standings** — a ranking strip sits on top; a leaderboard beside the grid shows
   each player's points, whose turn it is, and how many cards remain.
8. **End** — after all **25 cards** are answered correctly, the final leaderboard is
   shown with the winner, plus options to replay (same players) or start with new ones.

The browser owns all game state (players, scores, turns, the board, and which question
ids have been used); the backend just serves questions and verifies answers.
