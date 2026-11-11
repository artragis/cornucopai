# Quickstart: validation du scoreboard Cornucopia

Runnable validation scenarios for feature `006-game-scoreboard`. They prove: tour
grouping and leader rotation, couleur majeure (incl. WC joker), scoring (scenario
points + tour bonus), the extended leg/atelier schemas, the API v0.6.0 response, the
dedicated `/ateliers/:id` page with scoreboard, the 5-second polling, and legacy
atelier backward compatibility. Decisions: `research.md` (R-045…R-053); data model:
`data-model.md`; contracts: `contracts/`.

Prerequisites: `cd backend && ./mvnw test` (CI, fake brain, one family per game);
`cd frontend && npm test` (Vitest). Optionally the manual run below.

## 1. Backend tests (CI-safe, no real LLM)

```bash
cd backend && ./mvnw test
```

Expected:

- `GameMasterTourTest` (new): a 3-player game produces tours where each player with
  cards plays exactly once per tour; tour 1 leader is random (seeded); the tour winner
  leads the next tour; all-REJECTED tour → next leader is random (FR-001, SC-001).
- `ScoreTest` (new): +1 point per VALIDATED leg in the major suit; 0 for VALIDATED off
  suit; 0 for REJECTED; +1 bonus to the highest major-suit card per tour; winner =
  max score, ties included (FR-006…FR-011, SC-002, SC-007).
- `CardValueOrderTest` (new): `2 < 3 < … < 10 < J < Q < K < A`; `"10" > "2"` (not
  alphabetical) (FR-010, R-046).
- WC joker tests: WC led → major suit set by the next non-WC card; WC played later
  counts as major suit for scoring (FR-003, FR-004, SC-003).
- `ModelContractTest` (updated): leg v3 round-trip through `AtelierStore`; legacy v2
  leg file (without the new fields) reads with nulls (FR-017, SC-006).
- `AtelierResourceTest` (updated): `GET /api/ateliers/{id}` returns `tours`, `scores`,
  `winner`; a legacy atelier returns empty tours/scores and null winner without error
  (FR-012, FR-017, SC-006).
- Existing suites stay green.

## 2. Inspect a persisted atelier after a game (CI or manual)

```bash
cat backend/ateliers/<id>/atelier.json | jq '{tours, scores, winner}'
cat backend/ateliers/<id>/leg-001-*.json | jq '{cardName, suitId, cardValue, turnIndex, majorSuit, points, validation}'
```

Expected: `tours` lists each tour with `index`, `leader`, `majorSuit`, `legIndices`,
`winner`, `bonusAwarded`; `scores` maps each player to their total; `winner` names the
top scorer(s). Each leg carries `turnIndex`, `majorSuit`, `suitId`, `cardValue`,
`points` (leg-schema.json v3.0.0).

## 3. API check

```bash
curl -s localhost:8080/api/ateliers/<id> | jq '{tours: [.tours[] | {index, leader, majorSuit, winner}], scores, winner}'
# legacy atelier (pre-006):
curl -s localhost:8080/api/ateliers/atelier20261110-111343 | jq '{tours, scores, winner, legs: [.legs[0] | keys]}'
```

Expected: v0.6.0 response with tours/scores/winner; legacy atelier returns `tours: []`
(or absent), `scores: {}` (or absent), `winner: []`/null, and legs without the new
fields — no 500 (FR-017, SC-006).

## 4. Frontend tests

```bash
cd frontend && npm test
```

Expected:

- `router.spec.ts`: routes `/` and `/ateliers/:id` resolve; history items are
  router-links (FR-013, R-049).
- `AtelierDetailPage.spec.ts`: renders the scoreboard (scores per player, winner or
  "en cours") above the legs list; each leg shows player, card, famille, scénario,
  validation, points, couleur majeure (FR-014).
- `useAtelierPolling.spec.ts`: with fake timers, `getAtelier` is called every 5 s;
  the interval stops when `finishedAt` is set and on unmount; polling errors keep the
  last state (FR-015, FR-016, SC-005, R-050).

## 5. Manual end-to-end run

```bash
export CORNUCOPIA_HOME=/Users/francois/cornucopia
cd backend && ./mvnw quarkus:dev &
cd frontend && npm run dev
# open http://localhost:5173 → Configurer une partie → lancer
# you land on /ateliers/<id> — scoreboard on top, legs below, polling every 5 s
# click "Ateliers précédents" on / → each atelier is a link to its /ateliers/<id> page
```

Expected: after POST the app navigates to `/ateliers/<id>`; the scoreboard shows
players with their points and the winner (or "en cours"); legs appear with their
points and couleur majeure; refreshing the URL keeps the page (dedicated route); a
legacy atelier page shows "données partielles" with 0 points.

## 6. Dev-server SPA fallback note

Vite dev server handles history-mode routing out of the box. For `vite preview` or a
static build served by a plain HTTP server, add a SPA fallback (e.g. `appType: 'spa'`
is the Vite default; for nginx add `try_files $uri /index.html;`) so that
`/ateliers/<id>` survives a refresh.
