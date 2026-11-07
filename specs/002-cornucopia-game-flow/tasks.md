---

description: "Task list template for feature implementation"
---

# Tasks: Flux complet d'une partie de Cornucopia jouée par des agents IA

**Input**: Design documents from `/specs/002-cornucopia-game-flow/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: INCLUDED and MANDATORY — constitution principle II (Test-First, non-negotiable): every test task is written and MUST FAIL before its implementation task runs (Red-Green-Refactor).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- Web app: `backend/src/main/java/com/cornucopai/…`, `backend/src/test/java/com/cornucopai/…`, `frontend/src/…`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization — new dependency, config, package layout

- [x] T001 Add TipTap WYSIWYG dependency (R-007) to `frontend/package.json` (`@tiptap/vue-3`, `@tiptap/starter-kit`), run `npm install`, and add a Vite dev proxy `/api` → `http://localhost:8080` in `frontend/vite.config.ts`
- [x] T002 Add `cornucopai.ateliers.root-dir=ateliers` to `backend/src/main/resources/application.properties` (R-002); keep the JDWP debug comment untouched
- [x] T003 Create package directories: `backend/src/main/java/com/cornucopai/game/agent`, `backend/src/main/java/com/cornucopai/game/model`, `backend/src/main/java/com/cornucopai/api/dto`, and matching `backend/src/test/java/com/cornucopai/game/…`, `frontend/src/components`, `frontend/src/services`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core domain model, persistence, and agent interface that ALL user stories depend on. TDD: tests T004–T006 are written first and MUST fail before T007–T010.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Tests (write first, watch them fail)

- [x] T004 [P] Write `AtelierStoreTest` in `backend/src/test/java/com/cornucopai/game/AtelierStoreTest.java` — uses JUnit `@TempDir` (R-010): atelier folder named `atelier{yyyyMMdd-HHmmss}` with dashes and no colons (FR-004); writes `atelier.json`; writes then updates `leg-{NNN}-{playerSlug}.json` (NNN zero-padded 3 digits, R-008) with exactly the 5 fields of `contracts/leg-schema.json` (`cardName`, `scenario`, `explanation`, `precisions` array, `validation`) starting at `PENDING` with `precisions: []` and flipping to `VALIDATED`/`REJECTED`; list/read round-trip
- [x] T005 [P] Write `PlaceholderCardsTest` in `backend/src/test/java/com/cornucopai/game/PlaceholderCardsTest.java` — fixed deck of 8 placeholder cards (R-005); shuffle reproducible with a seeded `java.util.Random` injected via constructor; round-robin deal to N hands
- [x] T006 [P] Write model/enum contract tests in `backend/src/test/java/com/cornucopai/game/model/ModelContractTest.java` — `ValidationStatus` enum is exactly `PENDING|VALIDATED|REJECTED`; `Leg` requires non-empty `cardName`/`scenario`/`explanation`, `precisions` non-null array, `validation` non-null; `Archetype` enum is exactly `GRUMPY|HAPPY|GREEDY|PROF`; `Vote` is `YES|NO`; `Reaction` = precision(string)|question(string)|pass; `Answer` = knows(string)|doesNotKnow

### Implementation

- [x] T007 [P] Implement domain model in `backend/src/main/java/com/cornucopai/game/model/`: `ValidationStatus.java`, `Vote.java`, `Reaction.java`, `Answer.java`, `Archetype.java`, `Leg.java`, `Participant.java`, `Atelier.java` (fields per `data-model.md`: Atelier = id, createdAt, featureHtml, playerCount, players, turnOrder, deck; Leg = the 5 JSON fields)
- [x] T008 Implement `PlayerAgent` interface in `backend/src/main/java/com/cornucopai/game/PlayerAgent.java` (R-001) with methods `playCard`, `react`, `answerQuestion`, `vote`, plus `name()` and `archetype()`
- [x] T009 Implement `PlaceholderCards` in `backend/src/main/java/com/cornucopai/game/PlaceholderCards.java` — 8 cards `Carte-Placeholder-1..8`, constructor takes a `java.util.Random` (seeded in tests, default `new Random()` in prod)
- [x] T010 Implement `AtelierStore` in `backend/src/main/java/com/cornucopai/game/AtelierStore.java` — `@Singleton` with `@ConfigProperty(name = "cornucopai.ateliers.root-dir")`, plus a package-visible constructor taking a `Path` + `ObjectMapper` for tests; creates `atelier{yyyyMMdd-HHmmss}` dirs, persists/updates JSON leg files and `atelier.json`, lists and reads ateliers; uses JBoss logging (constitution IV)
- [x] T011 [P] Implement API DTO records in `backend/src/main/java/com/cornucopai/api/dto/`: `CreateAtelierRequest(playerCount:int, featureHtml:String)`, `AtelierSummary`, `AtelierResponse`, `ErrorResponse` — exactly matching `contracts/ateliers-api.yaml`

