# Data Model: Flux complet d'une partie de Cornucopia jouée par des agents IA

Derived from `spec.md` (Key Entities + FR-001…FR-015) and the decisions in
`research.md`. Persistence is filesystem JSON (R-002, R-008); there is no
database in this increment.

## Entities

### Atelier (partie)

Represents one game session. Persisted as `<root>/atelier{yyyyMMdd-HHmmss}/atelier.json`.

| Field | Type | Description | Rules |
|---|---|---|---|
| `id` | string | Folder name, e.g. `atelier2026-11-07-173000` | Unique; dashes, no colons; generated from creation timestamp with seconds |
| `createdAt` | ISO-8601 instant | Creation timestamp | Set once at creation |
| `featureHtml` | string (HTML) | WYSIWYG description of the feature under analysis | Non-empty; stored verbatim from the form |
| `playerCount` | integer | Number of players chosen in the form | ≥ 2 |
| `players` | array of Participant | All participants | Size == `playerCount` |
| `turnOrder` | array of string (player names) | Order drawn at random at the first turn | Permutation of `players`; fixed for the whole game (FR-005) |
| `deck` | array of string (card names) | Placeholder deck dealt at start | 8 placeholder cards (R-005), shuffled, dealt round-robin |

### Participant (joueur agent)

| Field | Type | Description | Rules |
|---|---|---|---|
| `name` | string | e.g. `Joueur-1` | Unique within the atelier |
| `archetype` | enum `Archetype` | `GRUMPY` \| `HAPPY` \| `GREEDY` \| `PROF` | Assigned round-robin in order [Grumpy, Happy, Greedy, Prof] (R-004) |

### Leg (tour de jeu) — the 5-field JSON file

Persisted as `<root>/atelier{id}/leg-{NNN}-{playerSlug}.json` (R-008).
Contract: `contracts/leg-schema.json`.

| Field | Type | Description | Rules |
|---|---|---|---|
| `cardName` | string | Name of the played card | From the placeholder deck |
| `scenario` | string | Threat scenario evoked by the player | Non-empty |
| `explanation` | string | The player's explanation | Non-empty; Greedy's is excessively long (archétype behavior) |
| `precisions` | array of string | Precisions and "Je sais" answers collected during the turn | Initially `[]` (FR-007); appended during the reaction phase (FR-009, FR-010) |
| `validation` | enum `ValidationStatus` | `PENDING` \| `VALIDATED` \| `REJECTED` | Written as `PENDING` first, updated after the vote (FR-012) |

### PlayerAgent (interface, not persisted)

The behavioral contract each participant fulfills (R-001):

| Method | Returns | Role in the flow |
|---|---|---|
| `playCard(hand, context)` | `{card, scenario, explanation}` | FR-006 — the current player gives card + scenario + explanation |
| `react(leg, context)` | `Reaction` = precision (string) \| question (string) \| pass | FR-008 — each other player, one by one |
| `answerQuestion(question)` | `Answer` = `knows(string)` \| `doesNotKnow` | FR-010 — the card player answers "Je sais" + answer or "Je sais pas" |
| `vote(leg, context)` | `Vote` = `YES` \| `NO` | FR-011 — "valides-tu ?" |

Deterministic implementations: `GrumpyAgent`, `HappyAgent`, `GreedyAgent`,
`ProfAgent` (FR-014, behaviors in spec US-4). A LangChain4j-backed implementation
will be added behind the same interface in a later iteration.

## Enums

- **Archetype**: `GRUMPY`, `HAPPY`, `GREEDY`, `PROF`
- **ValidationStatus**: `PENDING` → `VALIDATED` | `REJECTED` (see state machine below)
- **ReactionType**: `PRECISION`, `QUESTION`, `PASS`
- **AnswerType**: `KNOWS` (with payload), `DOES_NOT_KNOW`
- **Vote**: `YES`, `NO`

## Leg state machine

```text
PENDING --(all other players reacted: precision / question+answer / pass)--> VOTE
VOTE --(strictly more than half of the voters voted YES)--> VALIDATED
VOTE --(otherwise, incl. tie or even split)--> REJECTED
```

- The leg file is written with `PENDING` as soon as the card is played
  (FR-007), updated in place as precisions arrive, and finally flipped to
  `VALIDATED`/`REJECTED` after the vote (FR-012).
- Voters = all participants **except** the card player (R-006). Majority is
  computed over the voters: `yes > voters / 2` (strict).

## Game flow (orchestration order)

```text
create atelier folder + atelier.json
shuffle turnOrder once, store it (FR-005)
deal placeholder deck round-robin
while any player still has cards:
    current = next player in turnOrder
    current.playCard() -> write leg file (PENDING, precisions=[])
    for each other player p (in turnOrder):
        p.react() -> PRECISION: append to precisions
                     | QUESTION: current.answerQuestion()
                         - KNOWS(answer): append answer to precisions
                         - DOES_NOT_KNOW: re-ask p for precision/pass only (no new question)
                     | PASS: next
    for each other player p: p.vote() -> count YES/NO
    validation = (yes > voters/2) ? VALIDATED : REJECTED -> update leg file
end while -> game over (FR-013)
```

## Validation rules (from spec)

- `playerCount` ≥ 2 (form rejects otherwise) — edge case in spec.
- `featureHtml` non-empty — form rejects otherwise.
- Atelier id contains no `:` — guaranteed by the `yyyyMMdd-HHmmss` format (FR-004).
- A question asked after a `DOES_NOT_KNOW` answer is refused; the player may only
  give a precision or pass (spec edge case, FR-010).
