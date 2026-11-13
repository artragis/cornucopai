---

description: "Task list for feature 009-configurable-agents — agents configurables en objets JSON, page /agents dédiée, sélection d'agents dans le formulaire d'atelier"
---

# Tasks: Agents configurables (objets JSON) + page de gestion + sélection dans le formulaire

**Input**: Design documents from `/specs/009-configurable-agents/`

**Prerequisites**: plan.md (required), spec.md (required for user stories, incl. Clarifications session 2026-11-12), research.md (R-001…R-012), data-model.md, contracts/ (agents-api.yaml v1.0.0, ateliers-api.yaml v0.8.0, agent-schema.json, atelier-schema.json v0.8.0), quickstart.md

**Tests**: INCLUDED and MANDATORY — constitution principle II (Test-First, non-negotiable): every test task is written and MUST FAIL before its implementation task runs (Red-Green-Refactor). Backend CI stays LLM-free (fake brain `cornucopai.llm.brain=fake` in test profile — spec 004 R-031; one single card family per game test — FR-014 of spec 004; MCP mocked — spec 007 R-055). Frontend tests use Vitest.

**Organization**: Tasks are grouped by user story (US1–US3 from spec.md) to enable independent implementation and testing of each story. US2 depends on US1 (the form fetches the agent list from `/api/agents`); US3 depends on US2 (the launch path rework).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1…US3)
- Include exact file paths in descriptions

## Path Conventions

- Backend main: `backend/src/main/java/com/cornucopai/…`, tests: `backend/src/test/java/com/cornucopai/…`
- Backend config: `backend/src/main/resources/application.properties`, test config: `backend/src/test/resources/application.properties`
- Frontend: `frontend/src/…`, tests colocated (`*.spec.ts`)
- Spec artifacts: `specs/009-configurable-agents/…`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: confirm the green baseline before touching anything

- [x] T001 Run `cd backend && ./mvnw test` and `cd frontend && npm test` and confirm both suites are green (baseline) before any change

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: the `Agent` model + filesystem store + validation service + config. ALL user stories depend on these (US1 exposes them via HTTP/UI, US2/US3 consume them at launch). TDD: T002–T004 first, MUST fail before T005–T008.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Tests (write first, watch them fail)

- [x] T002 [P] Write `AgentModelTest` in `backend/src/test/java/com/cornucopai/game/model/AgentModelTest.java` — `Agent` record round-trips through Jackson per `contracts/agent-schema.json`; field constraints verbatim: `id` format uuid, `agentName` minLength 1, `openAiCompatibleUrl` pattern `^https?://`, `apiKeyVar` pattern `^$|^[A-Za-z_][A-Za-z0-9_]*$` (default empty), `modelName` minLength 1, `personalityPrompt` minLength 1 (R-003, FR-019)
- [x] T003 [P] Write `AgentStoreTest` in `backend/src/test/java/com/cornucopai/game/AgentStoreTest.java` — CRUD in a `@TempDir` root: create writes exactly one file `{id}.json` (atomic: write tmp then move, R-001), list returns all sorted by `agentName`, get unknown id → `Optional.empty()`, update overwrites the same file (id unchanged, FR-003), delete removes the file (FR-005)
- [x] T004 [P] Write `AgentServiceTest` in `backend/src/test/java/com/cornucopai/game/AgentServiceTest.java` — FR-019 validation verbatim: `agentName`, `openAiCompatibleUrl`, `modelName`, `personalityPrompt` required and non-blank after trim; `openAiCompatibleUrl` must be an absolute `http`/`https` URL; `apiKeyVar` empty OR matching `^[A-Za-z_][A-Za-z0-9_]*$`; FR-018 duplicate `agentName` → conflict exception (409 at HTTP layer); create assigns a `UUID.randomUUID()` id (FR-003); update with a different id → rejected (FR-003)

### Implementation

