---

description: "Task list template for feature implementation"
---

# Tasks: Sélection des familles de cartes et distribution des vraies cartes Cornucopia

**Input**: Design documents from `/specs/003-card-set-selection/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md (R-011…R-020), data-model.md, contracts/, quickstart.md

**Tests**: INCLUDED and MANDATORY — constitution principle II (Test-First, non-negotiable): every test task is written and MUST FAIL before its implementation task runs (Red-Green-Refactor).

**Organization**: Tasks are grouped by user story (US1–US4 from spec.md) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- Backend: `backend/src/main/java/com/cornucopai/…`, tests `backend/src/test/java/com/cornucopai/…`, fixture `backend/src/test/resources/cornucopia-fixture/source/`
- Frontend: `frontend/src/…`, tests `frontend/src/*.spec.ts`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: new YAML dependency, Cornucopia home config, package layout, test fixture

- [X] T001 Add `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` (no `<version>` — managed by the Quarkus BOM) to `backend/pom.xml` (R-011)
- [X] T002 Add `cornucopai.cornucopia-home=${CORNUCOPIA_HOME:}` to `backend/src/main/resources/application.properties` (R-012); keep the existing JDWP debug comment untouched
- [X] T003 Create directories `backend/src/main/java/com/cornucopai/game/card/`, `backend/src/main/java/com/cornucopai/game/card/model/`, and test mirrors `backend/src/test/java/com/cornucopai/game/card/` + `backend/src/test/java/com/cornucopai/game/card/model/`
- [X] T004 [P] Create the hermetic test fixture under `backend/src/test/resources/cornucopia-fixture/source/`: a minimal `webapp-cards-2.2-en.yaml` (old version, must be ignored), `webapp-cards-3.0-en.yaml` + `webapp-cards-3.0-fr.yaml` (latest, with suits VE/AT and a few cards each), and `eop-cards-5.0-en.yaml` (no `fr`, only `en`) — 2 sets, 2 languages (R-020)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: card domain model + YAML catalog parsing service that ALL user stories depend on. TDD: tests T005–T006 first, MUST fail before T007–T010.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Tests (write first, watch them fail)

- [X] T005 [P] Write `CardCatalogServiceTest` in `backend/src/test/java/com/cornucopai/game/card/CardCatalogServiceTest.java` — pointed at the fixture via `cornucopai.cornucopia-home`: keeps only the latest version per edition (webapp 3.0, webapp 2.2 ignored, R-013); lists languages lowercase from file suffixes (FR-014/FR-018); parses families from the `en` file with `id`/`name`/`cardCount` + `totalCards` (R-014); normalizes codes lowercase (`FR`→`fr`) and never treats `fa` as `fr`; returns empty catalog when home unset/invalid (FR-011)
- [X] T006 [P] Write `CardModelContractTest` in `backend/src/test/java/com/cornucopai/game/card/model/CardModelContractTest.java` — `Card` requires non-empty `id`,`value`,`setId`,`suitId`,`suitName`,`language`,`description`; `CardSet` = `id`(edition),`version`,`languages`,`families`,`totalCards`; `CardFamily` = `id`,`name`,`setId`,`cardCount`; global family id is `setId:familyId` (e.g. `webapp:VE`)

### Implementation

- [X] T007 [P] Implement card models `Card.java`, `CardSet.java`, `CardFamily.java` in `backend/src/main/java/com/cornucopai/game/card/model/` exactly per `data-model.md`
- [X] T008 Implement YAML mapping DTO `CardSetFile` (meta.edition/version/language, suits[].id/name/cards[].id/value/desc/url/misc) + a YAML `ObjectMapper` on `YAMLFactory` in `backend/src/main/java/com/cornucopai/game/card/CardSetFile.java` (R-011)
- [X] T009 Implement `CardCatalogService` in `backend/src/main/java/com/cornucopai/game/card/CardCatalogService.java` — reads `cornucopai.cornucopia-home`, scans `<home>/source/*-cards-*.yaml`, keeps max version per edition (numeric semver compare, R-013), languages from suffixes, families parsed from the kept `en` file, caches the catalog (R-019); exposes `catalog()`, `availableLanguages()`, `resolveLanguage(setId, lang)` (chosen lang else `en`, same version only, R-015/FR-016), and `cardsFor(setId, familyIds, resolvedLang)`
- [X] T010 [P] Add DTOs `CardCatalogDto`, `SetDto`, `FamilyDto`, `CardDto` in `backend/src/main/java/com/cornucopai/api/dto/` matching `contracts/card-catalog-api.yaml`

**Checkpoint**: Foundation ready — card model + catalog parsing tested; user stories can now start.

---

## Phase 3: User Story 1 — Lister les sets et leurs familles (Priority: P1)

**Goal**: expose the catalog over HTTP and render the set → familles tree in the configure dialog.

**Independent Test**: `GET /api/card-catalog` returns the sets (latest version only) with families and languages (quickstart §1); the dialog shows the tree (quickstart §6).

### Tests for US1 (write first, MUST fail)

- [X] T011 [P] [US1] Write `CardCatalogResourceTest` in `backend/src/test/java/com/cornucopai/api/CardCatalogResourceTest.java` — `GET /api/card-catalog` → 200 with `availableLanguages` + `sets[{id,version,languages,totalCards,families[{id,name,cardCount}]}]` per `contracts/card-catalog-api.yaml`; `sets: []` when the repo is unconfigured (FR-011)
- [X] T012 [P] [US1] Extend `frontend/src/ConfigureGameDialog.spec.ts` — on open the dialog fetches the catalog and renders one tree node per set with its families (id, name, cardCount)

### Implementation for US1

- [X] T013 [US1] Implement `CardCatalogResource` (`GET /api/card-catalog`) in `backend/src/main/java/com/cornucopai/api/CardCatalogResource.java` (FR-003, FR-014), delegating to `CardCatalogService`
- [X] T014 [US1] Frontend: add `getCardCatalog()` + catalog types to `frontend/src/services/atelierApi.ts`, and render the set → familles tree in `frontend/src/components/ConfigureGameDialog.vue`

**Checkpoint**: US1 functional — catalog listed via API and in the UI.

---

## Phase 4: User Story 2 — Sélectionner des familles et la langue au démarrage (Priority: P1)

**Goal**: hierarchical family selection + language selector (default `fr`) in the dialog; `POST /api/ateliers` carries `language` + `families`.

**Independent Test**: checking a set checks all its families; « Lancer » disabled until ≥1 family; POST transmits `language`/`families` and rejects empty/unknown (quickstart §2, §4).

### Tests for US2 (write first, MUST fail)

- [X] T015 [P] [US2] Extend `backend/src/test/java/com/cornucopai/api/AtelierResourceTest.java` + add `CardSelectionValidationTest` in `backend/src/test/java/com/cornucopai/game/card/CardSelectionValidationTest.java` — POST with `families: []` → 400 (FR-006); unknown `setId:familyId` → 400 naming it (FR-012); `language` defaults to `fr` when absent (FR-015); codes normalized lowercase (FR-018)
- [X] T016 [P] [US2] Extend `frontend/src/ConfigureGameDialog.spec.ts` — language `v-select` defaults to `fr` with options from `catalog.availableLanguages`; one checkbox per family; checking a set toggles all its families; launch payload is `{playerCount, featureHtml, language, families}`; launch disabled while no family checked (FR-006)

### Implementation for US2

- [X] T017 [US2] Modify `CreateAtelierRequest.java` in `backend/src/main/java/com/cornucopai/api/dto/` — add `language` (default `"fr"`) and `families: List<String>` of `setId:familyId` (FR-005, FR-015)
- [X] T018 [US2] Modify `AtelierResource.create` in `backend/src/main/java/com/cornucopai/api/AtelierResource.java` — 400 on empty `families` (FR-006), unknown pair (FR-012), unconfigured repo (FR-011)
- [X] T019 [US2] Frontend `frontend/src/components/ConfigureGameDialog.vue` + `frontend/src/services/atelierApi.ts`: add language selector (default `fr`), hierarchical set→familles checkboxes, extend launch payload with `language` + `families`

**Checkpoint**: US2 functional — families + language selectable and transmitted.

---

## Phase 5: User Story 3 — Distribuer les vraies cartes et les jouer (Priority: P1)

**Goal**: build a real combined deck from selected families (per-set language with `en` fallback), deal round-robin, and run the unchanged engine on `Card` objects; legs reference real card ids; `deck` persisted as full `Card[]`.

**Independent Test**: a POST with selected families yields `deck` containing only those families' real cards; every leg `cardName` is a real id present in `deck`; `webapp` cards `language=fr`, `eop` cards `language=en`; no `Carte-Placeholder-*` (quickstart §2, §3, §7).

### Tests for US3 (write first, MUST fail)

- [X] T020 [P] [US3] Write `CardDeckServiceTest` in `backend/src/test/java/com/cornucopai/game/card/CardDeckServiceTest.java` — deck built only from selected families across sets (FR-007/FR-008); per-set resolved language = chosen lang if present else `en`, never an older version (FR-016/R-015); `resolvedLanguage` recorded (FR-017); no duplicate family (FR-013); shuffle reproducible with seeded `Random`; round-robin deal; cards carry real `id`+`description`
- [X] T021 [P] [US3] Update `GameMasterTest.java` + `GameEndTest.java` in `backend/src/test/java/com/cornucopai/game/` — hands are `Deque<Card>`; `agent.playCard(Card,…)`; `leg.cardName` = real id present in `atelier.deck`; `deck` persisted as `Card[]` in `atelier.json` (FR-009/FR-010); game ends when all hands empty
- [X] T022 [P] [US3] Update the 4 agent tests in `backend/src/test/java/com/cornucopai/game/agent/` (`GrumpyAgentTest`, `HappyAgentTest`, `GreedyAgentTest`, `ProfAgentTest`) — new `playCard(Card, featureHtml)` signature; scenario/explanation derived from `card.description()`
- [X] T023 [P] [US3] Extend `AtelierResourceTest.java` — POST response `Atelier` has `deck: Card[]`, `language`, `selectedFamilies`, `sets[{id,resolvedLanguage}]`; every leg `cardName` ∈ deck ids (contracts/ateliers-api.yaml v0.3.0)

### Implementation for US3

- [X] T024 [US3] Implement `CardDeckService` in `backend/src/main/java/com/cornucopai/game/card/CardDeckService.java` — `buildDeck(selectedFamilies, language, Random)` → `List<Card>` + per-set `resolvedLanguage`, reading each touched set's file in the resolved language (FR-007/FR-008/FR-016/FR-017)
- [X] T025 [US3] Change `PlayerAgent.playCard(String card, …)` → `playCard(Card card, String featureHtml)` in `backend/src/main/java/com/cornucopai/game/PlayerAgent.java` (R-016)
- [X] T026 [US3] Switch the engine to real cards: `GameMaster.java` hands `Deque<String>`→`Deque<Card>` and `playCard(card,…)` (loop otherwise unchanged); `AtelierService.java` uses `CardCatalogService`+`CardDeckService`, persists `deck: List<Card>`, `language`, `selectedFamilies`, `sets`; **delete** `PlaceholderCards.java` and `PlaceholderCardsTest.java` — files `backend/src/main/java/com/cornucopai/game/GameMaster.java`, `backend/src/main/java/com/cornucopai/game/AtelierService.java`
- [X] T027 [US3] Modify `Atelier.java` in `backend/src/main/java/com/cornucopai/game/model/` — `deck` type `List<Card>`, add `language`, `selectedFamilies`, `sets` fields (data-model.md); update `AtelierResponse.java` DTO + `contracts/atelier-schema.json` stays as written
- [X] T028 [US3] Update the 4 archetype agents to `playCard(Card,…)` using `card.id()`/`card.description()` — `backend/src/main/java/com/cornucopai/game/agent/{GrumpyAgent,HappyAgent,GreedyAgent,ProfAgent}.java`
- [X] T029 [US3] Modify `AtelierResource.java` POST to pass `language`+`families` to the service and return the new fields (FR-005/FR-010); update `frontend/src/services/atelierApi.ts` `AtelierResponse` (`deck: Card[]`, `language`, `selectedFamilies`, `sets`)

**Checkpoint**: US3 functional — real cards dealt and played end-to-end.

---

## Phase 6: User Story 4 — Gérer les cas d'erreur de configuration (Priority: P2)

**Goal**: clear errors when the repo is unconfigured or a family is unknown; frontend surfaces them.

**Independent Test**: no `CORNUCOPIA_HOME` → catalog empty + POST 400; unknown family → 400 with its name; empty families → 400; UI shows the messages (quickstart §4, §5).

### Tests for US4 (write first, MUST fail)

- [X] T030 [P] [US4] Add `CardErrorCasesTest` in `backend/src/test/java/com/cornucopai/api/CardErrorCasesTest.java` — repo unconfigured → `GET /api/card-catalog` `sets: []` and POST → 400 (FR-011); unknown `setId:familyId` → 400 naming it (FR-012); `families: []` → 400 (FR-006); no atelier folder created on 400
- [X] T031 [P] [US4] Extend `frontend/src/ConfigureGameDialog.spec.ts` + `App.spec.ts` — shows an "empty catalog / dépôt non configuré" message when `sets` is empty; shows the server `error` from a 400 in a `v-alert`

### Implementation for US4

- [X] T032 [US4] Backend: `CardCatalogService` returns empty catalog + logs when home unset/invalid; `AtelierResource`/`AtelierService` return 400 with explicit messages and create no atelier dir (FR-011/FR-012) — files `backend/src/main/java/com/cornucopai/game/card/CardCatalogService.java`, `backend/src/main/java/com/cornucopai/api/AtelierResource.java`, `backend/src/main/java/com/cornucopai/game/AtelierService.java`
- [X] T033 [US4] Frontend: surface empty-catalog and 400 errors — `frontend/src/components/ConfigureGameDialog.vue`, `frontend/src/App.vue`

**Checkpoint**: US4 functional — errors handled and visible.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T034 [P] Run `quickstart.md` scenarios 1–8 against the real repo (`export CORNUCOPIA_HOME=/Users/francois/cornucopia`) and record outcomes in `specs/003-card-set-selection/quickstart.md`
- [X] T035 [P] Run full gates: `cd backend && ./mvnw test`, `cd frontend && npm test && npm run lint && npm run build` — all green
- [X] T036 [P] Add the diary/billet passage (constitution) capturing the catalog + language-fallback decisions (R-011…R-020) under `diary/`
- [X] T037 Update `README.md` with the `CORNUCOPIA_HOME` setup step; confirm `.gitignore` covers `backend/ateliers/` and the fixture is under test resources

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies — start immediately
- **Foundational (Phase 2)**: depends on Setup — BLOCKS all user stories
- **US1 (Phase 3)**, **US2 (Phase 4)**: depend on Foundational; independent of each other
- **US3 (Phase 5)**: depends on Foundational + US1 (catalog) + US2 (families/language plumbing) — needs both to build the deck
- **US4 (Phase 6)**: depends on Foundational + US1/US2 — validates the same inputs
- **Polish (Phase 7)**: depends on all stories

### User Story Dependencies

- **US1 (P1)**: after Foundational only — catalog listing
- **US2 (P1)**: after Foundational — selection + language transmission (can parallel US1)
- **US3 (P1)**: after US1 + US2 — real-card dealing and play
- **US4 (P2)**: after US1 + US2 — error handling on the same inputs

### Within Each User Story

- Tests MUST be written and FAIL before implementation (constitution II)
- Models before services; services before endpoints; core before integration
- Story checkpoint before moving to the next

### Parallel Opportunities

- Phase 1: T001–T004 in parallel (different files)
- Phase 2: T005, T006 (tests) in parallel; then T007, T008, T010 in parallel; T009 after T007/T008
- Phase 3: T011, T012 in parallel; then T013, T014 in parallel
- Phase 4: T015, T016 in parallel; then T017, T018, T019
- Phase 5: T020–T023 in parallel; then T024, T025, then T026, T027, T028, T029
- Phase 6: T030, T031 in parallel; then T032, T033
- Phase 7: T034–T037 in parallel where independent

## Parallel Example: User Story 3 (Phase 5)

```bash
# Launch all US3 tests together (they fail — Card/deck service don't exist yet):
Task: "CardDeckServiceTest in backend/src/test/java/com/cornucopai/game/card/CardDeckServiceTest.java"
Task: "Update GameMasterTest + GameEndTest in backend/src/test/java/com/cornucopai/game/"
Task: "Update 4 agent tests in backend/src/test/java/com/cornucopai/game/agent/"
Task: "Extend AtelierResourceTest deck/language/sets assertions"

# Then implement, one file each where possible:
Task: "CardDeckService in backend/src/main/java/com/cornucopai/game/card/CardDeckService.java"
Task: "PlayerAgent.playCard(Card,…) in backend/src/main/java/com/cornucopai/game/PlayerAgent.java"
Task: "GameMaster + AtelierService Card switch; delete PlaceholderCards"
Task: "Atelier model deck: List<Card> + language/selectedFamilies/sets"
Task: "4 archetype agents playCard(Card,…)"
```

## Implementation Strategy

### MVP First (catalog + selection + real cards = playable game with real cards)

1. Phase 1: Setup
2. Phase 2: Foundational (CRITICAL — blocks all stories)
3. Phase 3: US1 (catalog listing)
4. Phase 4: US2 (family + language selection)
5. Phase 5: US3 (real cards dealt and played) → **STOP and VALIDATE** with quickstart §1–§3, §7 → this is the MVP: a game played with real Cornucopia cards
6. Phase 6: US4 (error handling)
7. Phase 7: Polish, diary, gates

### Incremental Delivery

Each phase is a checkpoint: Setup+Foundational → US1 → US2 → US3 (MVP) → US4 → gates. Every change traces to a task ID and to the FR/SC it serves.

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps each task to its user story (US1–US4 = spec.md stories)
- TDD per constitution II: red → green → refactor; no production code without a failing test first
- Verify tests fail before implementing each phase
- Commit after each task or logical group
- Stop at each checkpoint to validate the story independently (quickstart.md scenarios)
- Avoid: vague tasks, same-file conflicts, hidden cross-story dependencies
