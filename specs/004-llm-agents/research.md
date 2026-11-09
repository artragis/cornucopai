# Research: Agents joueurs pilotés par LLM (configuration par agent + toolbox carte)

Phase 0 output. Each unknown from the Technical Context of `plan.md` is resolved
below with a decision, a rationale, and the alternatives considered. These decisions
are binding for Phase 1 (data model, contracts, quickstart) and for `/speckit-tasks`.
Decisions continue the project numbering after spec 003 (R-011…R-020).

Verified against the local Maven repository: `quarkus-langchain4j-core` 1.15.0
pulls `dev.langchain4j:langchain4j` + `langchain4j-core` **1.23.0**
(`langchain4j.version=1.23.0` in `quarkus-langchain4j-parent-1.15.0.pom`), so all API
references below are checked against the 1.23.0 sources jars, not guessed.

## R-021 — Intégration LangChain4j : AiServices programmatique par agent

- **Decision**: Each LLM player agent gets its **own** `AiServices` proxy built
  programmatically per game:
  `AiServices.builder(PlayerAiService.class).chatModel(chatModel)
  .systemMessageProvider(id -> systemPrompt).chatMemory(memory).tools(cardInfoTool).build()`.
  The `ChatModel` is built per agent from its own config
  (`OpenAiChatModel.builder().baseUrl(...).apiKey(...).modelName(...).timeout(...)`).
  Quarkus config-property–driven models (`quarkus.langchain4j.*`) are **not** used
  for agents, because those properties are global while FR-001 requires a
  **per-agent** provider.
- **Rationale**: `AiServices` (verified in `langchain4j-1.23.0-sources.jar`) exposes
  `chatModel`, `systemMessageProvider`, `chatMemory`, `tools(Object...)`, `build()` —
  everything needed, with per-instance isolation. One proxy per agent per atelier
  gives memory isolation for free (R-025).
- **Alternatives considered**: (a) `@RegisterAiService` CDI beans with
  `quarkus.langchain4j.openai.*` properties — rejected: global config cannot express
  per-agent base URLs (FR-001/FR-015); (b) calling the ChatModel directly without
  AiServices — rejected: we would re-implement tool loop, memory wiring and
  structured output ourselves.

## R-022 — Nouvelle dépendance pour le modèle OpenAI-compatible

- **Decision**: Add `dev.langchain4j:langchain4j-open-ai` with a new pom property
  `<langchain4j.version>1.23.0</langchain4j.version>` — the exact version already
  pulled transitively by `quarkus-langchain4j-core` 1.15.0 (verified in its parent
  pom), so no version drift. This artifact provides `OpenAiChatModel`, the standard
  client for any OpenAI-compatible endpoint (custom `baseUrl`, optional `apiKey`,
  `modelName`, `timeout`, `maxRetries`).
- **Rationale**: The constitution mandates LangChain4j; using its official OpenAI
  module targets any OpenAI-compatible server (LM Studio / Ollama `/v1` / bionic)
  with a base URL only. Recorded as a deliberate new dependency per the
  constitution; version pinned to the already-used 1.23.0.
- **Alternatives considered**: (a) `io.quarkiverse.langchain4j:quarkus-langchain4j-open-ai`
  extension — rejected: its config model is global per named model, not per agent
  (see R-021); we only need the client classes; (b) hand-rolled HTTP client to
  `/v1/chat/completions` — rejected: we would lose the tool-calling loop, memory
  and retries that AiServices provides.

## R-023 — Toolbox : un `@Tool` LangChain4j branché sur un service de domaine

- **Decision**: The toolbox is a plain CDI/plain object `CardInfoTool` with one
  method annotated `@Tool(name = "get_card_info", value = "...")`
  (`dev.langchain4j.agent.tool.Tool`, verified in `langchain4j-core-1.23.0`):
  `String getCardInfo(@P("identifiant de la carte, ex. VE2") String cardId)`.
  It delegates to `CardInfoService` and returns a Markdown string (id, valeur,
  famille, description, url, mappings, contenu riche). Registered per agent via
  `AiServices.builder(...).tools(cardInfoTool)` (verified signature
  `tools(Object...)` in 1.23.0).
