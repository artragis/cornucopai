# Data Model: Agents joueurs pilotés par LLM (config par agent + toolbox carte)

Derived from `spec.md` (Key Entities + FR-001…FR-015) and the decisions in
`research.md` (R-021…R-034). Persistence stays filesystem JSON (R-002 from spec 002);
`atelier.json` gains the resolved per-agent LLM configs (FR-012). Nothing LLM-related
is persisted across ateliers except the config (memory is per game, R-025/R-034).

## Entities

### AgentLlmConfig (configuration LLM d'un agent) — persisted, R-028

One per player, index-aligned with `Atelier.players`. Persisted in `atelier.json` as
`agentConfigs[]` (contracts/atelier-schema.json).

| Field | Type | Description | Rules |
|---|---|---|---|
| `providerType` | string enum | `OPENAI_COMPATIBLE` \| `ACP` | Required; unknown value → 400 at launch |
| `baseUrl` | string? | OpenAI-compatible base URL, e.g. `http://localhost:1235/v1` | Required when `providerType=OPENAI_COMPATIBLE`, non-blank; default from `cornucopai.llm.base-url` (FR-002) |
| `apiKey` | string? | Optional API key | Empty = no auth (default, bionic local) |
| `modelName` | string? | Model name sent to the endpoint | Default from `cornucopai.llm.model` (may be blank → placeholder sent, see R-028) |
| `acpProvider` | string? | `github-copilot` \| `mistral-vibe` | Required when `providerType=ACP`; unknown → 400 |
| `command` | string? | Optional CLI override for the ACP provider | Test seam; defaults to the provider name |

Defaults when the request has no `agents` array: one `OPENAI_COMPATIBLE` config per
player with baseUrl=`http://localhost:1235/v1`, no key (FR-002).

### ConfigurationAgent (agent de la partie) — request-level, R-032

| Field | Type | Description | Rules |
|---|---|---|---|
| `name` | string? | Player name | Defaults to `Joueur-N` |
| `archetype` | enum | `GRUMPY` \| `HAPPY` \| `GREEDY` \| `PROF` | Defaults to round-robin order (R-004) |
| `llm` | AgentLlmConfig fields (inline) | Per-agent provider | See AgentLlmConfig |

`agents.length` must be ≤ `playerCount` (extra entries → 400).

### LlmBrain (interface interne, non persisté) — R-029

The single seam between the agent and the LLM transport. Methods (all take the
context as data, return DTOs):

- `PlayedCardDto playCard(PlayContext ctx)`
- `ReactionDto react(ReactContext ctx)`
- `AnswerDto answerQuestion(String question)`
- `VoteDto vote(Leg leg, Participant cardPlayer)`

Implementations: `LangChain4jBrain` (OPENAI_COMPATIBLE, AiServices + tools + memory,
R-021/R-023/R-025) and `AcpCliBrain` (ACP, one-shot CLI, no toolbox this increment,
R-029). Tests use a scripted fake (R-031).

### DTOs de sortie du LLM (records, R-027)

| DTO | Fields | Maps to |
|---|---|---|
| `PlayedCardDto` | `scenario`, `explanation` | `PlayedCard(card.id(), scenario, explanation)` — cardName never from the LLM |
| `ReactionDto` | `type` ∈ `PRECISION`\|`QUESTION`\|`PASS`, `text` | `Reaction.Precision(text)` / `Reaction.Question(text)` / `Reaction.Pass()`; invalid/blank → fallback `Pass` (R-030) |
| `AnswerDto` | `knows` (boolean), `answer` | `Answer.Knows(answer)` / `Answer.DoesNotKnow()`; `knows=true` with blank answer → `DoesNotKnow` |
| `VoteDto` | `vote` ∈ `YES`\|`NO` | `Vote.YES` / `Vote.NO`; invalid → `NO` |

### InfoCarte (résultat de l'outil carte) — R-024, built on demand, cached index

| Field | Type | Source | Rules |
|---|---|---|---|
| `id` | string | YAML `cards[].id` | The queried id |
| `value` | string | YAML `value` | — |
| `setId`, `version` | string | YAML `meta` | Latest version per edition (R-013) |
| `suitId`, `suitName` | string | YAML `suits[]` | — |
| `language` | string | YAML `meta.language` | Language of the card file used |
| `description` | string | YAML `desc` | Non-empty |
| `url`, `misc` | string? | YAML | Optional |
| `mappings` | object? | `<edition>-mappings-<version>.yaml` | `stride`, `stride_print`, `capec`, `capec_map`, `owasp_asvs`, `owasp_asvs_print`, `owasp_dev_guide`, `owasp_dev_guide_print`, `safecode` — absent file → null + note |
| `explanation` | string? | `data/cards/<set>-cards-<ver>-en/<familyDir>/<id>/explanation.md` | Absent → null + note |
| `technicalNote` | string? | `.../technical-note.md` | Absent → null + note |
| `locations` | array of `{setId, version, suitId}` | index | >1 entry when the id exists in several sets; atelier's sets preferred (FR-008) |

Tool contract (`@Tool get_card_info(String cardId)`): returns InfoCarte rendered as
Markdown; unknown id → the string `"Carte inconnue : <id>"` (FR-009).

### Atelier — extended from spec 003

Adds one field; everything else unchanged:

| Field | Type | Description | Rules |
|---|---|---|---|
| `agentConfigs` | array of AgentLlmConfig | Resolved per-player LLM configs | Index-aligned with `players`; persisted (FR-012); required in schema |

### Leg, Participant, Card, CardSet, CardFamily

Unchanged from specs 002/003 (`leg-schema.json` stays 5 fields; `cardName` = real card
id referencing `deck` in `atelier.json`).

## State / lifecycle

- `AgentLlmConfig` lifecycle: built from request+defaults at launch → persisted with
  the atelier → read back only for display/replay. No transitions.
- Per-game runtime objects (AiService proxy, `ChatMemory`, brain, tool) are created in
  `AtelierService.createAndRun` and discarded at the end (R-034): memory never leaves
  the game (FR-010).
- LLM failure at any decision point → fallback value for that decision only (R-030);
  the leg/vote state machine in `GameMaster` is unchanged.
