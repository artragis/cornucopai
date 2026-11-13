# Data Model: Agents conscients du projet — MCP IDE, mémoire partagée, vote par arbre de décision

Derived from `spec.md` (Key Entities + FR-001…FR-015 + clarifications 2026-11-11)
and `research.md` (R-045…R-065). Persistence stays filesystem JSON; all changes are
additive with tolerant legacy reads (same pattern as spec 006 v3).

## Entities

### McpIdeConfig — NEW, runtime config, not persisted (R-048)

| Field | Type | Default | Description |
|---|---|---|---|
| `enabled` | boolean | `true` | Master switch; `false` skips MCP entirely |
| `url` | string | `http://127.0.0.1:64462/stream` | MCP streamable-HTTP endpoint (FR-001) |
| `allowedToolNames` | Set<string> | the 7 tool names (FR-002) | Whitelist applied via `McpToolProvider.filter` (R-046) |
| `timeoutSeconds` | int | `30` | Transport timeout |

MicroProfile properties: `cornucopai.mcp.ide.enabled`, `.url`, `.tools`, `.timeout-seconds`.

### ProjectFact (fait projet) — NEW, persisted in `atelier.json` (R-053)

| Field | Type | Rules |
|---|---|---|
| `text` | string, minLength 1 | The fact itself (e.g. « pas d'API REST publique ») |
| `author` | string | Name of the agent that discovered it |
| `turnIndex` | int | Tour where it was discovered (FR-006) |
| `timestamp` | Instant | When it was recorded |

Record: `ProjectFact(String text, String author, int turnIndex, Instant timestamp)`.

### SharedProjectMemory — NEW, runtime per-atelier + persisted (R-049, R-053)

One instance per atelier. Thread-safe ordered list of `ProjectFact`.
- `List<ProjectFact> facts()` — all facts, for the `get_project_facts` tool.
- `void add(ProjectFact)` — for the `add_project_fact` tool.
- `List<ProjectFact> recent(int n)` — the n most recent, for prompt injection (n=20, FR-008).
- Persisted as `projectFacts` in `atelier.json`; loaded at atelier read (legacy → empty list).
- Isolated per atelier; optional copy from a source atelier at creation (FR-007, dropdown).

### ProjectFactsTool — NEW, one per atelier, `@Tool` provider (R-049)

| Tool | Returns |
|---|---|
| `get_project_facts()` | JSON array of all `ProjectFact` |
| `add_project_fact(String fact)` | Confirmation string; author/turn/timestamp filled by the tool from context |

Registered in every brain's `.tools(...)` next to `CardInfoTool` and `TourInfoTool`.

### VoteDecision — NEW, persisted in the leg file `votes` array (R-051, R-053)

| Field | Type | Rules |
|---|---|---|
| `voter` | string | Name of the voting agent |
| `vote` | string | `YES` \| `NO` |
| `explanationCheck` | string | `OK` \| `INSUFFICIENT` (branch a, FR-009) |
| `applicabilityCheck` | string | `OK` \| `NOT_APPLICABLE` (branch b, FR-009) |
| `justification` | string | Short free-text reason (FR-010) |

Record: `VoteDecision(String voter, String vote, String explanationCheck, String applicabilityCheck, String justification)`.

### Leg (tour) — v3 → v4 (R-053)

Persisted as `leg-{NNN}-{playerSlug}.json`, contracts/leg-schema.json **v4.0.0**:
additive field `votes: array of VoteDecision` (empty for legacy legs; legacy v1
`precisions` conversion of v2 unchanged, R-036 of spec 005). All other fields
unchanged (cardName, scenario, explanation, entries, validation, turnIndex,
majorSuit, suitId, cardValue, points).

### Atelier — extended (R-053)

Additive field `projectFacts: List<ProjectFact>` in `atelier.json` (atelier-schema
v0.7.0). Legacy ateliers read with an empty list. `CreateAtelierRequest` gains
optional `inheritFactsFrom: string` (source atelier id); when set, facts are copied
at creation (FR-007).

### DebateTurn — NEW, runtime only, not persisted (R-052)

One per debate round: `{String agent, String argument}`. Held transiently in
`LegContext` (alongside the current leg), injected into vote prompts as DONNÉES.

## Relationships

```
Atelier 1──* ProjectFact          (persisted in atelier.json)
Atelier 1──1 SharedProjectMemory  (runtime, persisted snapshot)
Leg 1──* VoteDecision             (persisted in leg-*.json votes[])
LegContext 1──1 Leg (current)     (spec 005 R-038)
LegContext 1──* DebateTurn        (transient, this feature)
McpIdeConfig ──► McpToolProvider ──► 7 whitelisted MCP tools (per atelier, shared)
ProjectFactsTool ──► SharedProjectMemory (per atelier, shared by agents)
```

## Validation rules (from FRs)

- `ProjectFact.text` non-blank (FR-006).
- `VoteDecision.vote` ∈ {YES, NO}; checks ∈ {OK, INSUFFICIENT} / {OK, NOT_APPLICABLE} (FR-009/FR-010).
- MCP tool names ⊆ configured whitelist (FR-002, enforced by provider filter R-046).
- Prompt injection window = 20 most recent facts (FR-008, clarification 2026-11-11).

## Unchanged

`Card`, `AgentLlmConfig`, `Participant`, `Tour`, `PlayerAgent` contract,
`GameMaster` turn/vote/score rules (only the debate step + VoteDecision persistence
are added, R-052/R-053), the spec-004 fallback (R-030), leg v2/v3 fields.
