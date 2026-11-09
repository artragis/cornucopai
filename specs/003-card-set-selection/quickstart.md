# Quickstart: validation de la sélection des familles et des vraies cartes

Runnable validation scenarios for feature `003-card-set-selection`. They prove the
feature end-to-end: catalog listing, family selection, language fallback, real-card
dealing, and error cases. Implementation details live in `tasks.md`; contracts in
`contracts/`; data model in `data-model.md`; decisions in `research.md`.

Prerequisites:

- Backend: `cd backend && ./mvnw quarkus:dev` (port 8080; JDWP debug on 5005 must stay
  available — constitution).
- Frontend: `cd frontend && npm install && npm run dev`.
- The Cornucopia repo root is provided through the environment variable
  `CORNUCOPIA_HOME` (spec requirement), e.g. `export CORNUCOPIA_HOME=/Users/francois/cornucopia`.
  It is read as `cornucopai.cornucopia-home=${CORNUCOPIA_HOME:}` in
  `backend/src/main/resources/application.properties` (R-012).

## 1. Catalog: 7 sets, latest version only, languages exposed

```bash
export CORNUCOPIA_HOME=/Users/francois/cornucopia
curl -s localhost:8080/api/card-catalog | jq '{availableLanguages, sets: [.sets[] | {id, version, languages, nFamilies: (.families|length)}]}'
```

Expected: exactly 7 sets — companion 1.0, cumulus 1.1, dbd 1.0, eop 5.0, mlsec 1.0,
mobileapp 2.0, webapp 3.0 (no webapp 2.2, no mobileapp 1.0/1.1). `fr` appears only in
`webapp`; every set lists `en`. Each set lists its families with `id`, `name`, `cardCount`.

## 2. Create a game in French with selected families → real cards dealt

```bash
curl -s -X POST localhost:8080/api/ateliers -H 'Content-Type: application/json' -d '{
  "playerCount": 4,
  "featureHtml": "<p>Feature login OAuth2</p>",
  "language": "fr",
  "families": ["webapp:VE", "webapp:AT", "eop:SP"]
}' | jq '{id, language, selectedFamilies, sets, deckSample: .deck[0:3], legsSample: [.legs[0:2][] | {cardName, validation}]}'
```

Expected: `201`. `deck` is an array of full Card objects whose `id`/`suitId` belong only
to the selected families (`webapp:VE`, `webapp:AT`, `eop:SP`); `webapp` cards have
`language: "fr"`, `eop` cards have `language: "en"` (fallback, R-015) and `sets` records
`[{id:"webapp",resolvedLanguage:"fr"},{id:"eop",resolvedLanguage:"en"}]`. Every leg's
`cardName` is a real id (e.g. `VE2`) present in `deck`; no `Carte-Placeholder-*` anywhere.

## 3. Language fallback: a set without `fr` resolves to `en`, game still launches

Use only `eop:SP` (eop has no `fr`):

```bash
curl -s -X POST localhost:8080/api/ateliers -H 'Content-Type: application/json' -d '{
  "playerCount": 2, "featureHtml": "<p>x</p>", "language": "fr", "families": ["eop:SP"]
}' | jq '{sets, lang: .deck[0].language}'
```

Expected: `201`, `sets: [{id:"eop", resolvedLanguage:"en"}]`, every card `language: "en"`.

## 4. Error cases → 400, no atelier folder created

```bash
# no family selected
curl -s -o /dev/null -w '%{http_code}\n' -X POST localhost:8080/api/ateliers \
  -H 'Content-Type: application/json' -d '{"playerCount":2,"featureHtml":"<p>x</p>","families":[]}'
# unknown family
curl -s -X POST localhost:8080/api/ateliers -H 'Content-Type: application/json' \
  -d '{"playerCount":2,"featureHtml":"<p>x</p>","families":["eop:ZZ"]}'
```

Expected: `400` with a clear `error` message for both; no new directory under
`backend/ateliers/`.

## 5. Repo not configured → empty catalog, launch refused