- [x] T005 [P] Implement `Agent.java` in `backend/src/main/java/com/cornucopai/game/model/Agent.java` — record `{String id, String agentName, String openAiCompatibleUrl, String apiKeyVar, String modelName, String personalityPrompt}`, Jackson-annotated like other model records; compact constructor validates the FR-019 constraints (R-003)
- [x] T006 [P] Implement `AgentStore.java` in `backend/src/main/java/com/cornucopai/game/AgentStore.java` — mirrors `AtelierStore` pattern: root dir from `cornucopai.agents.root-dir` (default `agents/`), one `{id}.json` per agent, atomic write, list/get/update/delete (R-001)
- [x] T007 [P] Implement `AgentService.java` in `backend/src/main/java/com/cornucopai/game/AgentService.java` — orchestrates store + uniqueness check on `agentName` (trimmed value, FR-018) + id immutability on update (FR-003) + UUID generation on create; throws typed exceptions mapped to 400/404/409 by the resource
- [x] T008 [P] Config + gitignore — add `cornucopai.agents.root-dir=${AGENTS_ROOT_DIR:agents}` to `backend/src/main/resources/application.properties`, `cornucopai.agents.root-dir=target/test-agents` to `backend/src/test/resources/application.properties`, and `agents/` + `backend/agents/` to `.gitignore` next to the existing `ateliers/` entries (R-001)

**Checkpoint**: Foundation ready — `Agent`/`AgentStore`/`AgentService` compile, their tests green, config bound.

---

## Phase 3: User Story 1 — Page de gestion des agents + CRUD /api/agents (Priority: P1) 🎯 MVP

**Goal**: dedicated `/agents` page (list/create/edit/delete) backed by `AgentResource` `@Path("/api/agents")` per `contracts/agents-api.yaml` v1.0.0 (FR-001…FR-005). Depends on Phase 2.

**Independent Test**: `AgentResourceTest` covers 201+Location/400/409/404/204 (SC-001, SC-002); `AgentsPage.spec.ts` covers list + create + edit (id unchanged) + delete; navigating to `/agents` works from the home nav button.

### Tests (write first, MUST fail)

- [x] T009 [P] [US1] Write `AgentResourceTest` in `backend/src/test/java/com/cornucopai/api/AgentResourceTest.java` — REST Assured against `contracts/agents-api.yaml` v1.0.0: `POST /api/agents` → 201 + `Location: /api/agents/{id}` + body with server-generated uuid (FR-003); 400 on each FR-019 violation (blank agentName, non-http URL, blank modelName, blank personalityPrompt, invalid apiKeyVar pattern); 409 on duplicate agentName (FR-018); `GET /api/agents` → 200 sorted by agentName; `GET /api/agents/{id}` → 200 / 404; `PUT /api/agents/{id}` → 200 with same id, 400 if body id ≠ path id (FR-003), 404 unknown; `DELETE /api/agents/{id}` → 204 then GET → 404 (FR-005)
- [x] T010 [P] [US1] Write `agentsApi.spec.ts` in `frontend/src/services/agentsApi.spec.ts` — mocked fetch asserts methods/URLs/bodies for listAgents/createAgent/getAgent/updateAgent/deleteAgent against `/api/agents*`
- [x] T011 [P] [US1] Write `AgentsPage.spec.ts` in `frontend/src/pages/AgentsPage.spec.ts` — renders the agent list (names); create dialog validates required fields client-side (FR-019) and posts; edit keeps the same id in the PUT (FR-004); delete asks confirmation then row disappears (FR-005)
- [x] T012 [P] [US1] Update `router.spec.ts` in `frontend/src/router.spec.ts` — route `/agents` exists and resolves to `AgentsPage`
- [x] T013 [P] [US1] Write `HomePage.spec.ts` in `frontend/src/HomePage.spec.ts` — a "Gérer les agents" nav button links to `/agents`

### Implementation for US1

