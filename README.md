# CornucopAI

Playground where AI agents play games of Cornucopia, the card game used for
threat modeling.

- **Backend** (`backend/`): Quarkus 3.40.x (LTS, 3.40.1.1) on Java 25, LangChain4j
  via the Quarkiverse `quarkus-langchain4j` extension, built with Maven (wrapper
  included — no system Maven needed).
- **Frontend** (`frontend/`): Vue 3 + Vuetify 4, built with Vite 8, on Node.js
  (26 LTS recommended, 24 LTS minimum).

## Prerequisites

- JDK 25 (LTS)
- Node.js 26 LTS (24 LTS accepted as minimum)
- git
- A local clone of the [Cornucopia](https://cornucopiawiki.org/) card repository
  (the `cornucopia` git repo), pointed to by the `CORNUCOPIA_HOME` environment variable

No system Maven required: `backend/mvnw` downloads the pinned Maven 3.9.x.

### Cornucopia card repository

The backend reads real Cornucopia cards from `CORNUCOPIA_HOME/source/*-cards-*.yaml`
(see `specs/003-card-set-selection/`). Set it before starting the backend:

```sh
export CORNUCOPIA_HOME=/path/to/cornucopia
```

Without it, the card catalog is empty and game creation returns `400`.

## Commands

### Backend

```sh
cd backend
./mvnw verify        # compile + test + package (fast-jar in target/quarkus-app/)
./mvnw quarkus:dev   # dev mode with live reload on http://localhost:8080
```

Smoke endpoint: `GET http://localhost:8080/api/health` → `{"status":"UP"}`
(see `specs/001-project-bootstrap/contracts/health-api.yaml`).

### Debug mode (always available — constitution v1.3.0)

```sh
cd backend
MAVEN_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" ./mvnw quarkus:dev
# then attach your IDE debugger to localhost:5005
```

### Java code style (Google Java Style — enforced)

The backend follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html),
enforced at repo level by two Maven plugins bound to the `verify` phase (so they also run in CI):

- **Checkstyle** (`maven-checkstyle-plugin` + Checkstyle 14.4.0) lints `src/main/java` and
  `src/test/java` against the ruleset in `backend/config/checkstyle/checkstyle.xml`
  (vendored `google_checks.xml`) with project suppressions in
  `backend/config/checkstyle/checkstyle-suppressions.xml`.
- **Spotless** (`spotless-maven-plugin` + google-java-format) formats the same sources.

```sh
cd backend
./mvnw spotless:apply   # reformat all Java sources to Google Java Style (idempotent)
./mvnw verify           # fails if any Checkstyle violation or unformatted file remains
```

### Frontend

```sh
cd frontend
npm ci               # install pinned dependencies (package-lock.json)
npm run dev          # dev server with HMR
npm run build        # production build → frontend/dist/
npm test             # Vitest unit tests
npm run lint         # ESLint
```

## CI

GitHub Actions (`.github/workflows/ci.yml`) runs the same commands as local
(`./mvnw verify` for the backend; `npm ci && npm run lint && npm test && npm run build`
for the frontend) on every push and pull request.

## Spec-Kit workflow

Feature artifacts live under `specs/` (spec.md, plan.md, research.md, tasks.md,
quickstart.md, contracts/). Current feature: `specs/003-card-set-selection/`.
The project constitution is at `.specify/memory/constitution.md` (v1.3.0).

## About

### Why is there some french here?

I started this project as a test of "spec-kit flow" and a toy project for my own knowledge of langchain4j & acp sdk.
I am french and when I prompted my code harness I did it in french. Hence the mix of french and english.

### Why the history talks about "billet" or "diary"

As it was my first real project with spec-kit and mainly vibe coding, I took note and wrote a blog article that I posted in zeste de savoir websize. 
On this website blog articles are called "billet" or "tribune libre". The article was written by hand but I wanted to see what article would have been written if I told AI to do so. That's what the history shows. Once the experiment is over, I removed every elements of it.

### Why java?

Because I want something I know and that was not python. I didn't have time to both learn a new language and experiment vibe coding.
So I only had go, c#, php and java left. A dice decided. Java it was.

### Why vuejs and vuetify?

Sorry I did not take notes for why I chose this one. Perhaps it was just my mood at the time, after all vuejs is the last framework I used in the company I work for and for zestedesavoir we don't use framework. Perhaps I secretly hoped it would magically create a useable UI. It didn't.

### Next steps?

- reduce call to models to reduce cost. Today on a real project (zestedesavoir.com), with two suits, 4 players 
  - mistral large 2 times
  - deepseek flash once
  - google/gemma-4-26b-a4b
  the full run cost me 4€ of API credit. I will explore more way to do this.
- Calling code harnesses (first will be github-copilot, junie-cli and mistral-vibe as first citizen as I personaly use them) through ACP
- Once the cornucopia game is over, summarize everything in one document/set of task
- Start a game as command line?
- Anything else that comes to me after that.
- Anything else the community (if it exists?) proposes.
- A real UI?
