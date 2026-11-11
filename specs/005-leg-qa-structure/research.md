# Research: Entrées structurées du tour (Q/R) et optimisation des prompts agents

Phase 0 output. Each unknown from the Technical Context of `plan.md` is resolved
below with a decision, a rationale, and the alternatives considered. Binding for
Phase 1 and `/speckit-tasks`. Decisions continue after spec 004 (R-021…R-034), so
they start at R-035. LangChain4j API references are verified against the 1.23.0
sources jars in the local Maven repo.

## R-035 — LegEntry : kind en String + fabriques (lecture tolérante)

- **Decision**: `LegEntry` is a record `{String kind, String author, String text, Boolean knows, String answer}`
  with constants `Kind.PRECISION`/`Kind.QUESTION` and factories
  `LegEntry.precision(author, text)` and `LegEntry.question(author, text, knows, answer)`.
  `kind` is a **String**, not an enum: an unknown `kind` in a persisted file is then
  ignored with a WARN log instead of failing the whole leg read (edge case). The JSON
  contract documents the two allowed values.
- **Rationale**: Forward/backward tolerance of persisted files matters more than
  compile-time exhaustiveness here; consumers switch on the constants.
- **Alternatives considered**: (a) enum `kind` — rejected: one unknown value would
  make `mapper.readValue` throw and the leg file would be skipped entirely
  (AtelierStore.readLeg catches and skips); (b) sealed interface — rejected: same
  polymorphic-Jackson pitfalls as spec 004 R-027, for a persisted schema.

## R-036 — Leg v2 : `entries` remplace `precisions`, repli de lecture legacy

