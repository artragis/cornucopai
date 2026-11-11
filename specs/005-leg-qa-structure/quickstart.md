# Quickstart: validation des entrées structurées du tour et des prompts optimisés

Runnable validation scenarios for feature `005-leg-qa-structure`. They prove: the v2
leg schema (structured entries with Q&A), the `get_tour_info` tool, the slimmed
conditional react prompt, the capped minimal vote, the French terminology, and the
legacy v1 read compatibility. Decisions: `research.md` (R-035…R-044); data model:
`data-model.md`; contracts: `contracts/`.

Prerequisites: `cd backend && ./mvnw test` (CI, fake brain, one family per game);
optionally the real run below against the local bionic server.

## 1. Tests (CI-safe, no real LLM)

```bash
cd backend && ./mvnw test
```

Expected:

- `LegEntryTest`: v2 round-trip through `AtelierStore`; legacy v1 file (`precisions:
  ["..."]`) loads as PRECISION entries with author `"inconnu"`; unknown `kind`
  tolerated (FR-003, SC-001, SC-007).
- `TourInfoToolTest`: `get_tour_info` returns the current leg's entries including a
  QUESTION with `knows=false`; "Aucun tour en cours" when no leg is set (FR-004, SC-006).
- `PromptsTest`: no prompt contains the word `leg`; react prompt has the two clean
  variants (3 statuses vs PRECISION/PASS only) and does NOT inline the scenario; vote
  prompt forbids reasoning (US-3/US-4/US-5, SC-002…SC-005).
- `VoteCapTest`: the vote call is configured with a 16-token cap (R-040, SC-002).
- `GameMasterTest`/`SingleFamilyLlmGameTest`: a question + answer persists as ONE
  `QUESTION` entry with `knows`/`answer`; single family only (FR-010, SC-008).
- Existing suites updated to `entries` (`AtelierStoreTest`, `ModelContractTest`,
  `AtelierResourceTest`) stay green.

## 2. Inspect a persisted v2 leg after a game (CI or manual)

```bash
# after any test/dev game:
cat backend/ateliers/<id>/leg-001-*.json | jq '{cardName, entries, validation}'
# or from a test run: backend/target/test-ateliers/<id>/leg-001-*.json
```

Expected: `entries` is an array of objects `{kind, author, text, knows?, answer?}` —
questions carry `knows`/`answer`, precisions don't (leg-schema.json v2.0.0).

## 3. Legacy atelier stays readable

```bash
# the smoke-test atelier from spec 004 is a real v1 file:
cat backend/ateliers/atelier20261108-232136/leg-001-joueur-2.json | jq 'keys'
curl -s localhost:8080/api/ateliers/atelier20261108-232136 | jq '.legs[0].entries[0]'
```

Expected: the v1 file lists `precisions` on disk, but the API returns it converted to
`entries` (PRECISION entries, author `"inconnu"`) without error (FR-003).

## 4. Manual real-LLM run: cheaper vote, leaner react prompt

```bash
export CORNUCOPIA_HOME=/Users/francois/cornucopia
cd backend && LLM_MODEL=qwen/qwen3.8-27b ./mvnw quarkus:dev
curl -s -X POST localhost:8080/api/ateliers -H 'Content-Type: application/json' -d '{
  "playerCount": 2, "featureHtml": "<p>Feature login OAuth2</p>", "families": ["webapp:VE"]
}' | jq '{id, legs: [.legs[] | {cardName, entries: [.entries[] | {kind, author, knows}], validation}]}'
```

Expected (vs spec 004 smoke test): the `vote` LLM calls now log a duration of a few
seconds instead of minutes (16-token cap, R-040); the logs show `get_tour_info`
being called during the reaction phase instead of the full leg being in the prompt;
`entries` contain QUESTION entries with `knows`/`answer`.
