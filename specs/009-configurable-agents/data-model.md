# Data Model: Agents configurables (objets JSON)

Derived from `spec.md` (Key Entities + FR-001…FR-020 + Clarifications 2026-11-12) and
`research.md` (R-001…R-012). Persistence stays filesystem JSON: one file per agent under
`agents/` (R-001), ateliers under `ateliers/` with a per-player snapshot (R-005).

## Entities

### Agent — persisted, one file `agents/{id}.json` (R-001)

The configurable agent object managed on the dedicated page (US-1).

| Field | Type | Description | Rules |
|---|---|---|---|
| `id` | string (UUID) | Technical identifier | Generated at creation (`UUID.randomUUID()`), immutable on update (FR-003); file name `{id}.json` |
| `agentName` | string | Human-readable name, shown in UI | Required, non-blank after trim; unique across all agents (FR-018, 409 on duplicate) |
| `openAiCompatibleUrl` | string | LLM endpoint URL (local, Mistral API, Azure Foundry…) | Required; must be an absolute `http`/`https` URL (FR-019) |
| `apiKeyVar` | string? | Name of the env var holding the API key | Optional; empty = no key required (FR-006); if set, must match `^[A-Za-z_][A-Za-z0-9_]*$`. **The key value is never stored** |
| `modelName` | string | Model name sent to the endpoint | Required, non-blank (FR-019) |
| `personalityPrompt` | string | Free-text personality/instructions, replaces the old archetype | Required, non-blank (FR-019, clarification Q4); transmitted as system-prompt addition at game time (FR-012) |

Lifecycle: created via `POST /api/agents` → listed → updated via `PUT` (`id` unchanged) →
deleted via `DELETE` (file removed; no longer selectable, FR-005). Deleting an agent does
not touch existing ateliers: they keep their snapshot (FR-017, edge case).

### AgentSnapshot — persisted inside `atelier.json` as `agentSnapshots[]` (R-005, FR-017)

Immutable copy of the agent config at launch, index-aligned with `players[]`.

| Field | Type | Rules |
|---|---|---|
| `id` | string | Agent id at launch |
| `agentName` | string | Copied |
| `openAiCompatibleUrl` | string | Copied |
| `apiKeyVar` | string? | Copied (name only — never the key value, FR-006/FR-017) |
| `modelName` | string | Copied |
| `personalityPrompt` | string | Copied |

### Participant — persisted in `atelier.json` as `players[]`, modified (R-007)

| Field | Type | Description | Rules |
|---|---|---|---|
| `name` | string | Player name | `Joueur-N`, N = position in the order drawn at launch (FR-016, FR-020) |
| `agentId` | string | Id of the agent driving this player | References an `Agent.id` at launch; snapshot in `agentSnapshots[]` |
| `archetype` | string enum? | **Legacy only** (`GRUMPY`/`HAPPY`/`GREEDY`/`PROF`) | Read from pre-009 ateliers (FR-014); never written anymore |

Two participants may share the same `agentId` (FR-009): distinct `name`, distinct turns,
same agent configuration.

### Atelier — persisted, extended (FR-017, R-005/R-011)

Existing fields unchanged (`id`, `createdAt`, `featureHtml`, `playerCount` kept for legacy
reads, `players`, `turnOrder`, `language`, `selectedFamilies`, `deck`, `sets`, legs…).

- `players[]` gains `agentId` (and loses `archetype` on new writes).
- New `agentSnapshots[]`: index-aligned with `players[]`, one `AgentSnapshot` per player.
- Legacy `agentConfigs[]` (spec 004): still readable, marked deprecated, no longer written.

### CreateAtelierRequest — API v0.8.0 request body (R-004)

| Field | Type | Rules |
|---|---|---|
| `agentIds` | string[] | **Required, min 2** (FR-010); each entry binds one participant to that agent; duplicates allowed (FR-009); unknown id → 400 |
| `featureHtml` | string | Required, non-empty (unchanged) |
| `language` | string? | Default `fr` (unchanged) |
| `families` | string[] | Required, ≥ 1 `setId:familyId` (unchanged) |
| `inheritFactsFrom` | string? | Optional source atelier id (spec 007, unchanged) |

Removed from v0.8.0: `playerCount` (integer) and `agents[]` (spec 004 `AgentConfigDto`).

## Relationships

```text
Agent 1 ──< Participant (agentId) >── 1 AgentSnapshot (per atelier, at launch)
Atelier 1 ──< players[] / agentSnapshots[] (index-aligned)
```

- An `Agent` exists independently of any atelier (managed on `/agents`).
- An `Atelier` never references a live `Agent` after launch: it owns its snapshots
  (clarification Q2 = option A, FR-017).

## Validation rules (summary, from FR-018/FR-019/FR-013)

1. `agentName` non-blank + unique across agents → else 409.
2. `openAiCompatibleUrl` absolute http/https URL; `modelName`, `personalityPrompt`
   non-blank → else 400 (FR-019).
3. `apiKeyVar` empty or valid env var name → else 400.
4. At launch: every `agentIds[]` entry must exist → else 400; `agentIds.size() ≥ 2` →
   else 400 (FR-010).
5. At launch: if `apiKeyVar` set and `System.getenv(apiKeyVar)` is null → 400 naming the
   missing variable (FR-013).
6. `id` immutable on `PUT` (FR-003): a different `id` in the body/path → 400.

## State transitions

`Agent`: `created → updated* → deleted` (delete is terminal; file removed). No other
states. `Atelier`: unchanged lifecycle; snapshots are write-once at creation.
