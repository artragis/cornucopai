# Data Model: Sélection des familles de cartes et vraies cartes Cornucopia

Derived from `spec.md` (Key Entities + FR-001…FR-018) and the decisions in
`research.md` (R-011…R-020). Persistence stays filesystem JSON (R-002 from spec 002);
the new card catalog is read from the Cornucopia repo via `CORNUCOPIA_HOME` (R-012).

## Entities

### Set (édition / jeu de cartes) — R-013, R-014

One Cornucopia deck, latest version only, discovered from `<CORNUCOPIA_HOME>/source/`.

| Field | Type | Description | Rules |
|---|---|---|---|
| `id` | string | Edition, e.g. `webapp` | Unique; only the highest version per edition is kept (numeric semver compare, R-013) |
| `version` | string | e.g. `3.0` | The max version for this edition |
| `languages` | array of string | Available language codes, lowercase, e.g. `[en, fr, es]` | From the file suffixes of this version; `en` always present (R-014) |
| `families` | array of Family | Suits of this set (structure parsed from the `en` file) | — |
| `totalCards` | integer | Sum of `cardCount` over families | Derived |
| `resolvedLanguage` | string | Language actually used for this set in a given game | Chosen language if available, else `en` (R-015); set per atelier, not on the catalog |

### Family (famille / suit) — selectable unit

| Field | Type | Description | Rules |
|---|---|---|---|
| `id` | string | Suit id, e.g. `VE` | Unique within a set |
| `name` | string | e.g. « Data Validation & Encoding » | From the YAML `suits[].name` |
| `setId` | string | Owning edition | — |
| `cardCount` | integer | Number of cards in this suit | Derived from YAML |
| **global id** | string | `setId:familyId`, e.g. `webapp:VE` | This is the value sent in the create request and checked for unknown (FR-012) |

### Card (carte réelle) — R-016

| Field | Type | Description | Rules |
|---|---|---|---|
| `id` | string | e.g. `VE2` | From YAML `cards[].id`; unique within a set |
| `value` | string | e.g. `2`, `J`, `A` | From YAML `cards[].value` |
| `setId` | string | Edition | — |
| `suitId` | string | Family id, e.g. `VE` | — |
| `suitName` | string | Family name | — |
| `language` | string | Language of this card's file, e.g. `fr`/`en` | Resolved language of its set (R-015) |
| `description` | string | `desc` text | The threat description; what agents use to build scenario/explanation |
| `url` | string? | Optional card URL | From YAML `cards[].url` |
| `misc` | string? | Optional misc note | From YAML `cards[].misc` |

### Atelier (partie) — extended from spec 002

Persisted as `<root>/atelier{yyyyMMdd-HHmmss}/atelier.json` (R-002, schema:
`contracts/atelier-schema.json`).

| Field | Type | Description | Rules |
|---|---|---|---|
| `id` | string | `atelier{yyyyMMdd-HHmmss}` | Unchanged from 002 |
| `createdAt` | ISO-8601 instant | Creation timestamp | Unchanged |
| `featureHtml` | string (HTML) | WYSIWYG feature description | Unchanged |
| `playerCount` | integer | ≥ 2 | Unchanged |
| `players` | array of Participant | name + archetype | Unchanged |
| `turnOrder` | array of string | Drawn once at first turn | Unchanged (FR-005 of 002) |
| `language` | string | Chosen language, default `fr` | FR-015; lowercase |
| `selectedFamilies` | array of string | `setId:familyId` pairs chosen | FR-005; ≥ 1 (FR-006); no duplicates (FR-013) |
| `deck` | array of Card | **Real cards dealt, full objects** | Replaces the placeholder `List<String>`; shuffled, dealt round-robin (R-018); id+description persisted here |
| `sets` | array of { id, resolvedLanguage } | Language actually used per set | FR-017; e.g. `[{id:"webapp", resolvedLanguage:"fr"}, {id:"eop", resolvedLanguage:"en"}]` |
| `finishedAt` | ISO-8601 instant? | Set when the game ends | Unchanged |

### Leg (tour de jeu) — 5 fields, contract unchanged from spec 002

Persisted as `leg-{NNN}-{playerSlug}.json` (schema: `contracts/leg-schema.json`).

| Field | Type | Description | Rules |
|---|---|---|---|
| `cardName` | string | **Real card id**, e.g. `VE2` | FR-009; references a `Card` in `atelier.json` `deck` which carries the description |
| `scenario` | string | Threat scenario | Non-empty |
| `explanation` | string | Player's explanation | Non-empty |
| `precisions` | array of string | Precisions / "Je sais" answers | Initially `[]` |
| `validation` | enum PENDING/VALIDATED/REJECTED | Vote result | Unchanged |

### DTOs exposed over HTTP (contracts/)

- `CardCatalog` = `{ availableLanguages: string[], sets: SetDto[] }` — `GET /api/card-catalog`.
- `SetDto` = Set (id, version, languages, families, totalCards).
- `FamilyDto` = { id, name, cardCount }.
- `CreateAtelierRequest` = `{ playerCount, featureHtml, language?, families: string[] }` — `POST /api/ateliers`.
- `Atelier` response = atelier fields with `deck: Card[]`, `language`, `selectedFamilies`, `sets`.

## Deck building & language resolution (rules)

```text
catalog: scan <home>/source/*-cards-*.yaml
  → group by meta.edition
  → per edition keep max version (numeric semver)            [R-013]
  → languages(edition) = lowercase suffixes of kept files      [R-014]
  → families parsed from the kept "en" file                    [R-014]

game creation (language L, families F = [setId:familyId, …]):
  → validate every pair exists in catalog, else 400           [FR-012]
  → require |F| ≥ 1, else 400                                  [FR-006]
  → for each set touched by F:
        resolved = (L in languages(set)) ? L : "en"            [R-015]
        parse <set>-cards-<ver>-<resolved>.yaml
        keep only suits whose familyId ∈ F for that set
  → deck = all kept cards (Card objects) → shuffle(seed) → deal round-robin [R-018]
  → atelier.json: deck=Card[], language=L, selectedFamilies=F,
                   sets=[{id, resolvedLanguage}]
```

- Language code normalized to lowercase before any comparison; `fa` ≠ `fr` (R-015).
- Fallback to `en` happens within the **latest version only**; an older version is
  never used for language reasons (R-013 + R-015).
- `en` is guaranteed in every latest version, so the fallback never fails (R-014).
