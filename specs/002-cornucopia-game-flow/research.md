# Research: Flux complet d'une partie de Cornucopia jouée par des agents IA

Phase 0 output. Each unknown from the Technical Context of `plan.md` is resolved
below with a decision, a rationale, and the alternatives considered. These decisions
are binding for Phase 1 (data model, contracts, quickstart) and for `/speckit-tasks`.

## R-001 — Abstraction des agents joueurs (LangChain4j maintenant ou plus tard ?)

- **Decision**: Define a `PlayerAgent` interface in the backend
  (`com.cornucopai.game.PlayerAgent`) exposing the four interactions of the game
  flow — `playCard()`, `react()` (précision / question / passer), `answerQuestion()`,
  `vote()`. Ship four deterministic implementations now (Grumpy, Happy, Greedy,
  Prof). A LangChain4j `AiService`-backed implementation is plugged in a later
  iteration behind the same interface.
- **Rationale**: The spec's first increment only needs "dumb players"; calling an
  LLM for deterministic behavior would add latency, cost, and non-reproducible
  tests, violating YAGNI (constitution principle V) and TDD reproducibility. The
  constitution mandates LangChain4j as the orchestration stack — the interface is
  the seam where LangChain4j agents will be wired, and `quarkus-langchain4j-core`
  is already a declared dependency. No LLM call is made in this increment.
- **Alternatives considered**: (a) hardcode the four behaviors directly in the
  game master — rejected, it would couple the orchestrator to the archetypes and
  make the future LLM agent impossible to add cleanly; (b) use LangChain4j
  `AiService` for the dumb players too — rejected, non-deterministic and slower
  for no benefit at this stage.

## R-002 — Emplacement de persistance des ateliers sur le disque

- **Decision**: Each game creates a folder
  `<root>/atelier{yyyyMMdd-HHmmss}/` (dashes, no colons) containing one
  `atelier.json` metadata file and one JSON file per leg
  (`leg-{NNN}-{player}.json`, NNN zero-padded on 3 digits). `<root>` defaults
  to `ateliers/` relative to the backend working directory, configurable via
  `cornucopai.ateliers.root-dir` in `application.properties`. `ateliers/` is
  added to `.gitignore` (runtime data, not source).
- **Rationale**: The spec mandates `atelier{date}{heure}` naming with dashes and
  one JSON per leg. Keeping the root configurable avoids polluting the repo
  and lets tests point at a temp directory. Storing under the backend working dir
  keeps the repo layout rule (only `backend/` and `frontend/` as top-level source
  dirs) intact, since `ateliers/` is data, not source, and is gitignored.
- **Alternatives considered**: (a) database (H2/PostgreSQL) — rejected, the spec
  explicitly says "stocké sur le disque" as JSON files and YAGNI forbids a DB for
  this increment; (b) repo-root `ateliers/` — rejected, would clutter the root
  and risk accidental commits.

## R-003 — Contrat HTTP exposé par le backend

- **Decision**: REST endpoints under `/api/ateliers`:
  - `POST /api/ateliers` — body `{ playerCount, featureHtml }`, creates the
    atelier folder, deals the placeholder cards, runs the whole game
    synchronously, returns `201` with the atelier summary (id, players, turn
    order, legs with their final validation status).
  - `GET /api/ateliers` — list of atelier ids with creation date and player count.
  - `GET /api/ateliers/{id}` — full atelier detail including every leg.
  Full contract in `contracts/ateliers-api.yaml`; leg file format in
  `contracts/leg-schema.json`.
- **Rationale**: The frontend only needs "Configurer une partie" → "Lancer".
  With dumb players the whole game completes in well under a second, so a
  synchronous POST is the simplest correct design (YAGNI). Listing/detail
  endpoints let the user review past ateliers without touching the filesystem.
- **Alternatives considered**: (a) async job with polling (`POST` returns 202 +
  `GET /api/ateliers/{id}/status`) — rejected for v1 because dumb players are
  instant; the synchronous contract can evolve to async later without breaking the
  resource shape; (b) WebSocket/SSE for live game feed — rejected, out of scope
  for this increment.

## R-004 — Répartition des archétypes entre les joueurs

- **Decision**: The configuration form only asks for the player count (per spec).
  Archetypes are assigned round-robin in the fixed order
  `[Grumpy, Happy, Greedy, Prof]` — player 1 is Grumpy, player 2 Happy, …, player
  5 Grumpy again, etc. Player names are `Joueur-1` … `Joueur-N`.
- **Rationale**: The spec fixes four archetypes but only a player count in the
  form; a deterministic round-robin assignment makes games reproducible (TDD) and
  guarantees every archetype is present in any game with ≥ 4 players. An explicit
  per-player archetype picker is deferred to a later iteration.
- **Alternatives considered**: (a) random assignment — rejected, breaks
  reproducibility of tests; (b) let the user pick each archetype — rejected, out
  of the spec's scope for this increment ("il faudra dire combien il y a de
  joueur").

## R-005 — Deck de cartes placeholders et distribution

