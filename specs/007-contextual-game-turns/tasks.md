---

description: "Task list for feature 007-contextual-game-turns — agents conscients du projet (MCP IDE, mémoire partagée, vote par arbre de décision)"
---

# Tasks: Agents conscients du projet — MCP IDE, mémoire partagée et vote par arbre de décision

**Input**: Design documents from `/specs/007-contextual-game-turns/`

**Prerequisites**: plan.md (required), spec.md (required for user stories, incl. Clarifications session 2026-11-11), research.md (R-045…R-062), data-model.md, contracts/ (ateliers-api.yaml v0.7.0, leg-schema.json v4.0.0, atelier-schema.json v0.7.0), quickstart.md

**Tests**: INCLUDED and MANDATORY — constitution principle II (Test-First, non-negotiable): every test task is written and MUST FAIL before its implementation task runs (Red-Green-Refactor). Backend CI stays LLM-free and MCP-free (fake brain `cornucopai.llm.brain=fake`, mock `McpTransport` via `cornucopai.mcp.ide.mock=true` in test profile — R-055, one single card family per game test — spec 004 FR-014). Frontend tests use Vitest.

**Organization**: Tasks are grouped by user story (US1–US5 from spec.md) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1…US5)
- Include exact file paths in descriptions

## Path Conventions

- Backend main: `backend/src/main/java/com/cornucopai/…`, tests: `backend/src/test/java/com/cornucopai/…`
- Frontend: `frontend/src/…`, tests colocated (`*.spec.ts`)
- Spec artifacts: `specs/007-contextual-game-turns/…`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: confirm the green baseline before touching anything

- [X] T001 Run `cd backend && ./mvnw test` and `cd frontend && npm test` and confirm both suites are green (baseline) before any change

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: additive model + config extensions — `ProjectFact`, `VoteDecision`, `Atelier.projectFacts`, `Leg.votes`, `McpIdeConfig`, and the two new Maven deps. ALL user stories depend on these. TDD: T002–T005 first, MUST fail before T006–T010.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Tests (write first, watch them fail)

- [X] T002 [P] Write `ProjectFactModelTest` in `backend/src/test/java/com/cornucopai/game/memory/ProjectFactModelTest.java` — `ProjectFact` record `{text (minLength 1), author, turnIndex, timestamp}` round-trips through Jackson (data-model.md, R-053)
- [X] T003 [P] Write `VoteDecisionModelTest` in `backend/src/test/java/com/cornucopai/game/model/VoteDecisionModelTest.java` — `VoteDecision` record `{voter, vote ∈ {YES,NO}, explanationCheck ∈ {OK,INSUFFICIENT}, applicabilityCheck ∈ {OK,NOT_APPLICABLE}, justification}` round-trips through Jackson; leg-schema.json v4.0.0 validation (R-051, FR-009/FR-010)
- [X] T004 [P] Update `ModelContractTest` in `backend/src/test/java/com/cornucopai/game/model/ModelContractTest.java` — `Atelier` gains `projectFacts: List<ProjectFact>` and a legacy atelier JSON (without it) reads with an empty list; `Leg` gains `votes: List<VoteDecision>` and a legacy v3 leg JSON reads with an empty list (R-053, atelier-schema v0.7.0, leg-schema v4.0.0)
- [X] T005 [P] Write `McpIdeConfigTest` in `backend/src/test/java/com/cornucopai/game/mcp/McpIdeConfigTest.java` — config binding: `cornucopai.mcp.ide.url` default `http://127.0.0.1:64462/stream`, `.tools` default the 7 comma-separated names, `.timeout-seconds` default 30, `.enabled` default true, `cornucopai.memory.prompt-window` default 20 (R-048, FR-002, FR-014, clarifications 2026-11-11). NOTE (revised 2026-11-11, R-061): `cornucopai.debate.enabled` now defaults to **false** — the debate-default assertion was removed from this test and the default lives in `AtelierService`/application.properties, not in `McpIdeConfig`

### Implementation

