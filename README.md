# GuessTheBest — Quiz Game

A "Who Wants to Be a Millionaire"–style quiz game ("Legyen Ön is milliomos" in
Hungarian): every question has four answers, exactly one of which is correct. The
questions are in **Hungarian**.

- **Backend:** Kotlin + [Ktor](https://ktor.io/), backed by SQLite (plain JDBC).
- **Frontend:** [Vue 3](https://vuejs.org/) + [Vite](https://vitejs.dev/), served by nginx in Docker.

The fastest way to run it is [with Docker](#running-with-docker-recommended).

---

## Structure

```
guessthebest/
├── backend/                 # Ktor REST API + SQLite
│   ├── src/main/kotlin/io/adroit/guessthebest/
│   │   ├── Application.kt        # entry point, routes, plugins
│   │   ├── Database.kt           # SQLite schema + seed loading
│   │   ├── QuestionRepository.kt # queries
│   │   └── Models.kt             # API DTOs
│   └── src/main/resources/
│       ├── questions.json        # seed set loaded at startup (generated, see tools/)
│       └── logback.xml
├── frontend/                # Vue 3 + Vite client
│   └── src/
│       ├── App.vue               # game logic and state
│       ├── api.js                # backend calls
│       ├── style.css             # styling
│       └── components/           # StartScreen / QuestionView / ResultScreen
│   ├── Dockerfile           # multi-stage build (JDK build -> JRE runtime)
│   └── ...
├── frontend/
│   ├── Dockerfile           # multi-stage build (Vite build -> nginx)
│   ├── nginx.conf           # serves the SPA + reverse-proxies /api -> backend
│   └── ...
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
| `GET`  | `/api/questions?count=10` | Random questions. Does **not** include the correct answer. |
| `GET`  | `/api/questions/categories` | List of available categories. |
| `POST` | `/api/questions/{id}/answer` | Check an answer. Body: `{ "selectedIndex": 0-3 }`. Response: `{ "correct": true/false, "correctIndex": 0-3 }`. |

> The correct answer is never sent to the client alongside the questions — validation
> always happens server-side via the `POST .../answer` endpoint.

### Example

```bash
curl "http://localhost:8080/api/questions?count=1"
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

Currently a simple quiz mode: 10 random questions, with instant feedback per answer
(correct/incorrect highlighting) and a final score at the end.

**Planned extension:** the full "millionaire" ladder (15 questions, prize tiers) and
lifelines (50:50, ask the audience, phone a friend). The data model (category +
difficulty) is already prepared for this.
