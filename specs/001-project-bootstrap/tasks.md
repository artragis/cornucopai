---

description: "Task list for feature 001-project-bootstrap: Maven/Quarkus backend + Vite/Vue frontend bootstrap"
---

# Tasks: Project Bootstrap (Maven backend + Vite frontend)

**Input**: Design documents from `/specs/001-project-bootstrap/`

**Prerequisites**: plan.md (required), spec.md (user stories US1–US4), research.md,
data-model.md, contracts/health-api.yaml, quickstart.md, constitution v1.3.0

**Tests**: INCLUDED — constitution v1.3.0 Principle II (Test-First, NON-NEGOTIABLE)
mandates TDD: each test task MUST be written and observed FAILING before its
implementation task runs.

**Organization**: grouped by user story (US1–US4 from spec.md); each story is an
independently testable increment.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: user story label (US1–US4); Setup/Foundational/Polish have no label
- Every task names its exact file path

## Path Conventions

- Web app (constitution v1.3.0): `backend/` and `frontend/` at repository root;
  sources in `backend/src/`, `frontend/src/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: repository skeleton and hygiene common to both sides

- [x] T001 Create root directories `backend/`, `frontend/`, `.github/workflows/` per plan.md Project Structure
- [x] T002 Update root `.gitignore` with `backend/target/`, `*.class`, `frontend/node_modules/`, `frontend/dist/`, `*.log`, `.env*`, `.DS_Store`
- [x] T003 [P] Write root `README.md`: prerequisites (JDK 25, Node.js 26 LTS, git; Maven via wrapper — no system Maven), commands (`backend/`: `./mvnw verify`, `./mvnw quarkus:dev`; `frontend/`: `npm ci`, `npm run dev`, `npm run build`), and debug mode (JDWP port 5005, constitution v1.3.0)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: build-tooling skeletons that MUST exist before any user story work

**CRITICAL**: no user story work can begin until this phase is complete

- [x] T004 [P] Create `backend/pom.xml`: Quarkus 3.40.x LTS BOM (latest 3.40.1.1, `io.quarkus.platform:quarkus-bom`), `maven.compiler.release=25`, deps `quarkus-rest`, `quarkus-rest-jackson`, `quarkus-smallrye-health`, `io.quarkiverse.langchain4j:quarkus-langchain4j`; test deps `quarkus-junit5` + AssertJ
- [x] T005 [P] Add Maven wrapper to `backend/` (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`) pinned to Maven 3.9.x; `chmod +x backend/mvnw`
- [x] T006 [P] Create `backend/src/main/resources/application.properties` (`quarkus.http.port=8080`; comment documenting JDWP debug on 5005)
- [x] T007 [P] Create `frontend/package.json`: vue 3, vuetify 4, vite 8, `@vitejs/plugin-vue`, typescript, vitest, `@vue/test-utils`, eslint, prettier; `engines.node >=24` (26 LTS recommended); scripts `dev`/`build`/`test`/`lint`
- [x] T008 [P] Create `frontend/vite.config.ts`, `frontend/tsconfig.json`, `frontend/index.html`, `frontend/eslint.config.js`, `frontend/.prettierrc`
- [x] T009 Verify foundation: `cd backend && ./mvnw -q compile` succeeds and `cd frontend && npm ci` succeeds

**Checkpoint**: foundation ready — user story work can begin

---

## Phase 3: User Story 1 — Backend build + smoke endpoint (Priority: P1) MVP

**Goal**: `./mvnw verify` in `backend/` compiles, tests, packages a runnable fast-jar;
`GET /api/health` returns 200 `{"status":"UP"}`

**Independent Test**: quickstart.md scenarios 1, 2 and 2b pass on a fresh clone

### Tests for User Story 1 (write FIRST, must FAIL)

- [x] T010 [US1] Contract test for `GET /api/health` per `specs/001-project-bootstrap/contracts/health-api.yaml` in `backend/src/test/java/com/cornucopai/api/HealthResourceTest.java` (asserts 200 + `{"status":"UP"}`); run `./mvnw test` — MUST FAIL (endpoint not implemented)

### Implementation for User Story 1

- [x] T011 [US1] Implement smoke endpoint `GET /api/health` returning `{"status":"UP"}` in `backend/src/main/java/com/cornucopai/api/HealthResource.java`
- [x] T012 [US1] Run `cd backend && ./mvnw verify` — green; fast-jar present in `backend/target/quarkus-app/`; wall time under 5 min excluding downloads (SC-001)
- [x] T013 [US1] Validate runtime: `./mvnw quarkus:dev` + `curl localhost:8080/api/health` → 200 `{"status":"UP"}` (quickstart §2); then validate debug mode `MAVEN_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" ./mvnw quarkus:dev` and attach a debugger to `localhost:5005` (quickstart §2b, constitution v1.3.0)

**Checkpoint**: US1 fully functional and testable independently

---

## Phase 4: User Story 2 — Frontend dev server (Priority: P1)

**Goal**: `npm run dev` in `frontend/` serves the smoke page locally with HMR

**Independent Test**: quickstart.md scenario 3 passes (page served in under 30 s, hot reload on edit)

### Tests for User Story 2 (write FIRST, must FAIL)

