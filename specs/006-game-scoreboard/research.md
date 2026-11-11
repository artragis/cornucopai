# Research: Scoreboard Cornucopia — couleur majeure, tours, scoring, page de résultats

Phase 0 output. Each unknown from the Technical Context of `plan.md` is resolved
below with a decision, a rationale, and the alternatives considered. Binding for
Phase 1 and `/speckit-tasks`. Decisions continue after spec 005 (R-035…R-044), so
they start at R-045.

## R-045 — Regroupement des legs en tours (tricks) dans le GameMaster

- **Decision**: The `GameMaster` tracks a `currentTourIndex` (1-based) and a
  `tourLeader` (name of the player who opens the current tour). A new tour starts when
  the previous tour is complete — i.e. when every player still holding cards has played
  exactly once in the current tour. Concretely, the engine iterates the `tourLeader`
  then each subsequent player in the shuffled `turnOrder` (skipping players with empty
  hands), one leg per player; after the last player of the pass, the tour closes and a
  new one begins with the next leader. Tour 1 leader = random pick from `turnOrder`.
  Next leader = the tour winner (highest major-suit card, FR-009); if all legs of the
  tour were REJECTED (no winner), the next leader is drawn at random. The existing
  `turnOrder` field in `Atelier` remains the *base* order (shuffled once); the per-tour
  leader rotation is derived from it at runtime and the `Tour` objects persist the
  actual leader.
- **Rationale**: This matches the clarified FR-001 (session 2026-11-10): a tour imposes
  that every player still in the game plays exactly once; the winner leads the next.
- **Alternatives considered**: (a) fixed turn order, first player always leads —
  rejected by the user in clarification (real Cornucopia rule: winner leads); (b) random
  leader each tour — rejected by the user in favor of the authentic rule.

## R-046 — Ordre canonique des valeurs de cartes (FR-010)

- **Decision**: A `CardValueOrder` utility maps each card `value` string to a rank:
  `2→2, 3→3, …, 10→10, J→11, Q→12, K→13, A→14`. Comparison is by rank, never
  alphabetical. Unknown values rank 0 (lowest) and log a WARN.
- **Rationale**: FR-010 fixes the order 2 < … < 10 < J < Q < K < A; numeric strings
  like `"10"` must not sort before `"2"` (alphabetical trap).
- **Alternatives considered**: (a) alphabetical string compare — rejected: `"10" < "2"`
  alphabetically, wrong; (b) parse int with face-card map — same thing, this IS the
  chosen approach, just centralized.

## R-047 — Extension du schéma Leg (v3.0.0, additif)

- **Decision**: `Leg` gains 5 optional fields, all absent on legacy files (read as
  null/0): `turnIndex` (int, 1-based tour number), `majorSuit` (string, suitId of the
  tour's major suit, null if undetermined), `suitId` (string, family of the played
  card), `cardValue` (string, value of the played card), `points` (int, points earned
  on this leg: 0 or 1 for the validated scenario, +1 if it also won the tour bonus).
  Contract `leg-schema.json` → **v3.0.0** (additive; v2 files read fine). No legacy
  setter needed — Jackson ignores missing fields with defaults.
- **Rationale**: FR-018 requires each leg to be autonomous in the history; additive
  fields keep v2/v1 files readable (FR-017).
- **Alternatives considered**: (a) a separate `score-*.json` file per leg — rejected:
  two files to keep in sync, more I/O; (b) recompute points at read time — rejected:
  couples the store to game rules.

## R-048 — Extension du schéma Atelier (tours, scores, winner)

- **Decision**: `Atelier` gains: `tours` (array of `Tour`), `scores` (map
  playerName → int total points), `winner` (array of player names — empty while the
  game is running, one or more names at the end, ties included). `Tour` is a new
  persisted record: `{ index, leader, majorSuit, legIndices: [int], winner: string?,
  bonusAwarded: boolean }`. All additive; legacy ateliers read with empty tours,
  empty scores, null winner → the API/frontend show "données partielles" (FR-017).
- **Rationale**: FR-005/FR-011/FR-012 require tours, per-player scores and the winner
  to be persisted and returned by the API.
