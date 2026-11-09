# Implementation Plan: Sélection des familles de cartes et distribution des vraies cartes Cornucopia

**Branch**: `003-card-set-selection` | **Date**: 2026-11-08 | **Spec**: [specs/003-card-set-selection/spec.md](spec.md)

**Input**: Feature specification from `/specs/003-card-set-selection/spec.md`

## Summary

Replace the fixed placeholder deck of spec 002 with the real Cornucopia cards. The
backend reads the Cornucopia repo root from the `CORNUCOPIA_HOME` environment variable,
scans `<CORNUCOPIA_HOME>/source/*-cards-*.yaml`, keeps only the latest version per
edition (7 sets), and exposes a catalog `GET /api/card-catalog` (sets → families →
languages). The frontend adds a language selector (default `fr`) and a hierarchical
set → familles selector to the existing « Configurer une partie » dialog. `POST
/api/ateliers` now takes `language` and `families` (`setId:familyId`); the backend reads
each touched set in the chosen language (falling back to `en` per set, never to an older
version), builds one combined shuffled deck of the selected families' real cards, deals
it round-robin exactly as before, and runs the unchanged game engine on those real cards.
`Atelier.deck` becomes a list of full `Card` objects (id + description persisted in
`atelier.json`); each leg's `cardName` is the real card id. Decisions: `research.md`
(R-011…R-020); entities: `data-model.md`; contracts: `contracts/`; validation:
`quickstart.md`.

## Technical Context

**Language/Version**: Java 25 (backend); TypeScript 5.9 on Node.js ≥ 24 (frontend)

**Primary Dependencies**: Backend — Quarkus 3.40.1.1 (`quarkus-rest`,
`quarkus-rest-jackson`, `quarkus-smallrye-health` already declared),
`io.quarkiverse.langchain4j:quarkus-langchain4j-core` 1.15.0 already declared (no LLM
call in this increment). **New deliberate dependency** (R-011, recorded per
constitution): `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` (version
managed by the Quarkus BOM) to parse the Cornucopia YAML with the same Jackson stack
already used for JSON. Frontend — Vue 3.5, Vuetify 4.3, Vite 8, Vitest 5, TipTap 3
(already declared); no new frontend dependency (the tree selector uses built-in
Vuetify components).

**Storage**: Filesystem only, unchanged from spec 002 (R-002). Ateliers under
`cornucopai.ateliers.root-dir` (default `ateliers/`, gitignored). New read-only input:
the Cornucopia repo at `cornucopai.cornucopia-home=${CORNUCOPIA_HOME:}` (R-012) — empty
means "not configured" → empty catalog + launch refused (FR-011).

**Testing**: Backend — JUnit 5 (`quarkus-junit`) + AssertJ + REST Assured, with
`cornucopai.cornucopia-home` pointed at a hermetic fixture
`backend/src/test/resources/cornucopia-fixture/source/` and a seeded `Random` (R-020).
Frontend — Vitest + `@vue/test-utils`. TDD per constitution: tests first in
`/speckit-tasks`.

**Target Platform**: Linux server (backend port 8080); any modern evergreen browser

**Project Type**: web application (backend REST API + frontend SPA)

**Performance Goals**: `GET /api/card-catalog` < 200 ms after first scan (catalog is
cached, R-019); a full 4-player game on real cards completes in under 5 s with dumb
players (unchanged from 002); Quarkus startup < 2 s.

**Constraints**: latest version per edition only (R-013); language default `fr` with
per-set fallback to `en` within the latest version only (R-015); at least one family
required, unknown `setId:familyId` → 400 (FR-006, FR-012); no duplicate family in the
deck (FR-013); leg keeps its 5-field schema, `cardName` = real id, description lives in
`atelier.json` deck (R-016); JDWP debug on 5005 must remain available (constitution);
only `backend/` and `frontend/` as top-level source dirs (constitution); the game
engine (turn order, legs, votes) is unchanged — only the dealt cards become real.

**Scale/Scope**: one atelier at a time; 2–6 players; deck size = sum of cards of the
selected families (tens to ~hundreds of cards); 7 sets in the catalog; no LLM calls.

## Constitution Check

*GATE: passed before Phase 0; re-checked after Phase 1 design — still passing.*

| Principle | Status | Evidence |
|---|---|---|
| I. Specification-Driven Development | PASS | `spec.md` written, clarified (3 Q→A in `## Clarifications`), checklist all green before this plan |
| II. Test-First (non-negotiable) | PASS (enforced downstream) | R-020: tests first in tasks.md; fixture repo + seeded RNG for reproducibility; coverage gates in quickstart §8 |
| III. Modularity / SRP | PASS | New `CardCatalogService` (scan/parse YAML), `CardDeckService` (build+deal real deck), `Card`/`Set`/`Family` models; `GameMaster` only switches hand type `String`→`Card` (R-018); REST layer in resources |
| IV. Documentation & Transparency | PASS | `contracts/` (4 files), `data-model.md`, `quickstart.md`, this plan; decisions R-011…R-020 with rationale in `research.md`; structured logging required in tasks |
| V. Simplicity (YAGNI) | PASS | Catalog cached, no refresh endpoint (R-019); filesystem JSON not DB; synchronous POST kept; one new backend dep (jackson-dataformat-yaml) justified in R-011 |
| Tech standards (Quarkus/Java 25/LangChain4j; Vue 3/Vuetify) | PASS | Stack unchanged; one new backend dep recorded (R-011); no new frontend dep |
| Repo layout (`backend/`, `frontend/` only) | PASS | Fixture lives under `backend/src/test/resources` (test resource, not a top-level source dir); ateliers remain gitignored data |
| Debug mode (JDWP 5005) | PASS | Unchanged; quickstart prerequisites restate it |
| CI = local commands | PASS | Existing GitHub Actions run the same Maven/npm commands; tasks extend them |
| Diary / billet | PASS (task) | tasks.md includes a diary passage capturing the card-catalog/language-fallback decisions, per constitution |

