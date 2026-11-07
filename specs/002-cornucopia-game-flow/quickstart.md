# Quickstart / Validation Guide: Flux complet d'une partie de Cornucopia

Runnable scenarios proving the feature end-to-end. Prerequisites: JDK 25,
Node.js ≥ 24, backend built (`cd backend && ./mvnw verify`). Contracts:
`contracts/ateliers-api.yaml` (HTTP) and `contracts/leg-schema.json` (leg
files). Data model: `data-model.md`.

## 1. Lancer une partie complète via l'API (SC-001, US-1, US-2)

```sh
cd backend
./mvnw quarkus:dev &
# in another shell:
curl -s -X POST http://localhost:8080/api/ateliers \
  -H 'Content-Type: application/json' \
  -d '{"playerCount": 4, "featureHtml": "<p>Feature login OAuth2</p>"}'
```

Expected: HTTP `201` with a JSON `Atelier` whose `id` looks like
`atelier2026-11-07-173000`, `players` has 4 entries with archetypes
GRUMPY/HAPPY/GREEDY/PROF, `turnOrder` is a permutation of the 4 player names, and
`legs` is non-empty with each leg having the 5 fields and a final
`validation` of `VALIDATED` or `REJECTED` (never `PENDING`).

## 2. Vérifier la persistance sur le disque (SC-002, FR-004, FR-007)

```sh
ls ateliers/                       # one folder per launched game
ls ateliers/atelier2026-11-07-173000/
# expected: atelier.json + leg-001-*.json, leg-002-*.json, ...
cat ateliers/atelier2026-11-07-173000/leg-001-*.json
```

Expected: folder name uses dashes (no colons); one JSON per leg named
`leg-{NNN}-{player}.json`; each file validates against
`contracts/leg-schema.json` (5 fields: `cardName`, `scenario`,
`explanation`, `precisions`, `validation`).

## 3. Règle de majorité stricte (SC-003, FR-012)

Run a 4-player game (voters per leg = 3): a leg is `VALIDATED` only when
all 3 voters say yes. Check in the returned JSON that no leg with 1 or 2 "oui"
is marked `VALIDATED`. With an even voter count or a tie, expect `REJECTED`.

## 4. Comportements déterministes des archétypes (SC-004, FR-014, US-4)

In a 4-player game, inspect the legs and precisions:

- Grumpy votes NO on every leg and asked a question each time.
- Happy votes YES and never added a precision or question.
- Greedy's `explanation` is markedly longer than the others'; his precisions are
  excessively long; he votes YES on Happy's legs and NO on Grumpy's.
- Prof gives short precisions on Happy's legs, asks a question before a
  precision on Grumpy's, passes on Greedy's; votes NO on Grumpy's, YES on others.

## 5. Stabilité de l'ordre des tours (SC-005, FR-005)

Across all legs of one atelier, the sequence of playing players follows the
`turnOrder` array cyclically and never deviates.

## 6. Lister et relire un atelier (FR-004, contracts)

```sh
curl -s http://localhost:8080/api/ateliers
curl -s http://localhost:8080/api/ateliers/atelier2026-11-07-173000
curl -s http://localhost:8080/api/ateliers/does-not-exist   # expect 404 {"error": ...}
```

Expected: `200` with a summary list; `200` with the full atelier; `404` with an
`error` message for the unknown id.

## 7. Formulaire frontend « Configurer une partie » (US-1, FR-001..FR-003)

```sh
cd frontend
npm ci
npm run dev
```

Open the printed local URL. Expected: a « Configurer une partie » action opens a
form with a player-count field and a WYSIWYG editor (TipTap, R-007) for the
feature description — no card-family choice in this increment. Clicking « Lancer »
with valid input POSTs to `/api/ateliers` and shows the resulting legs; an
invalid form (playerCount < 2 or empty description) is blocked with a message and
no request is sent.

## 8. Validation d'erreur du formulaire (edge cases)

- playerCount = 1 → « Lancer » blocked, no POST.
- empty WYSIWYG content → « Lancer » blocked, no POST.
- `POST /api/ateliers` with `playerCount: 1` → HTTP `400` with `{"error": ...}`.

## 9. Debug mode toujours disponible (constitution v1.3.0)

```sh
cd backend
MAVEN_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" ./mvnw quarkus:dev
```

Expected: JDWP agent on port 5005; an IDE debugger can attach. This ability MUST
never be removed.
