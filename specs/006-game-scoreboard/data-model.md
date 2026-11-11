# Data Model: Scoreboard Cornucopia — tours, couleur majeure, scores

Derived from `spec.md` (Key Entities + FR-001…FR-018 + Clarifications session 2026-11-10)
and `research.md` (R-045…R-053). Persistence stays filesystem JSON; schemas extend
additively (leg v3.0.0, API v0.6.0); legacy files read with defaults.

## Entities

### Tour (trick) — NEW, persisted in atelier.json (R-045, R-048)

| Field | Type | Description | Rules |
|---|---|---|---|
| `index` | int | 1-based tour number | Unique per atelier |
| `leader` | string | Name of the player who opened this tour | Tour 1: random; next: previous tour winner, or random if all REJECTED |
| `majorSuit` | string? | suitId of the tour's major suit | First non-WC card of the tour; null if all cards were WC |
| `legIndices` | int[] | Turn indices (1-based) of the legs in this tour | One per player still holding cards |
| `winner` | string? | Player who won the bonus point | Highest major-suit card (WC counts as major suit); null if no bonus awarded |
| `bonusAwarded` | boolean | Whether the +1 bonus was given | False if all legs REJECTED or no major suit and no comparison possible |

### Leg (tour de jeu) — v3.0.0, additive fields (R-047)

Persisted as `leg-{NNN}-{playerSlug}.json`. Existing 5 fields unchanged; 5 new
optional fields (absent on legacy files → null/0):

| Field | Type | Rules |
|---|---|---|
| `cardName` | string | Real card id (unchanged) |
| `scenario` | string, minLength 1 | Unchanged |
| `explanation` | string, minLength 1 | Unchanged |
| `entries` | array of LegEntry | Unchanged (v2) |
| `validation` | enum PENDING/VALIDATED/REJECTED | Unchanged |
| `turnIndex` | int? | NEW — 1-based tour this leg belongs to |
| `majorSuit` | string? | NEW — couleur majeure of that tour (null if undetermined) |
| `suitId` | string? | NEW — family of the played card |
| `cardValue` | string? | NEW — value of the played card (for bonus comparison) |
| `points` | int? | NEW — points earned on this leg (0, 1, or 2 if scenario point + bonus) |

### Atelier — extended, additive (R-048)

Existing fields unchanged. New optional fields (absent on legacy ateliers → empty/null):

| Field | Type | Rules |
|---|---|---|
| `tours` | Tour[] | NEW — all tours of the game, ordered by index |
| `scores` | map string→int | NEW — total points per player (scenario points + bonus points) |
| `winner` | string[] | NEW — player(s) with the highest score at game end; empty while running |

### CardValueOrder — utility (R-046)

Maps card `value` strings to ranks: `2→2 … 10→10, J→11, Q→12, K→13, A→14`.
Comparison by rank; unknown values rank 0 + WARN log. Never alphabetical.

### Score / Scoreboard — derived, not persisted separately

- **Score** per player = sum of `points` over all their legs (scenario points +
  bonus points). Persisted as `scores` map in atelier.json.
- **Scoreboard** = UI rendering of `scores` + `winner` + `turnOrder` + per-tour
  major suit and winner, shown on `/ateliers/:id`.

## State transitions

- **Tour lifecycle**: `OPEN` (leader playing) → legs accumulate → `CLOSED` when all
  players with cards have played → bonus awarded → next tour `OPEN` with new leader.
- **Leg lifecycle**: unchanged — PENDING → VALIDATED/REJECTED after vote; `points`
  computed after validation and after the tour closes (bonus).
- **Atelier lifecycle**: unchanged — `finishedAt` null while running, set at end;
  `winner` computed and persisted when `finishedAt` is set.

## Validation rules (from FR)

- A tour MUST contain exactly one leg per player still holding cards (FR-001).
- `majorSuit` = suitId of the first non-WC card in the tour; WC never sets it (FR-003).
- `points` on a leg = 1 if VALIDATED and suitId == majorSuit (or suitId == "WC" and
  majorSuit != null); else 0; +1 bonus added to the tour winner's leg (FR-006…FR-009).
- `winner` = all players whose score equals the max score (ties included) (FR-011).
- Card values within a suit are unique → the bonus always has exactly one winner
  (clarification 2026-11-10).

## Unchanged

`Card`, `Participant`, `Archetype`, `LegEntry`, `SetResolution`, `AgentLlmConfig`,
`PlayerAgent` contract, the GameMaster vote/re-ask rules, the spec-004/005 fallback.