- [x] T014 [P] [US1] Implement `AgentDto.java` in `backend/src/main/java/com/cornucopai/api/dto/AgentDto.java` — `AgentInput` (agentName, openAiCompatibleUrl, apiKeyVar, modelName, personalityPrompt) + `Agent` adds `id`; mapping to/from `game.model.Agent` (never exposes any API key value — only the var name, FR-006)
- [x] T015 [US1] Implement `AgentResource.java` in `backend/src/main/java/com/cornucopai/api/AgentResource.java` — `@Path("/api/agents")` with GET list / POST create (201 + Location) / GET `{id}` / PUT `{id}` / DELETE `{id}`; maps service exceptions to 400/404/409 per contract v1.0.0
- [x] T016 [P] [US1] Implement `agentsApi.ts` in `frontend/src/services/agentsApi.ts` — TS `AgentDto`/`Agent` types + CRUD fetch functions (Vite proxy `/api` → 8080 already configured)
- [x] T017 [US1] Implement `AgentsPage.vue` in `frontend/src/pages/AgentsPage.vue` — Vuetify list of agents + create/edit `v-dialog` with `v-form` (required fields per FR-019) + delete with confirmation; error alerts from 400/409 bodies
- [x] T018 [P] [US1] Wire navigation — add `{ path: '/agents', component: AgentsPage }` in `frontend/src/router.ts` and a "Gérer les agents" button in `frontend/src/HomePage.vue` card actions

**Checkpoint**: US1 functional — full CRUD over HTTP and in the UI, id stable, names unique, tests green (SC-001, SC-002).

---

## Phase 4: User Story 2 — Sélection d'agents dans le formulaire d'atelier + lancement par `agentIds[]` (Priority: P1)

**Goal**: `POST /api/ateliers` v0.8.0 takes `agentIds[]` (min 2, duplicates allowed) instead of `playerCount`/`agents`; backend resolves agents, draws participant order randomly (FR-020), names players `Joueur-N` after the shuffle (FR-016), and persists `agentSnapshots[]` (FR-017); the form's player-count field becomes an accessible multi-select of agents (FR-007/FR-008/FR-009/FR-010). Depends on Phase 2 + US1.

**Independent Test**: reworked `AgentConfigResolverTest` (unknown id → error, duplicate id → two participants same agentId, seeded shuffle keeps association, min 2); reworked `AtelierResourceAgentsTest` (201 with players carrying agentId + Joueur-N, 400 unknown id / size < 2 / missing env var FR-013); `atelier.json` contains `agentSnapshots[]` without any key value (FR-017); legacy atelier still reads (FR-014); `ConfigureGameDialog.spec.ts` (selector min 2, same agent twice, empty state links `/agents`, keyboard/ARIA).

### Tests (write first, MUST fail)

- [x] T019 [P] [US2] Rework `AgentConfigResolverTest` in `backend/src/test/java/com/cornucopai/game/llm/AgentConfigResolverTest.java` — `resolve(List<String> agentIds)` via `AgentStore`: unknown id → `IllegalArgumentException` (→400); `agentIds.size() < 2` → error (FR-010); duplicate id allowed → two participants with the same `agentId` (FR-009); with a seeded `Random`, participant↔agent association preserved but order shuffled and names are `Joueur-N` assigned AFTER the shuffle (FR-016/FR-020)
- [x] T020 [P] [US2] Rework `AtelierAgentConfigsPersistenceTest` in `backend/src/test/java/com/cornucopai/game/AtelierAgentConfigsPersistenceTest.java` — after a fake-brain game launched with `agentIds`, `ateliers/<id>/atelier.json` contains `agentSnapshots[]` index-aligned with `players[]`, each snapshot = `{id, agentName, openAiCompatibleUrl, apiKeyVar, modelName, personalityPrompt}` and contains NO apiKey value (FR-017, SC-005); editing/deleting the agent afterwards does not alter the persisted atelier
- [x] T021 [P] [US2] Update `ModelContractTest` in `backend/src/test/java/com/cornucopai/game/model/ModelContractTest.java` — `Participant` gains `agentId` and a legacy JSON with `archetype` still deserializes (unknown/legacy field tolerated, FR-014); `Atelier` gains `agentSnapshots` and a legacy `atelier.json` (with `agentConfigs`, without snapshots) reads with an empty list; validate against `contracts/atelier-schema.json` v0.8.0
- [x] T022 [P] [US2] Rework `AtelierResourceAgentsTest` in `backend/src/test/java/com/cornucopai/api/AtelierResourceAgentsTest.java` — `POST /api/ateliers` with `agentIds: [idA, idA]` → 201, `players` = two entries with `agentId=idA` and distinct `Joueur-N` names (FR-009/FR-016); unknown agentId → 400; `agentIds` of size 1 → 400 (FR-010); agent with `apiKeyVar` set but env var absent → 400 whose message names the missing variable (FR-013, SC-009)
- [x] T023 [P] [US2] Rework `ConfigureGameDialog.spec.ts` in `frontend/src/ConfigureGameDialog.spec.ts` — the player-count input is GONE; a multi-select (`v-autocomplete` multiple chips) lists agents from mocked `agentsApi.listAgents()` (item-title `agentName`, item-value `id`); launch requires ≥ 2 selections (FR-010); the same agent may be selected twice (FR-009); with zero agents, an empty-state message links to `/agents`; options are keyboard-focusable with ARIA labels (FR-008, SC-008); emitted launch payload is `{agentIds, featureHtml, language, families, inheritFactsFrom?}`

