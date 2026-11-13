# Quickstart: validation des agents conscients du projet (MCP IDE, mémoire partagée, vote par arbre de décision)

Runnable validation scenarios for feature `007-contextual-game-turns`. They prove
the feature end-to-end: unit tests with fake LLM brains and a deterministic MCP
mock (CI-safe, one family per game), toolbox inspection, shared-memory sharing,
vote decision tree, debate, and MCP-down resilience. Implementation details live
in `tasks.md`; contracts in `contracts/`; data model in `data-model.md`; decisions
in `research.md` (R-045…R-065).

Prerequisites:

- Backend: `cd backend && ./mvnw quarkus:dev` (port 8080; JDWP debug on 5005 must stay
  available — constitution).
- Cornucopia repo root via env var: `export CORNUCOPIA_HOME=/path/to/cornucopia`.
- MCP IDE server (manual runs only): the user's IDE MCP server at
  `http://127.0.0.1:64462/stream` (default, no auth). In CI/tests, the mock is used
  automatically (`cornucopai.mcp.ide.mock=true` in the test profile, R-055).

## 1. Tests (CI-safe, no real LLM, no real MCP)

```bash
cd backend && ./mvnw test
```

Expected (spec 007, SC-001…SC-008):

- **Toolbox inspection** (SC-001): a unit test builds a brain and asserts the toolbox
  contains the 7 whitelisted MCP tool names + `get_card_info` + `get_tour_info` +
  `get_project_facts` + `add_project_fact`. The mock transport lists exactly those 7
  MCP tools, and the provider filter keeps only the whitelisted ones (R-046).
- **MCP call in a game** (SC-002): an integration test with the mock transport and
  fake brains asserts the mock's `executeTool("list_directory_tree")` was invoked
  at least once during a 2-player, single-family game.
- **Shared memory** (SC-003, SC-006): agent A calls `add_project_fact`, agent B's
  `get_project_facts` returns it; a second atelier starts with an empty memory.
- **Vote decision tree** (SC-004, SC-005): fake brain scripted to return
  `NOT_APPLICABLE` + `NO` for a non-applicable scenario → at least 2 of 3 persisted
  `votes` have `vote=NO` and `applicabilityCheck=NOT_APPLICABLE`; every persisted
  vote has all 5 fields (schema-validated against leg-schema.json v4.0.0).
- **Vote prompt** (SC-008): unit test asserts the vote prompt contains both decision-tree
  questions (in French) and that the vote call uses the capped model (`VOTE_MAX_TOKENS`).
- **Debate** (US5): with `cornucopai.debate.enabled=true` (default), the debate
  orchestrator runs before the vote and each voter's prompt contains the other
  agents' arguments; with `=false`, no debate, direct vote.
- **MCP down** (SC-007): mock transport that throws on connect → the game still
  completes end-to-end, MCP tools return error strings, no exception propagates.
- **Fact inheritance** (FR-007): POST with `inheritFactsFrom=<atelier-with-facts>`
  → the new atelier's `projectFacts` equals the source's; without it → empty.
- All pre-existing tests (specs 001–006) still pass unchanged.

## 2. Manual end-to-end game with the real IDE MCP server (dev only, never in CI)

```bash
export CORNUCOPIA_HOME=/path/to/cornucopia
# IDE MCP server running at http://127.0.0.1:64462/stream (default config)
curl -s -X POST localhost:8080/api/ateliers -H 'Content-Type: application/json' -d '{
  "playerCount": 3,
  "featureHtml": "<p>Feature login OAuth2</p>",
  "language": "fr",
  "families": ["webapp:VE"]
}' | jq '{id, projectFacts, legs: [.legs[] | {cardName, validation, votes}]}'
```

Expected: `201`. Logs show the MCP handshake once (`cacheToolList`), agents calling
`list_directory_tree` / `read_file` / `search_*` during play, `add_project_fact`
calls recorded in `projectFacts` of `atelier.json`, and each leg's `votes` array
populated with structured decisions (vote + explanationCheck + applicabilityCheck +
justification). A deliberately non-applicable scenario (e.g. « attaque de l'API REST
publique » when the project has none) yields NO votes with
`applicabilityCheck=NOT_APPLICABLE`.

## 3. Frontend: fact-inheritance dropdown

```bash
cd frontend && npm run dev
```

Open the create-atelier form: when at least one existing atelier has
`hasProjectFacts=true`, a dropdown « Réutiliser les faits projet de l'atelier… »
lists them (R-056). Selecting one sends `inheritFactsFrom` in the POST body; the
created atelier starts with those facts copied (FR-007).

## 4. Configuration knobs (no code change, FR-014)

```properties
# application.properties or env vars
cornucopai.mcp.ide.enabled=true
cornucopai.mcp.ide.url=http://127.0.0.1:64462/stream
cornucopai.mcp.ide.tools=get_project_dependencies,get_project_modules,list_directory_tree,read_file,search_file,search_regex,search_text
cornucopai.mcp.ide.timeout-seconds=30
cornucopai.debate.enabled=true        # false → vote direct sans débat (FR-013)
cornucopai.memory.prompt-window=20    # faits injectés dans le prompt (FR-008)
```
