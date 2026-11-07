# Quickstart / Validation Guide: Project Bootstrap

Runnable scenarios that prove the bootstrap works end-to-end. Prerequisites on a
fresh machine: JDK 25, Node.js 26 LTS (24 LTS minimum), git. No system Maven
needed (wrapper included).

## 1. Backend build (SC-001, US-1)

```sh
cd backend
./mvnw verify
```

Expected: compilation, tests, and packaging succeed; a runnable fast-jar exists
under `backend/target/quarkus-app/`. Measure wall time — must be under 5 minutes
excluding dependency download.

## 2. Backend smoke endpoint (FR-005)

```sh
cd backend
./mvnw quarkus:dev &
# then, in another shell:
curl -s http://localhost:8080/api/health
```

Expected: HTTP 200 with `{"status":"UP"}` (see `contracts/health-api.yaml`).

## 2b. Backend debug mode (constitution v1.3.0)

```sh
cd backend
MAVEN_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" ./mvnw quarkus:dev
```

Expected: the app starts with a JDWP agent listening on port 5005; an IDE
debugger can attach to `localhost:5005` and hit breakpoints. This ability MUST
never be removed by any build or configuration change.

## 3. Frontend dev server (SC-002, US-2)

```sh
cd frontend
npm ci
npm run dev
```

Expected: a local URL is printed; opening it serves the smoke page in under
30 seconds from command start; editing `src/App.vue` hot-reloads without
restarting.

## 4. Frontend production build (SC-003, US-3)

```sh
cd frontend
npm run build
```

Expected: `frontend/dist/` is emitted with optimized static assets in under
2 minutes.

## 5. Lint and tests (frontend) / tests (backend)

```sh
cd frontend && npm run lint && npm test
cd backend && ./mvnw test
```

Expected: all green. A deliberately broken file or failing test must make the
command exit non-zero with a clear message (edge case: clear failure, no partial
success).

## 6. CI parity (SC-004, US-4)

Push to the feature branch and watch `.github/workflows/ci.yml`.

Expected: the pipeline runs the same commands as above for both `backend/` and
`frontend/` and passes with no manual intervention.