### Implementation for US2

- [x] T024 [P] [US2] Implement `AgentSnapshot.java` in `backend/src/main/java/com/cornucopai/game/llm/AgentSnapshot.java` — record `{String id, String agentName, String openAiCompatibleUrl, String apiKeyVar, String modelName, String personalityPrompt}` (persisted, never contains the key value — FR-017)
- [x] T025 [P] [US2] Modify `Participant.java` in `backend/src/main/java/com/cornucopai/game/model/Participant.java` — becomes `(String name, String agentId)`; legacy `archetype` in old JSON is ignored on read (Quarkus Jackson ignores unknown properties by default, FR-014)
- [x] T026 [US2] Modify `Atelier.java` + `AtelierStore.java` in `backend/src/main/java/com/cornucopai/game/model/Atelier.java` and `backend/src/main/java/com/cornucopai/game/AtelierStore.java` — add `List<AgentSnapshot> agentSnapshots` persisted in `atelier.json`; keep legacy `agentConfigs` readable, deprecated, no longer written (R-005/R-011)
- [x] T027 [US2] Modify `CreateAtelierRequest.java` in `backend/src/main/java/com/cornucopai/api/dto/CreateAtelierRequest.java` — remove `playerCount` and `agents`, add `List<String> agentIds` (contract v0.8.0, R-004)
- [x] T028 [US2] Rework `AgentConfigResolver.java` in `backend/src/main/java/com/cornucopai/game/llm/AgentConfigResolver.java` — `resolve(List<String> agentIds)`: load agents from `AgentStore` (unknown → IllegalArgumentException), shuffle participants with the injected `Random` (FR-020), name `Joueur-N` after shuffle (FR-016), build `AgentSnapshot` per participant (FR-017)
- [x] T029 [US2] Modify `AtelierService.java` in `backend/src/main/java/com/cornucopai/game/AtelierService.java` — use the new resolver, playerCount = `agentIds.size()`, persist snapshots, pass snapshots to the agent factory
- [x] T030 [US2] Modify `AtelierResource.java` in `backend/src/main/java/com/cornucopai/api/AtelierResource.java` — POST reads `agentIds`, maps resolver errors to 400 (incl. FR-013 message naming the missing env var)
- [x] T031 [P] [US2] Update DTOs — `AtelierResponse.java` / `ParticipantDto` in `backend/src/main/java/com/cornucopai/api/dto/` and `frontend/src/services/atelierApi.ts`: `CreateAtelierRequest {agentIds, featureHtml, language, families, inheritFactsFrom?}`, `ParticipantDto {name, agentId, archetype? deprecated}`
- [x] T032 [P] [US2] Modify `ConfigureGameDialog.vue` in `frontend/src/components/ConfigureGameDialog.vue` — replace the player-count input with a `v-autocomplete` (`multiple`, `chips`, `closable-chips`, label "Agents participants", items = agents from `agentsApi`, `item-title="agentName"`, `item-value="id"`), min 2 (FR-010), empty-state alert with router-link to `/agents`, launch emits `agentIds` (FR-007/FR-008)
- [x] T033 [US2] Modify `HomePage.vue` in `frontend/src/HomePage.vue` — `onLaunch` payload type gains `agentIds: string[]`, drops `playerCount`