No violations → Complexity Tracking has only the justified new dependency (below).

## Project Structure

### Documentation (this feature)

```text
specs/003-card-set-selection/
├── spec.md                    # Feature specification (/speckit-specify + /speckit-clarify)
├── plan.md                    # This file (/speckit-plan)
├── research.md                # Phase 0 output — decisions R-011..R-020
├── data-model.md              # Phase 1 output — Set/Family/Card/Atelier/Leg + deck rules
├── quickstart.md              # Phase 1 output — validation scenarios
├── contracts/
│   ├── card-catalog-api.yaml  # OpenAPI: GET /api/card-catalog
│   ├── ateliers-api.yaml      # OpenAPI v0.3.0: POST/GET /api/ateliers (language, families, deck: Card[])
│   ├── leg-schema.json        # 5-field leg file (cardName = real id now)
│   └── atelier-schema.json    # atelier.json (deck: Card[], language, selectedFamilies, sets)
├── checklists/
│   └── requirements.md        # Spec quality checklist (all green)
└── tasks.md                   # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cornucopai/
│   ├── api/
│   │   ├── AtelierResource.java        # MODIFY: POST body += language, families; 400 cases (FR-006/FR-011/FR-012)
│   │   ├── CardCatalogResource.java    # NEW: GET /api/card-catalog (FR-003, FR-014)
│   │   └── dto/
│   │       ├── CreateAtelierRequest.java   # MODIFY: + language, families
│   │       ├── AtelierResponse.java        # MODIFY: + language, selectedFamilies, sets, deck: Card[]
│   │       ├── CardCatalogDto.java         # NEW
│   │       └── …
│   └── game/
│       ├── card/                            # NEW package (SRP)
│       │   ├── CardCatalogService.java   # scan <home>/source, latest version per edition, parse en structure, cache (R-013/R-014/R-019)
│       │   ├── CardDeckService.java      # resolve language per set (fallback en), build combined deck from selected families, shuffle+deal (R-015/R-018)
│       │   └── model/
│       │       ├── Card.java             # NEW: id, value, setId, suitId, suitName, language, description, url?, misc? (R-016)
│       │       ├── CardSet.java          # NEW: id(edition), version, languages, families
│       │       └── CardFamily.java       # NEW: id, name, setId, cardCount
│       ├── GameMaster.java               # MODIFY: hands Deque<String>→Deque<Card>; playCard(Card,…); loop unchanged
│       ├── PlayerAgent.java              # MODIFY: playCard(String)→playCard(Card, featureHtml)
│       ├── agent/*.java                  # MODIFY: 4 archetypes use card.id()/description()
│       ├── PlaceholderCards.java         # DELETE (replaced by CardDeckService)
│       ├── AtelierService.java           # MODIFY: use catalog+deck services, persist deck: Card[], language, selectedFamilies, sets
│       ├── model/Atelier.java            # MODIFY: deck List<Card>, + language, selectedFamilies, sets
│       ├── model/Leg.java                # UNCHANGED 5 fields (cardName now real id)
│       └── AtelierStore.java             # UNCHANGED (Jackson already handles Card via atelier.json)
├── src/main/resources/application.properties  # + cornucopai.cornucopia-home=${CORNUCOPIA_HOME:}
├── pom.xml                               # + jackson-dataformat-yaml (R-011)
└── src/test/…                            # NEW: fixture repo + TDD tests (R-020); update GameMasterTest/PlaceholderCardsTest

frontend/
└── src/
    ├── components/ConfigureGameDialog.vue  # MODIFY: + language v-select (default fr) + hierarchical set→familles tree (Vuetify), launch gating on ≥1 family
    ├── services/atelierApi.ts              # MODIFY: + getCardCatalog(), CreateAtelierRequest += language, families; AtelierResponse deck: Card[]
    └── *.spec.ts                           # MODIFY/NEW: Vitest for selector + language default + gating
```

**Structure Decision**: Web application split (Option 2) — backend REST API + frontend
SPA, matching the existing `backend/` + `frontend/` layout mandated by the constitution.
New backend code is isolated in a `com.cornucopai.game.card` package (catalog parsing +
deck building) so `GameMaster`/`AtelierService` only change type/persistence, keeping the
game engine untouched. The test fixture lives under `backend/src/test/resources` (a test
resource, not a top-level source dir).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| New dependency `jackson-dataformat-yaml` | The Cornucopia cards are YAML; the project already maps JSON with Jackson | Raw SnakeYAML would add a second mapping API and rely on a transitive dep; hand-rolled parsing is error-prone (R-011) |
| `deck` type changes `List<String>` → `List<Card>` | Agents need the real description at play time and the id+description must be persisted (FR-009) | Keeping strings and re-reading YAML at view time would make the atelier non self-contained on disk (R-016) |

No constitution principle violations; the two rows above are justified design choices,
not principle breaches.
