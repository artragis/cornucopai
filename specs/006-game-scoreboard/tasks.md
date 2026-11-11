---

description: "Task list for feature 006-game-scoreboard — scoreboard Cornucopia"
---

# Tasks: Scoreboard Cornucopia — couleur majeure, points et page de résultats

**Input**: Design documents from `/specs/006-game-scoreboard/`

**Prerequisites**: plan.md (required), spec.md (required for user stories, incl. Clarifications session 2026-11-10), research.md (R-045…R-053), data-model.md, contracts/ (ateliers-api.yaml v0.6.0, leg-schema.json v3.0.0), quickstart.md

**Tests**: INCLUDED and MANDATORY — constitution principle II (Test-First, non-negotiable): every test task is written and MUST FAIL before its implementation task runs (Red-Green-Refactor). Backend CI stays LLM-free (fake brain `cornucopai.llm.brain=fake`, one single card family per game test — spec 004 FR-014). Frontend tests use Vitest + fake timers for polling.

**Organization**: Tasks are grouped by user story (US1–US5 from spec.md) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1…US5)
- Include exact file paths in descriptions

## Path Conventions

- Backend main: `backend/src/main/java/com/cornucopai/…`, tests: `backend/src/test/java/com/cornucopai/…`
- Frontend: `frontend/src/…`, tests colocated (`*.spec.ts`)
- Spec artifacts: `specs/006-game-scoreboard/…`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: confirm the green baseline before touching anything

- [X] T001 Run `cd backend && ./mvnw test` and `cd frontend && npm test` and confirm both suites are green (baseline) before any change

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: the additive model extensions — `Tour`, `Leg` v3 fields, `Atelier` extended, `CardValueOrder`. ALL user stories depend on these. TDD: T002–T004 first, MUST fail before T005–T008.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Tests (write first, watch them fail)

- [X] T002 [P] Write `CardValueOrderTest` in `backend/src/test/java/com/cornucopai/game/model/CardValueOrderTest.java` — canonical order `2<3<…<10<J<Q<K<A` (FR-010, R-046); `"10" > "2"` (not alphabetical); unknown value ranks 0
- [X] T003 [P] Write `TourModelTest` in `backend/src/test/java/com/cornucopai/game/model/TourModelTest.java` — `Tour` record round-trips through Jackson with fields `index, leader, majorSuit, legIndices, winner, bonusAwarded` (R-048)
- [X] T004 [P] Update `ModelContractTest` in `backend/src/test/java/com/cornucopai/game/model/ModelContractTest.java` — `Leg` v3 round-trip: new optional fields `turnIndex, majorSuit, suitId, cardValue, points` serialize/deserialize; a legacy v2 leg JSON (without them) reads with null/0 defaults (FR-017, R-047); `Atelier` gains `tours, scores, winner` and a legacy atelier JSON reads with empty/null (R-048)

### Implementation

- [X] T005 [P] Implement `CardValueOrder.java` in `backend/src/main/java/com/cornucopai/game/model/CardValueOrder.java` — rank map `2→2…10→10, J→11, Q→12, K→13, A→14`, `compare(String, String)` by rank, unknown → 0 + WARN (R-046)
- [X] T006 [P] Implement `Tour.java` in `backend/src/main/java/com/cornucopai/game/model/Tour.java` — persisted record `{int index, String leader, String majorSuit (nullable), List<Integer> legIndices, String winner (nullable), boolean bonusAwarded}` (R-045, R-048)
- [X] T007 [P] Modify `Leg.java` in `backend/src/main/java/com/cornucopai/game/model/Leg.java` — add 5 optional fields `turnIndex (Integer), majorSuit (String), suitId (String), cardValue (String), points (Integer)` with getters/setters; Jackson defaults null on legacy files (R-047, leg-schema v3.0.0)
- [X] T008 [P] Modify `Atelier.java` in `backend/src/main/java/com/cornucopai/game/model/Atelier.java` — add `List<Tour> tours`, `Map<String,Integer> scores`, `List<String> winner` with getters/setters; default empty/null on legacy reads (R-048)

