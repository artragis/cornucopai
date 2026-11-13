# Quickstart: validation des agents configurables (objets JSON)

Runnable validation scenarios for feature `009-configurable-agents`. They prove the
feature end-to-end: backend tests (TDD, fake LLM brains — CI-safe), the agents CRUD
API, the new launch flow with `agentIds[]`, and a manual run through the UI. Implementation
details live in `tasks.md`; contracts in `contracts/`; data model in `data-model.md`;
decisions in `research.md` (R-001…R-012).

Prerequisites:

- Backend: `cd backend && ./mvnw quarkus:dev` (port 8080; JDWP debug on 5005 must stay
  available — constitution).
- Frontend: `cd frontend && npm run dev` (Vite proxy `/api` → `http://localhost:8080`).
- Cornucopia repo root: `export CORNUCOPIA_HOME=/Users/francois/cornucopia`.
- Optional, for the manual end-to-end game only: a local OpenAI-compatible server at
  `http://localhost:1235/v1` (no key needed — `apiKeyVar` empty).

## 1. Backend tests (CI-safe, no real LLM)

```bash
cd backend && ./mvnw test
```

Expected (spec 009, FR-001…FR-020, SC-001…SC-010):

- `AgentStoreTest`: create/list/get/update/delete; one file `agents/{id}.json` per agent;
  id is a UUID and never changes on update (FR-003, SC-001); duplicate `agentName` →
  conflict (FR-018); validation rejects blank fields, non-URL `openAiCompatibleUrl`,
  invalid `apiKeyVar` (FR-019).
- `AgentResourceTest` (REST Assured): `POST /api/agents` 201 (+ Location), 400 on invalid
  body, 409 on duplicate name; `GET` list/detail, 404 unknown id; `PUT` keeps id (FR-004);
  `DELETE` 204 then 404 (FR-005).
- Reworked `AgentConfigResolverTest`: `agentIds` → participants + snapshots; unknown agent
  id → 400; same id twice → two distinct participants sharing the agent (FR-009);
  `Joueur-N` naming after the seeded random shuffle (FR-016/FR-020); env var named by
  `apiKeyVar` missing → launch refused naming the variable (FR-013, SC-009).
- `AtelierServiceTest` (fake brains, one card family as in spec 004): `atelier.json`
  contains `agentSnapshots[]` index-aligned with `players[]`, each with the agent's full
  config **but never the API key value** (FR-017, SC-005); editing/deleting the agent
  afterwards does not change the persisted atelier.
- Legacy read test (FR-014, SC-010): a pre-009 `atelier.json` (with `archetype` +
  `agentConfigs`) still loads and is served by `GET /api/ateliers/{id}`.
- Deterministic agent tests (Grumpy/Happy/Greedy/Prof) still pass unchanged — test-only
  (FR-011, spec 004 SC-006).

## 2. Frontend tests

```bash
cd frontend && npm test
```

Expected:

- `AgentsPage.spec.ts`: list renders agent names; create/edit dialog validates required
  fields client-side (FR-019); delete asks for confirmation then removes the row (FR-005).
- `ConfigureGameDialog.spec.ts`: the « nombre de joueurs » field is gone; the agent
  selector requires ≥ 2 selections (FR-010); the same agent can be picked twice (FR-009);
  with zero agents configured, an empty-state message links to `/agents` (US-2 scenario 4);
  the selector is keyboard-operable (FR-008, SC-008).
- `agentsApi.spec.ts`: fetch calls hit `/api/agents*` with the right methods/bodies.

## 3. Manual end-to-end through the UI

1. `cd backend && ./mvnw quarkus:dev` and `cd frontend && npm run dev`.
2. Open `http://localhost:5173/agents` → create an agent, e.g. name « Analyste »,
   URL `http://localhost:1235/v1`, `apiKeyVar` empty, model `qwen3-coder`,
   personality « Tu es un analyste sécurité tatillon. » → it appears in the list with a
   stable id (SC-001). Edit its personality → id unchanged (FR-004). Create a second
   agent. Delete one → gone from the list (FR-005).
3. Home → « Configurer une partie » → the player-count field is replaced by the agent
   selector; pick the same agent twice + the other once (FR-009), pick a family, launch.
4. The game runs; afterwards open the atelier detail: `players[]` shows `Joueur-N` names
   with `agentId`, and the persisted `ateliers/atelier*.json/atelier.json` contains
   `agentSnapshots[]` with the full config and **no API key value** (FR-017).
5. API key path: create an agent with `apiKeyVar: MISTRAL_API_KEY` and URL
   `https://api.mistral.ai/v1`; launch a game **without** the env var set → 400 naming
   `MISTRAL_API_KEY` (FR-013). Then `export MISTRAL_API_KEY=…` and relaunch → the game
   starts (key resolved via getenv at launch, never stored — SC-005).

## 4. Contract references

- `contracts/agents-api.yaml` v1.0.0 — CRUD `/api/agents`.
- `contracts/ateliers-api.yaml` v0.8.0 — `POST /api/ateliers` with `agentIds[]`.
- `contracts/agent-schema.json` — one `agents/{id}.json` file.
- `contracts/atelier-schema.json` — `atelier.json` v0.8.0 delta (players[].agentId,
  agentSnapshots[]).
