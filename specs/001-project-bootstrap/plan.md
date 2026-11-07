# Implementation Plan: Project Bootstrap (Maven backend + Vite frontend)

**Branch**: `001-project-bootstrap` | **Date**: 2026-11-07 | **Spec**: [specs/001-project-bootstrap/spec.md](spec.md)

**Input**: Feature specification from `/specs/001-project-bootstrap/spec.md`

## Summary

Bootstrap the CornucopAI repository so both halves of the project build with one
command each: a Maven/Quarkus backend under `backend/` (Java 25, LangChain4j) and a
Vite/Vue 3 frontend under `frontend/` (Vuetify), plus CI running the same lint/test/
build commands and a smoke endpoint/page proving the pipeline end-to-end. No game
logic yet — scaffolding only.

## Technical Context

**Language/Version**: Java 25 (LTS) for the backend; TypeScript on Node.js 26 LTS
for the frontend toolchain

**Primary Dependencies**: Backend — Quarkus 3.40.x LTS (latest release: emergency
3.40.1.1; BOM `io.quarkus.platform:quarkus-bom`),
Maven 3.9.x with wrapper, `io.quarkiverse.langchain4j:quarkus-langchain4j` (LangChain4j
Quarkus extension), `quarkus-rest` + `quarkus-rest-jackson`, `quarkus-smallrye-health`.
Frontend — Vite 8 + `@vitejs/plugin-vue`, Vue 3, Vuetify 4, TypeScript.

**Storage**: N/A at bootstrap (no persistence yet; in-memory only)

**Testing**: Backend — JUnit 5 via `quarkus-junit5` + AssertJ. Frontend — Vitest +
`@vue/test-utils`. Lint/format — ESLint (flat config) + Prettier on the frontend;
backend relies on Maven build + tests.

**Target Platform**: Linux server for the backend; any modern evergreen browser
for the frontend

**Project Type**: web application (backend REST API + frontend SPA)

**Performance Goals**: backend build (compile+test+package) under 5 minutes cold
(SC-001); frontend dev server serving a page under 30 seconds (SC-002); frontend
production build under 2 minutes (SC-003); Quarkus application startup under 2 seconds

**Constraints**: one command per side to build/run (`./mvnw verify` in `backend/`,
`npm run dev` / `npm run build` in `frontend/`); CI is GitHub Actions
(`.github/workflows/`) and MUST run the same commands as local; the Quarkus app
MUST always stay launchable in debug mode (JDWP port 5005 via `quarkus:dev`); wrong
or missing tool versions MUST fail with a clear message

**Scale/Scope**: bootstrap scaffolding only — build tooling, one smoke endpoint
(`GET /api/health`), one smoke page; no Cornucopia game logic, no database

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Gates derived from `.specify/memory/constitution.md` v1.3.0:

| Gate | Source | Status |
|------|--------|--------|
| Backend in `backend/`, frontend in `frontend/` at repo root | Technology & Quality Standards | PASS |
| Backend = Quarkus + Java 25 + LangChain4j | Technology & Quality Standards | PASS |
| Frontend = Vue 3 + Vuetify | Technology & Quality Standards | PASS |
| Test-first: tests written before implementation (enforced in tasks.md) | Principle II | PASS |
| CI on GitHub Actions, same commands as local | Development Workflow | PASS |
| Quarkus app always launchable in debug mode (JDWP 5005) | Development Workflow | PASS |
| Required tool versions documented | FR-007 / Development Workflow | PASS |
| Simplicity: no extra modules, no premature abstractions | Principle V | PASS |

No violations → Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/001-project-bootstrap/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
backend/                          # Maven / Quarkus (Java 25)
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/wrapper/ # Maven wrapper (no system Maven required)
└── src/
    ├── main/java/com/cornucopai/
    │   └── api/HealthResource.java        # smoke endpoint GET /api/health
    ├── main/resources/application.properties
    └── test/java/com/cornucopai/          # JUnit 5 smoke tests

frontend/                         # Vite + Vue 3 + Vuetify
├── package.json
├── vite.config.ts
├── tsconfig.json
├── eslint.config.js
├── index.html
└── src/
    ├── main.ts
    ├── App.vue                            # smoke page
    └── ...

.github/workflows/ci.yml          # lint + test + build for both sides
README.md                         # prerequisites + commands
```

**Structure Decision**: Web application layout (Option 2) — `backend/` and
`frontend/` at the repository root, as mandated by constitution v1.3.0. The
backend is a single Maven module (simplicity principle: no multi-module build at
bootstrap). The frontend is a single Vite SPA.

## Complexity Tracking

No constitution violations — nothing to justify.
