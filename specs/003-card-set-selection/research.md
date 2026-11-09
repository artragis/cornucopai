# Research: Sélection des familles de cartes et distribution des vraies cartes Cornucopia

Phase 0 output. Each unknown from the Technical Context of `plan.md` is resolved
below with a decision, a rationale, and the alternatives considered. These decisions
are binding for Phase 1 (data model, contracts, quickstart) and for `/speckit-tasks`.
Decisions continue the project numbering after spec 002 (R-001…R-010).

## R-011 — Librairie de parsing YAML

- **Decision**: Add `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml`
  (version managed by the Quarkus BOM, so declared without `<version>`) and parse
  the Cornucopia YAML files with a Jackson `ObjectMapper` built on `YAMLFactory`.
  One YAML mapper is created once (ApplicationScoped) and reused by the catalog
  service; mapping goes through plain DTO/record classes (`CardSetFile`, `Suit`,
  `CardEntry`) annotated for Jackson, consistent with the existing `AtelierStore`
  which already uses Jackson. This is a deliberate new dependency, recorded here
  per the constitution ("adding a dependency is a deliberate decision recorded in
  the plan").
- **Rationale**: The project already standardizes on Jackson for JSON persistence;
  using Jackson for YAML too avoids a second mapping stack and lets the same
  `ObjectMapper` conventions (field visibility, etc.) apply. The Quarkus BOM manages
  the version, so no version drift.
- **Alternatives considered**: (a) raw SnakeYAML (also BOM-managed, transitively
  present via SmallRye Config) — rejected because it would introduce a second
  mapping API next to Jackson and rely on a transitive dependency; (b) hand-rolled
  YAML parsing — rejected, error-prone.

## R-012 — Lecture de la variable d'environnement `CORNUCOPIA_HOME`

- **Decision**: In `application.properties`, declare
  `cornucopai.cornucopia-home=${CORNUCOPIA_HOME:}`. MicroProfile Config resolves
  `${CORNUCOPIA_HOME}` from the environment variable of that exact name (the spec's
  required mechanism), while the `cornucopai.cornucopia-home` property remains
  overridable in `application.properties` / test profiles. An empty value means
  "dépôt non configuré" → empty catalog, and launching a game is refused (FR-011).
  The backend reads it with `@ConfigProperty(name = "cornucopai.cornucopia-home")`;
  tests set the property directly to a fixture directory.
- **Rationale**: Keeps the env var as the production source of truth (spec) while
  staying testable without real env vars.
- **Alternatives considered**: (a) read `@ConfigProperty(name = "CORNUCOPIA_HOME")`
  directly — rejected, hard to override in `@QuarkusTest`; (b) system property only —
  rejected, the spec explicitly says environment variable.

## R-013 — « Dernière version » par édition (set)

- **Decision**: Scan `<home>/source/` for `*-cards-*.yaml`. Group files by edition
  (the `meta.edition` field). For each edition, keep only the file(s) with the
  highest version, comparing versions numerically segment by segment
  (split on `.`, compare as integers; e.g. `3.0` > `2.2`, `2.0` > `1.1` > `1.0`).
  Only that version is exposed. At the time of writing this yields 7 sets:
  companion 1.0, cumulus 1.1, dbd 1.0, eop 5.0, mlsec 1.0, mobileapp 2.0, webapp 3.0.
- **Rationale**: Spec clarification (session 2026-11-08): one version per edition,
  the most recent; older versions are never listed nor selectable.
- **Alternatives considered**: (a) expose all versions — rejected by the
  clarification; (b) lexicographic string compare — rejected, `10.0` < `9.0`
  lexicographically, so numeric segment compare is used.

## R-014 — Structure du catalogue vs contenu des cartes

- **Decision**: For each set (edition + latest version), the available languages are
  the suffixes of the files `<edition>-cards-<version>-<lang>.yaml`. The suit/family
  structure (suit ids, names, card counts) is parsed from the **`en`** file of that
  version (always present), because the suit structure is language-independent and
  `en` is the canonical fallback. Card *descriptions* are read at deck-build time
  from the chosen-language file (fallback `en`, see R-015). So the catalog endpoint
  parses only the `en` files, and deck building parses only the files for the selected
  families in the resolved language.
- **Rationale**: Avoids parsing every translation just to list families; `en` is
  guaranteed in every latest version (verified on the repo).
- **Alternatives considered**: (a) parse every language file at startup to list
  families — rejected, wasteful and the structure is identical across languages;
  (b) hardcode the suit list — rejected, must come from the YAML.

## R-015 — Langue par défaut et repli (fallback)

- **Decision**: Default language is **`fr`** (spec clarification), selectable in the
  game creator and sent in the POST body (`language`). For each selected set, if
  `<edition>-cards-<version>-<language>.yaml` exists in the latest version, use it;
  otherwise fall back to the **`en`** file of the *same* latest version — never to an
  older version. The resolved language per set is recorded on the atelier and surfaced
  in the UI (badge). Language codes are normalized to lowercase before comparison
  (`FR` → `fr`); `fa` (persan) is never treated as `fr`. Verified on the repo: in the
  latest version, `fr` exists only for `webapp` 3.0; `en` exists for all 7 sets, so the
  `en` fallback is always available.
- **Rationale**: Spec clarification: default French, react when a set lacks the
  language. `en` is the only language guaranteed in every set, so it is the safe
  fallback; reverting to an older version for language reasons would contradict
  R-013.
- **Alternatives considered**: (a) fail the game if a set lacks the language —
  rejected, the spec says "il faut pouvoir y réagir", i.e. degrade gracefully;
  (b) fall back to any available language — rejected, non-deterministic; `en` is
  the stable canonical choice.

## R-016 — Identité de carte et persistance (où va la description)

- **Decision**: A real card is a value object `Card` =
  `{ id, value, setId (edition), suitId, suitName, language, description, url?, misc? }`.
  `Atelier.deck` changes from `List<String>` to `List<Card>` (full objects), so each
  dealt card is persisted in `atelier.json` with its real id **and** its description.
  The `Leg` keeps exactly its 5 fields from spec 002 (contract unchanged); its
  `cardName` is now the real card id (e.g. `VE2`), which references the full `Card`
  (with description) stored in `atelier.json`. Agents receive the full `Card`:
  `PlayerAgent.playCard(Card card, String featureHtml)` (signature changes from
  `String card` to `Card card`), so scenario/explanation are built from the real
  description. This satisfies FR-009/FR-010 without altering the 5-field leg schema.
- **Rationale**: Keeps the leg contract stable (spec 002) while making the real id +
  description persisted and available to the engine and agents; the deck in
  `atelier.json` is the single place where full cards live.
- **Alternatives considered**: (a) add a 6th `cardDescription` field to the leg —
  rejected, would break the 002 leg-schema and duplicate data already in `atelier.json`;
  (b) store only ids in the deck and re-read YAML at view time — rejected, the atelier
  must be self-contained on disk.

## R-017 — Contrat HTTP

- **Decision**:
  - New `GET /api/card-catalog` → `{ availableLanguages: string[], sets: SetDto[] }`
    where `SetDto = { id (edition), version, languages[], families: FamilyDto[], totalCards }`
    and `FamilyDto = { id (suit id), name, cardCount }`. Powers the hierarchical selector.
  - `POST /api/ateliers` request extended: `{ playerCount, featureHtml, language?, families: string[] }`
    with `language` defaulting to `"fr"` and `families` a list of `"setId:familyId"`
    (e.g. `"webapp:VE"`). `400` if `families` is empty or contains an unknown pair
    (FR-006, FR-012). Response `Atelier` gains `language`, `selectedFamilies`, and
    `deck` becomes `Card[]` (`Card = { id, value, setId, suitId, suitName, language, description }`).
  - Contracts in `contracts/card-catalog-api.yaml`, updated `contracts/ateliers-api.yaml`,
    updated `contracts/leg-schema.json`, new `contracts/atelier-schema.json`.
- **Rationale**: The frontend needs the catalog before showing the form, and the
  create request must carry the chosen language and families. One new read endpoint
  + an extended POST is the minimal change.
- **Alternatives considered**: (a) embed the catalog in `GET /api/ateliers` — rejected,
  the catalog is needed *before* creating an atelier; (b) separate endpoint per set —
  rejected, one endpoint is simpler for the tree selector.

## R-018 — Construction et distribution du deck réel

- **Decision**: Build the deck by collecting every card of every selected family
  (`setId:familyId`), reading each set's file in its resolved language (R-015). Shuffle
  with the injectable `java.util.Random` (seeded in tests) and deal round-robin exactly
  as `PlaceholderCards.deal` did. `PlaceholderCards` is removed; a new
  `CardDeckService` (or method on the catalog service) produces `List<Card>`.
  `GameMaster` switches its hands from `Deque<String>` to `Deque<Card>` and calls
  `agent.playCard(card, featureHtml)`; the rest of the turn loop is unchanged.
- **Rationale**: Spec assumption: only the dealt cards change, the game engine (turn
  order, legs, votes) is unchanged. Round-robin dealing and seeded shuffle preserve
  reproducibility (R-010 from spec 002).
- **Alternatives considered**: (a) keep `String` cards and look up the description
  later — rejected, agents need the description at play time; (b) one deck per family
  dealt separately — rejected, the spec says a single combined shuffled deck.

## R-019 — Mise en cache du catalogue

- **Decision**: Scan `<home>/source/` once, lazily, and cache the catalog in an
  `@ApplicationScoped CardCatalogService`. No refresh endpoint (YAGNI). Tests inject
  the service with a fixture directory (a small copy of `source/` containing a couple
  of sets/languages) by setting `cornucopai.cornucopia-home` to that fixture.
- **Rationale**: The catalog is small and static for the process lifetime; re-scanning
  per request would be wasteful and the spec does not ask for hot reload.
- **Alternatives considered**: (a) re-scan on every `GET /api/card-catalog` — rejected,
  unnecessary I/O; (b) startup eager scan — rejected, fails fast behavior is better
  when the env var is missing (empty catalog, FR-011).

## R-020 — Tests et reproductibilité

- **Decision**: Backend tests set `cornucopai.cornucopia-home` to a fixture directory
  (committed under `backend/src/test/resources/cornucopia-fixture/source/` with 2 sets,
  2 languages, to keep tests fast and hermetic) and use a seeded `Random`. Catalog
  tests assert 7 sets on the real repo (or the fixture's expected set) and that `fr`
  is available only for `webapp`. Fallback tests assert a set without `fr` resolves to
  `en` with the resolved language recorded. API tests assert `400` on empty `families`
  and on unknown `setId:familyId`, and that `deck` contains only cards from selected
  families with real ids. Frontend tests (Vitest) assert the tree selector renders
  sets→families, the language selector defaults to `fr`, and « Lancer » is disabled
  until at least one family is checked. TDD per constitution: tests first in
  `/speckit-tasks`.
- **Rationale**: Constitution principle II + reproducibility; a fixture keeps tests
  hermetic and fast without depending on the real cornucopia repo path.
- **Alternatives considered**: (a) tests against the real `/Users/francois/cornucopia`
  — rejected, not portable in CI; (b) no fixture, mock the service — rejected, the
  parsing itself must be tested against real YAML.
