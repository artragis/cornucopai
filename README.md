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

No system Maven required: `backend/mvnw` downloads the pinned Maven 3.9.x.

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
quickstart.md, contracts/). Current feature: `specs/001-project-bootstrap/`.
The project constitution is at `.specify/memory/constitution.md` (v1.3.0).