- **Alternatives considered**: (a) store only scores, recompute tours on read —
  rejected: major suit and tour winner are needed for the UI and cannot be recomputed
  without the card play order.

## R-049 — Route dédiée frontend : ajout de vue-router

- **Decision**: Add `vue-router@4` to `frontend/package.json`. `main.ts` installs the
  router; `src/router.ts` declares two routes: `/` → `App.vue` (home with history),
  `/ateliers/:id` → `AtelierDetailPage.vue`. History items in `App.vue` become
  `<router-link :to="`/ateliers/${a.id}`">`. After `createAtelier` (POST), the app
  navigates to `/ateliers/${id}`. Vite `server.proxy` already forwards `/api` to the
  backend; for the dev server the SPA fallback is Vite's default (history mode works
  out of the box in dev; for `vite preview`/static hosting a fallback config may be
  needed — noted in quickstart).
- **Rationale**: The clarification (session 2026-11-10) chose a dedicated route with a
  shareable URL over a modal/inline panel. vue-router is the standard Vue 3 solution.
- **Alternatives considered**: (a) modal/inline — rejected by clarification; (b)
  hand-rolled hash routing — rejected: no real URL, poor UX, reinvents the wheel.

## R-050 — Polling de la page de détail (5 s)

- **Decision**: A `useAtelierPolling(atelierId)` composable wraps `getAtelier(id)` in a
  `setInterval(5000)`. It starts on mount of `AtelierDetailPage`, clears the interval
  when `finishedAt` becomes non-null (FR-016) and on unmount. On each tick it replaces
  the reactive atelier ref with the fresh response. Polling errors are caught and
  ignored (the last good state stays, edge case FR-015). No WebSocket/SSE (YAGNI).
- **Rationale**: FR-015/SC-005 with the clarified 5 s interval; simple, testable with
  Vitest fake timers.
- **Alternatives considered**: (a) WebSocket/SSE — rejected: overkill for one atelier
  at a time, adds backend complexity; (b) 2 s interval — rejected by clarification in
  favor of 5 s.

## R-051 — Contrat API v0.6.0 (GET /api/ateliers/{id})

- **Decision**: `AtelierResponse` (and the `Atelier` schema in `ateliers-api.yaml`)
  gains `tours: Tour[]`, `scores: Record<string, number>`, `winner: string[]`. The `Leg`
  schema gains `turnIndex?`, `majorSuit?`, `suitId?`, `cardValue?`, `points?`. API version
  → **0.6.0**. POST response unchanged in shape (same extended Atelier). No new
  endpoint; the existing GET serves the detail page.
- **Rationale**: FR-012 requires the detail API to return tours, per-leg points,
  scores and winner; additive fields keep old clients working.
- **Alternatives considered**: (a) a separate `/api/ateliers/{id}/scoreboard`
  endpoint — rejected: one GET is enough, the detail page needs everything at once.

## R-052 — Détection de la famille WC (joker)

- **Decision**: A card is WC iff its `suitId` equals `"WC"` (case-sensitive, matches the
  Cornucopia data files). The `GameMaster` checks `card.suitId().equals("WC")` when
  determining the major suit: a WC led at tour start does not set the major suit; the
  first non-WC card does (cascade). A WC played later is treated as major suit for
  scoring (FR-004).
- **Rationale**: The fixture files use `suitId: "WC"` for the Wild Card suit; FR-003/FR-004
  clarified in session 2026-11-10.
- **Alternatives considered**: (a) detect by suitName containing "WILD" — rejected:
  localized names vary (fr/en), suitId is stable.

## R-053 — Compatibilité lecture des ateliers legacy (FR-017)

- **Decision**: All new fields on `Leg` and `Atelier` are optional with defaults
  (null / 0 / empty). Jackson reads v1/v2 leg files and pre-006 atelier files without
  error. The API returns `tours: []`, `scores: {}`, `winner: []`, and legs without the
  new fields as nulls; the frontend detects this (no `tours` or all-null `majorSuit`)
  and shows a "données partielles" banner with 0 points (SC-006).
- **Rationale**: FR-017 / edge case: legacy ateliers must stay readable with a degraded
  scoreboard.
- **Alternatives considered**: (a) a migration rewrites old files on first read —
  rejected: YAGNI, append-only data, constitution principle V.