```bash
unset CORNUCOPIA_HOME   # or start the backend without it
curl -s localhost:8080/api/card-catalog | jq '.sets | length'   # 0
curl -s -o /dev/null -w '%{http_code}\n' -X POST localhost:8080/api/ateliers \
  -H 'Content-Type: application/json' -d '{"playerCount":2,"featureHtml":"<p>x</p>","families":["webapp:VE"]}'  # 400
```

Expected: catalog `sets` empty (FR-011) and POST returns `400` (no cards available).

## 6. Frontend: hierarchical selector + language default fr

Open the frontend, click « Configurer une partie »:

- a language selector is visible and defaults to **Français (fr)**;
- the card selector is a tree **set → familles**; checking a set checks all its families;
- sets without `fr` show an `EN` badge;
- « Lancer » stays disabled until at least one family is checked (and playerCount ≥ 2 and
  feature non-empty).

## 7. Persistence on disk

After scenario 2, inspect the atelier folder:

```bash
ls backend/ateliers/atelier*/ | head
jq '.deck[0], .language, .selectedFamilies, .sets' backend/ateliers/atelier*/atelier.json
cat backend/ateliers/atelier*/leg-001-*.json | jq '{cardName, validation}'
```

Expected: `atelier.json` contains `deck` (full Cards with `description`), `language`,
`selectedFamilies`, `sets`; each `leg-*.json` has the 5 fields with a real `cardName`.

## 8. Tests (TDD — written first in /speckit-tasks)

```bash
cd backend && ./mvnw test                 # JUnit5 + AssertJ + REST Assured, fixture repo, seeded Random
cd frontend && npm test                    # Vitest: tree selector, language default fr, launch gating
```

Coverage gates for this feature: catalog parsing (7 sets, fr only on webapp), language
fallback to `en`, deck contains only selected families' cards, 400 on empty/unknown
families, 400 when repo unconfigured.

---

## Validation results — 2026-11-08 (T034)

Executed against the real repo (`CORNUCOPIA_HOME=/Users/francois/cornucopia`) with
`./mvnw quarkus:dev` running on port 8080.

| # | Scenario | Result | Notes |
|---|----------|--------|-------|
| 1 | Catalog listing | PASS | 8 sets returned (not 7): the file `webapp-cards-2.2-hu.yaml` declares `edition: "ecommerce"` in its meta, so it forms a separate set `ecommerce 2.2` (lang `hu` only). All other sets match expectations: companion 1.0, cumulus 1.1, dbd 1.0, eop 5.0, mlsec 1.0, mobileapp 2.0, webapp 3.0. Latest-version-only filtering works (no webapp 2.2 as `webapp`, no mobileapp 1.0/1.1). `fr` appears only in `webapp 3.0`. |
| 2 | Create game fr + families | PASS | 201, `language: "fr"`, `sets: [{webapp→fr},{eop→en}]`, deck of 39 real cards (VE+AT+SP), all `leg.cardName` in deck, no placeholders. |
| 3 | Language fallback eop | PASS | 201, `sets: [{eop→en}]`, all cards `language: "en"`. |
| 4 | Error cases | PASS | Empty families → 400 `{"error":"families must contain at least one setId:familyId"}`; unknown `eop:ZZ` → 400 `{"error":"unknown family: eop:ZZ"}`; no atelier dir created on 400. |
| 5 | Repo unconfigured | PASS (unit) | Covered by `CardErrorCasesTest` (2 tests): empty catalog + POST 400 when `CORNUCOPIA_HOME` unset. |
| 6 | Frontend selector | PASS (unit) | Covered by `ConfigureGameDialog.spec.ts` (6 tests): tree, language default `fr`, launch gating. Manual UI check not performed in this run. |
| 7 | Persistence on disk | PASS | `atelier.json` contains `deck` (full Cards), `language`, `selectedFamilies`, `sets`; 39 `leg-*.json` files each with the 5 fields and a real `cardName`. |
| 8 | Tests | PASS | Backend: 70 tests, 0 failures, BUILD SUCCESS. Frontend: 7 tests passed, lint 0 errors (1 v-html warning), build succeeds.
