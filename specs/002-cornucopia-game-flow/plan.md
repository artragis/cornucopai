# Implementation Plan: Flux complet d'une partie de Cornucopia jouée par des agents IA

**Branch**: `002-cornucopia-game-flow` | **Date**: 2026-11-07 | **Spec**: [specs/002-cornucopia-game-flow/spec.md](spec.md)

**Input**: Feature specification from `/specs/002-cornucopia-game-flow/spec.md`

## Summary

Build the complete game flow of a Cornucopia workshop played by AI agents, with
the backend acting as game master. A user opens « Configurer une partie », enters
the player count and a WYSIWYG description of the feature to analyze, and clicks
« Lancer ». The backend draws the turn order once, then loops over turns: the
current agent plays a card (scenario + explanation) persisted as a 5-field JSON
leg file, each other agent reacts (precision / question / pass), the card
player answers questions ("Je sais" / "Je sais pas"), everyone votes
("valides-tu ?"), and the leg is flipped to VALIDATED (strict majority of yes)
or REJECTED. The game ends when all hands are empty. Four deterministic archetypes
(Grumpy, Happy, Greedy, Prof) implement the `PlayerAgent` interface; LangChain4j
LLM agents plug in behind the same interface later. Decisions: `research.md`;
entities and flow: `data-model.md`; HTTP + file contracts: `contracts/`.

## Technical Context

**Language/Version**: Java 25 (backend); TypeScript 5.9 on Node.js ≥ 24 (frontend)

**Primary Dependencies**: Backend — Quarkus 3.40.1.1 (`quarkus-rest`,
`quarkus-rest-jackson`, `quarkus-smallrye-health` already declared),
`io.quarkiverse.langchain4j:quarkus-langchain4j-core` 1.15.0 already declared (no
LLM call in this increment — see R-001). Frontend — Vue 3.5, Vuetify 4.3,
Vite 8, Vitest 5; **new deliberate dependency**: `@tiptap/vue-3` +
`@tiptap/starter-kit` for the WYSIWYG editor (R-007, recorded per constitution).

**Storage**: Filesystem only. `<root>/atelier{yyyyMMdd-HHmmss}/` with
`atelier.json` + one `leg-{NNN}-{player}.json` per played card. `<root>` =
`cornucopai.ateliers.root-dir` (default `ateliers/`, gitignored). No database
(R-002).

**Testing**: Backend — JUnit 5 (`quarkus-junit`) + AssertJ + REST Assured, with
`cornucopai.ateliers.root-dir` pointed at a `@TempDir` and a seeded `Random` for
deterministic game-flow tests (R-010). Frontend — Vitest + `@vue/test-utils`.
TDD per constitution: tests first in `/speckit-tasks`.

**Target Platform**: Linux server (backend on port 8080); any modern evergreen
browser (frontend)

**Project Type**: web application (backend REST API + frontend SPA)

**Performance Goals**: a full 4-player game (POST /api/ateliers) completes in
under 5 seconds with dumb players; Quarkus startup under 2 seconds; frontend dev
server serving the form under 30 seconds

**Constraints**: POST runs the game synchronously (R-003); placeholder cards
only, no family choice (spec); turn order fixed after the first draw (FR-005);
strict-majority vote over voters = all players except the card player (R-006);
JDWP debug mode on port 5005 must remain available (constitution); only
`backend/` and `frontend/` as top-level source dirs (constitution)

**Scale/Scope**: one atelier at a time; 2–6 players; 8-card placeholder deck;
~2–8 legs per game; 4 deterministic archetypes; no LLM calls yet

## Constitution Check

*GATE: passed before Phase 0; re-checked after Phase 1 design — still passing.*

