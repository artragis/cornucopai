# Implementation Plan: Scoreboard Cornucopia — couleur majeure, points et page de résultats

**Branch**: `006-game-scoreboard` | **Date**: 2026-11-10 | **Spec**: [specs/006-game-scoreboard/spec.md](spec.md)

**Input**: Feature specification from `/specs/006-game-scoreboard/spec.md`

## Summary

Implement the Cornucopia scoreboard end-to-end. (1) The `GameMaster` groups legs into
**tours** (tricks): each tour is a full pass where every player still holding cards plays
exactly once; the first player of tour 1 is drawn at random, then the winner of the
previous tour (highest card of the major suit) leads the next, or a random draw if all
legs of the previous tour were REJECTED. (2) The first non-WC card of a tour sets the
**couleur majeure**; a WC card is always treated as the major suit (joker), and a WC led
shifts the major-suit decision to the next non-WC card. (3) **Scoring**: +1 point per
VALIDATED leg whose card is in the major suit, +1 bonus point per tour to the highest
major-suit card (values 2…10 < J < Q < K < A). (4) The `Leg` and `Atelier` JSON schemas
gain tour/score fields (backward-compatible read of legacy ateliers). (5) The API
`GET /api/ateliers/{id}` returns tours, per-player scores and the winner. (6) The frontend
gains a dedicated route `/ateliers/:id` (vue-router, new dependency) showing the
scoreboard above all legs, with a 5-second polling while the atelier is unfinished.
Decisions: `research.md` (R-045…R-053); entities: `data-model.md`; contracts:
`contracts/`; validation: `quickstart.md`.

## Technical Context

**Language/Version**: Java 25 (backend, Quarkus); TypeScript 5.9 + Vue 3.5 (frontend)

**Primary Dependencies**: Backend unchanged — Quarkus 3.x, `quarkus-langchain4j-core`,
Jackson (incl. YAML). Frontend: add `vue-router@4` (new dependency, recorded here per
constitution — needed for the dedicated `/ateliers/:id` route, FR-013). Vuetify 4.3.2
unchanged.

**Storage**: Filesystem JSON, unchanged. `atelier.json` gains `tours`, `scores`,
`winner`; `leg-*.json` gains `turnIndex`, `majorSuit`, `suitId`, `cardValue`, `points`
(contracts v3.0.0 / API v0.6.0). Legacy ateliers (without these fields) stay readable
with defaults (FR-017).

**Testing**: Backend JUnit 5 + AssertJ + REST Assured, fake brain
(`cornucopai.llm.brain=fake`), one family per game test. Frontend Vitest +
`@vue/test-utils` + jsdom. TDD per constitution: new/updated tests first.

**Target Platform**: Linux server (backend port 8080); any modern browser (frontend)

**Project Type**: web application (backend REST API + frontend SPA)

**Performance Goals**: page de détail renders scoreboard + all legs < 2 s after click
(SC-004); polling refresh < 6 s after a new leg appears server-side (SC-005, 5 s
interval + margin); full backend test suite < 2 min with fake brains.

**Constraints**: leg schema v3 and API v0.6.0 are additive (new optional fields) with
tolerant read of v2/legacy files (FR-017); card value ordering is the canonical
2…10,J,Q,K,A (FR-010), never alphabetical; WC family = `suitId == "WC"`; a tour requires
every player still holding cards to play exactly once; the GameMaster remains the sole
arbiter of major suit, tour boundaries and scoring; JDWP 5005 stays available; only
`backend/` + `frontend/` top-level dirs; CI never calls a real LLM; one family per game
test.

**Scale/Scope**: one atelier at a time; 2–6 players; legs per atelier bounded by deck
size; polling is one `setInterval` per open detail page, no WebSocket/SSE.

## Constitution Check

*GATE: passed before Phase 0; re-checked after Phase 1 design — still passing.*