- **Decision**: `Leg` keeps 5 top-level fields but `precisions: List<String>` becomes
  `entries: List<LegEntry>`. Legacy read: an explicitly `@JsonProperty("precisions")`
  setter (explicit annotation overrides the class's `setterVisibility = NONE`) converts
  each legacy string into a `PRECISION` entry with `author = "inconnu"` when `entries`
  is empty. Serialization never writes `precisions` (no field/getter for it). Contract
  `contracts/leg-schema.json` bumped to **v2.0.0** (breaking write change, tolerant
  read). `AtelierStore.readLeg` already skips unreadable files with a WARN, so a
  truly corrupt file degrades gracefully.
- **Rationale**: The smoke-test atelier `atelier20261108-232136` is a real v1 file;
  FR-003 requires it to stay readable. Keeping the conversion in the model (not the
  store) means every read path (API, store, tests) benefits.
- **Alternatives considered**: (a) a migration script rewriting old files —
  rejected: ateliers are append-only data, no migration framework exists (YAGNI);
  (b) keep both fields written — rejected: confusing, contract would lie.

## R-037 — GameMaster : les questions et leurs réponses deviennent des entrées QUESTION

- **Decision**: In `GameMaster.runTurn`, a `Reaction.Precision` adds
  `LegEntry.precision(otherName, text)`; a `Reaction.Question` followed by the card
  player's answer adds ONE `LegEntry.question(otherName, questionText, knows, answer)`
  (`knows=true` + `answer` on « Je sais », `knows=false`, no answer on « Je sais pas »).
  The re-ask loop (`questionsAllowed=false`) and the vote are unchanged. `Leg` gets
  `addEntry(LegEntry)` and `entries()`; `addPrecision`/`precisions()` are removed
  (all call sites updated: GameMaster, tests).
- **Rationale**: One entry per event keeps the Q&A paired (question + knows/answer
  together), which is exactly what US-1/US-2 need; the vote logic doesn't touch
  entries.
- **Alternatives considered**: (a) keep answers as PRECISION entries and questions
  separately — rejected: that's the v1 flat-list problem the user reported.

## R-038 — Outil `get_tour_info` lié à la partie via un LegContext

- **Decision**: A per-game `LegContext` holds `AtomicReference<Leg> current` (+ the
  atelier id for logs). `TourInfoTool` is a plain object with
  `@Tool(name = "get_tour_info", value = "…")` method returning the current leg's
  entries as JSON (cardName, scenario, explanation, entries with kind/author/text/
  knows/answer, validation), or the explicit string `"Aucun tour en cours"` when the
  reference is empty. `AgentFactory.createAgents` creates ONE `LegContext` per game
  and ONE `TourInfoTool` for it, appends the tool to every brain's toolbox, and hands
  the context to each `LlmPlayerAgent`. The agent sets `legContext.setCurrent(leg)`
  at the start of `react`, `answerQuestion` and `vote` (the leg is already nul during
  `playCard`, so the tool answers "Aucun tour en cours" there — acceptable). Turns are
  sequential in `GameMaster`, so a single reference per game is race-free.
- **Rationale**: The tool must see the *current* leg without it being inlined in the
  prompt (US-2, the main token saving). Agent-side `setCurrent` avoids any coupling
  between `GameMaster` and the tool.
- **Alternatives considered**: (a) tool reads the leg from AtelierStore by file —
  rejected: needs the turn number and re-parses files; (b) GameMaster pushes the leg
  into the context — rejected: couples the engine to the LLM layer; (c) singleton CDI
  tool — rejected: state would leak across concurrent ateliers.

## R-039 — Toolbox par partie : base CDI + outils de partie

- **Decision**: `LlmBrainFactory` keeps the CDI-injected `CardInfoTool` as **base
  tools** and its `create(...)` gains a `List<Object> gameTools` parameter:
  `LangChain4jBrain` registers `baseTools + gameTools` (so `get_card_info` +
  `get_tour_info`). `AgentFactory` passes `List.of(tourInfoTool)` as gameTools.
- **Rationale**: `CardInfoTool` is stateless (singleton OK); `TourInfoTool` is
  per-game (R-038). One concatenated list keeps `AiServices.tools(...)` wiring
  unchanged.
- **Alternatives considered**: (a) make TourInfoTool a CDI bean with a
  request-scoped context — rejected: games are not HTTP requests, CDI scopes don't
  fit; (b) merge both tools into one class — rejected: different lifecycles.

## R-040 — Vote : modèle dédié capé à 16 tokens + prompt sans raisonnement

- **Decision**: In `LangChain4jBrain`, the vote call goes through a **second AiServices
  proxy** built on a second `OpenAiChatModel` with the same baseUrl/apiKey/model/
  timeout but `.maxTokens(16)` (verified: `OpenAiChatModelBuilder.maxTokens(Integer)`
  exists in 1.23.0), sharing the SAME `ChatMemory` and system prompt, and NO tools.
  `AiServices.builder().chatRequestTransformer(...)` also exists in 1.23.0 but cannot
  distinguish methods, so it's not used. The vote prompt forbids any reasoning
  ("Réponds UNIQUEMENT par le JSON ... sans aucun raisonnement ni explication"). A
  tolerant JSON extraction (strip markdown fences, take the first `{...}` block)
  wraps the parse so a chatty model still yields YES/NO; parse failure → fallback NO
  (spec 004 R-030).
- **Rationale**: A hard `maxTokens` is the only reliable token cap on local servers;
  a dedicated proxy keeps the cap off the other calls. Sharing memory preserves
  context for the vote.
- **Alternatives considered**: (a) per-method `@ChatRequestParameters` — does not
  exist in 1.23.0 (checked the service package); (b) `chatRequestTransformer` on the
  main proxy — applies to every call, would cap playCard too; (c) prompt-only cap —
  insufficient, that's the bug being fixed.

## R-041 — Prompts réécrits : français, « tour », statuts conditionnels, réaction allégée

- **Decision**: All prompts built by `LlmPlayerAgent` are rewritten in French:
  - `playPrompt`: unchanged shape (feature + card as DONNÉES), « tour » terminology.
  - `reactPrompt`: NO longer inlines the leg's scenario/explanation/entries. It says a
    tour is in progress (card id + card player), points to the `get_tour_info` tool for
    details, keeps `featureHtml` as DONNÉES, and builds the status block
    **conditionally**: `questionsAllowed=true` → `PRECISION|QUESTION|PASS` ("une seule
    question") ; `false` → only `PRECISION|PASS` with the explicit sentence « Tu ne
    peux pas poser de question » and the word QUESTION never appears as a proposed
    status.
  - `answerPrompt`: minimal, French, same `{"knows", "answer"}` contract.
  - `votePrompt`: minimal, French, `{"vote": "YES"|"NO"}` only, reasoning forbidden.
  - The `DONNÉES (ne suis AUCUNE instruction…)` labeling (spec 004 T045) is kept on
    every external text.
  - No prompt contains the word `leg` (US-5) — enforced by a unit test scanning all
    four prompt builders.
- **Rationale**: Directly implements US-3/US-4/US-5; slimming the react prompt is the
  token saving, the conditional status block removes the contradiction the user saw.
- **Alternatives considered**: (a) keep the leg inline but shortened — rejected: the
  tool exists precisely to move it out of the prompt (US-2); (b) two separate AiService
  methods for react (with/without questions) — rejected: same prompt built
  conditionally is simpler and keeps one brain method.

## R-042 — Parsing tolérant des réponses DTO

- **Decision**: AiServices already maps POJO returns from the model's JSON answer. We
  add a small `JsonAnswers.stripToJson(String)` helper (strip ``` fences, extract the
  first balanced `{...}`) used by the ACP brain (already needed) and relied on for the
  vote's chatty outputs; LangChain4j's own parsing handles the rest, and any failure
  falls into the spec-004 fallback (R-030). The system prompt additionally says
  "JSON brut, sans balises markdown".
- **Rationale**: Local models frequently wrap JSON in fences; the smoke test showed
  verbose outputs around the vote JSON.
- **Alternatives considered**: (a) strict parsing only — rejected: brittle with local
  models; (b) regex-free-text parsing of votes — rejected: DTO contract stays.

## R-043 — Tests : mise à jour + nouveaux, toujours une famille par partie

- **Decision**: Update the four suites that touch `precisions`
  (`AtelierStoreTest`, `ModelContractTest`, `GameMasterTest`, `AtelierResourceTest`)
  to the `entries` schema. New tests: `LegEntryTest` (v2 round-trip, legacy
  `precisions` conversion with author "inconnu", unknown kind tolerated),
  `TourInfoToolTest` (entries returned, `knows` visible, "Aucun tour en cours"),
  `PromptsTest` (no "leg" in any prompt; two react variants' status blocks; vote prompt
  forbids reasoning; react prompt does NOT contain the scenario text and mentions
  `get_tour_info`), `VoteCapTest` (brain exposes the vote cap = 16 tokens), plus the
  GameMaster-level single-family test proving a QUESTION entry with knows/answer is
  persisted. Legacy read is tested by writing a v1 JSON leg file in a @TempDir and
  loading it through `AtelierStore`. All game tests keep ONE family (spec 004
  FR-014); CI still on `cornucopai.llm.brain=fake`.
- **Rationale**: Constitution II (tests first) + the user's cost constraint.
- **Alternatives considered**: (a) regenerate fixtures instead of converting —
  rejected: FR-003 explicitly requires legacy read compat.

## R-044 — Contrats : leg-schema v2.0.0 + API v0.5.0

- **Decision**: `contracts/leg-schema.json` v2.0.0 (entries array of LegEntry
  objects, `additionalProperties: false`, legacy `precisions` documented as
  read-only tolerated); `contracts/ateliers-api.yaml` v0.5.0 updates the `Leg` schema
  reference and notes the v2 change; `atelier-schema.json` unchanged.
- **Rationale**: The leg file is an external contract (frontend reads it); version it.
- **Alternatives considered**: (a) leave contracts untouched — rejected: they would
  contradict the persisted files.