- [X] T006 [P] Add deps to `backend/pom.xml` — `dev.langchain4j:langchain4j-mcp`, `langchain4j-agentic` and `langchain4j-agentic-patterns` at `${langchain4j-beta.version}` = **1.23.0-beta33** (these artifacts exist only on a separate beta line; core stays at `${langchain4j.version}` = 1.23.0, R-045)
- [X] T007 [P] Implement `ProjectFact.java` in `backend/src/main/java/com/cornucopai/game/memory/ProjectFact.java` — record `{String text, String author, int turnIndex, Instant timestamp}`, Jackson-annotated like other model records (R-053)
- [X] T008 [P] Implement `VoteDecision.java` in `backend/src/main/java/com/cornucopai/game/model/VoteDecision.java` — record with the 5 fields typed as the enums `VoteChoice` / `ExplanationCheck` / `ApplicabilityCheck` (R-051; revised 2026-11-11, R-060: real enums instead of String constants, no post-hoc validation)
- [X] T009 [P] Modify `Atelier.java` in `backend/src/main/java/com/cornucopai/game/model/Atelier.java` — add `List<ProjectFact> projectFacts` (default empty) + getter/setter (R-053)
- [X] T010 [P] Modify `Leg.java` in `backend/src/main/java/com/cornucopai/game/model/Leg.java` — add `List<VoteDecision> votes` (default empty) + getter/setter; keep v2 legacy `precisions` read (R-053)
- [X] T011 [P] Implement `McpIdeConfig.java` in `backend/src/main/java/com/cornucopai/game/mcp/McpIdeConfig.java` — CDI bean reading the 6 config properties into a record (R-048)

**Checkpoint**: Foundation ready — additive models v4.0.0/v0.7.0 compile, legacy reads covered, deps resolved.

---

## Phase 3: User Story 1 — Accès au projet via MCP IDE (Priority: P1) 🎯 MVP

**Goal**: connect the streamable-HTTP MCP server (default `http://127.0.0.1:64462/stream`), whitelist the 7 tools via `McpToolProvider.filter`, wire the provider into every agent's AiServices next to the existing `@Tool`s, non-blocking on MCP failure. Depends on Phase 2.

**Independent Test**: a unit test inspects a brain's toolbox and finds the 7 whitelisted MCP tool names + `get_card_info` + `get_tour_info` (SC-001); an integration test with the mock transport and fake brains asserts `executeTool("list_directory_tree")` was called during a 2-player single-family game (SC-002); a resilience test with a throwing mock transport runs a full game end-to-end (SC-007, FR-004).

### Tests (write first, MUST fail)

- [X] T012 [P] [US1] Write `McpToolProviderFactoryTest` in `backend/src/test/java/com/cornucopai/game/mcp/McpToolProviderFactoryTest.java` — with a mock `McpTransport` listing 10 tools (the 7 whitelisted + 3 extra), the built provider exposes exactly the 7 whitelisted names (filter, R-046, FR-002); `failIfOneServerFails(false)`; provider build with a throwing transport returns an empty provider, no exception (R-047, FR-004)
- [X] T013 [P] [US1] Write `ToolboxWiringMcpTest` in `backend/src/test/java/com/cornucopai/game/llm/ToolboxWiringMcpTest.java` — a `LangChain4jBrain` built with the MCP provider + base tools has all tools visible (SC-001); assert `McpToolProvider` is passed via `.toolProvider(...)` in addition to `.tools(...)`
- [X] T014 [P] [US1] Write `McpResilienceGameTest` in `backend/src/test/java/com/cornucopai/game/McpResilienceGameTest.java` — full fake-brain game with a mock transport that throws on connect: game completes, legs persisted, MCP tools returned error strings, no propagation (SC-007, FR-004)
- [X] T015 [P] [US1] Implement `MockMcpClient.java` in `backend/src/main/java/com/cornucopai/game/mcp/MockMcpClient.java` (in **src/main**, not src/test, because the test profile sets `cornucopai.mcp.ide.mock=true` and must never open a real MCP connection) — in-memory `McpClient` returning scripted `ToolSpecification`s (the 7 + 3 decoys) and scripted execution results; toggleable failure mode (R-055, FR-015)

### Implementation for US1