| Principle | Status | Evidence |
|---|---|---|
| I. Specification-Driven Development | PASS | `spec.md` + clarifications (session 2026-11-10) + checklist all green before this plan |
| II. Test-First (non-negotiable) | PASS (enforced downstream) | New tests first: GameMaster tour/scoring tests, model contract tests (leg v3), API resource tests, frontend route/polling component tests; fake brain; one family per game |
| III. Modularity / SRP | PASS | `Tour` in `game.model`; scoring logic in a dedicated `ScoreCalculator` (or private methods of `GameMaster`); `GameMaster` orchestrates tours; frontend `AtelierDetailPage.vue` + `useAtelierPolling` composable |
| IV. Documentation & Transparency | PASS | contracts v0.6.0 / leg-schema v3.0.0, data-model.md, quickstart.md, decisions R-045…R-053; structured logs kept (tour start, major suit, points awarded, winner) |
| V. Simplicity (YAGNI) | PASS | Additive schema fields + read-time defaults instead of a migration; one new frontend dependency (vue-router) justified by the dedicated route; polling via plain `setInterval`, no WebSocket |
| Tech standards (Quarkus/Java 25/LangChain4j; Vue 3/Vuetify) | PASS | Stack unchanged except vue-router (recorded) |
| Repo layout | PASS | All changes under `backend/` and `frontend/` |
| Debug mode (JDWP 5005) | PASS | Unchanged |
| CI = local commands | PASS | CI stays LLM-free (fake brain); frontend `vitest run` |
| Security-sensitive review | PASS | No new external input; polling calls an existing authenticated-local API |
| Diary | PASS (task) | tasks.md includes a diary entry for the scoreboard feature |

No violations → Complexity Tracking below lists only justified choices.

## Project Structure

### Documentation (this feature)

```text
specs/006-game-scoreboard/
├── spec.md                    # Feature specification + Clarifications (session 2026-11-10)
├── plan.md                    # This file (/speckit-plan)
├── research.md                # Phase 0 output — decisions R-045..R-053
├── data-model.md              # Phase 1 output — Tour / Leg v3 / Atelier extended / Score
├── quickstart.md              # Phase 1 output — validation scenarios
├── contracts/
│   ├── ateliers-api.yaml      # v0.6.0 — Atelier gains tours/scores/winner; Leg gains turn fields
│   └── leg-schema.json        # v3.0.0 — additive leg fields (turnIndex, majorSuit, suitId, cardValue, points)
├── checklists/
│   └── requirements.md        # Spec quality checklist (all green)
└── tasks.md                   # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cornucopai/
│   ├── game/
│   │   ├── model/
│   │   │   ├── Tour.java              # NEW: index, majorSuit, legIndices, winner, bonusAwarded (R-045)
│   │   │   ├── Leg.java               # MODIFY: + turnIndex, majorSuit, suitId, cardValue, points (v3, R-047)
│   │   │   ├── Atelier.java           # MODIFY: + tours, scores (Map<String,Integer>), winner (R-048)
│   │   │   └── CardValueOrder.java    # NEW: canonical 2..10,J,Q,K,A comparison (R-046)
│   │   ├── GameMaster.java            # MODIFY: tour grouping, major-suit detection, scoring (R-045, R-052)
│   │   └── AtelierStore.java          # unchanged (schemas extend additively)
│   └── api/dto/
│       └── AtelierResponse.java       # MODIFY: + tours, scores, winner; LegDto extended
└── src/test/…                         # NEW GameMasterTourTest, ScoreTest; MODIFY ModelContractTest, AtelierResourceTest, GameMasterTest

frontend/
├── package.json                       # MODIFY: + vue-router@4 (R-049)
├── src/
│   ├── main.ts                        # MODIFY: install router
│   ├── router.ts                      # NEW: routes / and /ateliers/:id (R-049)
│   ├── App.vue                        # MODIFY: <router-view> + history items become router-links
│   ├── pages/
│   │   └── AtelierDetailPage.vue      # NEW: scoreboard + legs list + polling (R-050)
│   ├── composables/
│   │   └── useAtelierPolling.ts       # NEW: setInterval 5 s on getAtelier, stops when finishedAt set (R-050)
│   └── services/atelierApi.ts         # MODIFY: LegDto/AtelierResponse types extended
└── src/…                              # NEW AtelierDetailPage.spec.ts, router.spec.ts
```

**Structure Decision**: Same web-app split as before. Backend changes are confined to
the model (`Tour`, `Leg` v3, `Atelier` extended, `CardValueOrder`), the engine
(`GameMaster` tour grouping + scoring), and the DTO. Frontend adds vue-router, one page
component, one composable for polling, and updates `App.vue` to use router-links in the
history.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Add `vue-router` dependency | FR-013 clarification: dedicated route `/ateliers/:id` with own URL | Hash-based manual routing or a modal would not give a shareable URL or survive refresh |
| Leg/Atelier schema v3 (additive) | Scoreboard must be autonomous per leg/atelier (FR-018, FR-012) | Recomputing scores on every read from raw legs would couple the API to game rules and break legacy ateliers |
| `Tour` as a persisted entity | FR-005 requires the major suit per tour to be relisable | Storing majorSuit only on legs would duplicate it and lose the tour winner |
| Scoring inside `GameMaster` (not a separate service) | The GameMaster already owns the turn loop and the card data | A separate service would need the full hand/leg state and duplicate the tour grouping |

No constitution principle violations; the rows above are justified design choices.