**Checkpoint**: Foundation ready — additive model v3 compiles, legacy reads covered by tests.

---

## Phase 3: User Story 1 — Couleur majeure et regroupement en tours (Priority: P1) 🎯 MVP

**Goal**: the `GameMaster` groups legs into tours (one leg per player with cards per tour), draws the tour-1 leader at random, and each tour's first non-WC card sets the `majorSuit`. Depends on Phase 2.

**Independent Test**: a seeded 3-player fake-brain game produces `tours` in atelier.json where each tour has exactly one leg per player still holding cards; tour 1 leader is from the shuffled order; the next tour's leader is the previous tour's winner; `majorSuit` equals the first non-WC card's suitId of each tour (FR-001…FR-005, SC-001, SC-003; quickstart §1–§2).

### Tests (write first, MUST fail)

- [X] T009 [P] [US1] Write `GameMasterTourTest` in `backend/src/test/java/com/cornucopai/game/GameMasterTourTest.java` — seeded `Random`, 3 players, fake brain, one family `webapp:VE`: assert tours count, one leg per player per tour, tour-1 leader random, next leader = previous winner, all-REJECTED tour → next leader random (FR-001, clarification 2026-11-10)
- [X] T010 [P] [US1] Extend `GameMasterTest` in `backend/src/test/java/com/cornucopai/game/GameMasterTest.java` — each persisted leg carries `turnIndex`, `suitId`, `cardValue` matching its card; each tour carries `majorSuit` = first non-WC suitId (FR-002, FR-005, R-052)

### Implementation for US1

- [X] T011 [US1] Modify `GameMaster.java` in `backend/src/main/java/com/cornucopai/game/GameMaster.java` — replace the flat turn loop with a tour loop: maintain `currentTour`, `tourLeader`, and a per-tour leg list; tour 1 leader = `order.get(random.nextInt(order.size()))`; after a tour closes, next leader = tour winner or random if all REJECTED (R-045, clarification 2026-11-10); set `leg.turnIndex/suitId/cardValue` when persisting; detect `majorSuit` = first non-WC `suitId` of the tour and write it on every leg of the tour + the `Tour` (FR-002, FR-003, R-052); persist `tours` in the atelier (FR-005)
- [X] T012 [US1] Modify `AtelierStore.java` in `backend/src/main/java/com/cornucopai/game/AtelierStore.java` if needed — ensure `saveAtelier` is called after each tour closes so `tours` are persisted incrementally (polling sees progress)

**Checkpoint**: US1 functional — tours and couleur majeure persisted and testable.

---

## Phase 4: User Story 2 — Famille Cornucopia (joker WC) (Priority: P1)

**Goal**: WC led at tour start shifts the major-suit decision to the next non-WC card (cascade); WC played later counts as the major suit for scoring. Depends on US1.

**Independent Test**: a game including `webapp:WC` asserts: (a) WC led → `majorSuit` is the next non-WC suit; (b) two WCs led → third card's suit; (c) WC played later has `suitId="WC"` but is treated as major suit for points (FR-003, FR-004, SC-003).

### Tests (write first, MUST fail)

- [X] T013 [P] [US2] Extend `GameMasterTourTest` in `backend/src/test/java/com/cornucopai/game/GameMasterTourTest.java` — add a fixture family `webapp:WC` case: WC led → majorSuit = next non-WC card's suit; all-WC tour → majorSuit null; WC later in tour → leg.suitId="WC", treated as major (R-052, FR-003, FR-004)

### Implementation for US2

- [X] T014 [US2] Modify `GameMaster.java` in `backend/src/main/java/com/cornucopai/game/GameMaster.java` — in major-suit detection, skip cards with `suitId.equals("WC")` until the first non-WC card (cascade through the tour); if all are WC, majorSuit stays null; in scoring, a WC card counts as major suit when majorSuit is non-null (FR-003, FR-004, R-052)

**Checkpoint**: US2 functional — joker rules covered by tests.

---