**Checkpoint**: Foundation ready — model + persistence + agent interface compile and their tests pass; user story work can now begin.

---

## Phase 3: User Story 2 - Déroulement orchestré d'un tour par le maître du jeu (Priority: P1) 🎯 MVP core

**Goal**: The game master loop — draw turn order once, then per turn: current player plays card → leg JSON written PENDING → each other player reacts (precision / question / pass) → card player answers questions ("Je sais"+answer appended to precisions / "Je sais pas" → questioner re-asks without questions) → all others vote → strict majority flips leg to VALIDATED/REJECTED (FR-005..FR-012).

**Independent Test**: `GameMasterTest` runs a full 3-player game with stub `PlayerAgent`s on a `@TempDir` and asserts leg files + vote outcomes — no HTTP, no archetypes needed.

### Tests (write first, watch them fail)

- [x] T012 [P] [US2] Write `GameMasterTest` in `backend/src/test/java/com/cornucopai/game/GameMasterTest.java` — stub agents in test sources; asserts: turn order drawn once and stable across turns (FR-005, SC-005); one `leg-{NNN}-{player}.json` per turn written PENDING then updated (FR-007, SC-002); precisions appended on PRECISION and on KNOWS answers (FR-009, FR-010); after DOES_NOT_KNOW the same player is re-asked and cannot ask again (spec edge case); validation = VALIDATED iff yes > voters/2 strictly over voters = all players except the card player (R-006, FR-012, SC-003), tie/even → REJECTED; loop advances to next player in turnOrder

### Implementation

- [x] T013 [US2] Implement `GameMaster` in `backend/src/main/java/com/cornucopai/game/GameMaster.java` (R-009) — synchronous loop per `data-model.md` flow diagram; constructor takes `AtelierStore`, `PlaceholderCards`, `List<PlayerAgent>`, `Random`; shuffles a copy of players once into `turnOrder`; deals deck round-robin; each turn: `playCard` → store leg PENDING → for each other player in turnOrder: `react()` → PRECISION appends to precisions / QUESTION → card player `answerQuestion()` → KNOWS appends answer, DOES_NOT_KNOW re-asks that player with questions disabled / PASS → next; then each other player `vote()` → strict majority over voters → update leg VALIDATED/REJECTED; JBoss logging at each step (constitution IV)

**Checkpoint**: User Story 2 fully functional and testable independently (engine + persistence, stub agents).

---

## Phase 4: User Story 4 - Archétypes de joueurs simples (Priority: P2)

**Goal**: Four deterministic agents behind `PlayerAgent` (FR-014, US-4 acceptance scenarios).

**Independent Test**: Each `*AgentTest` drives the agent through a leg context and asserts its exact deterministic behavior.

### Tests (write first, watch them fail)

- [x] T014 [P] [US4] Write `GrumpyAgentTest` in `backend/src/test/java/com/cornucopai/game/agent/GrumpyAgentTest.java` — votes NO on every leg; asks a question before passing each turn
- [x] T015 [P] [US4] Write `HappyAgentTest` in `backend/src/test/java/com/cornucopai/game/agent/HappyAgentTest.java` — votes YES; never emits a precision or a question (always PASS)
- [x] T016 [P] [US4] Write `GreedyAgentTest` in `backend/src/test/java/com/cornucopai/game/agent/GreedyAgentTest.java` — explanation and precisions are markedly longer than other agents'; votes YES on Happy's legs, NO on Grumpy's
- [x] T017 [P] [US4] Write `ProfAgentTest` in `backend/src/test/java/com/cornucopai/game/agent/ProfAgentTest.java` — short precision on Happy's legs; question then precision on Grumpy's; PASS on Greedy's; votes NO on Grumpy's, YES on all others

### Implementation

- [x] T018 [P] [US4] Implement `GrumpyAgent` in `backend/src/main/java/com/cornucopai/game/agent/GrumpyAgent.java`
- [x] T019 [P] [US4] Implement `HappyAgent` in `backend/src/main/java/com/cornucopai/game/agent/HappyAgent.java`
- [x] T020 [P] [US4] Implement `GreedyAgent` in `backend/src/main/java/com/cornucopai/game/agent/GreedyAgent.java`
- [x] T021 [P] [US4] Implement `ProfAgent` in `backend/src/main/java/com/cornucopai/game/agent/ProfAgent.java`
- [x] T022 [US4] Implement `AgentFactory` in `backend/src/main/java/com/cornucopai/game/agent/AgentFactory.java` — maps `Archetype` → agent and assigns archetypes round-robin in fixed order `[GRUMPY, HAPPY, GREEDY, PROF]` to players `Joueur-1..N` (R-004)

