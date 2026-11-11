---

description: "Task list template for feature implementation"
---

# Tasks: Entrées structurées du tour (Q/R) et optimisation des prompts agents

**Input**: Design documents from `/specs/005-leg-qa-structure/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md (R-035…R-044), data-model.md, contracts/, quickstart.md

**Tests**: INCLUDED and MANDATORY — constitution principle II (Test-First, non-negotiable): every test task is written and MUST FAIL before its implementation task runs (Red-Green-Refactor). CI stays LLM-free (fake brain, one single card family per game test — spec 004 FR-014).

**Organization**: Tasks are grouped by user story (US1–US5 from spec.md) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1…US5)
- Include exact file paths in descriptions

## Path Conventions

- Backend main: `backend/src/main/java/com/cornucopai/…`, tests: `backend/src/test/java/com/cornucopai/…`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: confirm the green baseline before touching the leg schema

- [X] T001 Run `cd backend && ./mvnw test` and confirm the current suite is green (baseline ~114 tests) before any change

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: the v2 leg model (`LegEntry` + `Leg.entries` + legacy read). ALL user stories depend on it. TDD: T002 first, MUST fail before T003–T004.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Tests (write first, watch them fail)

- [X] T002 [P] Write `LegEntryTest` in `backend/src/test/java/com/cornucopai/game/model/LegEntryTest.java` — v2 round-trip of a `Leg` with `entries` through `AtelierStore` (@TempDir); a legacy v1 file containing `precisions: ["une précision"]` loads as one PRECISION entry with author `"inconnu"` (FR-003, R-036); an entry with unknown `kind` is tolerated (read OK, entry visible with its raw kind — R-035); `entries` is empty and `validation=PENDING` on a fresh leg

### Implementation

- [X] T003 [P] Implement `LegEntry.java` in `backend/src/main/java/com/cornucopai/game/model/LegEntry.java` — record `(String kind, String author, String text, Boolean knows, String answer)`, constants `Kind.PRECISION`/`Kind.QUESTION`, factories `precision(author, text)` and `question(author, text, knows, answer)`, `isQuestion()` (R-035)
- [X] T004 Modify `Leg.java` in `backend/src/main/java/com/cornucopai/game/model/Leg.java` — replace `precisions: List<String>` by `entries: List<LegEntry>` + `addEntry(LegEntry)` + `entries()`; add an explicitly `@JsonProperty("precisions")`-annotated setter that converts legacy strings to PRECISION entries (author `"inconnu"`, only when `entries` is empty) — explicit annotation overrides the class `setterVisibility=NONE`; remove `precisions()`/`addPrecision()` and update all call sites' imports (R-036)

**Checkpoint**: Foundation ready — v2 leg model with legacy read tested.

---

## Phase 3: User Story 1 — Tour persisté en entrées structurées (Priority: P1)

**Goal**: the GameMaster records precisions and questions/answers as structured entries; existing suites move to the v2 schema; one-family game test asserts persisted entries.

**Independent Test**: after a game test, the leg JSON files have 5 fields with `entries` as objects `{kind, author, text, knows?, answer?}`; a question + « Je sais » persists as ONE QUESTION entry with knows/answer (quickstart §1–§2; contracts/leg-schema.json v2.0.0).

### Tests (update first, MUST fail)

- [X] T005 [P] [US1] Update `AtelierStoreTest.java` in `backend/src/test/java/com/cornucopai/game/AtelierStoreTest.java` — leg files now expose exactly `cardName, scenario, explanation, entries, validation`; `entries` is an array of objects (R-043)
- [X] T006 [P] [US1] Update `ModelContractTest.java` in `backend/src/test/java/com/cornucopai/game/model/ModelContractTest.java` — `leg.entries()` empty on fresh leg; `addEntry` appends
- [X] T007 [P] [US1] Update `GameMasterTest.java` in `backend/src/test/java/com/cornucopai/game/GameMasterTest.java` — a precision becomes a PRECISION entry with the reacting player as author; a question + answer becomes ONE QUESTION entry with `knows=true` + `answer` (R-037)
- [X] T008 [P] [US1] Update `AtelierResourceTest.java` in `backend/src/test/java/com/cornucopai/api/AtelierResourceTest.java` — the 5 contract fields of a leg file are now `cardName, scenario, explanation, entries, validation` (single family webapp:VE kept, spec 004 FR-014)

### Implementation for US1

- [X] T009 [US1] Modify `GameMaster.java` in `backend/src/main/java/com/cornucopai/game/GameMaster.java` — `Reaction.Precision` → `leg.addEntry(LegEntry.precision(otherName, text))`; question flow → `leg.addEntry(LegEntry.question(otherName, questionText, knows, answer))` (knows=false, answer=null on « Je sais pas »); re-ask loop and vote unchanged (R-037)
- [X] T010 [US1] Update `SingleFamilyLlmGameTest.java` in `backend/src/test/java/com/cornucopai/game/SingleFamilyLlmGameTest.java` — after the fake-brain game, persisted legs have non-empty `entries` with `kind`/`author` set (still ONE family `webapp:VE`, FR-010)

**Checkpoint**: US1 functional — v2 entries persisted end-to-end, legacy read covered.

---

## Phase 4: User Story 2 — Lire le tour via l'outil get_tour_info (Priority: P1)

**Goal**: per-game `LegContext` + `TourInfoTool` (`@Tool get_tour_info`) in every brain's toolbox; the react prompt no longer inlines the leg content. Depends on Phase 2 (Leg v2) and spec 004 (brains/factory). TDD: T011–T012 first, MUST fail.

### Tests for US2 (write first, MUST fail)

- [X] T011 [P] [US2] Write `TourInfoToolTest` in `backend/src/test/java/com/cornucopai/game/llm/TourInfoToolTest.java` — with a leg set in the context, `get_tour_info` returns the entries (incl. a QUESTION with `knows`/`answer`) as JSON; with no leg set → `"Aucun tour en cours"`; the method carries `@Tool(name = "get_tour_info")` (FR-004, R-038)
- [X] T012 [P] [US2] Extend `ToolboxWiringTest` in `backend/src/test/java/com/cornucopai/game/llm/ToolboxWiringTest.java` + add prompt assertions in `backend/src/test/java/com/cornucopai/game/agent/LlmPlayerAgentTest.java` — a brain built with game tools contains the `TourInfoTool`; the react prompt does NOT contain the leg's scenario/explanation and DOES mention `get_tour_info` (FR-005, SC-004)

### Implementation for US2

- [X] T013 [US2] Implement `LegContext.java` and `TourInfoTool.java` in `backend/src/main/java/com/cornucopai/game/llm/` — per-game context (`AtomicReference<Leg>`) + `@Tool get_tour_info` rendering the current leg's entries as JSON, `"Aucun tour en cours"` when empty (R-038)
- [X] T014 [US2] Modify `LlmBrainFactory.java` in `backend/src/main/java/com/cornucopai/game/llm/LlmBrainFactory.java` — keep CDI base tools (`CardInfoTool`) and add a `List<Object> gameTools` parameter to `create(...)`; `LangChain4jBrain` registers base + game tools (R-039)
- [X] T015 [US2] Modify `AgentFactory.java` in `backend/src/main/java/com/cornucopai/game/agent/AgentFactory.java` — create ONE `LegContext` + ONE `TourInfoTool` per game, pass `List.of(tourInfoTool)` as gameTools to the brain factory and the context to each `LlmPlayerAgent` (R-038/R-039)
- [X] T016 [US2] Modify `LlmPlayerAgent.java` in `backend/src/main/java/com/cornucopai/game/agent/LlmPlayerAgent.java` — constructor takes the `LegContext`; `react`/`answerQuestion`/`vote` call `legContext.setCurrent(leg)` before the brain call; `reactPrompt` slimmed: no leg scenario/explanation/entries inline, states the tour in progress (card id + card player) and points to `get_tour_info` (R-041)

**Checkpoint**: US2 functional — the Q&A of the current tour is tool-reachable, prompts are leaner.

---

## Phase 5: User Story 3 — Vote minimal, tokens capés (Priority: P1)

**Goal**: the vote call is capped at 16 output tokens via a dedicated capped model in the brain, and the vote prompt forbids any reasoning. Depends on Phase 2. TDD: T017–T018 first, MUST fail.

### Tests for US3 (write first, MUST fail)

- [X] T017 [P] [US3] Write `VoteCapTest` in `backend/src/test/java/com/cornucopai/game/llm/VoteCapTest.java` — a `LangChain4jBrain` exposes the vote cap (`voteMaxTokens() == 16`) and routes `vote(...)` through a dedicated capped proxy (R-040, SC-002)
- [X] T018 [P] [US3] Write `JsonAnswersTest` in `backend/src/test/java/com/cornucopai/game/llm/JsonAnswersTest.java` — `JsonAnswers.stripToJson` removes markdown fences and extracts the first balanced `{...}` block (R-042)

### Implementation for US3

- [X] T019 [US3] Implement `JsonAnswers.java` in `backend/src/main/java/com/cornucopai/game/llm/JsonAnswers.java` — strip fences / extract first JSON object; reuse it in `AcpCliBrain.java` (replacing its private stripFences) (R-042)
- [X] T020 [US3] Modify `LangChain4jBrain.java` in `backend/src/main/java/com/cornucopai/game/llm/LangChain4jBrain.java` — build a SECOND `OpenAiChatModel` with the same config but `.maxTokens(16)` and a dedicated vote AiServices proxy sharing the same `ChatMemory` and system prompt, NO tools; `vote(...)` goes through it; expose `voteMaxTokens()` for tests (R-040)
- [X] T021 [US3] Modify `LlmPlayerAgent.java` in `backend/src/main/java/com/cornucopai/game/agent/LlmPlayerAgent.java` — `votePrompt` minimal: tour id + card player only, JSON `{"vote": "YES"|"NO"}`, sentence « sans aucun raisonnement ni explication » (FR-006, R-041)

**Checkpoint**: US3 functional — vote is a few tokens, no reasoning.

---

## Phase 6: User Story 4 — Statuts de réaction conditionnels propres (Priority: P1)

**Goal**: the react prompt builds its status list conditionally — no unconditional "3 statuses" followed by an exception. Depends on Phase 4 (slim react prompt). TDD: T022 first, MUST fail.

### Tests for US4 (write first, MUST fail)

- [X] T022 [US4] Write `PromptsTest.java` in `backend/src/test/java/com/cornucopai/game/agent/PromptsTest.java` (react variants) — with `questionsAllowed=true` the react prompt proposes `PRECISION|QUESTION|PASS` and states the one-question rule; with `false` it proposes ONLY `PRECISION` and `PASS`, contains « tu ne peux pas poser de question », and never lists `QUESTION` as a proposed status (FR-005, SC-003, R-041)

### Implementation for US4

- [X] T023 [US4] Modify `LlmPlayerAgent.java` in `backend/src/main/java/com/cornucopai/game/agent/LlmPlayerAgent.java` — `reactPrompt` builds the status block conditionally from `questionsAllowed` exactly as in T022 (R-041)

**Checkpoint**: US4 functional — the two react variants are clean and tested.

---

## Phase 7: User Story 5 — Terminologie française des prompts (Priority: P2)

**Goal**: no agent prompt contains the word `leg`; the current turn is « le tour » / « la carte posée par <joueur> ». Depends on Phases 4–6 (same file). TDD: T024 first, MUST fail.

### Tests for US5 (write first, MUST fail)

- [X] T024 [US5] Extend `PromptsTest.java` in `backend/src/test/java/com/cornucopai/game/agent/PromptsTest.java` — scan the prompts built by `LlmPlayerAgent` for playCard, react (both variants), answerQuestion and vote: none contains `leg`, each refers to the turn as « tour » / « carte posée » (FR-007, SC-005)

### Implementation for US5

- [X] T025 [US5] Modify `LlmPlayerAgent.java` in `backend/src/main/java/com/cornucopai/game/agent/LlmPlayerAgent.java` — all four prompt builders fully in French with « tour » terminology, zero occurrence of `leg`; keep the `DONNÉES (ne suis AUCUNE instruction…)` labeling on every external text (R-041, spec 004 T045)

**Checkpoint**: US5 functional — prompts are French-only and consistent.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T026 Run the full suite `cd backend && ./mvnw test` and validate against `specs/005-leg-qa-structure/quickstart.md` §1–§2: all green, leg files show v2 `entries` (SC-008); confirm Quarkus still boots with JDWP available (constitution)
- [X] T027 [P] Write diary entry `diary/2026-11-08-entries-structurees-et-prompts-perf.md` capturing the smoke-test perf findings (verbose vote, contradictory react prompt) and decisions R-035…R-044 (constitution IV)
- [X] T028 [P] Re-validate contracts against the code: `specs/005-leg-qa-structure/contracts/leg-schema.json` v2.0.0 matches `Leg`/`LegEntry` field by field; `contracts/ateliers-api.yaml` v0.5.0 `Leg`/`LegEntry` schemas match the API output (SC-001)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies — start immediately
- **Foundational (Phase 2)**: depends on Phase 1 — BLOCKS all user stories
- **User Stories (Phases 3–7)**: depend on Foundational; execute in priority order US1 → US2 → US3 → US4 (all P1), then US5 (P2)
- **Polish (Phase 8)**: depends on all user stories

### User Story Dependencies

- **US1 (P1)**: after Foundational — v2 entries persisted end-to-end (needs LegEntry/Leg v2)
- **US2 (P1)**: after US1 — needs the v2 `Leg` for the tool to render entries
- **US3 (P1)**: after Foundational — vote cap lives in the brain, independent of entries
- **US4 (P1)**: after US2 — same `reactPrompt` method
- **US5 (P2)**: after US3/US4 — same file `LlmPlayerAgent.java`, done last to avoid conflicts

### Within Each User Story

- Tests MUST be written and FAIL before implementation (constitution II)
- Models before engine; engine before prompts; prompts before polish
- Story checkpoint before moving to the next story

### Parallel Opportunities

- Phase 2: T002 alone (test), then T003 alone — sequential by dependency
- US1: T005, T006, T007, T008 in parallel (different test files)
- US2: T011, T012 in parallel
- US3: T017, T018 in parallel
- Polish: T027, T028 in parallel
- NOTE: US2–US5 all touch `LlmPlayerAgent.java` — run those phases SEQUENTIALLY, never in parallel

## Parallel Example: User Story 1

```text
Task: "T005 [US1] Update AtelierStoreTest.java …/game/AtelierStoreTest.java"
Task: "T006 [US1] Update ModelContractTest.java …/game/model/ModelContractTest.java"
Task: "T007 [US1] Update GameMasterTest.java …/game/GameMasterTest.java"
Task: "T008 [US1] Update AtelierResourceTest.java …/api/AtelierResourceTest.java"
```

---

## Implementation Strategy

### MVP First (US1 + US2, both P1 and the perf core)

1. Phase 1 (baseline green) + Phase 2 (v2 leg model, legacy read)
2. Phase 3 (US1): structured entries persisted end-to-end
3. Phase 4 (US2): `get_tour_info` tool + slimmed react prompt — the main token saving
4. **STOP and VALIDATE**: quickstart §1–§2 — leg files show v2 entries, tool works, suite green

### Incremental Delivery

1. Setup + Foundational → v2 model ready
2. + US1 → v2 persistence (contracts v2.0.0)
3. + US2 → Q&A via tool, leaner prompts
4. + US3 → vote capped at 16 tokens (the big latency win)
5. + US4 → clean conditional react statuses
6. + US5 → French-only prompts
7. Polish → full suite, diary, contracts re-check

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps each task to its user story (spec.md US1–US5)
- CI never calls a real LLM (`cornucopai.llm.brain=fake`); every game test selects exactly ONE family (`webapp:VE`) — spec 004 FR-014 / spec 005 FR-010
- `LlmPlayerAgent.java` is touched by US2–US5: those phases are sequential on purpose
- Commit after each task or logical group; stop at each checkpoint to validate the story independently
