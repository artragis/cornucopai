# Phase 0 Research: Project Bootstrap

All decisions below resolve the unknowns from the Technical Context. Sources:
public release pages / release notes consulted on 2026-11-07.

## Backend toolchain

- **Decision**: Quarkus 3.40.x LTS line (latest release: emergency 3.40.1.1) via
  `io.quarkus.platform:quarkus-bom`.
  - **Rationale**: 3.40.x is the current LTS stream as of 2026-11; full Java 25
    support landed in Quarkus 3.31, so every 3.40.x release supports Java 25
    officially. LTS stream = long maintenance, matching Java 25 LTS.
  - **Alternatives considered**: latest non-LTS (above 3.40.x) — rejected,
    shorter support window; older LTS 3.33.x — rejected, superseded by 3.40.x.
- **Decision**: Java 25 (LTS, released Sept 2025), compiled with
  `maven.compiler.release=25`.
  - **Rationale**: mandated by constitution v1.2.0 and the feature description.
- **Decision**: Maven 3.9.x via the Maven wrapper (`./mvnw`), so no system Maven
  install is required; the wrapper pins the exact Maven version.
  - **Alternatives considered**: system Maven — rejected, version drift across
    machines (SC-005 requires a clear failure on wrong tooling; the wrapper
    removes the variable).
- **Decision**: LangChain4j through the Quarkiverse Quarkus extension
  (`io.quarkiverse.langchain4j:quarkus-langchain4j-*`), latest stable 1.x, rather
  than raw `dev.langchain4j` artifacts.
  - **Rationale**: gives CDI injection of chat models/agents and build-time
    config binding inside Quarkus; raw langchain4j would need manual wiring.
  - **Alternatives considered**: plain `dev.langchain4j:langchain4j` — works
    but loses Quarkus integration; rejected for the agent-playground use case.
- **Decision**: REST via `quarkus-rest` + `quarkus-rest-jackson`; health via
  `quarkus-smallrye-health`; tests via `quarkus-junit5` (JUnit 5) + AssertJ.
  - **Rationale**: Quarkus standard stack; JUnit 5 keeps the familiar TDD loop.
- **Decision**: smoke endpoint `GET /api/health` returning
  `200 {"status":"UP"}` (see `contracts/health-api.yaml`).

## Frontend toolchain

- **Decision**: Vite 8 (stable, 2026) with `@vitejs/plugin-vue` and TypeScript.
  - **Rationale**: mandated by the feature description; Vite 8 is the current
    stable major with fast dev server and HMR (needed for SC-002).
  - **Alternatives considered**: Vite 7 — rejected, superseded; Webpack —
    rejected, not requested and slower.
- **Decision**: Node.js 26 LTS as the frontend toolchain runtime (Node 24 LTS
  accepted as minimum fallback).
  - **Rationale**: Node 26 entered LTS in October 2026; engines field in
    `package.json` will pin `>=24` with a warning for non-LTS.
- **Decision**: Vue 3 + Vuetify 4 (current stable; v3.13.x is the final v3 line
  if a conservative pin is ever needed).
  - **Rationale**: constitution mandates Vue 3 + Vuetify; Vuetify 4 is stable
    since Feb 2026 and targets Vue 3.
- **Decision**: Vitest + `@vue/test-utils` for unit tests; ESLint flat config +
  Prettier for lint/format.
  - **Rationale**: standard Vite-ecosystem testing/linting; satisfies the
    constitution's quality gates.

## Build / CI / repo hygiene

- **Decision**: CI on GitHub Actions (`.github/workflows/ci.yml`) running, for
  each side, the same commands as local: `./mvnw verify` (backend),
  `npm ci && npm run lint && npm run build` (frontend).
  - **Rationale**: FR-006 / SC-004 require identical local and CI gates; the
    project will be exposed on GitHub, so GitHub Actions is the mandated CI
    (constitution v1.3.0, Development Workflow).
  - **Alternatives considered**: GitLab CI — rejected, project is hosted on
    GitHub.
- **Decision**: keep the Quarkus app launchable in debug mode at all times —
  `./mvnw quarkus:dev` with JDWP on port 5005
  (`MAVEN_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"`),
  IDE debugger attaches to `localhost:5005`.
  - **Rationale**: constitution v1.3.0 (Development Workflow) requires that no
    build or config change ever removes the ability to debug the app; this
    facilitates day-to-day development.
- **Decision**: root `.gitignore` covers `backend/target/`, `*.class`,
  `frontend/node_modules/`, `frontend/dist/`, logs, `.env*`; plus a root
  `README.md` documenting prerequisites (JDK 25, Node 26 LTS) and the commands.
  - **Rationale**: FR-007 requires documented tool versions and commands.

## Resolved unknowns

No `[NEEDS CLARIFICATION]` markers remain in the plan: storage is N/A at
bootstrap, all tool versions pinned above, project type is a web application.