**Checkpoint**: All four archetypes behave exactly as specified (SC-004); the engine can now run a real game.

---

## Phase 5: User Story 1 - Configurer et lancer une partie (Priority: P1)

**Goal**: « Configurer une partie » form (player count + WYSIWYG feature description, no card-family choice) + « Lancer » → POST `/api/ateliers` runs the whole game synchronously and returns the atelier (FR-001..FR-004, R-003). Depends on Phase 3 (engine) and Phase 4 (agents).

**Independent Test**: REST Assured test on the endpoints + Vitest on the form; quickstart §1, §6, §7, §8.

### Tests (write first, watch them fail)

- [x] T023 [P] [US1] Write `AtelierResourceTest` in `backend/src/test/java/com/cornucopai/api/AtelierResourceTest.java` — REST Assured with `cornucopai.ateliers.root-dir` pointed at a `@TempDir`: POST `{playerCount:4, featureHtml}` → 201 with id `atelier…`, 4 players with archetypes GRUMPY/HAPPY/GREEDY/PROF (round-robin, R-004), `turnOrder` a permutation of player names, non-empty `legs` each with 5 fields and validation ∈ {VALIDATED, REJECTED} never PENDING (SC-001, SC-002); POST `playerCount:1` → 400 `{"error":…}`; POST empty `featureHtml` → 400; GET `/api/ateliers` → 200 list; GET `/api/ateliers/{id}` → 200 full atelier; GET unknown id → 404
- [x] T024 [P] [US1] Write `ConfigureGameDialog.spec.ts` in `frontend/src/` — Vitest + `@vue/test-utils`: renders player-count field and TipTap WYSIWYG editor (R-007); « Lancer » blocked with a message when playerCount < 2 or description empty (no request sent); valid form emits launch payload `{playerCount, featureHtml}`; no card-family selector present (FR-003)

### Implementation

- [x] T025 [US1] Implement `AtelierResource` in `backend/src/main/java/com/cornucopai/api/AtelierResource.java` — `@Path("/api/ateliers")`: POST (validate `playerCount >= 2` and non-blank `featureHtml` → 400 per contract, else create+run → 201), GET list, GET `{id}` (404 if unknown), per `contracts/ateliers-api.yaml`
- [x] T026 [US1] Implement `AtelierService` in `backend/src/main/java/com/cornucopai/game/AtelierService.java` — builds `Atelier` (id, createdAt, featureHtml, players via `AgentFactory`, turnOrder via engine), persists `atelier.json`, runs `GameMaster`, returns `AtelierResponse`; `@Singleton`, JBoss logging
- [x] T027 [US1] Implement frontend: `atelierApi.ts` in `frontend/src/services/atelierApi.ts` (POST/GET via fetch, `/api` proxied in dev), `ConfigureGameDialog.vue` in `frontend/src/components/ConfigureGameDialog.vue` (Vuetify dialog + TipTap editor + « Configurer une partie »/« Lancer » buttons), wire the dialog into `frontend/src/App.vue`

**Checkpoint**: User Story 1 fully functional end-to-end — form → POST → game played → JSON files on disk → atelier returned.

---

## Phase 6: User Story 3 - Fin de partie quand les cartes sont épuisées (Priority: P2)

**Goal**: The game stops exactly when every hand is empty; the atelier folder contains one leg file per card played and the atelier is marked finished (FR-013, US-3).

**Independent Test**: `GameEndTest` with 2 players and the 8-card deck — leg count == 8, no extra turn, `atelier.json` lists all legs and a `finishedAt` timestamp.

### Tests (write first, watch them fail)

- [x] T028 [P] [US3] Write `GameEndTest` in `backend/src/test/java/com/cornucopai/game/GameEndTest.java` — seeded game with 2 players: asserts the loop ends exactly when all hands are empty, leg file count equals deck size (8), playing order cycles through `turnOrder`, and `atelier.json` is final and complete (FR-013)

### Implementation

- [x] T029 [US3] Finalize end-of-game semantics: in `GameMaster` (`backend/src/main/java/com/cornucopai/game/GameMaster.java`) ensure the loop condition is "any hand non-empty" and, on exit, `AtelierStore` writes the final `atelier.json` including `finishedAt`; in `Atelier` (`backend/src/main/java/com/cornucopai/game/model/Atelier.java`) add the `finishedAt` field if missing

**Checkpoint**: Games terminate cleanly and completely; all user stories independently functional.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Validation, quality gates, diary (constitution)

