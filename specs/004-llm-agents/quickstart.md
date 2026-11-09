# Quickstart: validation des agents joueurs pilotés par LLM

Runnable validation scenarios for feature `004-llm-agents`. They prove the feature
end-to-end: unit tests with fake LLM brains (CI-safe, one family per game), the
card-info tool against the fixture, a manual end-to-end game against the local bionic
server, and the LLM-failure fallback. Implementation details live in `tasks.md`;
contracts in `contracts/`; data model in `data-model.md`; decisions in `research.md`
(R-021…R-034).

Prerequisites:

- Backend: `cd backend && ./mvnw quarkus:dev` (port 8080; JDWP debug on 5005 must stay
  available — constitution).
- Cornucopia repo root via env var: `export CORNUCOPIA_HOME=/Users/francois/cornucopia`
  (read as `cornucopai.cornucopia-home=${CORNUCOPIA_HOME:}`, R-012).
- Optional, for the manual end-to-end run only: the local bionic server up at
  `http://localhost:1235/v1` (the default LLM base URL, FR-002). No key needed.

## 1. Tests (CI-safe, no real LLM)

```bash
cd backend && ./mvnw test
```

Expected (spec 004, FR-003/FR-014, SC-005/SC-006):

- The deterministic agent tests (`GrumpyAgentTest`, `HappyAgentTest`, `GreedyAgentTest`,
  `ProfAgentTest`) still pass unchanged — those agents are test-only now (SC-006).
- New `LlmPlayerAgentTest` passes with a scripted fake `LlmBrain` (R-031): DTO→domain
  mapping, and the fallback path (brain throws / returns garbage → Pass / DoesNotKnow /
  NO / generic scenario, game continues — FR-013, SC-007).
- New `CardInfoServiceTest`/`CardInfoToolTest` pass against the extended fixture
  (`cornucopia-fixture` gains a mini `cornucopia.owasp.org/data/cards/...` tree and a
  `webapp-mappings-3.0.yaml`): VE2 returns description + mappings + explanation +
  technical note; unknown id → "carte inconnue" (FR-009); multi-set id → locations
  listed (FR-008).
- New game-level integration test passes: **one single family** (`webapp:VE` in the
  fixture = 2 cards), 2 players, fake brains → at most 2 legs (FR-014, SC-005), and
  `atelier.json` contains `agentConfigs` index-aligned with `players` (FR-012, SC-008).

## 2. Manual end-to-end game against the local bionic server (dev only, never in CI)

```bash
export CORNUCOPIA_HOME=/Users/francois/cornucopia
# bionic running at http://localhost:1235/v1 (default config, no override needed)
curl -s -X POST localhost:8080/api/ateliers -H 'Content-Type: application/json' -d '{
  "playerCount": 2,
  "featureHtml": "<p>Feature login OAuth2</p>",
  "language": "fr",
  "families": ["webapp:VE"]
}' | jq '{id, agentConfigs, legs: [.legs[] | {cardName, scenario: (.scenario[0:80]), validation}]}'
```

Expected: `201`. Only the `webapp:VE` family is dealt (≤ 13 legs with the real repo,
2 with the fixture). Every leg's scenario/explanation/precisions are LLM-written (no
deterministic strings), `validation` is VALIDATED or REJECTED per the strict-majority
rule, and `agentConfigs` shows `providerType: OPENAI_COMPATIBLE`,
`baseUrl: http://localhost:1235/v1`, no `apiKey` (FR-002, FR-012). Logs show the
`get_card_info` tool being called by agents when they need card details (FR-005/FR-006).

## 3. Per-agent provider override via the API (FR-001, R-032)

```bash
curl -s -X POST localhost:8080/api/ateliers -H 'Content-Type: application/json' -d '{
  "playerCount": 2,
  "featureHtml": "<p>Feature paiement</p>",
  "families": ["webapp:VE"],
  "agents": [
    {"archetype": "GRUMPY", "providerType": "OPENAI_COMPATIBLE", "baseUrl": "http://localhost:1235/v1"},
    {"archetype": "HAPPY", "providerType": "ACP", "acpProvider": "mistral-vibe"}
  ]
}' | jq '.agentConfigs'
```

Expected: `201` (or a graceful fallback game if the ACP CLI is absent — FR-013),
`agentConfigs[0].providerType == "OPENAI_COMPATIBLE"`,
`agentConfigs[1].providerType == "ACP"` with `acpProvider == "mistral-vibe"`.

Invalid config is rejected (edge case):

```bash
curl -s -X POST localhost:8080/api/ateliers -H 'Content-Type: application/json' -d '{
  "playerCount": 2, "featureHtml": "<p>x</p>", "families": ["webapp:VE"],
  "agents": [{"providerType": "OPENAI_COMPATIBLE", "baseUrl": ""}]
}'
# expected: 400 {"error": "...blank baseUrl..."}
```

## 4. LLM down → the game still completes (FR-013, SC-007)

```bash
# stop bionic (or point LLM_BASE_URL at a dead port for this run), then:
curl -s -X POST localhost:8080/api/ateliers -H 'Content-Type: application/json' -d '{
  "playerCount": 2, "featureHtml": "<p>x</p>", "families": ["webapp:VE"]
}' | jq '{id, finished: (.finishedAt != null), legs: [.legs[] | .validation]}'
```

Expected: `201`, `finished == true`, all legs have a final validation status, and the
server logs contain WARN lines for each failed LLM call (fallback applied, game never
crashes).

## 5. Inspect a persisted atelier

```bash
ls backend/ateliers/ | tail -1
jq '{agentConfigs, firstLeg: .legs[0]}' backend/ateliers/<id>/atelier.json 2>/dev/null || \
  cat backend/ateliers/<id>/leg-001-*.json | jq .
```

Expected: `atelier.json` contains `agentConfigs` (FR-012); each `leg-NNN-*.json`
keeps the unchanged 5-field shape (`cardName` = real card id, `leg-schema.json`).