**Checkpoint**: US2 functional — the form selects agents, POST v0.8.0 works, snapshots persisted without key values, legacy ateliers still readable (SC-003, SC-004, SC-005, SC-010).

---

## Phase 5: User Story 3 — Parties 100 % pilotées par LLM, archetype/ACP retirés du déroulement (Priority: P2)

**Goal**: each participant is driven exclusively by its agent snapshot's LLM config (FR-011): `OpenAiChatModel` with `baseUrl = openAiCompatibleUrl`, `modelName`, apiKey resolved via env seam `getenv(apiKeyVar)` (FR-006/FR-013), system prompt = base game prompt + `personalityPrompt` (FR-012). The ACP branch, `providerType`, and archetype prompts leave the launch path. Depends on US2.

**Independent Test**: reworked `LlmBrainFactoryTest` (brain built from snapshot: baseUrl/apiKey/model from the agent; missing env var → error naming it FR-013; no ACP branch FR-011; prompt contains personalityPrompt, no archetype FR-012); `SystemPromptsTest` updated; `SingleFamilyLlmGameTest` still green with fake brains and proves decisions come from the agent's LLM config (SC-007); deterministic agent tests (Grumpy/Happy/Greedy/Prof) pass unchanged, test-only.

### Tests (write first, MUST fail)

- [x] T034 [P] [US3] Rework `LlmBrainFactoryTest` in `backend/src/test/java/com/cornucopai/game/llm/LlmBrainFactoryTest.java` — brain created from an `AgentSnapshot`: `OpenAiChatModel` gets `baseUrl = snapshot.openAiCompatibleUrl()` and `modelName = snapshot.modelName()`; apiKey resolved through an injectable env seam (default `System::getenv`, tests inject a map) — `apiKeyVar` empty → no key (FR-006), set + present → key used, set + absent → exception naming the variable (FR-013); NO ACP/providerType branch exists (FR-011); system prompt = base + `personalityPrompt`, contains no archetype text (FR-012)
- [x] T035 [P] [US3] Update `SystemPromptsTest` in `backend/src/test/java/com/cornucopai/game/llm/SystemPromptsTest.java` — `forArchetype` removed from the game flow (or deprecated test-only); new `forPersonality(String personalityPrompt)` = base rules + output contract + personality prose (FR-012)
- [x] T036 [P] [US3] Update `SingleFamilyLlmGameTest` in `backend/src/test/java/com/cornucopai/game/SingleFamilyLlmGameTest.java` + `LlmPlayerAgentTest` in `backend/src/test/java/com/cornucopai/game/agent/LlmPlayerAgentTest.java` — game launched via `agentIds` with fake brains completes; every decision traces to the participant's `AgentSnapshot` (SC-007); deterministic agent tests unchanged and still pass (test-only, FR-011)
- [x] T037 [P] [US3] Delete `AcpCliBrainTest` in `backend/src/test/java/com/cornucopai/game/llm/AcpCliBrainTest.java` together with its subject (FR-011)

### Implementation for US3