- **Decision**: A fixed placeholder deck of 8 cards (names `Carte-Placeholder-1` …
  `Carte-Placeholder-8`, placeholder text) is shuffled with a seeded RNG and dealt
  round-robin to the players. Each turn, the current player plays their next card.
  The game ends when every hand is empty. Real Cornucopia card families are a
  later iteration (per spec).
- **Rationale**: The spec says placeholder cards and "jusqu'à ce que les joueurs
  n'aient plus aucune carte" — a finite dealt deck gives a well-defined end
  condition. A seeded shuffle keeps test runs reproducible while still being a
  random draw in production.
- **Alternatives considered**: (a) infinite deck — rejected, no end condition;
  (b) one card per player only — rejected, too short to exercise the flow;
  (c) full Cornucopia deck now — rejected, explicitly deferred by the spec.

## R-006 — Majorité du vote de validation : sur quelle population ?

- **Decision**: The card player does not vote on their own leg. Voters are all
  *other* participants. Validation is `VALIDATED` iff strictly more than half of
  the voters answered "oui", else `REJECTED`. With an even split (or a minority of
  "oui"), the leg is `REJECTED`.
- **Rationale**: The spec says the game master asks "une nouvelle fois leur
  [les autres participants]" — the flow describes the other players reacting and
  voting. Computing the majority over the voters (not over all players) matches
  "plus de la moitié (strictement) a voté oui" applied to those who voted. Edge
  cases (tie, even voter count) resolve to REJECTED, as pinned in the spec's edge
  cases.
- **Alternatives considered**: (a) all players including the card player vote —
  rejected, the card player already committed an explanation and the spec's flow
  only re-asks the others; (b) majority over all participants regardless of voters
  — rejected, would make a 2-voter game impossible to validate (1 oui out of 2 is
  not strictly more than half of 3 players).

## R-007 — Éditeur WYSIWYG côté frontend

- **Decision**: Use TipTap (`@tiptap/vue-3` + `@tiptap/starter-kit`) as the
  WYSIWYG editor for the feature description; the submitted value is the editor's
  HTML, sent as `featureHtml` in the POST body and stored verbatim in
  `atelier.json`. This is a deliberate new dependency, recorded here per the
  constitution ("adding a dependency is a deliberate decision recorded in the
  plan").
- **Rationale**: The spec explicitly requires a "zone de texte wysiwyg". Vuetify 4
  ships no rich-text editor, so a real WYSIWYG needs a dedicated component.
  TipTap is the standard Vue 3 choice, headless and tree-shakeable.
- **Alternatives considered**: (a) plain `v-textarea` — rejected, the spec says
  WYSIWYG; (b) CKEditor/TinyMCE — rejected, heavier, harder to integrate with
  Vue 3 + Vuetify 4 than TipTap.

## R-008 — Format et nommage des fichiers leg

- **Decision**: One file per leg named `leg-{NNN}-{playerSlug}.json` where
  NNN is the global turn index (001, 002, …) across the whole game and
  `playerSlug` is the sanitized player name. Content is exactly the 5 fields of the
  spec: `cardName`, `scenario`, `explanation`, `precisions` (array of strings),
  `validation` (`PENDING` | `VALIDATED` | `REJECTED`). JSON Schema in
  `contracts/leg-schema.json`. The file is first written with
  `validation: "PENDING"` and `precisions: []`, then updated in place as
  precisions are added and finally when the vote resolves.
- **Rationale**: Matches the spec's 5-field JSON exactly; writing PENDING first then
  updating mirrors the real flow (the leg exists before the vote) and makes
  crash-resumability possible later.
- **Alternatives considered**: (a) a single JSON array for the whole atelier —
  rejected, the spec says "un json" per played card stored on disk; (b) append-only
  event log — rejected, over-engineered for this increment.

## R-009 — Orchestration du maître du jeu : synchrone, dans le backend

- **Decision**: The game master is a backend service
  (`com.cornucopai.game.GameMaster`) invoked by the `AtelierResource` on POST. It
  runs the full loop in-memory (shuffle turn order once, then per turn: play →
  reactions → vote), persisting each leg file as it goes, and returns the
  completed atelier. Turn order is stored in `atelier.json`.
- **Rationale**: Single-threaded synchronous orchestration is the simplest design
  that satisfies the spec (constitution V). One atelier at a time is assumed for
  this increment.
- **Alternatives considered**: (a) reactive/async orchestration with a state
  machine persisted between steps — rejected, YAGNI for dumb players; the
  synchronous loop is fully determined by the spec; (b) orchestrating from the
  frontend — rejected, the spec says "le programme est le maître du jeu" and the
  backend owns persistence.

## R-010 — Tests et reproductibilité

- **Decision**: Backend tests use JUnit 5 + AssertJ + REST Assured with
  `cornucopai.ateliers.root-dir` pointed at a JUnit `@TempDir`; the placeholder
  shuffle uses an injectable `java.util.Random` seed so game-flow tests are
  deterministic. Frontend tests use Vitest + `@vue/test-utils` for the
  configuration form. TDD per constitution: tests are written before the
  production code in `/speckit-tasks`.
- **Rationale**: Constitution principle II (test-first, non-negotiable) plus
  reproducibility of the dumb-player flows.
- **Alternatives considered**: (a) unseeded shuffle in tests — rejected,
  flaky tests.