- **Rationale**: `@Tool` methods are discovered by reflection by AiServices and
  exposed to the model as function calls; returning a String keeps the tool
  robust regardless of how the model serializes structured results, and the
  Markdown is readable in logs. The tool's data logic lives in a testable service,
  not in the annotation.
- **Alternatives considered**: (a) returning the `InfoCarte` POJO directly —
  possible (AiServices JSON-serializes tool results) but harder to eyeball in logs
  and couples the model to our Java types; (b) `ToolProvider`/`ToolSpecification`
  maps — rejected: more wiring for a single tool; `@Tool` is the idiomatic minimal
  path and scales by adding methods.

## R-024 — Sources de l'outil « infos carte » et résolution de l'identifiant

- **Decision**: `CardInfoService` (new, in `com.cornucopai.game.card`) builds a
  cached index at first use by scanning the **latest-version** `source/*-cards-*.yaml`
  of every edition (reusing `CardCatalogService`'s scan/version logic, R-013):
  `cardId → [(setId, version, suitId, suitName, CardEntry)]`. For a given id it then
  reads: (1) the card entry (id, value, desc, url, misc) from the set's YAML;
  (2) the mappings from `source/<edition>-mappings-<version>.yaml` (same suits/cards
  skeleton, plus `stride`, `capec`, `owasp_asvs`, `owasp_dev_guide`, `safecode`,
  `capec_map` when present — verified on `webapp-mappings-3.0.yaml`, e.g. VE2 →
  STRIDE [I], CAPEC [54,113,116,…], ASVS […]); (3) the rich content by **walking**
  `cornucopia.owasp.org/data/cards/<edition>-cards-<version>-en/<familyDir>/<cardId>/`
  and reading `explanation.md` + `technical-note.md` (verified: 80 card dirs under
  `webapp-cards-3.0-en`, each with those two files; family dirs are slugged suit
  names like `data-validation-&-encoding`). Walking is used instead of computing
  the slug, so renames in the upstream repo cannot break the lookup. Only `-en`
  rich content exists at the time of writing (spec assumption). If the id exists in
  several sets, all locations are listed and the current atelier's sets win when
  known (FR-008); unknown id → explicit "carte inconnue" string (FR-009); missing
  rich content or mappings → returned data + explicit note, never an error
  (edge cases).
- **Rationale**: The three sources are complementary: YAML = authoritative short
  description, mappings = security cross-references, `data/cards` = long-form
  scenario/mitigations that make the agent's plays credible. One id-only entry point
  matches FR-007.
- **Alternatives considered**: (a) slug computation from suit name — rejected:
  fragile (`&`, accents, renames); walking the directory is exact; (b) scraping the
  website HTML — rejected: markdown files are the source of truth on disk;
  (c) indexing `webapp-capec-3.0.yaml`/`webapp-asvs-3.0.yaml` separately — not
  needed: per-card mappings already live in `*-mappings-*.yaml`.

## R-025 — Mémoire : une fenêtre par agent et par atelier

- **Decision**: Each agent's AiServices proxy gets its own
  `MessageWindowChatMemory.builder().id(atelierId + "/" + agentName)
  .maxMessagesProvider(id -> 40).build()` (`dev.langchain4j.memory.chat.MessageWindowChatMemory`,
  verified in the `langchain4j-1.23.0` aggregate jar — builder requires `id` and a
  `maxMessagesProvider` in 1.23.0). Because each agent has its own AiService
  instance and its own `ChatMemory`, memory is isolated per agent and per game with
  no shared store and nothing persisted across ateliers (FR-010, US-4).
- **Rationale**: AiServices appends every user/assistant exchange to the attached
  memory, so the agent naturally "remembers" the legs of the current game; the
  sliding window (40 messages) bounds cost/latency. Per-instance memory avoids any
  cross-atelier leakage by construction.
- **Alternatives considered**: (a) shared `ChatMemoryStore` keyed by memoryId —
  rejected: shared mutable state across concurrent ateliers risks leakage and
  needs cleanup; (b) no memory, context re-injected in each prompt — rejected:
  duplicates tokens every turn and contradicts US-4; (c) persisting memory to disk
  per atelier — rejected: FR-010 says nothing persists between ateliers.

## R-026 — Prompt système : personnalité d'archétype + règles + contrat de sortie

- **Decision**: `SystemPrompts.forArchetype(Archetype)` builds the system prompt:
  rules of Cornucopia (what a leg is, reaction phase, question/answer, vote),
  the personality of the archetype (Grumpy/Happy/Greedy/Prof as prose, no code),
  and the **output contract** (the LLM must answer in the exact JSON shape of the
  requested DTO — see R-027). The user message per call carries the concrete game
  context (feature HTML, card, leg, question…).
- **Rationale**: Keeps the four archetypes alive as personalities (spec assumption)
  without deterministic code (FR-003); the output contract makes parsing reliable.
- **Alternatives considered**: (a) one generic prompt for all agents — rejected:
  US-4 requires per-archetype personality; (b) few-shot examples per archetype —
  deferred (YAGNI), can be added to the prompt later without structural change.

## R-027 — Sorties typées : DTOs + mapping, pas de désérialisation polymorphe

- **Decision**: The AiService interface `PlayerAiService` has four methods, each
  taking a single `@V("input") String` user prompt (built by the agent with full
  context) and returning a small record DTO: `PlayedCardDto(scenario, explanation)`,
  `ReactionDto(type, text)` with type ∈ PRECISION|QUESTION|PASS,
  `AnswerDto(knows, answer)` (knows=false → "Je sais pas"),
  `VoteDto(vote)` with vote ∈ YES|NO. AiServices maps the model's JSON answer onto
  the record (LangChain4j structured output). `LlmPlayerAgent` then maps DTO →
  domain types (`PlayedCard`, `Reaction.*`, `Answer.*`, `Vote`), applying the
  fallback (R-030) on any mismatch. `cardName` always comes from the dealt card,
  never from the LLM.
- **Rationale**: Records are trivially deserializable by Jackson (used by
  LangChain4j); mapping to the sealed domain types (`Reaction`, `Answer`) happens
  in our code, avoiding polymorphic-deserialization pitfalls on sealed interfaces.
  Single-string input keeps the prompt fully under our control.
- **Alternatives considered**: (a) returning domain sealed interfaces directly
  from the AiService — rejected: Jackson needs `@JsonTypeInfo` on the sealed
  interfaces and any model deviation breaks the game loop; (b) free-text answers
  parsed by regex — rejected: brittle.

## R-028 — Modèle de configuration par agent (ACP vs URL OpenAI)

- **Decision**: Persisted entity `AgentLlmConfig` (record): `providerType`
  ∈ `OPENAI_COMPATIBLE | ACP`; for `OPENAI_COMPATIBLE`: `baseUrl`, `apiKey`
  (optional), `modelName`; for `ACP`: `acpProvider` ∈ `github-copilot |
  mistral-vibe` (+ optional `command` override). Defaults come from MicroProfile
  config: `cornucopai.llm.base-url=${LLM_BASE_URL:http://localhost:1235/v1}`,
  `cornucopai.llm.api-key=${LLM_API_KEY:}`, `cornucopai.llm.model=${LLM_MODEL:}`,
  `cornucopai.llm.timeout-seconds=${LLM_TIMEOUT_SECONDS:120}` (FR-002). Per-agent
  overrides arrive as an optional `agents` array in `POST /api/ateliers`
  (`CreateAtelierRequest` extension); when absent, every player gets the defaults
  with round-robin archetypes as today. The resolved per-agent configs are
  persisted in `atelier.json` (new `agentConfigs` field, index-aligned with
  `players`) — FR-012. Unknown provider type / blank base URL on
  OPENAI_COMPATIBLE → 400 at launch (edge case, no half-configured atelier).
- **Rationale**: FR-001/FR-002/FR-015/FR-012 in one model; env-var defaults keep the
  zero-config path on the local bionic server (cost control); persisting with the
  atelier makes games inspectable/replayable.
- **Alternatives considered**: (a) config only in `application.properties` —
  rejected: not per-agent (FR-001); (b) a separate config file per atelier —
  rejected: `atelier.json` is already the atelier's source of truth; (c) default
  model name hardcoded — rejected: servers differ (LM Studio ignores the name,
  Ollama needs a real one), so it stays an env var with empty default and the
  client sends a placeholder when blank (documented in quickstart).

## R-029 — Un seul agent, des « brains » interchangeables (ACP vs LangChain4j)

- **Decision**: Exactly one `PlayerAgent` implementation for games:
  `LlmPlayerAgent` (FR-003). It depends on a small internal interface `LlmBrain`
  with the four DTO methods of R-027. Two brains:
  - `LangChain4jBrain` — the R-021 AiServices proxy (ChatModel + system prompt +
    memory + `CardInfoTool`), used for `OPENAI_COMPATIBLE` configs. This is the
    default and the only brain exercised end-to-end this increment.
  - `AcpCliBrain` — for `ACP` configs (`github-copilot`, `mistral-vibe`): one-shot
    invocation of the provider CLI as a subprocess with the full prompt, output
    parsed like any other brain answer. **Documented limitation**: ACP CLI agents
    get no LangChain4j toolbox and no memory this increment (the tool loop and
    memory live in our orchestration); the card info they need is inlined in the
    prompt when relevant. The brain is unit-tested with a fake command.
  `LlmBrainFactory` builds the right brain from an `AgentLlmConfig`.
  Deterministic agents (`GrumpyAgent`…`ProfAgent`) stay in the codebase **only**
  for unit tests; `AgentFactory` no longer returns them for games.
- **Rationale**: "Une seule implémentation des agents basés sur l'IA" = one
  `PlayerAgent`; the LLM transport behind it is pluggable, which is exactly what
  makes the per-agent ACP/URL choice (FR-001) possible without duplicating agent
  logic. ACP via CLI one-shot is the pragmatic transport for copilot/vibe CLIs;
  a full ACP stdio session is future work and is out of scope for this increment.
- **Alternatives considered**: (a) full ACP (Agent Client Protocol) stdio client
  now — rejected: heavy protocol work, untestable in CI, not needed to satisfy the
  config requirement; (b) separate PlayerAgent per provider type — rejected:
  violates the single-implementation requirement (FR-003).

## R-030 — Repli non bloquant en cas d'échec du LLM (FR-013)

- **Decision**: Every `LlmBrain` call in `LlmPlayerAgent` is wrapped in try/catch
  (plus a DTO-validation step: unknown enum value, blank required field →
  fallback). On failure: structured log (WARN) with atelier/turn/agent, then a
  deterministic default for that decision only: `playCard` → scenario "Scénario
  générique" + explanation quoting the card id/description; `react` → `Pass`;
  `answerQuestion` → `DoesNotKnow`; `vote` → `NO`. The game always continues.
  Transport-level timeout comes from the ChatModel builder (`timeout`,
  R-028) and `maxRetries(1)`.
- **Rationale**: A local LLM is slow/flaky by nature; one bad call must never kill
  a game (edge case + SC-007).
- **Alternatives considered**: (a) fail the whole game on LLM error — rejected:
  contradicts FR-013; (b) retry loop with backoff — rejected: `maxRetries(1)` at
  the client is enough, more retries just multiply latency on a dead endpoint.

## R-031 — Tests : un seul famille par test, LLM simulé, fixture enrichie

- **Decision**: (1) Deterministic agent tests (`GrumpyAgentTest` etc.) are kept
  unchanged — they only cover the test-only agents (SC-006). (2) `LlmPlayerAgent`
  is unit-tested with a scripted fake `LlmBrain` (queues of DTOs, like
  `GameMasterTest.StubAgent`) — no HTTP, no model. (3) `CardInfoService` /
  `CardInfoTool` are tested against the existing hermetic fixture
  `backend/src/test/resources/cornucopia-fixture/`, **extended** with a mini
  `cornucopia.owasp.org/data/cards/webapp-cards-3.0-en/<family>/<id>/` tree
  (explanation.md + technical-note.md) and a `webapp-mappings-3.0.yaml` fixture, so
  the three data sources are all covered hermetically. (4) Game-level integration
  test: one atelier, **one single family** (`webapp:VE` in the fixture = 2 cards),
  2 players, fake brains → ≤ 2 legs (FR-014, SC-005). (5) A real end-to-end run
  against the local bionic server is a **manual** quickstart step, never in CI.
- **Rationale**: LLM calls are slow; one family + tiny fixture + fake brains make
  the suite fast, deterministic and CI-safe (constitution II: tests first, R-020
  pattern reused).
- **Alternatives considered**: (a) mocking LangChain4j's `ChatModel` — avoided:
  our `LlmBrain` seam is simpler and tests our mapping/fallback logic, not
  LangChain4j; (b) WireMock-style fake OpenAI server — rejected: heavier than a
  fake brain, and still no guarantee a local model behaves; (c) playing several
  families in tests — rejected: FR-014 exists precisely to bound the number of
  turns.

## R-032 — Contrat HTTP : extension de POST /api/ateliers, frontend inchangé

- **Decision**: `CreateAtelierRequest` gains an optional `agents` array
  (`[{name?, archetype?, providerType, baseUrl?, apiKey?, modelName?, acpProvider?}]`);
  `name` defaults to `Joueur-N`, `archetype` to the round-robin order as today.
  Response/`atelier.json` gain `agentConfigs: AgentLlmConfig[]` (index-aligned with
  `players`). Validation (400): unknown `providerType`/`acpProvider`, blank
  `baseUrl` for OPENAI_COMPATIBLE, `agents.length > playerCount`. The frontend is
  **unchanged** this increment (YAGNI): games launch with defaults (bionic local);
  a per-agent UI is a later increment. Contracts: `contracts/ateliers-api.yaml`
  v0.4.0 + `contracts/atelier-schema.json` updated; `leg-schema.json` unchanged.
- **Rationale**: FR-001/FR-015 need only request-level config; the spec's US-1 says
  "formulaire ou requête" — the API satisfies it now, the form later.
- **Alternatives considered**: (a) build the per-agent UI now — rejected: not
  required by any FR, adds frontend scope; (b) config via a separate YAML file per
  atelier — rejected (R-028c).

## R-033 — Où vivent les nouvelles classes (SRP, constitution III)

- **Decision**: New package `com.cornucopai.game.llm` for `AgentLlmConfig`,
  `LlmBrain`, `LangChain4jBrain`, `AcpCliBrain`, `LlmBrainFactory`, `PlayerAiService`
  + DTOs, `SystemPrompts`; `LlmPlayerAgent` in `com.cornucopai.game.agent` next to
  the test-only deterministic agents (their javadoc will say "unit tests only").
  `CardInfoService` + `CardInfoTool` + `CardInfo`/`CardMappings` in
  `com.cornucopai.game.card[.model]`. `AgentFactory` switches to building
  `LlmPlayerAgent`s; `AtelierService` resolves configs (request `agents` →
  defaults) and passes them down; `Atelier` gains `agentConfigs`.
- **Rationale**: Keeps the game engine (`GameMaster`, `PlayerAgent` contract)
  untouched; all LLM concerns in one package; card knowledge stays in the card
  package.
- **Alternatives considered**: (a) everything in `game.agent` — rejected: mixes
  LLM transport with card data access; (b) a top-level `llm/` source dir —
  rejected: constitution allows only `backend/` and `frontend/` as top-level dirs.

## R-034 — Concurrence et cycle de vie

- **Decision**: Scale stays "one atelier at a time" (as spec 003). AiService proxies,
  brains, memories and tools are built per game inside `AtelierService.createAndRun`
  and discarded after; nothing is cached across games except the read-only
  card-catalog/card-info indexes (like R-019). This makes memory isolation and
  config reproducibility structural rather than enforced by cleanup code.
- **Rationale**: FR-010 isolation + FR-012 persistence with zero shared mutable
  state; matches existing single-game scale.
- **Alternatives considered**: (a) pooled AiServices across games — rejected:
  would leak memory between ateliers and configs between providers.