## Phase 5: User Story 3 — Points : scénario validé + bonus carte la plus forte (Priority: P1)

**Goal**: +1 point per VALIDATED leg in the major suit, +1 bonus per tour to the highest major-suit card, scores per player persisted, winner computed at game end. Depends on US1 + US2.

**Independent Test**: a seeded game asserts exact point totals: scenario points (VALIDATED + major suit), bonus point per tour to the highest card by `CardValueOrder`, 0 for REJECTED or off-suit; `scores` map and `winner` (ties included) in atelier.json (FR-006…FR-011, SC-002, SC-007; quickstart §1–§2).

### Tests (write first, MUST fail)

- [X] T015 [P] [US3] Write `ScoreTest` in `backend/src/test/java/com/cornucopai/game/ScoreTest.java` — unit-test the scoring rules with hand-built legs/tours: VALIDATED+major → 1, VALIDATED off-suit → 0, REJECTED → 0, bonus to highest `CardValueOrder` rank among major-suit cards (WC included), winner = max score with ties (FR-006…FR-011)
- [X] T016 [P] [US3] Extend `GameMasterTourTest` in `backend/src/test/java/com/cornucopai/game/GameMasterTourTest.java` — after a full fake-brain game: each leg's `points` field is correct, `scores` sums per player, `winner` lists the top scorer(s), all persisted in atelier.json (FR-011, FR-018, SC-001, SC-002, SC-007)
- [X] T017 [P] [US3] Update `AtelierResourceTest` in `backend/src/test/java/com/cornucopai/api/AtelierResourceTest.java` — `GET /api/ateliers/{id}` returns `tours`, `scores`, `winner`, and legs with the v3 fields; a legacy atelier (fixture without them) returns empty tours/scores and nulls without error (FR-012, FR-017, SC-006, API v0.6.0)

### Implementation for US3

- [X] T018 [US3] Modify `GameMaster.java` in `backend/src/main/java/com/cornucopai/game/GameMaster.java` — after each leg's validation, compute its scenario point (VALIDATED && (suitId==majorSuit || suitId=="WC") → 1 else 0) and set `leg.points`; when a tour closes, award +1 bonus to the leg with the highest `CardValueOrder` rank among major-suit legs (set `tour.winner`, `bonusAwarded`, add 1 to that leg's `points`); after the game, compute `scores` (sum of leg points per player) and `winner` (max score, ties) and persist in the atelier (FR-006…FR-011, R-045)
- [X] T019 [US3] Modify `AtelierResponse.java` in `backend/src/main/java/com/cornucopai/api/dto/AtelierResponse.java` — add `tours`, `scores`, `winner` to the response record and map them from `Atelier` in `AtelierResponse.from(...)` (FR-012, API v0.6.0)

**Checkpoint**: US3 functional — scoring end-to-end, API v0.6.0 returns scoreboard data.

---

## Phase 6: User Story 4 — Page de détail `/ateliers/:id` avec scoreboard (Priority: P1)

**Goal**: dedicated route showing the scoreboard above all legs; history items are router-links; after POST the app navigates to the detail page. Depends on US3 (scores in API).

**Independent Test**: from the home page, clicking a history atelier navigates to `/ateliers/:id` showing the scoreboard (scores per player, winner or "en cours") above the legs list with per-leg details; refresh keeps the page (FR-013, FR-014, SC-004; quickstart §4–§5).

### Tests (write first, MUST fail)

- [X] T020 [P] [US4] Write `router.spec.ts` in `frontend/src/router.spec.ts` — routes `/` and `/ateliers/:id` resolve to the right components
- [X] T021 [P] [US4] Write `AtelierDetailPage.spec.ts` in `frontend/src/pages/AtelierDetailPage.spec.ts` — mount with a mocked `getAtelier` response: scoreboard renders scores per player + winner/"en cours"; each leg row shows player, card id, suitId, scénario, validation chip, points, majorSuit (FR-014)
- [X] T022 [P] [US4] Update `App.spec.ts` in `frontend/src/App.spec.ts` — history items are `<router-link>` to `/ateliers/${id}`; after `createAtelier` the app navigates to `/ateliers/${id}` (FR-013)

