# Research: Agents conscients du projet — MCP IDE, mémoire partagée, vote par arbre de décision

Phase 0 output. Each unknown from the Technical Context of `plan.md` is resolved
below with a decision, a rationale, and the alternatives considered. Binding for
Phase 1 and `/speckit-tasks`. Decisions continue after spec 006 (R-051…R-05x), so
they start at R-045. LangChain4j API references verified against the official docs
(docs.langchain4j.dev, tag 1.23.0) and the local Maven repo.

## R-045 — New dependencies: `langchain4j-mcp` and `langchain4j-agentic`, both **1.23.0-beta33**

- **Decision**: Add to `backend/pom.xml` under a dedicated `langchain4j-beta.version`
  property set to `1.23.0-beta33` (NOT `1.23.0`):
  - `dev.langchain4j:langchain4j-mcp:1.23.0-beta33` (MCP client)
  - `dev.langchain4j:langchain4j-agentic:1.23.0-beta33` (agentic workflows for the debate)
  Verified against Maven Central `maven-metadata.xml` (2026-11-11): the `langchain4j-mcp`
  and `langchain4j-agentic` modules follow a **separate beta versioning line**
  (`1.23.0-beta33` is the release aligned with core 1.23.0); a plain `1.23.0` of these
  two artifacts does not exist. Core/open-ai stay on `1.23.0`.
- **Rationale**: The MCP client is required for FR-001..FR-004; the agentic module
  gives us workflow patterns (sequential/loop/supervisor) for the debate (FR-013).
- **Alternatives considered**: (a) `quarkus-langchain4j-mcp` extension with
  declarative `application.properties` MCP config — rejected for now: the
  declarative approach wires MCP tools globally via CDI, but we need per-atelier
  dynamic wiring with a configurable URL and tool whitelist; programmatic
  `McpToolProvider` built in a factory gives that control (R-046). The Quarkus
  extension can be adopted later if the URL becomes static. (b) implement the MCP
  protocol by hand over HTTP — rejected: reinventing JSON-RPC + SSE is error-prone.
  (c) pin mcp/agentic to `1.23.0` — impossible, that version does not exist for
  these modules (build failure observed and corrected).

## R-046 — MCP client wiring: `StreamableHttpMcpTransport` + `DefaultMcpClient` + `McpToolProvider` with name filter

- **Decision**: A CDI factory (`McpToolProviderFactory` in `game.mcp`) builds, once
  per atelier (or once per JVM and shared), the chain:
  ```java
  McpTransport transport = StreamableHttpMcpTransport.builder()
          .url(config.url())                       // http://127.0.0.1:64462/stream
          .timeout(config.timeout())
          .logRequests(false).logResponses(false) // structured logging instead
          .build();
  McpClient client = DefaultMcpClient.builder()
          .key("ide-mcp")
          .transport(transport)
          .cacheToolList(true)                      // handshake once, not per agent
          .build();
  McpToolProvider provider = McpToolProvider.builder()
          .mcpClients(client)
          .failIfOneServerFails(false)              // FR-004: MCP down must not block the game
          .filter((c, tool) -> config.allowedToolNames().contains(tool.name())) // FR-002 whitelist
          .build();
  ```
  Each `LangChain4jBrain` then gets this provider via
  `AiServices.builder(...).toolProvider(provider)` **in addition to** the existing
  `.tools(...)` (card info, tour info, project facts) — LangChain4j merges both
  sources. The provider is built once per atelier and shared by all agents of that
  atelier (US1 scenario 5); the tool list is cached by the client.
- **Rationale**: `StreamableHttpMcpTransport` matches the `/stream` endpoint of the
  user's IDE MCP server. `cacheToolList(true)` avoids re-listing tools per agent.
  `.filter(BiPredicate<McpClient, ToolSpecification>)` is exactly the whitelist
  mechanism for FR-002. `failIfOneServerFails(false)` + a try/catch around provider
  build gives the non-blocking behavior of FR-004: if the server is down at build
  time, the provider yields zero tools and every MCP call returns a readable error
  string to the agent (LangChain4j wraps tool execution errors), the game continues.
