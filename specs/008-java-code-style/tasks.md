# Tasks: Style de code Java — lint (Checkstyle) et formatage automatique (Google Java Style)

**Input**: Design documents from `/specs/008-java-code-style/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md (R-063…R-067), data-model.md, contracts/build-commands.md, quickstart.md

**Tests**: No new unit tests — this is build tooling; validation is behavioral via the Maven gates themselves (quickstart.md scenarios). The existing backend suite MUST stay green through `./mvnw verify`.

**Organization**: Tasks are grouped by user story (US1…US4 from spec.md) so each story is independently verifiable. All commands run from `backend/` via `./mvnw`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: user story label (US1…US4); absent in Setup / Foundational / Polish phases

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the directory that will hold the versioned Checkstyle config.

- [X] T001 Create directory `backend/config/checkstyle/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Version the Google ruleset in-repo. Both US1 and US2 depend on these files.

**CRITICAL**: No user-story validation can pass until this phase is complete.

- [X] T002 [P] Vendor the official `google_checks.xml` from `com.puppycrawl.tools:checkstyle:14.4.0` (extract `google_checks.xml` from the 14.4.0 jar) into `backend/config/checkstyle/checkstyle.xml` (R-064; line length 100, 108 modules, severity `${org.checkstyle.google.severity}` default warning)
- [X] T003 [P] Create `backend/config/checkstyle/checkstyle-suppressions.xml` suppressing `MissingJavadocMethod` and `MissingJavadocType` (R-066), each entry carrying a comment citing R-066; reference it from `checkstyle.xml` via the `SuppressionFilter` property `org.checkstyle.google.suppressionfilter.config`

**Checkpoint**: Foundation ready — the ruleset is versioned and parseable by Checkstyle 14.4.0.

---

## Phase 3: User Story 1 — Vérification automatique du style à chaque build (Priority: P1) MVP

**Goal**: `./mvnw verify` fails on any Google Java Style violation, with a readable report (FR-001, FR-009).

**Independent Test**: introduce a deliberate violation in a `src/main/java` file → `./mvnw checkstyle:check` and `./mvnw verify` exit non-zero naming file/line/rule; revert → pass (quickstart §2).

### Implementation for User Story 1

- [X] T004 [US1] Add `org.apache.maven.plugins:maven-checkstyle-plugin:3.6.0` to `<build><plugins>` in `backend/pom.xml`, with plugin dependency `com.puppycrawl.tools:checkstyle:14.4.0`, and an execution of goal `check` bound to phase `verify` configured with: `configLocation` = `config/checkstyle/checkstyle.xml`, `propertyExpansion` `org.checkstyle.google.suppressionfilter.config=${project.basedir}/config/checkstyle/checkstyle-suppressions.xml`, `consoleOutput=true`, `failsOnError=true`, `violationSeverity=warning`, `includeTestSourceDirectory=true` (R-064)
- [X] T005 [US1] Run `cd backend && ./mvnw checkstyle:check`; fix every residual Google-style violation it reports in `backend/src/main/java` and `backend/src/test/java` (or add a justified suppression citing an R-xxx decision) until the check is green
- [X] T006 [US1] Validate the gate: add a deliberate violation (e.g. trailing whitespace / `snake_case` variable) → confirm `./mvnw verify` fails naming file+line+rule within 30 s of the check (SC-002, FR-009); revert and confirm green

**Checkpoint**: US1 fully functional — the lint is blocking in `verify` and reports are readable.

---

## Phase 4: User Story 2 — Formatage automatique du code Java (Priority: P1) MVP

**Goal**: `./mvnw spotless:apply` reformats all backend Java files to Google Java Style; `spotless:check` (bound to `verify`) fails on unformatted code (FR-003, FR-010).

**Independent Test**: break the formatting of one file → `./mvnw spotless:check` fails listing it; `./mvnw spotless:apply` restores it and a second `apply` produces no diff (idempotence) (quickstart §3).

### Implementation for User Story 2

- [X] T007 [US2] Add `com.diffplug.spotless:spotless-maven-plugin:3.11.0` to `<build><plugins>` in `backend/pom.xml` with `<java><googleJavaFormat/></java>` (google-java-format 2.0.0 default per R-063/R-065; targets `src/main/java` + `src/test/java`, excludes `target/`) and an execution of goal `check` bound to phase `verify` (so `spotless:apply` formats, `spotless:check` gates)
- [X] T008 [US2] Run the initial format pass: `cd backend && ./mvnw spotless:apply` over all ~125 Java files under `backend/src/main/java` and `backend/src/test/java` (FR-006)
- [X] T009 [US2] Validate idempotence and scope: re-run `./mvnw spotless:apply` → `git status` shows no further changes (FR-010); `./mvnw spotless:check` exits 0; confirm `target/` untouched
- [X] T010 [US2] Run the full gate `cd backend && ./mvnw verify` → exit 0 with both `checkstyle:check` and `spotless:check` passing and all existing tests green (SC-001: 100 % lint-clean)

**Checkpoint**: US2 fully functional — formatting is automatic, idempotent, and CI-gated via `verify`.

---