### Implementation for US4

- [X] T023 [US4] Add `vue-router@4` to `frontend/package.json` dependencies and run `npm install` (R-049; recorded per constitution)
- [X] T024 [US4] Create `router.ts` in `frontend/src/router.ts` — `createRouter` with `history` mode, routes `/` → `App.vue`, `/ateliers/:id` → `AtelierDetailPage.vue`; export it
- [X] T025 [US4] Modify `main.ts` in `frontend/src/main.ts` — `app.use(router)`
- [X] T026 [US4] Create `AtelierDetailPage.vue` in `frontend/src/pages/AtelierDetailPage.vue` — fetch via `getAtelier(route.params.id)`, render scoreboard card (players × scores, winner or "en cours", turn order) above a legs list (player, card id, suitId, cardValue, scénario, explication, entrées, validation chip, points, majorSuit); "données partielles" banner when `tours` is empty (FR-014, FR-017)
- [X] T027 [US4] Modify `App.vue` in `frontend/src/App.vue` — replace inline atelier card with `<router-view>`; history items become `<router-link :to="`/ateliers/${a.id}`">`; after `onLaunch` success, `router.push(`/ateliers/${atelier.id}`)` (FR-013)
- [X] T028 [US4] Modify `atelierApi.ts` in `frontend/src/services/atelierApi.ts` — extend `LegDto` with `turnIndex?, majorSuit?, suitId?, cardValue?, points?`; add `TourDto` and extend `AtelierResponse` with `tours, scores, winner` (API v0.6.0)

**Checkpoint**: US4 functional — dedicated detail page with scoreboard, clickable history, post-launch redirect.

---

## Phase 7: User Story 5 — Polling 5 s sur la page de détail (Priority: P2)

**Goal**: the detail page polls `GET /api/ateliers/{id}` every 5 s while the atelier is unfinished, then stops. Depends on US4.

**Independent Test**: with Vitest fake timers, `getAtelier` is called every 5000 ms; the interval clears when `finishedAt` is set and on unmount; a polling error keeps the last good state (FR-015, FR-016, SC-005; quickstart §4).

### Tests (write first, MUST fail)

- [X] T029 [P] [US5] Write `useAtelierPolling.spec.ts` in `frontend/src/composables/useAtelierPolling.spec.ts` — fake timers: `getAtelier` called at 5 s intervals; stops when response has `finishedAt` non-null; stops on unmount; a rejected fetch keeps the previous data and does not throw (FR-015, FR-016, R-050)

### Implementation for US5

- [X] T030 [US5] Create `useAtelierPolling.ts` in `frontend/src/composables/useAtelierPolling.ts` — composable taking `atelierId`, returning a reactive `Ref<AtelierResponse|null>`; `setInterval(5000)` calls `getAtelier`, replaces the ref on success, catches errors silently; clears interval when `finishedAt` non-null and on `onUnmounted` (R-050)
- [X] T031 [US5] Modify `AtelierDetailPage.vue` in `frontend/src/pages/AtelierDetailPage.vue` — use `useAtelierPolling(route.params.id)` instead of a one-shot fetch; initial load still immediate (FR-015)

**Checkpoint**: US5 functional — live-updating detail page during a game.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: backward compatibility validation, docs, diary, final full validation