- **Alternatives considered**: (a) one `McpClient` per agent — rejected: N
  handshakes per atelier, wasteful; (b) `.tools(...)` only, no toolProvider —
  impossible, MCP tools come from a provider; (c) Quarkus CDI `quarkus.langchain4j.mcp.*`
  properties — see R-045.

## R-047 — MCP failure handling: wrap provider build + tool calls

- **Decision**: `McpToolProviderFactory.create()` catches any exception during
  transport/client/provider build and returns an **empty-tool provider** (or null,
  in which case the brain skips `.toolProvider(...)`); a WARN log records the
  failure. At call time, LangChain4j already converts tool-execution exceptions
  into error strings returned to the model. Additionally, `LangChain4jBrain.call()`
  (existing) logs every LLM/tool call duration — MCP tool calls inside an LLM call
  are covered by that log. A dedicated `McpHealthCheck` startup log lists the tools
  actually exposed after the filter (for SC-001/SC-002 verification).
- **Rationale**: FR-004 + SC-007 require the game to run end-to-end with the MCP
  server down. Building the provider must never throw into game creation.
- **Alternatives considered**: (a) fail fast at startup if MCP down — rejected:
  contradicts FR-004/SC-007; (b) retry loop — rejected: YAGNI, the next atelier will
  reconnect anyway.

## R-048 — Configuration properties (FR-014)

- **Decision**: All MCP + debate settings are MicroProfile config properties with
  defaults, no code change needed:
  - `cornucopai.mcp.ide.enabled` (default `true`) — master switch; `false` skips MCP entirely
  - `cornucopai.mcp.ide.url` (default `http://127.0.0.1:64462/stream`)
  - `cornucopai.mcp.ide.tools` (default the 7 comma-separated tool names, FR-002)
  - `cornucopai.mcp.ide.timeout-seconds` (default `30`)
  - `cornucopai.debate.enabled` (default `true`, clarification 2026-11-11)
  - `cornucopai.memory.prompt-window` (default `20`, clarification 2026-11-11)
- **Rationale**: FR-014 (config-only, like spec 004 FR-015). Defaults match the user's
  test setup.
- **Alternatives considered**: (a) per-atelier MCP URL in the POST body — rejected:
  clarification 2026-11-11 says the MCP always serves the IDE's open project, one URL
  for all ateliers is enough.

## R-049 — Shared project memory: custom `@Tool`s backed by a per-atelier store (no native langchain4j shared memory)

