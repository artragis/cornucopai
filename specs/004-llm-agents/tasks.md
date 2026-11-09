---

description: "Task list template for feature implementation"
---

# Tasks: Agents joueurs pilotés par LLM, configurables par agent, avec toolbox carte

**Input**: Design documents from `/specs/004-llm-agents/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md (R-021…R-034), data-model.md, contracts/, quickstart.md

**Tests**: INCLUDED and MANDATORY — constitution principle II (Test-First, non-negotiable): every test task is written and MUST FAIL before its implementation task runs (Red-Green-Refactor). CI stays LLM-free: tests use a fake `LlmBrain` and one single card family per game (FR-014, R-031).

**Organization**: Tasks are grouped by user story (US1–US5 from spec.md) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4, US5)
- Include exact file paths in descriptions

## Path Conventions

- Backend main: `backend/src/main/java/com/cornucopai/…`, tests: `backend/src/test/java/com/cornucopai/…`
- Fixture: `backend/src/test/resources/cornucopia-fixture/` (cards under `source/`, rich content under `cornucopia.owasp.org/data/cards/…`)
- Config: `backend/src/main/resources/application.properties`, test config: `backend/src/test/resources/application.properties`
- Frontend: unchanged this increment (R-032)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: new LangChain4j OpenAI dependency, LLM default config, package layout, extended hermetic fixture

- [X] T001 Add `<langchain4j.version>1.23.0</langchain4j.version>` property and dependency `dev.langchain4j:langchain4j-open-ai:${langchain4j.version}` to `backend/pom.xml` (R-022 — same version quarkus-langchain4j-core 1.15.0 already pulls; recorded per constitution)
- [X] T002 Add LLM defaults to `backend/src/main/resources/application.properties`: `cornucopai.llm.base-url=${LLM_BASE_URL:http://localhost:1235/v1}`, `cornucopai.llm.api-key=${LLM_API_KEY:}`, `cornucopai.llm.model=${LLM_MODEL:}`, `cornucopai.llm.timeout-seconds=${LLM_TIMEOUT_SECONDS:120}` (R-028, FR-002); leave the existing JDWP debug comment untouched (constitution)
- [X] T003 Create directories `backend/src/main/java/com/cornucopai/game/llm/`, `backend/src/main/java/com/cornucopai/game/llm/dto/` and test mirrors `backend/src/test/java/com/cornucopai/game/llm/` + `.../game/llm/dto/` (R-033)
- [X] T004 [P] Extend the hermetic fixture under `backend/src/test/resources/cornucopia-fixture/`: add `source/webapp-mappings-3.0.yaml` (suits VE with cards VE2/VE3 carrying `stride`, `capec`, `owasp_asvs`, `owasp_dev_guide`, `safecode` — mirroring the real file shape, R-024) and a mini rich-content tree `cornucopia.owasp.org/data/cards/webapp-cards-3.0-en/data-validation-&-encoding/VE2/explanation.md` + `VE2/technical-note.md` + same for `VE3` (R-031)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: shared LLM data model + brain seam + pluggable factory with a fake for tests. ALL user stories depend on this phase. TDD: tests T005–T007 first, MUST fail before T008–T012.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Tests (write first, watch them fail)

- [X] T005 [P] Write `AgentLlmConfigTest` in `backend/src/test/java/com/cornucopai/game/llm/AgentLlmConfigTest.java` — per data-model.md: `providerType` ∈ `OPENAI_COMPATIBLE`|`ACP`; `OPENAI_COMPATIBLE` requires non-blank `baseUrl`; `ACP` requires `acpProvider` ∈ `github-copilot`|`mistral-vibe`; `apiKey`/`modelName`/`command` optional/nullable (R-028)
- [X] T006 [P] Write `AtelierAgentConfigsPersistenceTest` in `backend/src/test/java/com/cornucopai/game/AtelierAgentConfigsPersistenceTest.java` — an `Atelier` with `agentConfigs` round-trips through the Jackson store (`AtelierStore`) to `atelier.json` and back, index-aligned with `players` (FR-012)
- [X] T007 [P] Write `FakeLlmBrainTest` in `backend/src/test/java/com/cornucopai/game/llm/FakeLlmBrainTest.java` — the scripted test double returns canned DTOs (scenario/explanation, PRECISION, KNOWS+answer, YES) and captures the prompts it receives (used later by the prompt-injection test, T045)

### Implementation

- [X] T008 Implement `AgentLlmConfig` record in `backend/src/main/java/com/cornucopai/game/llm/AgentLlmConfig.java` — fields `providerType` (enum), `baseUrl?`, `apiKey?`, `modelName?`, `acpProvider?`, `command?`, plus `validate()` throwing on the rules quoted in T005 and a `defaultOpenAiCompatible(baseUrl)` factory (R-028, FR-002)
- [X] T009 Implement `LlmBrain` interface in `backend/src/main/java/com/cornucopai/game/llm/LlmBrain.java` — 4 methods taking context objects and returning DTOs: `playCard(...)`, `react(...)`, `answerQuestion(String)`, `vote(...)` (R-029)
- [X] T010 [P] Implement DTO records in `backend/src/main/java/com/cornucopai/game/llm/dto/`: `PlayedCardDto(scenario, explanation)`, `ReactionDto(type, text)` with `type` ∈ `PRECISION`|`QUESTION`|`PASS`, `AnswerDto(knows, answer)`, `VoteDto(vote)` with `vote` ∈ `YES`|`NO` (R-027)
- [X] T011 Modify `Atelier.java` in `backend/src/main/java/com/cornucopai/game/model/Atelier.java` — add `private List<AgentLlmConfig> agentConfigs` (Jackson field-visibility ANY like the other fields) + getter/setter, index-aligned with `players` (FR-012)
- [X] T012 Add `cornucopai.llm.brain=fake` (with a comment: keeps CI LLM-free, R-031) to `backend/src/test/resources/application.properties`, and implement the config-gated `FakeLlmBrain` test double in `backend/src/main/java/com/cornucopai/game/llm/FakeLlmBrain.java` (javadoc: only used when `cornucopai.llm.brain=fake`, never in production)

**Checkpoint**: Foundation ready — config model + brain seam + fake-brain tests green; user stories can now start.

---

## Phase 3: User Story 1 — Configurer le fournisseur d'IA de chaque agent (Priority: P1)

**Goal**: per-agent LLM provider config (ACP or OpenAI-compatible URL; default `http://localhost:1235/v1` no auth), validated at launch (400 on invalid), resolved, and persisted in `atelier.json` as `agentConfigs`.

**Independent Test**: POST /api/ateliers with no `agents` → 201 with default configs; with valid `agents` → per-agent configs in response and on disk; with invalid `agents` → 400 naming the problem (quickstart §2–§3; contracts/ateliers-api.yaml v0.4.0). TDD: T013–T014 first, MUST fail.

### Tests for US1 (write first, MUST fail)

- [X] T013 [P] [US1] Write `AgentConfigResolutionTest` in `backend/src/test/java/com/cornucopai/game/AgentConfigResolutionTest.java` — `AtelierService` with fake factory: request without `agents` → `playerCount` default configs (OPENAI_COMPATIBLE, baseUrl from `cornucopai.llm.base-url` default `http://localhost:1235/v1`, no apiKey, names `Joueur-N`, archetypes round-robin GRUMPY/HAPPY/GREEDY/PROF); request with `agents` → those configs win per player; `agents.size() > playerCount` → IllegalArgumentException (FR-001, FR-002, R-028/R-032)
- [X] T014 [P] [US1] Write `AtelierResourceAgentsTest` in `backend/src/test/java/com/cornucopai/api/AtelierResourceAgentsTest.java` (@QuarkusTest, fake brain via test props, single family `webapp:VE`) — 400 on unknown `providerType`, ACP without `acpProvider`, OPENAI_COMPATIBLE with blank `baseUrl`, `agents.length > playerCount`; 201 with response `agentConfigs` index-aligned with `players`; `agentConfigs` present in persisted `atelier.json` (FR-012, contracts v0.4.0)

### Implementation for US1

- [X] T015 [US1] Implement `LlmDefaults` in `backend/src/main/java/com/cornucopai/game/llm/LlmDefaults.java` — `@ConfigProperty` binding of `cornucopai.llm.base-url` (default `http://localhost:1235/v1`), `api-key`, `model`, `timeout-seconds` (default 120) (R-028, FR-002)
- [X] T016 [US1] Implement `AgentConfigDto` in `backend/src/main/java/com/cornucopai/api/dto/AgentConfigDto.java` (name?, archetype?, providerType, baseUrl?, apiKey?, modelName?, acpProvider?, command?) and add `agents` (optional `List<AgentConfigDto>`) to `CreateAtelierRequest.java` (R-032, contracts/ateliers-api.yaml v0.4.0)
- [X] T017 [US1] Modify `AtelierResource.create` in `backend/src/main/java/com/cornucopai/api/AtelierResource.java` — validate `agents`: 400 `ErrorResponse` on unknown providerType/acpProvider, blank baseUrl for OPENAI_COMPATIBLE, `agents.size() > playerCount` (edge cases, R-032)
- [X] T018 [US1] Modify `AtelierService.java` in `backend/src/main/java/com/cornucopai/game/AtelierService.java` — resolve agent configs (request `agents` or defaults via `LlmDefaults`), set `atelier.setAgentConfigs(...)`, pass configs to `AgentFactory` (FR-001, FR-012, FR-015)
- [X] T019 [US1] Modify `AtelierResponse.java` in `backend/src/main/java/com/cornucopai/api/dto/AtelierResponse.java` — add `agentConfigs` to the record and to `from(...)` (contracts v0.4.0)

**Checkpoint**: US1 functional — per-agent provider config validated, resolved, persisted, returned.

---

## Phase 4: User Story 2 — Jouer une partie complète avec des agents IA (Priority: P1)

**Goal**: one single `LlmPlayerAgent` implementation plays the whole game through a `LlmBrain`; deterministic agents leave the game path (stay for unit tests only); LLM failures fall back without blocking (FR-013). Depends on US1 (configs) and Phase 2 (brain seam). TDD: T020–T022 first, MUST fail.

### Tests for US2 (write first, MUST fail)

- [X] T020 [P] [US2] Write `LlmPlayerAgentTest` in `backend/src/test/java/com/cornucopai/game/agent/LlmPlayerAgentTest.java` with a scripted fake `LlmBrain`: playCard maps `PlayedCardDto`→`PlayedCard` with `cardName = card.id()` (never from the LLM); react maps `ReactionDto.type`→`Reaction.Precision/Question/Pass` (unknown/blank → `Pass`); answerQuestion `knows=true` with blank answer → `DoesNotKnow`; vote maps YES/NO (invalid → `NO`); brain throwing → fallback per R-030 (generic scenario+explanation quoting card id/description, `Pass`, `DoesNotKnow`, `NO`) and no exception escapes (FR-003, FR-004, FR-013, SC-007)
- [X] T021 [P] [US2] Write `AcpCliBrainTest` in `backend/src/test/java/com/cornucopai/game/llm/AcpCliBrainTest.java` — fake `command` (a tiny shell script emitting canned JSON): asserts the subprocess receives the prompt and the output maps to DTOs; missing command → exception (caught upstream by LlmPlayerAgent) (R-029)
- [X] T022 [US2] Write `AgentFactoryTest` in `backend/src/test/java/com/cornucopai/game/agent/AgentFactoryTest.java` + `LlmBrainFactoryTest` in `backend/src/test/java/com/cornucopai/game/llm/LlmBrainFactoryTest.java` — `createAgents(players, configs, atelierId)` returns `LlmPlayerAgent` instances named `Joueur-1..N` with round-robin archetypes, each with its own brain; no `GrumpyAgent`/`HappyAgent`/`GreedyAgent`/`ProfAgent` in the result (FR-003); the factory honors `cornucopai.llm.brain=fake` → `FakeLlmBrain`, else `LangChain4jBrain` for OPENAI_COMPATIBLE / `AcpCliBrain` for ACP (R-029, R-031)

### Implementation for US2

- [X] T023 [US2] Implement `PlayerAiService.java` in `backend/src/main/java/com/cornucopai/game/llm/PlayerAiService.java` — AiService interface, 4 methods each taking `@V("input") String` and returning the DTOs of T010 (R-021, R-027)
- [X] T024 [US2] Implement `LangChain4jBrain.java` in `backend/src/main/java/com/cornucopai/game/llm/LangChain4jBrain.java` — builds a per-agent `AiServices.builder(PlayerAiService.class).chatModel(OpenAiChatModel.builder().baseUrl(cfg.baseUrl).apiKey(cfg.apiKey).modelName(cfg.modelName).timeout(...).maxRetries(1).build()).systemMessageProvider(...).chatMemory(chatMemory).tools(tools).build()` (constructor takes `systemPrompt`, `ChatMemory`, `List<Object> tools` so US4 can inject policy; verified API in langchain4j 1.23.0, R-021/R-023/R-025)
- [X] T025 [P] [US2] Implement `AcpCliBrain.java` in `backend/src/main/java/com/cornucopai/game/llm/AcpCliBrain.java` — one-shot subprocess (`command` or provider name, prompt as arg), output parsed JSON→DTO; no toolbox/memory this increment (documented limitation, R-029)
- [X] T026 [US2] Implement `LlmPlayerAgent.java` in `backend/src/main/java/com/cornucopai/game/agent/LlmPlayerAgent.java` — the single `PlayerAgent` impl: builds user prompts with featureHtml/card/leg/question as labeled data sections (prompt-injection safe), calls the brain, maps DTO→domain sealed types, try/catch + DTO-validation fallback (R-027, R-030)
- [X] T027 [US2] Implement `LlmBrainFactory` in `backend/src/main/java/com/cornucopai/game/llm/LlmBrainFactory.java` (CDI bean reading `cornucopai.llm.brain`, default `auto`: fake → `FakeLlmBrain`, OPENAI_COMPATIBLE → `LangChain4jBrain`, ACP → `AcpCliBrain`, R-029) and modify `AgentFactory.java` in `backend/src/main/java/com/cornucopai/game/agent/AgentFactory.java` — `createAgents(players, configs, atelierId)` builds `LlmPlayerAgent` via the factory; update javadoc of `AbstractAgent`/the 4 deterministic agents to "unit tests only" (FR-003)
- [X] T028 [US2] Modify `AtelierService.java` — `createAndRun` passes resolved configs to `AgentFactory.createAgents` so the `GameMaster` runs on LLM agents (game engine untouched)

**Checkpoint**: US2 functional — a full game runs on LLM agents with fake-brain tests green; deterministic agents test-only.

---

## Phase 5: User Story 3 — Outil « infos carte » dans la toolbox (Priority: P1)

**Goal**: first LangChain4j toolbox tool `get_card_info(cardId)` aggregating YAML card entry + `*-mappings-*.yaml` + `cornucopia.owasp.org/data/cards/**` rich content; registered on every agent's AiService. Depends on US2 (brain exists). TDD: T029–T031 first, MUST fail.

### Tests for US3 (write first, MUST fail)

- [X] T029 [P] [US3] Write `CardInfoServiceTest` in `backend/src/test/java/com/cornucopai/game/card/CardInfoServiceTest.java` against the extended fixture: `VE2` → id/value/setId `webapp`/version `3.0`/suit `VE`+name/description/url + mappings (stride, capec, owasp_asvs, owasp_dev_guide, safecode) from `webapp-mappings-3.0.yaml` + `explanation`/`technicalNote` from the mini `data/cards` tree; unknown id `XX9` → explicit "carte inconnue" (FR-009); card without rich dir → YAML+mappings present + absence noted; id in several sets → `locations` listed, current atelier's sets preferred (FR-007, FR-008, R-024)
- [X] T030 [P] [US3] Write `CardInfoToolTest` in `backend/src/test/java/com/cornucopai/game/card/CardInfoToolTest.java` — the `@Tool` method `get_card_info("VE2")` returns Markdown containing description, mappings and explanation; `get_card_info("XX9")` returns the unknown-card string (FR-005, FR-006, R-023)
- [X] T031 [US3] Write `ToolboxWiringTest` in `backend/src/test/java/com/cornucopai/game/llm/ToolboxWiringTest.java` — a brain built by `LlmBrainFactory` for an OPENAI_COMPATIBLE config has `CardInfoTool` in its tools list (FR-005)

### Implementation for US3

- [X] T032 [US3] Implement mappings POJO in `backend/src/main/java/com/cornucopai/game/card/CardMappingsFile.java` — Jackson YAML mapping of `*-mappings-*.yaml` (suits[].cards[] with `stride`, `stride_print`, `capec`, `capec_map`, `owasp_asvs`, `owasp_asvs_print`, `owasp_dev_guide`, `owasp_dev_guide_print`, `safecode`; `@JsonIgnoreProperties(ignoreUnknown=true)`) and `CardMappings.java` in `game/card/model/` (R-024)
- [X] T033 [US3] Implement `CardInfo.java` in `backend/src/main/java/com/cornucopai/game/card/model/CardInfo.java` — fields per data-model.md (id, value, setId, version, suitId, suitName, language, description, url?, misc?, mappings?, explanation?, technicalNote?, locations[]) (FR-006)
- [X] T034 [US3] Implement `CardInfoService.java` in `backend/src/main/java/com/cornucopai/game/card/CardInfoService.java` — cached index `cardId → locations` built by scanning latest-version `source/*-cards-*.yaml` (reuse the catalog scan + numeric version compare, R-013); per id reads the card entry, the `<edition>-mappings-<version>.yaml`, and walks `cornucopia.owasp.org/data/cards/<edition>-cards-<version>-en/<familyDir>/<cardId>/` for `explanation.md`/`technical-note.md` (walking, not slug computation); prefers the current atelier's sets when given (FR-008) (R-024)
- [X] T035 [US3] Implement `CardInfoTool.java` in `backend/src/main/java/com/cornucopai/game/card/CardInfoTool.java` — `@Tool(name = "get_card_info", value = "…")` method `String getCardInfo(@P("identifiant de la carte, ex. VE2") String cardId)` returning Markdown built from `CardInfoService`; unknown id → "Carte inconnue : <id>" (R-023, FR-009)
- [X] T036 [US3] Modify `LlmBrainFactory.java` / `LangChain4jBrain.java` — register the `CardInfoTool` bean in `.tools(...)` for every agent (FR-005)

**Checkpoint**: US3 functional — agents can call `get_card_info` and get the full card picture from the id alone.

---

## Phase 6: User Story 4 — Mémoire par agent et prompt système par archétype (Priority: P2)

**Goal**: per-agent, per-atelier sliding-window memory (40 messages, isolated by construction) and per-archetype system prompts wired into the brains. Depends on US2. TDD: T037–T038 first, MUST fail.

### Tests for US4 (write first, MUST fail)

- [X] T037 [P] [US4] Write `MemoryIsolationTest` in `backend/src/test/java/com/cornucopai/game/llm/MemoryIsolationTest.java` — two agents built for different atelier ids get distinct `ChatMemory` instances whose ids are `<atelierId>/<agentName>` and whose window is capped at 40 messages (`MessageWindowChatMemory` maxMessagesProvider, R-025); nothing shared, nothing persisted across ateliers (FR-010)
- [X] T038 [P] [US4] Write `SystemPromptsTest` in `backend/src/test/java/com/cornucopai/game/llm/SystemPromptsTest.java` — `SystemPrompts.forArchetype` returns 4 distinct prompts; each contains the Cornucopia game rules, the output contract (exact JSON shapes of `PlayedCardDto`/`ReactionDto`/`AnswerDto`/`VoteDto`), and the archetype personality (GRUMPY/HAPPY/GREEDY/PROF as prose) (FR-011, R-026)

### Implementation for US4

- [X] T039 [US4] Implement `SystemPrompts.java` in `backend/src/main/java/com/cornucopai/game/llm/SystemPrompts.java` — `forArchetype(Archetype)` per R-026 (game rules + output contract + personality)
- [X] T040 [US4] Modify `LlmBrainFactory.java` — build the per-agent `MessageWindowChatMemory.builder().id(atelierId + "/" + agentName).maxMessagesProvider(id -> 40).build()` and pass `SystemPrompts.forArchetype(archetype)` as the system prompt into `LangChain4jBrain` (R-021, R-025, R-026, FR-010/FR-011)

**Checkpoint**: US4 functional — memory isolated per agent per atelier, prompts per archetype, tests green.

---

## Phase 7: User Story 5 — Tests à une seule famille de cartes (Priority: P2)

**Goal**: every game-level test plays exactly ONE family (bounded turns, no real LLM in CI). Depends on US1+US2 (fake brain already in test config).

- [X] T041 [US5] Modify `AtelierResourceTest.postCreatesRunsAndPersistsAFullGame` in `backend/src/test/java/com/cornucopai/api/AtelierResourceTest.java` — post a single family `["webapp:VE"]` (fixture: 2 cards → deck.size()=2, legs.size()=2), keep all existing assertions, runs LLM-free via the fake brain (FR-014, SC-005)
- [X] T042 [US5] Write `SingleFamilyLlmGameTest` in `backend/src/test/java/com/cornucopai/game/SingleFamilyLlmGameTest.java` — full `GameMaster` run with `LlmPlayerAgent`s + fake brains, 2 players, one family `webapp:VE` only: ≤ 2 legs, final validation statuses, `agentConfigs` persisted in atelier.json, no `PENDING` left (FR-003, FR-012, FR-014, SC-005)
- [X] T043 [US5] Add a comment block at the top of `backend/src/test/resources/application.properties` stating `cornucopai.llm.brain=fake` is what keeps CI LLM-free, and that any new game test MUST select exactly one family (FR-014, R-031)

**Checkpoint**: US5 functional — game tests are fast, bounded and CI-safe.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T044 [P] Structured-logging audit in `backend/src/main/java/com/cornucopai/game/agent/LlmPlayerAgent.java` + `backend/src/main/java/com/cornucopai/game/llm/LangChain4jBrain.java` (constitution IV): every LLM call, `get_card_info` tool call and fallback logs WARN/INFO with atelier id, turn, agent name — and `apiKey` is NEVER logged (security-sensitive code, constitution)
- [X] T045 [P] Prompt-injection hardening pass on `backend/src/main/java/com/cornucopai/game/agent/LlmPlayerAgent.java`: card text, `featureHtml` and questions are passed as labeled data sections of the user message, never concatenated into the system prompt; add a unit test in `backend/src/test/java/com/cornucopai/game/agent/LlmPlayerAgentTest.java` feeding hostile card text (fake brain captures the prompt) (constitution security review)
- [X] T046 Run the full backend suite `cd backend && ./mvnw test` and validate against `specs/004-llm-agents/quickstart.md` §1: all green, including the unchanged deterministic-agent tests in `backend/src/test/java/com/cornucopai/game/agent/` (SC-006) and `backend/src/test/java/com/cornucopai/api/HealthResourceTest.java`; confirm Quarkus still boots with JDWP debug available (constitution)
- [X] T047 Write diary entry `diary/2026-11-08-agents-llm-et-toolbox-carte.md` capturing the decisions R-021…R-034 and what was learned (constitution IV)
- [X] T048 [P] Re-validate contracts: `specs/004-llm-agents/contracts/ateliers-api.yaml` v0.4.0 and `atelier-schema.json` match the implemented DTOs (`AgentConfigDto`, `AgentLlmConfig`, `agentConfigs`) field by field

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies — start immediately
- **Foundational (Phase 2)**: depends on Phase 1 — BLOCKS all user stories
- **User Stories (Phases 3–7)**: depend on Foundational; execute in priority order US1 → US2 → US3 (all P1), then US4 → US5 (P2)
- **Polish (Phase 8)**: depends on all desired user stories

### User Story Dependencies

- **US1 (P1)**: after Foundational — config model, validation, persistence (no other story needed)
- **US2 (P1)**: after US1 — needs resolved `AgentLlmConfig`s to build brains
- **US3 (P1)**: after US2 — registers the card tool on the existing brains
- **US4 (P2)**: after US2 — injects memory + system-prompt policy into the brain factory
- **US5 (P2)**: after US1+US2 — single-family game tests with the fake brain

### Within Each User Story

- Tests MUST be written and FAIL before implementation (constitution II)
- Models before services; services before wiring; wiring before polish
- Story checkpoint before moving to the next story

### Parallel Opportunities

- Phase 1: T004 can run in parallel with T001–T003 (different files)
- Phase 2: T005, T006, T007 in parallel; T008, T010 in parallel (different files)
- US1: T013, T014 in parallel; T015, T016 in parallel
- US2: T020, T021, T022 in parallel; T024, T025 in parallel
- US3: T029, T030, T031 in parallel; T032, T033 in parallel
- US4: T037, T038 in parallel
- US5: T041, T042 in parallel
- Polish: T044, T045, T048 in parallel

## Parallel Example: User Story 3

```text
Task: "T029 [US3] CardInfoServiceTest in backend/src/test/java/com/cornucopai/game/card/CardInfoServiceTest.java"
Task: "T030 [US3] CardInfoToolTest in backend/src/test/java/com/cornucopai/game/card/CardInfoToolTest.java"
Task: "T031 [US3] ToolboxWiringTest in backend/src/test/java/com/cornucopai/game/llm/ToolboxWiringTest.java"
# then, once red:
Task: "T032 [US3] CardMappingsFile + CardMappings"
Task: "T033 [US3] CardInfo model"
```

---

## Implementation Strategy

### MVP First (all P1 stories: US1 + US2 + US3)

1. Complete Phase 1 (Setup) and Phase 2 (Foundational)
2. Complete Phase 3 (US1: per-agent provider config, persisted)
3. Complete Phase 4 (US2: single LLM agent plays the whole game, fallback on LLM failure)
4. Complete Phase 5 (US3: `get_card_info` toolbox wired into every agent)
5. **STOP and VALIDATE**: quickstart §2 — a full game against the local bionic server, one family, `agentConfigs` persisted, tool visible in logs

### Incremental Delivery

1. Setup + Foundational → foundation ready (fake brain keeps CI green)
2. + US1 → config API contract v0.4.0 works (quickstart §3)
3. + US2 → LLM game end-to-end (fake brain in CI, real bionic manually)
4. + US3 → agents use card info (SC-003)
5. + US4 → memory/prompt policy verified (FR-010/FR-011)
6. + US5 → test cost bounded (SC-005)
7. Polish → logs, prompt-injection hardening, diary, contracts re-validation

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps each task to its user story for traceability (spec.md US1–US5)
- Every game-level test selects exactly ONE family (`webapp:VE` in the fixture = 2 cards) — FR-014
- CI never calls a real LLM: `cornucopai.llm.brain=fake` in test config (R-031); the real bionic run is a manual quickstart step only
- Commit after each task or logical group; stop at each checkpoint to validate the story independently
- Avoid: vague tasks, same-file conflicts, cross-story dependencies that break independence (US3/US4 depend on US2's brain, which is fine — they are later in priority order)