- [X] T032 [P] Update `AtelierStoreTest` in `backend/src/test/java/com/cornucopai/game/AtelierStoreTest.java` — a real legacy atelier fixture (pre-006, e.g. `atelier20261110-111343`) loads via `loadAtelier`/`loadLegs` without error and exposes null/empty tour fields (FR-017, SC-006)
- [X] T033 [P] Run quickstart.md validation end-to-end: `cd backend && ./mvnw test`, inspect a persisted atelier.json + leg files with `jq`, `curl` the API v0.6.0, `cd frontend && npm test`, and the manual dev run (quickstart §1–§5)
- [X] T034 [P] Add a diary entry (constitution — Project Diary) in `specs/006-game-scoreboard/` or the billet manifest noting the scoreboard decisions (tour grouping, WC joker, scoring, vue-router addition)
- [X] T035 Run `cd backend && ./mvnw test` and `cd frontend && npm run lint && npm test` — full suite green (CI-equivalent, fake brain)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies — start immediately
- **Foundational (Phase 2)**: depends on Phase 1 — BLOCKS all user stories
- **User Stories (Phase 3–7)**: depend on Phase 2; US2 depends on US1, US3 depends on US1+US2, US4 depends on US3 (scores in API), US5 depends on US4 (page exists)
- **Polish (Phase 8)**: depends on all story phases

### User Story Dependencies

- **US1 (P1)**: after Foundational — no story deps — MVP core
- **US2 (P1)**: after US1 — extends GameMaster major-suit logic
- **US3 (P1)**: after US1+US2 — scoring needs tours + major suit
- **US4 (P1)**: after US3 — page needs scores/winner in API
- **US5 (P2)**: after US4 — polling attaches to the detail page

### Within Each User Story

- Tests MUST be written and MUST FAIL before implementation (constitution II)
- Models before engine (GameMaster) before API DTO before frontend
- Story complete before moving to the next

### Parallel Opportunities

- Phase 2: T002, T003, T004 (tests) and T005–T008 (models) are all [P] — different files
- Phase 3: T009, T010 tests are [P]
- Phase 5: T015, T016, T017 tests are [P]
- Phase 6: T020, T021, T022 tests are [P]; T023–T028 touch different frontend files
- Phase 8: T032, T033, T034 are [P]

---

## Parallel Example: Phase 2 (Foundational)

```bash
# All foundational tests in parallel:
Task: "T002 CardValueOrderTest in backend/src/test/java/com/cornucopai/game/model/CardValueOrderTest.java"
Task: "T003 TourModelTest in backend/src/test/java/com/cornucopai/game/model/TourModelTest.java"
Task: "T004 Update ModelContractTest in backend/src/test/java/com/cornucopai/game/model/ModelContractTest.java"
# All foundational models in parallel:
Task: "T005 CardValueOrder.java in backend/src/main/java/com/cornucopai/game/model/CardValueOrder.java"
Task: "T006 Tour.java in backend/src/main/java/com/cornucopai/game/model/Tour.java"
Task: "T007 Leg.java v3 fields in backend/src/main/java/com/cornucopai/game/model/Leg.java"
Task: "T008 Atelier.java extended in backend/src/main/java/com/cornucopai/game/model/Atelier.java"
```

---

## Implementation Strategy

### MVP First (US1 + US2 + US3 = backend scoreboard)

1. Complete Phase 1: Setup (green baseline)
2. Complete Phase 2: Foundational (model v3)
3. Complete Phase 3 (US1): tours + couleur majeure
4. Complete Phase 4 (US2): joker WC
5. Complete Phase 5 (US3): scoring + API v0.6.0
6. **STOP and VALIDATE**: backend scoreboard works end-to-end with fake brain (quickstart §1–§3)

### Incremental Delivery

1. Setup + Foundational → model ready
2. US1 → US2 → US3 → backend scoreboard validated (MVP backend)
3. US4 → detail page with scoreboard (MVP full stack)
4. US5 → live polling
5. Phase 8 → legacy compat + diary + full CI green

### Parallel Team Strategy

- Phase 2 models can be split across developers (different files, all [P])
- After Phase 5 (backend done), US4 and US5 can be worked by a frontend developer while another hardens tests

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps to spec.md user stories for traceability
- TDD non-negotiable (constitution II): every test task MUST fail before its implementation task
- One card family per game test (spec 004 FR-014), fake brain, CI LLM-free
- vue-router is the only new dependency — recorded in plan.md Complexity Tracking per constitution
- Legacy ateliers (pre-006) must stay readable — covered by T004, T017, T032 (FR-017)
- Commit after each task or logical group