- [x] T014 [US2] Smoke test in `frontend/src/App.spec.ts` (Vitest + `@vue/test-utils`): mounts `App.vue` and renders without crashing; run `npm test` — MUST FAIL (no `App.vue` yet)

### Implementation for User Story 2

- [x] T015 [P] [US2] Create `frontend/src/main.ts` (`createApp`, registers Vuetify plugin)
- [x] T016 [US2] Create `frontend/src/App.vue` smoke page using a Vuetify component (e.g. `v-card`)
- [x] T017 [US2] Validate `npm run dev` serves the page on a local URL in under 30 s (SC-002) and that editing `frontend/src/App.vue` hot-reloads without restart (quickstart §3)

**Checkpoint**: US2 works independently of US1

---

## Phase 5: User Story 3 — Frontend production build (Priority: P2)

**Goal**: `npm run build` emits optimized static assets to `frontend/dist/`

**Independent Test**: quickstart.md scenario 4 passes (build under 2 min, `dist/` emitted)

### Implementation for User Story 3

- [x] T018 [US3] Confirm `build` script (`vite build`) in `frontend/package.json` outputs to `frontend/dist/`; add a post-build check that `frontend/dist/index.html` exists
- [x] T019 [US3] Run `cd frontend && npm run build` — succeeds in under 2 min and emits `frontend/dist/` (SC-003)

**Checkpoint**: US3 works independently

---

## Phase 6: User Story 4 — CI parity (Priority: P2)

**Goal**: CI runs the same lint/test/build commands as local, for both sides

**Independent Test**: quickstart.md scenario 6 — push the branch and the pipeline passes with no manual intervention

### Implementation for User Story 4

- [x] T020 [US4] Create `.github/workflows/ci.yml`: job `backend` runs `./mvnw verify`; job `frontend` runs `npm ci && npm run lint && npm test && npm run build`; triggers on push/PR; same commands as local (GitHub Actions, constitution v1.3.0)
- [x] T021 [US4] Run locally the exact CI commands for both sides — all green; push and confirm the GitHub Actions workflow passes (SC-004)

**Checkpoint**: US4 works; local and CI gates are identical

---

## Phase 7: Polish & Cross-Cutting Concerns

- [x] T022 [P] Run the full `specs/001-project-bootstrap/quickstart.md` validation (scenarios 1–6 + 2b) end-to-end on a clean clone
- [x] T023 [P] Start the Zeste de Savoir diary (constitution v1.3.0, Project Diary): create `billet/manifest.json` (`type=OPINION`) and `billet/introduction.md` with the first passage capturing the bootstrap decisions (Quarkus 3.40.1.1, Vite 8, GitHub Actions, debug mode)
- [x] T024 Update `specs/001-project-bootstrap/spec.md` **Status** field to reflect implementation progress

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies — start immediately
- **Foundational (Phase 2)**: depends on Phase 1 — BLOCKS all user stories
- **User Stories (Phases 3–6)**: depend on Phase 2; proceed in priority order (US1 → US2 → US3 → US4) or in parallel if staffed; each is independently testable
- **Polish (Phase 7)**: depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1)**: after Phase 2; no dependency on other stories
- **US2 (P1)**: after Phase 2; independent of US1
- **US3 (P2)**: after Phase 2; depends only on the frontend skeleton (T007–T008), not on US2's page
- **US4 (P2)**: after Phase 2 and after US1/US2/US3 commands exist locally to mirror in CI

### Within Each User Story

- Test task MUST be written and observed FAILING before the implementation task (TDD, constitution Principle II)
- Implementation before runtime validation
- Story checkpoint before moving to the next story

### Parallel Opportunities

- Phase 1: T003 can run in parallel with T001–T002 (different files)
- Phase 2: T004–T008 are all [P] (different files); T009 after T004–T008
- Phase 4: T015 can run in parallel with T014's failure check setup (different files)
- Phase 7: T022, T023 are [P]

---

## Parallel Example: Phase 2 (Foundational)

```text
Task T004: backend/pom.xml
Task T005: backend/mvnw + .mvn/wrapper/
Task T006: backend/src/main/resources/application.properties
Task T007: frontend/package.json
Task T008: frontend/vite.config.ts, tsconfig.json, index.html, eslint.config.js, .prettierrc
→ all five can run together; then T009 verifies the foundation
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Complete Phase 1 (Setup)
2. Complete Phase 2 (Foundational) — CRITICAL
3. Complete Phase 3 (US1: backend build + smoke endpoint)
4. STOP and VALIDATE: quickstart §1, §2, §2b pass independently

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. US1 → validate → MVP (backend builds and serves `/api/health`)
3. US2 → validate → frontend dev server works
4. US3 → validate → production build works
5. US4 → validate → CI parity
6. Polish

---

## Notes

- [P] tasks touch different files and have no dependency on incomplete tasks
- [Story] labels map tasks to spec.md user stories for traceability
- Verify tests FAIL before implementing (constitution v1.3.0 Principle II)
- Commit after each task or logical group
- Stop at each checkpoint to validate the story independently
- Constitution v1.3.0 constraints apply throughout: `backend/`+`frontend/` layout, Quarkus 3.40.x/Java 25/LangChain4j, Vue 3/Vuetify/Vite 8, GitHub Actions CI, Quarkus debug mode (JDWP 5005) always available