- [X] T016 [US1] Implement `McpToolProviderFactory.java` in `backend/src/main/java/com/cornucopai/game/mcp/McpToolProviderFactory.java` — builds `StreamableHttpMcpTransport` → `DefaultMcpClient` (key `"ide-mcp"`, `cacheToolList(true)`) → `McpToolProvider` with `.mcpClients(...)`, `.failIfOneServerFails(false)`, `.filter((c, tool) -> config.allowedToolNames().contains(tool.name()))`; try/catch around build → empty provider + WARN (R-046, R-047); if `enabled=false` or `mock=true`, return mock/empty; one provider per atelier, shared by all agents (US1 scenario 5)
- [X] T017 [US1] Modify `LlmBrainFactory.java` in `backend/src/main/java/com/cornucopai/game/llm/LlmBrainFactory.java` — accept an optional `McpToolProvider` in `create(...)`; pass it to `LangChain4jBrain`
- [X] T018 [US1] Modify `LangChain4jBrain.java` in `backend/src/main/java/com/cornucopai/game/llm/LangChain4jBrain.java` — accept the provider, call `.toolProvider(provider)` on the AiServices builder in addition to `.tools(tools)`; null provider → skip (FR-003); log the exposed MCP tool names at startup (health check, R-047)
- [X] T019 [US1] Modify `AgentFactory.java` (or wherever brains are created per game) in `backend/src/main/java/com/cornucopai/game/agent/AgentFactory.java` — build one `McpToolProvider` per atelier via the factory and pass it to every brain
- [X] T020 [US1] Add config defaults to `backend/src/main/resources/application.properties` — `cornucopai.mcp.ide.enabled=true`, `cornucopai.mcp.ide.url=${MCP_IDE_URL:http://127.0.0.1:64462/stream}`, `cornucopai.mcp.ide.tools=get_project_dependencies,get_project_modules,list_directory_tree,read_file,search_file,search_regex,search_text`, `cornucopai.mcp.ide.timeout-seconds=30`, `cornucopai.mcp.ide.mock=${MCP_IDE_MOCK:false}` (R-048, FR-014)
- [X] T021 [US1] Add `cornucopai.mcp.ide.mock=true` to `backend/src/test/resources/application.properties` (R-055)

**Checkpoint**: US1 functional — toolbox contains the 7 MCP tools, mock works in CI, MCP-down game runs (SC-001, SC-002, SC-007).

---

## Phase 4: User Story 2 — Mémoire partagée des faits projet (Priority: P1)