- **Decision**: LangChain4j 1.23.0 has **no** native shared `ChatMemory` across
  `AiServices` instances. We follow the spec-005 pattern (R-038/R-039): a
  per-atelier `SharedProjectMemory` object (a thread-safe `List<ProjectFact>` +
  atelier id) held by the `AgentFactory`, exposed to every agent via two `@Tool`
  methods on a `ProjectFactsTool`:
  - `get_project_facts()` → JSON array of all facts (text, author, turnIndex, timestamp)
  - `add_project_fact(String fact)` → appends a `ProjectFact` (author = calling agent
    name, turnIndex = current leg's turnIndex, timestamp = now), returns confirmation
  Both tools are added to each brain's `.tools(...)` alongside `CardInfoTool`,
  `TourInfoTool`, and the MCP provider. Prompt injection of recent facts: the 20
  most recent facts are rendered into a `DONNÉES` section of the user prompt by
  `LlmPlayerAgent` (FR-008, clarification 2026-11-11); all facts via the tool.
- **Rationale**: Same proven pattern as `LegContext`/`TourInfoTool` (spec 005 R-038):
  one shared object per atelier, tools per agent, sequential turns → race-free.
  Keeps memory out of the system prompt (anti-injection, FR-008).
- **Alternatives considered**: (a) a shared `ChatMemory` implementation passed to
  all brains — rejected: ChatMemory stores chat messages, not structured facts, and
  would bloat every prompt with the full history; (b) persist facts only in
  `atelier.json` and read from disk in the tool — rejected: the in-memory list is
  the source of truth during the game, persisted at each save (R-053).

## R-050 — Debate + vote via `langchain4j-agentic-patterns`: `DebatePlanner` + `VotingPlanner`

- **Decision**: Add `dev.langchain4j:langchain4j-agentic-patterns:1.23.0-beta33`
  (separate artifact from `langchain4j-agentic`). Studied
  `dev.langchain4j.agentic.patterns.debate.DebatePlanner(int maxRounds)` — all
  sub-agents except the last are debaters, the last is the judge; the loop stops
  on convergence or `maxRounds` (we use 2). For the final vote aggregation,
  `dev.langchain4j.agentic.patterns.voting.VotingPlanner` with
  `VotingStrategy.majority()` is available if we want a council-style vote; but
  spec 007 keeps the per-agent YES/NO vote with justification (FR-009/FR-010).
  **Final choice (YAGNI, constitution V)**: the agentic `DebatePlanner` API proved
  too heavy for a fixed 2-round exchange, so `DebateOrchestrator` implements a
  plain in-code loop calling each voter's debate prompt — observable behavior
  identical (US5 scenarios). The debate (FR-013) was enabled by default at plan
  time but is **disabled by default since the 2026-11-11 review (R-061)**.
  The final vote contract is unchanged:
  `{"vote": "YES"|"NO", "explanationCheck": ..., "applicabilityCheck": ..., "justification": "..."}`
  with enum-typed fields (R-060).
- **Rationale**: The user explicitly asked to study langchain4j's Debate/Workflow
  offerings; `DebatePlanner` is exactly a bounded multi-agent debate.
- **Alternatives considered**: (a) supervisor pattern — overkill for fixed
  turn-taking; (b) no debate — initially rejected by clarification 2026-11-11
  (enabled by default), then the default was reversed to OFF by R-061 after the
  game review; (c) debate via shared ChatMemory — rejected: see R-049 (per-agent
  system prompts conflict on a shared ChatMemory instance; the agentic
  `AgenticScope` is the agentic-module way, but our facts memory stays a shared
  `@Tool`).

## R-051 — Vote decision tree: extend the vote DTO + prompt, keep token cap

- **Decision**: `VoteDto` becomes a record
  `{String vote, String explanationCheck, String applicabilityCheck, String justification}`
  where `explanationCheck` ∈ `{"OK","INSUFFICIENT"}` and `applicabilityCheck` ∈
  `{"OK","NOT_APPLICABLE"}` (FR-009/FR-010). The vote prompt (FR-011) enumerates
  the two decision-tree questions in French and instructs the model to consult
  `get_tour_info`, `get_project_facts`, and the MCP tools for branch (b). The vote
  LLM call stays on the dedicated capped model (`VOTE_MAX_TOKENS`, spec 005 R-040)
  — the decision tree is reasoning the model does internally; only the short
  structured justification is emitted. The `VoteDecision` (domain object) is
  persisted in the leg's new `votes` array (leg-schema v4.0.0). Fallback on LLM
  failure: vote NO with justification "repli LLM indisponible" (edge case, spec 004
  FR-013).
- **Rationale**: FR-009..FR-011 + SC-004/SC-005/SC-008. Keeping the token cap avoids
  the runaway-reasoning problem of spec 005 while still structuring the decision.
- **Alternatives considered**: (a) separate LLM call per branch — rejected: doubles
  latency per vote; (b) no structured justification, free-text only — rejected:
  SC-005 requires verifiable structure.

## R-052 — Debate orchestration point: in `GameMaster`, before the vote phase

- **Decision**: `GameMaster.runTurn` gains a debate step between the Q/R phase and
  the vote phase: if `cornucopai.debate.enabled=true` (default), call
  `DebateOrchestrator.runDebate(leg, voters, ...)` which returns a list of argument
  strings (one per voter per round, bounded to 2 rounds). NOTE (R-061, review
  2026-11-11): the flag now defaults to OFF — see R-061. The arguments are stored
  transiently on the `LegContext` (like the current leg, spec 005 R-038) and
  injected into each voter's vote prompt as a `DONNÉES` section. Then the normal
  vote loop runs with the decision-tree prompt (R-051).
- **Rationale**: Keeps the debate visible in the trace (US5 scenario 3) and in the
  persisted leg (arguments can be stored alongside votes if needed — optional,
  YAGNI for now, only votes+justifications are persisted per FR-010).
- **Alternatives considered**: (a) debate inside `LlmPlayerAgent.vote()` — rejected:
  an agent can't orchestrate other agents; (b) debate after the vote — rejected:
  the point is to inform the vote.

## R-053 — Persistence: `atelier.json` gains `projectFacts`, leg gains `votes`

- **Decision**: `Atelier` gains `private List<ProjectFact> projectFacts` (additive,
  legacy ateliers read with an empty list — Jackson default). `Leg` gains
  `private List<VoteDecision> votes` (leg-schema.json v4.0.0, additive like spec 006
  v3, tolerant read). `ProjectFact` = record `{String text, String author, int turnIndex, Instant timestamp}`.
  `VoteDecision` = record `{String voter, String vote, String explanationCheck, String applicabilityCheck, String justification}`.
  Both persisted via the existing `AtelierStore` (filesystem JSON). Héritage
  (FR-007, clarification 2026-11-11): `CreateAtelierRequest` gains an optional
  `inheritFactsFrom` (source atelier id); when set and the source has facts, they
  are copied into the new atelier's `projectFacts` at creation. The frontend shows
  a dropdown listing existing ateliers that have facts, only when creating a new
  atelier.
- **Rationale**: Follows the exact additive-evolution pattern of spec 006 (R-047/
  R-048) — legacy files stay readable, new fields default to empty.
- **Alternatives considered**: (a) separate `facts.json` file per atelier —
  rejected: one more file to manage, atelier.json is already the aggregate root;
  (b) store votes in a separate file — rejected: votes belong to the leg.

## R-054 — MCP tools are read-only and trusted (clarification 2026-11-11)

- **Decision**: The 7 whitelisted MCP tools are all read-only IDE operations
  (list/read/search). No write tools are exposed. Results are NOT wrapped in the
  `DONNÉES` anti-injection section (clarification 2026-11-11: localhost server is
  trusted). If `cornucopai.mcp.ide.url` is ever set to a non-localhost URL, the
  plan must be amended to add auth + encapsulation (recorded as a security note in
  plan.md Constitution Check).
- **Rationale**: User's explicit choice; constitution's security-sensitive review
  satisfied by documenting the trust boundary and the revisit condition.
- **Alternatives considered**: (a) always encapsulate MCP results — rejected by
  user (clarification Q2 = option B).

## R-055 — Testing strategy: deterministic MCP mock for CI, real server behind a profile

- **Decision**: Tests use a fake `McpTransport` (in-memory, returns scripted
  `ToolSpecification`s and scripted execution results for the 7 tools) injected
  into the `McpToolProviderFactory` via a `@ConfigProperty cornucopai.llm.brain=fake`-style
  switch: `cornucopai.mcp.ide.mock=true` (default in test profile). The mock lists
  exactly the 7 tools so the filter (R-046) is exercised, and `read_file` returns a
  fixed string. Integration test with the real `http://127.0.0.1:64462/stream` is a
  separate `@QuarkusTest` profile (`-Dtest=RealMcp*`) run manually, never in CI
  (FR-015). Fake `LlmBrain` (spec 004 R-031) scripted to return `VoteDecision`s with
  justifications, so SC-004/SC-005 are testable without a real LLM.
- **Rationale**: CI must stay hermetic (no network, no real LLM — constitution).
- **Alternatives considered**: (a) always real MCP in tests — rejected: CI has no
  IDE server; (b) no MCP in tests at all — rejected: SC-001/SC-002 need the
  toolbox inspection.

## R-059 — MCP client: no auto health-check loop, protocol version pinnable (observed 2026-11-11)

- **Decision**: `DefaultMcpClient` is built with `.autoHealthCheck(false)` by default
  (`cornucopai.mcp.ide.auto-health-check=false`) so that when the IDE MCP server is
  down, the client does NOT spawn a `mcp-server-health-checker` thread that retries and
  logs a WARN stack trace on every attempt (log spam observed 2026-11-11). The MCP
  protocol version can be pinned via `cornucopai.mcp.ide.protocol-version` (e.g.
  `2025-11-25`) to skip the two-probe auto-detection, which some servers reject.
  The property is bound as `Optional<String>` because SmallRye Config converts an empty
  string to null and fails a plain `String` injection.
- **Rationale**: The game already degrades gracefully without MCP (FR-004, empty
  provider); a background reconnection loop adds noise without value in our
  synchronous, one-atelier-at-a-time model. If a future version wants live
  reconnection, flip `auto-health-check=true`.
- **Alternatives considered**: (a) keep auto-health-check and just filter the log —
  rejected: the retries also burn threads/CPU; (b) custom reconnect on next atelier —
  already the behavior: each atelier builds a fresh client, so a server that comes
  back is picked up on the next game without any background loop.

## R-056 — Frontend: one dropdown for fact inheritance (FR-007)

- **Decision**: `ConfigureGameDialog.vue` (`frontend/src/components/ConfigureGameDialog.vue`, spec in `frontend/src/ConfigureGameDialog.spec.ts`) gains a `v-select` listing ateliers that
  have non-empty `projectFacts` (fetched from `GET /api/ateliers` summaries extended
  with a `hasProjectFacts` boolean, API v0.7.0). Label: « Réutiliser les faits projet
  de l'atelier… ». Default: none (memory starts empty). The selected source id is
  sent as `inheritFactsFrom` in the POST body (R-053).
- **Rationale**: Implements clarification 2026-11-11 (dropdown UI) with minimal UI
  change — one select, no new page.
- **Alternatives considered**: (a) checkbox "hériter des faits" without choosing
  the source — rejected: ambiguous when several ateliers have facts; (b) separate
  admin page — rejected: YAGNI.

## R-057 — Prompt changes: react/answer prompts mention MCP + facts tools (FR-012)

- **Decision**: `LlmPlayerAgent.reactPrompt` and `answerPrompt` add one line:
  « Tu peux utiliser les outils MCP (list_directory_tree, read_file, search_*, …)
  et get_project_facts / add_project_fact pour t'appuyer sur le projet analysé. »
  The system prompt (`SystemPrompts.RULES_AND_CONTRACT`) gains a bullet listing
  all available tools. Vote prompt extended per R-051. All prompts stay in French,
  no "leg" (spec 005 FR-007).
- **Rationale**: FR-012 — the model must know the tools exist to use them.
- **Alternatives considered**: (a) rely on tool descriptions alone — rejected:
  models use tools more reliably when the prompt mentions them.

## R-058 — SC mapping sanity check

- SC-001 (toolbox contents) → unit test inspecting brain tools + provider filter.
- SC-002 (real MCP call in a game) → integration test with mock transport, asserting
  the mock's `executeTool` was called for `list_directory_tree`.
- SC-003 (fact shared between agents) → integration test: agent A `add_project_fact`,
  agent B `get_project_facts` sees it.
- SC-004 (NO votes on non-applicable scenario) → scripted fake brain returning
  NOT_APPLICABLE + NO; assert persisted votes.
- SC-005 (100% votes have structured justification) → schema validation of leg JSON.
- SC-006 (atelier isolation) → two ateliers, facts don't leak.
- SC-007 (game runs with MCP down) → mock transport that throws; game completes.
- SC-008 (vote prompt has both tree questions + token cap) → unit test on prompt
  string + `VOTE_MAX_TOKENS` constant.

## R-060 — Vote DTO/decision fields are enums, not strings (review 2026-11-11)

- **Decision**: `VoteDto`, `VoteDecision` and the persisted `votes[]` use three enums —
  `VoteChoice` (YES/NO), `ExplanationCheck` (OK/INSUFFICIENT),
  `ApplicabilityCheck` (OK/NOT_APPLICABLE) — instead of free strings. Jackson maps the
  LLM's JSON enum names directly; an unmapped value fails deserialization and hits the
  existing fallback (vote NO, "repli LLM indisponible"), so no post-hoc string checks
  remain (the old `ALLOWED_VOTES.contains(...)` validation is gone).
- **Rationale**: Review feedback 2026-11-11: stringly-typed vote fields forced manual
  validation and allowed silent drift between prompt, DTO and persistence.
- **Alternatives considered**: (a) keep strings + validation — rejected: exactly the
  useless checks the review flagged; (b) sealed interface hierarchy — overkill for
  three fixed vocabularies.

## R-061 — Debate OFF by default; vote branch (b) requires factual MCP verification (review 2026-11-11)

- **Decision**: `cornucopai.debate.enabled` now defaults to `false` (env
  `DEBATE_ENABLED`, `AtelierService` `@ConfigProperty` default). `DebateOrchestrator`
  stays in the codebase and can be re-enabled per deployment. Independently, the vote
  prompt's branch (b) now REQUIRES a factual check in the real code via MCP tools
  (`read_file`, `search_regex`, `search_file`, `list_directory_tree`) with a citation
  (file, class, field, real type): a hypothetical answer like « si le champ était un
  string... » is explicitly called out as insufficient → `applicabilityCheck=NOT_APPLICABLE`,
  `vote=NO`.
- **Rationale**: Observed failure 2026-11-11: in a real game, a player asked a legitimate
  question (« `object_id` est-il un int ? ») and the card player answered with a
  hypothetical (« si c'était un string, l'attaque serait possible »). That evasive
  answer convinced the other voters and the leg was validated, even though the project
  actually uses an integer PK. The free-form debate amplified the problem instead of
  forcing a factual check; the decision-tree vote with mandatory MCP verification is
  the reliable path. Prompts were also converted to Java text blocks + `.formatted(...)`
  for readability.
- **Alternatives considered**: (a) keep debate on and patch its prompt — rejected:
  the debate's value was not demonstrated and it adds LLM calls per vote; (b) remove
  the debate code entirely — rejected for now: cheap to keep behind the flag if a
  future experiment wants it.

## R-062 — Project facts persisted at every turn end + inheritance seeds the shared memory (review 2026-11-11)

- **Decision**: `GameMaster.runTurn` ends with a snapshot of the agents' shared
  `SharedProjectMemory` into `atelier.projectFacts` + `store.saveAtelier(...)`, so
  facts discovered mid-game are in `atelier.json` after each turn (not only at game
  end). `PlayerAgent` exposes the memory via a new `sharedMemory()` default method
  (null for deterministic agents). Also, `AtelierService` now seeds the shared memory
  with the atelier's facts via `setAll(...)` after creation — previously the
  `inheritFactsFrom` dropdown copied facts into the atelier but never into the live
  memory, so inheritance had no effect on the agents.
- **Rationale**: Review feedback 2026-11-11: facts must survive restarts per turn, and
  the dropdown promise (clarification 2026-11-11) must actually reach the agents.
- **Alternatives considered**: (a) persist only at game end — rejected: a crashed
  run would lose all discoveries; (b) pass the memory into `GameMaster`'s constructor —
  rejected: the agents already hold it, the accessor avoids a second wiring path.