- [x] T030 [P] Run `quickstart.md` end-to-end: `cd backend && ./mvnw quarkus:dev`, `curl -X POST /api/ateliers …`, verify `ateliers/atelier…/` folder and every leg JSON against `contracts/leg-schema.json`, `GET` list/detail/404; then `cd frontend && npm run dev`, fill the form, launch, see legs; verify JDWP debug mode on port 5005 still works (constitution)
- [x] T031 [P] Quality gates: `cd backend && ./mvnw verify` (all tests green, coverage gate on `com.cornucopai.game` per constitution II) and `cd frontend && npm run lint && npm test && npm run build`
- [x] T032 Write diary passage in `diary/` capturing the game-master design decisions (R-001 interface `PlayerAgent`, R-002 filesystem persistence, R-006 majority rule) — required by the constitution's Project Diary section for the Zeste de Savoir billet
- [x] T033 Confirm CI (`.github/workflows/`) runs the same commands as local (`./mvnw verify`, `npm run lint`, `npm test`, `npm run build`) and passes

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational
- **Polish (Phase 7)**: Depends on all user stories

### User Story Dependencies

- **US2 (P1, Phase 3)**: after Foundational only — engine tested with stub agents, no dependency on other stories
- **US4 (P2, Phase 4)**: after Foundational — agents depend only on the `PlayerAgent` interface; independently testable
- **US1 (P1, Phase 5)**: after Foundational + US2 (engine) + US4 (real agents) — both are P1/P2 prerequisites of the « Lancer » e2e; scheduled after them so its end-to-end test runs a real game. Its form/API could be built earlier, but the story is only *complete* once the engine and agents exist.
- **US3 (P2, Phase 6)**: after US2 — terminates the loop the engine implements

### Within Each User Story

- Tests MUST be written and FAIL before implementation (constitution II)
- Models before services; services before endpoints; core before integration
- Story complete (checkpoint) before moving to the next

### Parallel Opportunities

- Phase 1: T001–T003 can run in parallel (different files)
- Phase 2: T004, T005, T006 (tests) in parallel; then T007, T009, T011 in parallel; T008, T010 sequential-ish (interface before store is fine in parallel too — different files)
- Phase 3: single engine file — T012 then T013
- Phase 4: T014–T017 (tests) in parallel, then T018–T021 (agents, one file each) in parallel, then T022
- Phase 5: T023 + T024 (backend/frontend tests) in parallel, then T025, T026, T027 (different files) in parallel
- Phase 6: T028 then T029
- Phase 7: T030–T033 in parallel where independent

## Parallel Example: User Story 4 (Phase 4)

```bash
# Launch all archetype tests together (they fail — agents don't exist yet):
Task: "GrumpyAgentTest in backend/src/test/java/com/cornucopai/game/agent/GrumpyAgentTest.java"
Task: "HappyAgentTest in backend/src/test/java/com/cornucopai/game/agent/HappyAgentTest.java"
Task: "GreedyAgentTest in backend/src/test/java/com/cornucopai/game/agent/GreedyAgentTest.java"
Task: "ProfAgentTest in backend/src/test/java/com/cornucopai/game/agent/ProfAgentTest.java"

# Then launch all four agent implementations together (one file each):
Task: "GrumpyAgent in backend/src/main/java/com/cornucopai/game/agent/GrumpyAgent.java"
Task: "HappyAgent in backend/src/main/java/com/cornucopai/game/agent/HappyAgent.java"
Task: "GreedyAgent in backend/src/main/java/com/cornucopai/game/agent/GreedyAgent.java"
Task: "ProfAgent in backend/src/main/java/com/cornucopai/game/agent/ProfAgent.java"
```

## Implementation Strategy

### MVP First (engine + launch = playable game)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: US2 (game engine) → test independently with stub agents
4. Complete Phase 4: US4 (real archetypes) → test independently
5. Complete Phase 5: US1 (form + API launch) → **STOP and VALIDATE** end-to-end with quickstart §1–§2 → this is the MVP: a full Cornucopia game played by AI agents
6. Add Phase 6: US3 (clean termination) → test independently
7. Phase 7: Polish, diary, CI

### Incremental Delivery

Each phase is a checkpoint: Setup+Foundational → engine (US2) → archetypes (US4) → launch UI/API (US1, MVP) → termination polish (US3) → gates. Every change traces to a task ID and to the FR/SC it serves.

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps each task to its user story for traceability (US1–US4 = spec.md stories)
- TDD per constitution II: red → green → refactor; no production code without a failing test first
- Verify tests fail before implementing each phase
- Commit after each task or logical group
- Stop at each checkpoint to validate the story independently (quickstart.md scenarios)
- Avoid: vague tasks, same-file conflicts, hidden cross-story dependencies