**Goal**: per-atelier `SharedProjectMemory` + two `@Tool`s (`get_project_facts` / `add_project_fact`), 20-recent-facts prompt injection as DONNÉES, persistence in `atelier.json`, optional inheritance via `inheritFactsFrom` + frontend dropdown. Depends on Phase 2 (and uses US1's toolbox wiring).

**Independent Test**: integration test — agent A calls `add_project_fact`, agent B's `get_project_facts` returns it (SC-003); a second atelier starts empty (SC-006); POST with `inheritFactsFrom` copies the source's facts (FR-007); prompt-window test asserts only the 20 most recent facts are in the prompt (FR-008).

### Tests (write first, MUST fail)

- [X] T022 [P] [US2] Write `SharedProjectMemoryTest` in `backend/src/test/java/com/cornucopai/game/memory/SharedProjectMemoryTest.java` — thread-safe add/facts/recent(20); `recent(n)` returns the n most recent in order (R-049, FR-008)
- [X] T023 [P] [US2] Write `ProjectFactsToolTest` in `backend/src/test/java/com/cornucopai/game/memory/ProjectFactsToolTest.java` — `get_project_facts` returns JSON array of all facts; `add_project_fact("...")` appends with author/turnIndex/timestamp filled from context; empty memory returns `[]` (R-049, FR-005/FR-006)
- [X] T024 [P] [US2] Write `SharedMemoryGameTest` in `backend/src/test/java/com/cornucopai/game/memory/SharedMemoryGameTest.java` — 2 fake-brain agents in one game: agent A's `add_project_fact` visible to agent B's `get_project_facts` (SC-003); a second atelier's memory is empty (SC-006); facts persisted in `atelier.json` `projectFacts` (R-053)
- [X] T025 [P] [US2] Extend `AtelierResourceTest` in `backend/src/test/java/com/cornucopai/api/AtelierResourceTest.java` — POST with `inheritFactsFrom=<id>` copies source facts into the new atelier; unknown `inheritFactsFrom` → 400; GET summary includes `hasProjectFacts` (FR-007, API v0.7.0, R-056)
- [X] T026 [P] [US2] Write `PromptInjectionTest` in `backend/src/test/java/com/cornucopai/game/agent/PromptInjectionTest.java` — with 25 facts in memory, the agent's user prompt contains exactly the 20 most recent inside a `DONNÉES` section, never in the system prompt (FR-008, clarification 2026-11-11)

### Implementation for US2

- [X] T027 [US2] Implement `SharedProjectMemory.java` in `backend/src/main/java/com/cornucopai/game/memory/SharedProjectMemory.java` — per-atelier store: `CopyOnWriteArrayList<ProjectFact>`, `add(...)`, `facts()`, `recent(int n)`, atelier id for logs (R-049)
- [X] T028 [US2] Implement `ProjectFactsTool.java` in `backend/src/main/java/com/cornucopai/game/memory/ProjectFactsTool.java` — `@Tool(name="get_project_facts")` returns JSON of all facts; `@Tool(name="add_project_fact")` appends a `ProjectFact` with author = calling agent, turnIndex = current leg's turnIndex, timestamp = now (R-049, FR-005/FR-006)
- [X] T029 [US2] Wire the tools: modify `LlmBrainFactory.java` to also accept the per-game `ProjectFactsTool` and add it to `allTools`; modify `AgentFactory.java` to create one `SharedProjectMemory` + one `ProjectFactsTool` per atelier and hand them to every brain (R-049, like spec 005 R-039)
- [X] T030 [US2] Modify `LlmPlayerAgent.java` in `backend/src/main/java/com/cornucopai/game/agent/LlmPlayerAgent.java` — inject the 20 most recent facts as a `DONNÉES` section into `reactPrompt`/`answerPrompt`/`votePrompt` user messages (FR-008); mention `get_project_facts`/`add_project_fact` in the prompts (FR-012, R-057)
- [X] T031 [US2] Modify `CreateAtelierRequest.java` in `backend/src/main/java/com/cornucopai/api/dto/CreateAtelierRequest.java` — add optional `inheritFactsFrom: String` (API v0.7.0, R-053)
- [X] T032 [US2] Modify `AtelierService.java` / `AtelierResource.java` — validate `inheritFactsFrom` (400 if unknown), copy source `projectFacts` into the new atelier at creation (FR-007, R-053); add `hasProjectFacts` to `AtelierSummary` (R-056)
- [X] T033 [US2] Modify `AtelierStore.java` in `backend/src/main/java/com/cornucopai/game/AtelierStore.java` — persist `projectFacts` inside `atelier.json` on every `saveAtelier` (R-053)
- [X] T034 [P] [US2] Frontend: modify `ConfigureGameDialog.vue` in `frontend/src/components/ConfigureGameDialog.vue` — add a `v-select` dropdown listing ateliers with `hasProjectFacts=true`, bound to `inheritFactsFrom` in the POST body (R-056, FR-007)
- [X] T035 [P] [US2] Frontend: write `ConfigureGameDialog.spec.ts` in `frontend/src/ConfigureGameDialog.spec.ts` — dropdown renders only when ateliers with facts exist; selecting one sets `inheritFactsFrom`; default none (R-056)

**Checkpoint**: US2 functional — shared memory works between agents, persists, inherits via dropdown (SC-003, SC-006, FR-007, FR-008).

---

## Phase 5: User Story 3 — Vote par arbre de décision (Priority: P1)

**Goal**: vote follows a 2-branch decision tree (explanation sufficient? + applicable to the real project via MCP/facts?), structured justification persisted per vote in `leg.votes[]`, token cap kept. Depends on Phase 2 + US1 (MCP tools) + US2 (facts).

**Independent Test**: scripted fake brain returns `NOT_APPLICABLE`+`NO` for a non-applicable scenario → at least 2/3 persisted votes have those values (SC-004); every persisted vote has all 5 fields (SC-005); vote prompt contains both tree questions and uses the capped model (SC-008).

### Tests (write first, MUST fail)

- [X] T036 [P] [US3] Write `VoteDecisionTreeTest` in `backend/src/test/java/com/cornucopai/game/llm/VoteDecisionTreeTest.java` — `VoteDto` extended to `{vote, explanationCheck, applicabilityCheck, justification}`; `LlmPlayerAgent.vote()` maps it to `VoteDecision` and persists; fake brain returning `NOT_APPLICABLE`/`NO` → leg's `votes[]` contains it (FR-009/FR-010, SC-004/SC-005); LLM failure → NO + "repli LLM indisponible" (edge case, R-051)
- [X] T037 [P] [US3] Update `SystemPromptsTest` / `PromptsTest` in `backend/src/test/java/com/cornucopai/game/agent/PromptsTest.java` — vote prompt contains the two decision-tree questions in French and points to `get_tour_info` + `get_project_facts` + MCP tools; still no reasoning allowed, capped model used (FR-011, SC-008, R-051)
- [X] T038 [P] [US3] Update `ModelContractTest` — leg-schema v4.0.0: a leg with `votes` round-trips; legacy v3 leg without `votes` reads empty (already in T004, extend assertions)

### Implementation for US3

- [X] T039 [US3] Modify `VoteDto.java` in `backend/src/main/java/com/cornucopai/game/llm/dto/VoteDto.java` — add `explanationCheck`, `applicabilityCheck`, `justification` fields (R-051)
- [X] T040 [US3] Modify `LlmPlayerAgent.votePrompt(...)` in `backend/src/main/java/com/cornucopai/game/agent/LlmPlayerAgent.java` — enumerate branch (a) « le scénario est-il suffisamment bien expliqué (scénario + explication + précisions du tour) ? » and branch (b) « le scénario est-il applicable au projet réel ? (utilise get_tour_info, get_project_facts et les outils MCP list_directory_tree/read_file/search_*) »; output JSON `{"vote":..., "explanationCheck":..., "applicabilityCheck":..., "justification":...}`; keep the capped vote model (FR-011, SC-008)
- [X] T041 [US3] Modify `LlmPlayerAgent.vote(...)` — build a `VoteDecision` from the DTO and return it alongside the `Vote` (extend the `PlayerAgent` contract or add a method `voteDecision(...)`); fallback NO + "repli LLM indisponible" (R-051)
- [X] T042 [US3] Modify `GameMaster.java` in `backend/src/main/java/com/cornucopai/game/GameMaster.java` — collect each voter's `VoteDecision` into `leg.votes[]` before computing validation; persist via existing `saveLeg` (FR-010, R-053)

**Checkpoint**: US3 functional — votes structured and persisted, non-applicable scenarios get motivated NO (SC-004, SC-005, SC-008).

---

## Phase 6: User Story 4 — Q/R enrichies par le contexte projet (Priority: P2)

**Goal**: react/answer prompts mention MCP tools + facts tools so agents can ground questions and answers in the real code. Depends on US1 + US2.

**Independent Test**: unit test asserts `reactPrompt` and `answerPrompt` contain the line about MCP tools and `get_project_facts`/`add_project_fact` (FR-012, R-057); integration test with fake brain: an agent's question references a fact discovered earlier.

### Tests (write first, MUST fail)

- [X] T043 [P] [US4] Extend `PromptsTest.java` in `backend/src/test/java/com/cornucopai/game/agent/PromptsTest.java` — `reactPrompt` (both variants) and `answerPrompt` mention `list_directory_tree`, `read_file`, `search_*`, `get_project_facts`, `add_project_fact` (FR-012, R-057); still French, no "leg"

### Implementation for US4

- [X] T044 [US4] Modify `reactPrompt(...)` and `answerPrompt(...)` in `backend/src/main/java/com/cornucopai/game/agent/LlmPlayerAgent.java` — add the tools-availability line (FR-012, R-057)
- [X] T045 [US4] Modify `SystemPrompts.java` in `backend/src/main/java/com/cornucopai/game/llm/SystemPrompts.java` — add a bullet in `RULES_AND_CONTRACT` listing all available tools (card info, tour info, project facts, MCP IDE tools) (R-057)

**Checkpoint**: US4 functional — prompts advertise the contextual tools (FR-012).

---

## Phase 7: User Story 5 — Débat structuré avant le vote (Priority: P2)

**Goal**: bounded argument exchange (max 2 rounds) between voter agents before the vote, **disabled by default** since the 2026-11-11 review (`cornucopai.debate.enabled=false`, R-061 — a hypothetical answer had convinced voters without factual verification), enableable via `=true`; implemented as a plain loop in `DebateOrchestrator`. Debate arguments injected into vote prompts as DONNÉES. Depends on US3.

**Independent Test**: with debate enabled, each voter's vote prompt contains the other agents' arguments (US5 scenario 3); with `cornucopai.debate.enabled=false`, no debate, direct vote (US5 scenario 4); debate result visible in logs.

### Tests (write first, MUST fail)

- [X] T046 [P] [US5] Write `DebateOrchestratorTest` in `backend/src/test/java/com/cornucopai/game/debate/DebateOrchestratorTest.java` — with fake brains, `runDebate(leg, voters)` returns one argument per voter per round, bounded to 2 rounds; arguments stored in `LegContext.debateTurns`; disabled flag → empty debate (FR-013, R-050/R-052)
- [X] T047 [P] [US5] Extend `PromptsTest` — vote prompt includes the debate arguments as a `DONNÉES` section when present (US5 scenario 3, R-052)

### Implementation for US5

- [X] T048 [US5] Implement `DebateOrchestrator.java` in `backend/src/main/java/com/cornucopai/game/debate/DebateOrchestrator.java` — bounded sequential exchange over voter agents (2 rounds max), using `langchain4j-agentic` workflow if clean, else plain loop calling each brain's `react`-style method with a debate prompt; result = `List<DebateTurn>` stored in `LegContext` (R-050, R-052)
- [X] T049 [US5] Modify `LegContext.java` in `backend/src/main/java/com/cornucopai/game/llm/LegContext.java` — add `AtomicReference<List<DebateTurn>> debateTurns` (transient, R-052, data-model.md)
- [X] T050 [US5] Modify `GameMaster.java` — before the vote loop in `runTurn`, if `debate.enabled`, call `DebateOrchestrator.runDebate(...)` and set `legContext.debateTurns`; then the vote loop runs with the debate-augmented prompt (R-052)
- [X] T051 [US5] Modify `LlmPlayerAgent.votePrompt(...)` — inject `debateTurns` as `DONNÉES` when non-empty (R-052)
- [X] T052 [US5] Add `cornucopai.debate.enabled=${DEBATE_ENABLED:false}` to `backend/src/main/resources/application.properties` (R-048, FR-013; revised 2026-11-11, R-061: OFF by default, `AtelierService` `@ConfigProperty` default `false` and `GameMaster` 2-arg constructor default `false`)

**Checkpoint**: US5 functional — debate runs before each vote only when explicitly enabled (FR-013, R-061); by default agents vote directly.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: validation end-to-end, logging, docs

- [X] T053 [P] Run quickstart §1 — `cd backend && ./mvnw test` green, all SC-001…SC-008 covered
- [X] T054 [P] Run quickstart §3 — `cd frontend && npm test` green (dropdown spec)
- [X] T055 [P] Structured logging audit — MCP tool calls, `add_project_fact`/`get_project_facts` calls, debate turns, vote decisions all logged with agent/atelier context; no apiKey logged (constitution IV, R-047)
- [X] T056 [P] Update `specs/007-contextual-game-turns/quickstart.md` if any command changed during implementation
- [X] T057 [P] Security note — verify MCP results are NOT wrapped in DONNÉES (clarification 2026-11-11), featureHtml/cards still are; add a comment in `McpToolProviderFactory` about the localhost trust boundary (R-054)
- [X] T058 Run full CI locally — `cd backend && ./mvnw verify` and `cd frontend && npm ci && npm run lint && npm test && npm run build` (constitution: CI = local commands)

---

## Phase 9: Post-review revisions 2026-11-11 (code review of a real game — R-059…R-062)

**Purpose**: traceability for the changes made after reviewing a real game (the debate let a hypothetical « si le champ était un string… » answer convince voters without any factual check). All tasks are DONE and covered by the green suites (backend 193/193, frontend 21/21).

- [X] T059 [P] [US3] Replace String vote fields with enums — new `VoteChoice`/`ExplanationCheck`/`ApplicabilityCheck` in `backend/src/main/java/com/cornucopai/game/model/`, `VoteDto` + `VoteDecision` enum-typed, `ALLOWED_VOTES` string validation removed, tests updated (`VoteDecisionModelTest`, `VoteDecisionTreeTest`, `ModelContractTest`, `LlmPlayerAgentTest`, `AcpCliBrainTest`, `FakeLlmBrainTest`) (R-060, FR-009/FR-010)
- [X] T060 [US5] Debate OFF by default — `cornucopai.debate.enabled=${DEBATE_ENABLED:false}` in `backend/src/main/resources/application.properties`, `AtelierService` `@ConfigProperty` default `false`, `GameMaster` 2-arg ctor default `false`, dead `McpIdeConfig.DEFAULT_DEBATE_ENABLED` removed; `DebateOrchestrator` kept behind the flag (R-061, FR-013)
- [X] T061 [US3] Strengthen vote prompt branch (b) — require factual MCP verification (cite file/class/field/real type via `read_file`/`search_regex`/`search_file`/`list_directory_tree`); hypothetical reasoning explicitly insufficient → `NOT_APPLICABLE` + NO (R-061, FR-009); convert `playPrompt`/`reactPrompt`/`answerPrompt`/`votePrompt`/debate prompt to Java text blocks + `.formatted(...)` in `LlmPlayerAgent.java`
- [X] T062 [US2] Persist project facts at every turn end — new `PlayerAgent.sharedMemory()` default method (null), overridden by `LlmPlayerAgent`; `GameMaster.runTurn` ends with `persistFacts(...)` (`atelier.setProjectFacts(...)` + `store.saveAtelier`); test `SharedMemoryGameTest.factsDiscoveredDuringATurnArePersistedInTheAtelierJson` (FR-016, R-062)
- [X] T063 [US2] Make inheritance reach the live memory — `AtelierService.createAndRun` seeds the shared memory via `memory.setAll(atelier.projectFacts())` after creation (previously facts were only copied into the JSON) (FR-016, FR-007, R-062)
- [X] T064 [P] [US1] MCP client hardening (observed WARN reconnection spam) — `autoHealthCheck(false)` by default (`cornucopai.mcp.ide.auto-health-check`, R-059), `cornucopai.mcp.ide.protocol-version=2025-11-25` pinned to skip auto-detection, bound as `Optional<String>` (SmallRye turns `""` into null); `McpIdeConfigTest.autoHealthCheckDefaultsToFalseAndProtocolVersionToAuto` (R-059)

**Checkpoint**: review deltas are now traceable to tasks; spec/plan/tasks consistent (see `/speckit-analyze` follow-up).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies — start immediately
- **Foundational (Phase 2)**: depends on Phase 1 — BLOCKS all user stories
- **User Stories (Phase 3+)**: depend on Phase 2; US3 depends on US1+US2; US4 depends on US1+US2; US5 depends on US3
- **Polish (Phase 8)**: depends on all desired user stories
- **Post-review revisions (Phase 9)**: depends on Phases 3–7 — revises US1/US2/US3/US5 code after the 2026-11-11 game review (R-059…R-062)

### User Story Dependencies

- **US1 (P1, MCP IDE)**: after Phase 2 — no dependency on other stories
- **US2 (P1, shared memory)**: after Phase 2 — uses US1's toolbox wiring but independently testable
- **US3 (P1, vote tree)**: after US1 + US2 (needs MCP tools + facts for branch b)
- **US4 (P2, Q/R enrichies)**: after US1 + US2 — prompt-only change
- **US5 (P2, débat)**: after US3 — vote-phase orchestration

### Within Each User Story

- Tests MUST be written and FAIL before implementation (constitution II)
- Models before services; services before wiring; wiring before integration

### Parallel Opportunities

- Phase 2: T002–T005 (tests) all [P]; T006–T011 (impl) all [P]
- US1: T012–T015 (tests) all [P]; T016–T021 mostly sequential (factory → brain → wiring → config)
- US2: T022–T026 (tests) all [P]; T034/T035 (frontend) parallel with backend
- US3/US4/US5: test tasks all [P] within each story
- Different user stories can be worked in parallel by different developers once their dependencies are met

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Complete Phase 1 (baseline green)
2. Complete Phase 2 (foundational models)
3. Complete Phase 3 (US1: MCP IDE in toolbox)
4. Complete Phase 4 (US2: shared memory)
5. **STOP and VALIDATE**: run quickstart §1 — SC-001/002/003/006/007 green
6. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. + US1 → MCP-aware agents (demo: agents call `list_directory_tree`)
3. + US2 → shared project memory (demo: facts persisted, inherited)
4. + US3 → structured vote decisions (demo: NO with NOT_APPLICABLE justification)
5. + US4 → enriched Q/R prompts
6. + US5 → debate before vote
7. Each story adds value without breaking previous stories

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [USx] label maps task to its user story for traceability (spec.md US1…US5)
- Every test task MUST be written and observed FAILING before its implementation task (constitution II, Red-Green-Refactor)
- CI stays hermetic: fake `LlmBrain` + mock `McpTransport` + one card family per game (R-055, spec 004 FR-014)
- Commit after each task or logical group
- Stop at each checkpoint to validate the story independently
- Avoid: vague tasks, same-file conflicts, cross-story dependencies that break independence