## Phase 5: User Story 3 — Intégration CI GitHub Actions (Priority: P2)

**Goal**: CI enforces style on every PR with no new job (FR-007, SC-004).

**Independent Test**: read `.github/workflows/ci.yml` and confirm the `backend` job runs `./mvnw verify` under Temurin 25 and the `pull_request` trigger has no branch filter, so any PR (including `008-*`) runs the gates.

### Implementation for User Story 3

- [X] T011 [US3] Read `.github/workflows/ci.yml`; confirm (a) `jobs.backend.steps` includes `./mvnw verify` with `working-directory: backend`, (b) `on.pull_request` is unfiltered → both `checkstyle:check` and `spotless:check` run in CI automatically; make NO change to the workflow (spec US3 acceptance scenario 3); record the confirmation in the commit message

**Checkpoint**: US3 verified — CI gates style via the existing `verify` command.

---

## Phase 6: User Story 4 — Configuration centralisée et versionnée (Priority: P2)

**Goal**: the style rules and commands are documented in the repo (FR-008).

**Independent Test**: a fresh clone + `./mvnw verify` applies the exact same rules with no manual setup; README documents the commands and config location.

### Implementation for User Story 4

- [X] T012 [US4] Add a "Style de code Java" subsection under the Backend commands in `README.md`: document `./mvnw spotless:apply` (format), `./mvnw verify` (lint + format gates), and point to `backend/config/checkstyle/checkstyle.xml` + `checkstyle-suppressions.xml` as the versioned Google ruleset (R-064/R-066)

**Checkpoint**: US4 done — rules versioned in `backend/config/checkstyle/` and documented in `README.md`.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T013 Run every quickstart.md validation scenario 1–5 end-to-end (`spotless:apply`, deliberate Checkstyle violation fails `verify`, `spotless:check` fails on unformatted file, full `./mvnw verify` green, `quarkus:dev` smoke)
- [X] T014 Confirm debug mode is preserved (constitution): `cd backend && ./mvnw quarkus:dev` starts on 8080 with JDWP available on 5005; stop it after confirmation
- [X] T015 Write diary passage `diary/2026-11-12-style-de-code-java-checkstyle-spotless.md` capturing the significant decisions R-063…R-067 (Spotless vs fmt, vendored google_checks 14.4.0, GJF 2.0.0, MissingJavadoc relaxation, no CI change) — constitution requires each significant decision as a diary entry

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies — start immediately
- **Foundational (Phase 2)**: depends on Phase 1 — BLOCKS all user stories (T004 needs `checkstyle.xml`/`checkstyle-suppressions.xml` to exist)
- **User Stories (Phase 3+)**: depend on Foundational; proceed in priority order P1 → P2. US1 (Phase 3) before US2 (Phase 4) because the initial `spotless:apply` (T008) is what makes the codebase pass the US1 lint gate (SC-001)
- **Polish (Phase 7)**: depends on all story phases being complete

### User Story Dependencies

- **US1 (P1)**: after Foundational; no dependency on other stories
- **US2 (P1)**: after Foundational; practically sequenced after US1 so the format pass can resolve lint violations (T005 → T008)
- **US3 (P2)**: read-only verification; after Foundational
- **US4 (P2)**: after Foundational; edits `README.md` only

### Within Each User Story

- Configure (pom) → run → validate → checkpoint
- No test tasks: validation is the build gate itself (quickstart.md)

### Parallel Opportunities

- T002 and T003 ([P], Phase 2): different files, run in parallel
- T012 (README.md) and T015 (diary/…md) touch different files and can be parallelized in the final stretch
- Everything else is sequential because each step depends on the previous command's outcome

---

## Parallel Example: Phase 2

```text
Task T002: vendor google_checks.xml → backend/config/checkstyle/checkstyle.xml
Task T003: create backend/config/checkstyle/checkstyle-suppressions.xml
```

---

## Implementation Strategy

### MVP First (US1 + US2, both P1)

1. Complete Phase 1 (Setup) + Phase 2 (Foundational)
2. Complete Phase 3 (US1): Checkstyle gate in `verify`
3. Complete Phase 4 (US2): Spotless format pass + gate in `verify` → `./mvnw verify` green
4. **STOP and VALIDATE**: quickstart §1–§4 pass; this is the MVP — style is enforced locally and in CI
5. Then Phase 5 (US3, verification only) and Phase 6 (US4, docs), then Phase 7 (Polish)

### Incremental Delivery

1. Setup + Foundational → ruleset versioned
2. US1 → lint blocks bad style
3. US2 → auto-format makes the codebase compliant (MVP complete)
4. US3 → CI confirmed enforcing (no change)
5. US4 → docs in README
6. Polish → quickstart + debug-mode check + diary entry

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] labels map tasks to spec.md user stories for traceability (FR-001…FR-010)
- Each story is independently verifiable via quickstart.md scenarios
- Commit after each task or logical group
- `backend/pom.xml` is edited in Phase 3 (T004) then Phase 4 (T007) — sequential phases, so no conflict; do NOT parallelize pom.xml edits
- `backend/target/` is never touched by lint or format