- [x] T038 [US3] Rework `LlmBrainFactory.java` in `backend/src/main/java/com/cornucopai/game/llm/LlmBrainFactory.java` — `create(AgentSnapshot snapshot, String agentName, String atelierId, …)`: `OpenAiChatModel.builder().baseUrl(snapshot.openAiCompatibleUrl()).modelName(snapshot.modelName()).apiKey(envResolver.apply(snapshot.apiKeyVar()))` (apiKey null when apiKeyVar empty — FR-006); system prompt via `SystemPrompts.forPersonality(snapshot.personalityPrompt())` (FR-012); remove the ACP branch and `providerType` handling from the launch path (FR-011); add the `EnvResolver` seam (default `System::getenv`) so FR-013 is testable
- [x] T039 [US3] Modify `SystemPrompts.java` in `backend/src/main/java/com/cornucopai/game/llm/SystemPrompts.java` — add `forPersonality(String)`; `forArchetype(Archetype)` no longer called by the game flow (the `Archetype` enum and deterministic agents stay for unit tests only)
- [x] T040 [US3] Delete `AcpCliBrain.java` in `backend/src/main/java/com/cornucopai/game/llm/AcpCliBrain.java` and remove `AgentConfigDto.java` in `backend/src/main/java/com/cornucopai/api/dto/AgentConfigDto.java` + its usages from the launch path; `AgentLlmConfig.java` stays readable-only for legacy `atelier.json` (FR-014, deprecated)
- [x] T041 [US3] Modify `AgentFactory.java` in `backend/src/main/java/com/cornucopai/game/agent/AgentFactory.java` — `createAgents(List<Participant> players, List<AgentSnapshot> snapshots, String atelierId)`: one brain per participant from its snapshot (index-aligned)

**Checkpoint**: US3 functional — games run LLM-only per agent config, ACP/archetype gone from the flow, legacy ateliers readable (SC-006, SC-007).

---

## Phase 6: Polish & Cross-Cutting Concerns

- [x] T042 Run full suites green: `cd backend && ./mvnw test` and `cd frontend && npm test` and `cd frontend && npm run lint` (CI parity — same commands as `.github/workflows/`)
- [x] T043 Verify JDWP debug still available: `MAVEN_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" ./mvnw quarkus:dev` in `backend/` starts on 8080 with debugger on 5005 (constitution)
- [x] T044 Execute the manual validation scenarios in `specs/009-configurable-agents/quickstart.md` §3 (UI end-to-end: create/edit/delete agents on `/agents`, launch with the same agent twice, inspect `atelier.json` snapshots, FR-013 400 with `MISTRAL_API_KEY` unset) and record outcomes
- [x] T045 Check `.github/workflows/` runs the same test/lint commands as local (constitution CI parity); adjust only if the new tests need an env var (e.g. none — fake brains)
- [x] T046 Add a diary passage in `diary/` (constitution: Zeste de Savoir billet) capturing the archetype→configurable-agent decision, the apiKeyVar design (key never persisted), and the snapshot semantics (FR-017)

---

## Dependencies (story completion order)

```text
Phase 1 (baseline) → Phase 2 (Agent model/store/service)
  → US1 (Phase 3: /api/agents CRUD + /agents page)  ← MVP
  → US2 (Phase 4: agentIds[] launch + selector + snapshots)  [needs US1's /api/agents]
  → US3 (Phase 5: LLM-only brains, ACP/archetype out)        [needs US2's launch path]
  → Phase 6 (polish)
```

## Parallel Execution Examples

- **Phase 2**: T002, T003, T004 (three test files) in parallel; then T005, T006, T007, T008 in parallel (different files).
- **US1**: T009, T010, T011, T012, T013 (tests, different files) in parallel; then T014, T016, T018 in parallel, T015 and T017 after their DTO/service exist.
- **US2**: T019–T023 (tests) in parallel; then T024, T025, T031, T032 (independent files) in parallel, T026–T030 sequential on the launch path, T033 last.
- **US3**: T034–T037 in parallel; then T038–T041.

## Implementation Strategy

**MVP = US1 (Phase 3)**: the `/agents` page + CRUD API alone deliver the configurable-agent management (spec US-1, SC-001/SC-002). US2 then makes ateliers consume agents; US3 completes the LLM-only launch. Each phase is independently testable per its checkpoint. TDD throughout: every test task MUST fail before its implementation task (constitution principle II).
