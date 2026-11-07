# Feature Specification: Project Bootstrap (Maven backend + Vite frontend)

**Feature Branch**: `001-project-bootstrap`

**Created**: 2026-11-07

**Status**: Implemented

**Input**: User description: "On va bootstraper le projet, donc toute la partie maven pour le backend et toute la partie build avec vite pour le frontend"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Build the backend with one command (Priority: P1)

As a developer, I want to compile, test, and package the backend from a single
standard command inside `backend/`, so that a fresh checkout produces a runnable
artifact with no manual steps.

**Why this priority**: Without a working backend build there is nothing to run,
test, or deploy; this is the foundation every other feature depends on.

**Independent Test**: On a fresh clone, run the backend build command alone; it
MUST succeed end-to-end (compile, tests, packaging) and emit a runnable artifact.

**Acceptance Scenarios**:

1. **Given** a fresh clone of the repository, **When** I run the standard Maven
   build command in `backend/`, **Then** compilation, tests, and packaging all
   succeed and a runnable artifact is produced.
2. **Given** a broken source file in `backend/`, **When** I run the build,
   **Then** the build fails with a clear error identifying the file and cause.

---

### User Story 2 - Run the frontend in development mode (Priority: P1)

As a developer, I want to start the frontend dev server with one command inside
`frontend/`, so that I can see the application locally and get automatic reload
on file changes while iterating.

**Why this priority**: Frontend iteration speed is the second pillar of the
bootstrap; without a dev server the UI cannot be developed or demonstrated.

**Independent Test**: Run the frontend dev command alone; it MUST serve the
application locally and reload on change without restarting the command.

**Acceptance Scenarios**:

1. **Given** a fresh clone, **When** I run the dev command in `frontend/`,
   **Then** the application is served on a local URL and responds to requests.
2. **Given** the dev server is running, **When** I edit a frontend source file,
   **Then** the change is reflected in the browser without manually restarting
   the server.

---

### User Story 3 - Produce a production build of the frontend (Priority: P2)

As a developer, I want to produce an optimized production build of the frontend
with one command, so that static assets are ready to be served by any web server.

**Why this priority**: Needed for deployment and CI validation, but not for local
development, so it comes after the dev server.

**Independent Test**: Run the production build command alone; it MUST complete
and emit a directory of static assets.

**Acceptance Scenarios**:

1. **Given** a fresh clone, **When** I run the production build command in
   `frontend/`, **Then** the build completes and emits optimized static assets
   in a dedicated output directory.

---

### User Story 4 - Same quality gates locally and in CI (Priority: P2)

As a contributor, I want lint, tests, and build to run with the same commands
locally and in CI, so that a green local build means a green pipeline.

**Why this priority**: Prevents "works on my machine" failures and enforces the
constitution's quality gates before merge.

**Independent Test**: Push the bootstrap commit; CI MUST run the same
lint/test/build commands and pass.

**Acceptance Scenarios**:

1. **Given** the bootstrap commit pushed, **When** CI runs, **Then** it executes
   lint, tests, and build for both `backend/` and `frontend/` and passes.
2. **Given** a lint or test violation, **When** CI runs, **Then** the pipeline
   fails and reports the violation.

---

### Edge Cases

- What happens when the required tool versions (JDK, Maven, Node.js) are missing
  or wrong on the machine? The build MUST fail with a clear message naming the
  missing requirement.
- How does the system handle dependency resolution failure (offline machine,
  unreachable registry)? The build MUST fail with a clear error, not a partial
  success.
- What happens on a clean CI environment with no pre-installed project state?
  The build MUST succeed from scratch using only the documented commands.
- What happens when `backend/` and `frontend/` are built in parallel in CI?
  Each build MUST be independent and not interfere with the other.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The repository MUST contain a `backend/` directory holding a
  complete Maven project that compiles, runs tests, and packages the application
  through the standard Maven lifecycle.
- **FR-002**: The backend build MUST target Java 25 and declare the Quarkus and
  LangChain4j dependencies required by the project (as pinned by the
  constitution), so the first feature can build on them.
- **FR-003**: The repository MUST contain a `frontend/` directory holding a
  complete Vite-based project that serves the application in development mode
  and builds optimized static assets for production.
- **FR-004**: The frontend MUST be a Vue 3 application using Vuetify as its
  component library (as pinned by the constitution).
- **FR-005**: Both sides MUST expose a minimal smoke check (a trivial backend
  endpoint that responds, and a trivial frontend page that renders) so the
  build pipeline is proven end-to-end without real business logic.
- **FR-006**: Lint, tests, and build MUST run in CI using the same commands as
  local development, for both `backend/` and `frontend/`.
- **FR-007**: The repository MUST document the required tool versions (JDK 25,
  Maven, Node.js) and the exact commands to build, test, and run each side.

### Key Entities *(include if feature involves data)*

- **Backend build artifact**: the packaged, runnable output of the Maven build
  (e.g. an executable jar), produced by the standard package phase.
- **Frontend build artifact**: the directory of optimized static assets
  (HTML/JS/CSS) produced by the production build, ready to be served statically.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On a fresh machine with only the documented prerequisites installed,
  a developer can complete the backend build (compile + test + package) with one
  command in under 5 minutes, excluding dependency download time.
- **SC-002**: A developer can start the frontend dev server with one command and
  receive a served page on a local URL in under 30 seconds.
- **SC-003**: The frontend production build completes and emits static assets in
  under 2 minutes on a standard CI machine.
- **SC-004**: CI runs lint, tests, and build for both `backend/` and `frontend/`
  on the bootstrap commit and passes with zero manual intervention.
- **SC-005**: A build attempted with a wrong or missing tool version fails with a
  clear, actionable error message (no silent partial success).

## Assumptions

- Maven is the mandated build tool for the backend and Vite the mandated build
  tool for the frontend (explicit user requirement, consistent with the pinned
  Quarkus and Vue 3 stacks).
- JDK 25, a compatible Maven version, and a current Node.js LTS are available on
  developer machines and CI runners; exact versions are documented by the
  project.
- The backend lives in `backend/` and the frontend in `frontend/` at the
  repository root (constitution v1.2.0).
- This feature is scaffolding only: it wires build tooling plus a minimal smoke
  endpoint/page; no Cornucopia game logic is included.
- Dependencies resolve from public Maven Central and the public npm registry.