| Principle | Status | Evidence |
|---|---|---|
| I. Specification-Driven Development | PASS | `spec.md` written and validated (checklist all green) before this plan |
| II. Test-First (non-negotiable) | PASS (enforced downstream) | R-010: tests written first in tasks.md; seeded RNG + `@TempDir` for reproducibility; coverage gates set in tasks |
| III. Modularity / SRP | PASS | `PlayerAgent` interface + 4 archetype impls; `GameMaster` orchestrator; `AtelierService` persistence — one purpose each (R-001, R-009) |
| IV. Documentation & Transparency | PASS | Contracts (`contracts/`), `data-model.md`, `quickstart.md`, this plan; decisions R-001…R-010 recorded with rationale in `research.md`; structured logging required in tasks |
| V. Simplicity (YAGNI) | PASS | Synchronous POST (R-003), filesystem JSON not DB (R-002), placeholder deck (R-005), no LLM call yet (R-001), no async job |
| Tech standards (Quarkus/Java 25/LangChain4j; Vue 3/Vuetify) | PASS | Stack unchanged; LangChain4j already declared, wired behind `PlayerAgent` for later; one new frontend dep (TipTap) recorded in R-007 |
| Repo layout (`backend/`, `frontend/` only) | PASS | Ateliers are runtime data under gitignored `ateliers/`, not a source dir |
| Debug mode (JDWP 5005) | PASS | Unchanged; quickstart §9 verifies it |
| CI = local commands | PASS | Existing GitHub Actions run the same Maven/npm commands; tasks extend them |
| Diary / billet | PASS (task) | tasks.md includes a diary passage capturing the game-master design decisions, per constitution |

No violations → Complexity Tracking table intentionally empty.

## Project Structure

### Documentation (this feature)

```text
specs/002-cornucopia-game-flow/
├── spec.md              # Feature specification (/speckit-specify)
├── plan.md              # This file (/speckit-plan)
├── research.md          # Phase 0 output — decisions R-001..R-010
├── data-model.md        # Phase 1 output — entities, enums, state machine, flow
├── quickstart.md        # Phase 1 output — validation scenarios
├── contracts/
│   ├── ateliers-api.yaml    # OpenAPI: POST/GET /api/ateliers
│   └── leg-schema.json   # JSON Schema of the 5-field leg file
├── checklists/
│   └── requirements.md  # Spec quality checklist (all green)
└── tasks.md             # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cornucopai/
│   ├── api/
│   │   ├── HealthResource.java        # existing smoke endpoint (001)
│   │   └── AtelierResource.java       # NEW: POST/GET /api/ateliers (R-003)
│   └── game/
│       ├── GameMaster.java            # NEW: turn loop orchestrator (R-009)
│       ├── PlayerAgent.java           # NEW: interface playCard/react/answerQuestion/vote (R-001)
│       ├── Archetype.java             # NEW: enum GRUMPY/HAPPY/GREEDY/PROF
│       ├── agent/
│       │   ├── GrumpyAgent.java       # NEW: votes NO, asks a question first
│       │   ├── HappyAgent.java        # NEW: votes YES, never reacts
│       │   ├── GreedyAgent.java       # NEW: long explanation/precisions, votes YES to Happy / NO to Grumpy
│       │   └── ProfAgent.java         # NEW: short precisions to Happy, question→precision to Grumpy, pass to Greedy, NO to Grumpy / YES to others
│       ├── model/
│       │   ├── Atelier.java           # NEW: id, createdAt, featureHtml, players, turnOrder, deck
│       │   ├── Participant.java       # NEW: name + archetype
│       │   ├── Leg.java            # NEW: 5-field JSON (cardName, scenario, explanation, precisions, validation)
│       │   ├── ValidationStatus.java  # NEW: PENDING/VALIDATED/REJECTED
│       │   ├── Reaction.java          # NEW: precision/question/pass
│       │   ├── Answer.java            # NEW: knows(answer)/doesNotKnow
│       │   └── Vote.java              # NEW: YES/NO
│       ├── PlaceholderCards.java      # NEW: fixed 8-card deck (R-005)
│       └── AtelierStore.java          # NEW: filesystem persistence of atelier.json + leg files (R-002, R-008)
├── src/main/resources/application.properties  # + cornucopai.ateliers.root-dir=ateliers
└── src/test/java/com/cornucopai/…      # NEW: TDD tests (R-010)

frontend/
├── src/
│   ├── App.vue                        # existing smoke page (001)
│   ├── components/
│   │   └── ConfigureGameDialog.vue    # NEW: form — player count + TipTap WYSIWYG + « Lancer » (FR-001..FR-003, R-007)
│   ├── services/
│   │   └── atelierApi.ts              # NEW: POST/GET /api/ateliers client
│   └── …
└── src/*.spec.ts                      # NEW: Vitest tests for the form
```

**Structure Decision**: Web application split (Option 2) — backend REST API +
frontend SPA, matching the existing `backend/` + `frontend/` layout mandated by
the constitution. All new backend code lives under `com.cornucopai.game` (domain)
and `com.cornucopai.api` (REST layer); all new frontend code under
`frontend/src/components` + `frontend/src/services`.

## Complexity Tracking

Empty — no constitution violations (see Constitution Check above).
