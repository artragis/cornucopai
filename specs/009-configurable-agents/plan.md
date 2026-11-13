# Implementation Plan: Agents configurables (objets JSON) avec page de gestion dédiée

**Branch**: `009-configurable-agents` | **Date**: 2026-11-12 | **Spec**: [specs/009-configurable-agents/spec.md](spec.md)

**Input**: Feature specification from `/specs/009-configurable-agents/spec.md`

## Summary

Replace the `Archetype` enum concept with first-class **Agent** objects, persisted as
one JSON file per agent, managed (CRUD) through a new dedicated frontend page and a new
`/api/agents` REST API. An agent is always LLM-backed: `openAiCompatibleUrl` +
`modelName` + `apiKeyVar` (env var name, resolved server-side at call time via
`System.getenv`, never stored in clear) + `personalityPrompt` (free text replacing the
old archetype) + `agentName` (unique, human-readable) + `id` (UUID, generated at
creation, immutable). The workshop creation form drops the « nombre de joueurs » field
for an accessible multi-select of agents (`agentIds[]`, min 2; the same agent may be
picked twice — two distinct players sharing one agent config). At launch, the backend
resolves each selected agent, shuffles participant order randomly (FR-020), names them
`Joueur-N` (FR-016), and persists a full **snapshot** of each agent config in
`atelier.json` (FR-017, API key value never included). The old launch mechanisms
(ACP providers, per-player `providerType`) are removed from the game flow (FR-011);
deterministic archetype agents stay test-only. Decisions: `research.md` (R-001…R-012);
entities: `data-model.md`; contracts: `contracts/` (`agents-api.yaml` v1.0.0,
`ateliers-api.yaml` v0.8.0, `agent-schema.json`, `atelier-schema.json`);
validation: `quickstart.md`.

## Technical Context

**Language/Version**: Java 25 (backend, unchanged); TypeScript 5.x on Node.js ≥ 24 (frontend, unchanged)

**Primary Dependencies**: Backend — Quarkus 3.40.1.1 (`quarkus-rest`,
`quarkus-rest-jackson`, `quarkus-smallrye-health`), `quarkus-langchain4j-core` 1.15.0 +
`dev.langchain4j:langchain4j-open-ai` 1.23.0 (already declared since spec 004, R-022) for
`OpenAiChatModel` with custom `baseUrl` — reused as-is with the agent's
`openAiCompatibleUrl`. **No new dependency** (UUID = `java.util.UUID`, env vars =
`System.getenv`, JSON = existing Jackson). Frontend — Vue 3.5.45 + Vuetify 4.3.2
(already declared); the agent selector uses `v-autocomplete` (multiple, chips,
keyboard/ARIA accessible out of the box, R-009). Vitest 5.1.0 + jsdom for tests.

**Storage**: Filesystem only, unchanged pattern (R-002 of spec 002). Agents under
`cornucopai.agents.root-dir` (default `agents/`, gitignored like `ateliers/`) — one file
`{id}.json` per agent (R-001). Ateliers under `cornucopai.ateliers.root-dir` (default
`ateliers/`); `atelier.json` gains `agentSnapshots[]` (FR-017) and `players[]` gains
`agentId`; legacy `agentConfigs[]` and `archetype` stay readable (FR-014, Quarkus Jackson
ignores unknown properties on read by default).

**Testing**: Backend — JUnit 5 + AssertJ + REST Assured (existing); TDD per
constitution: `AgentStoreTest`, `AgentResourceTest` (201/200/400/404/409), validation
tests (FR-019), reworked `AgentConfigResolverTest` (unknown agent id → 400, shuffle
preserves participant↔agent association, `Joueur-N` naming), `AtelierServiceTest` with
fake `LlmBrain` (snapshots persisted, env-var-missing → 400 naming the variable, FR-013),
legacy-atelier read test (FR-014). Game tests keep one single family + fake brains
(spec 004 R-031). Frontend — Vitest specs: `AgentsPage.spec.ts` (list/create/edit/delete),
`ConfigureGameDialog.spec.ts` (selector min 2, same agent twice allowed, empty state),
`agentsApi.spec.ts`.

**Target Platform**: Linux server (backend port 8080); any modern evergreen browser

**Project Type**: web application (backend REST API + frontend SPA)

**Performance Goals**: Agent CRUD is local file I/O — list/create/update/delete well
under 100 ms each. Game time stays LLM-bound (unchanged from spec 004). Quarkus startup
< 2 s unchanged.

**Constraints**: 2+ participants per atelier, each bound to an agent (FR-010);
`agentName` unique across agents (FR-018); `agentName`, `openAiCompatibleUrl` (valid
http/https URL), `modelName`, `personalityPrompt` required, `apiKeyVar` optional and, if
present, a valid env var name (FR-019); `id` UUID generated at creation, immutable
(FR-003); full agent snapshot copied into the atelier at launch, API key value never
copied (FR-017); random participant order at launch, association preserved (FR-020);
`Joueur-N` naming (FR-016); missing env var at launch → 400 naming the variable
(FR-013); legacy ateliers stay readable (FR-014); ACP/deterministic launch removed from
games, deterministic agents test-only (FR-011); JDWP debug on 5005 must remain
available (constitution); only `backend/` and `frontend/` as top-level source dirs
(constitution); CI on GitHub Actions must run the same commands as local.

## Constitution Check

*GATE: passed before Phase 0 research; re-checked after Phase 1 design (see end of file).*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Specification-Driven Development | OK | This plan traces to `spec.md` FR-001…FR-020 + Clarifications 2026-11-12; every artifact links back |
| II. Test-First (TDD) | OK | Test files listed in Testing above are written first in `/speckit-tasks`; no prod code without a failing test first |
| III. Modularity / SRP | OK | `AgentStore` (persistence), `AgentResource` (HTTP), `AgentService` (validation/uniqueness), `AgentConfigResolver` (launch resolution) each have one job |
| IV. Documentation & Transparency | OK | Contracts in `contracts/`, decisions with rationale in `research.md`, structured logging in store/service |
| V. Simplicity (YAGNI) | OK | Filesystem JSON (no DB), one file per agent, no new dependency, ACP removed rather than kept dormant |
| Tech standards (Quarkus/Java 25, LangChain4j, Vue 3/Vuetify, backend/+frontend/ layout, JDWP 5005, CI parity) | OK | No deviation; no new dependency to pin; `agents/` dir is data, not source |

## Project Structure

### Documentation (this feature)

```text
specs/009-configurable-agents/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   ├── agents-api.yaml          # NEW v1.0.0 — CRUD /api/agents
│   ├── ateliers-api.yaml        # v0.8.0 — POST /api/ateliers takes agentIds[]
│   ├── agent-schema.json        # one agents/{id}.json file
│   └── atelier-schema.json      # atelier.json delta: players[].agentId, agentSnapshots[]
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cornucopai/
│   ├── api/
│   │   ├── AgentResource.java        # NEW — @Path("/api/agents") CRUD (R-002)
│   │   ├── AtelierResource.java      # MODIFIED — POST consumes agentIds[]
│   │   └── dto/
│   │       ├── AgentDto.java         # NEW — request/response of /api/agents
│   │       ├── CreateAtelierRequest.java  # MODIFIED — playerCount/agents → agentIds
│   │       └── AgentConfigDto.java   # REMOVED from launch path (spec 004 legacy)
│   ├── game/
│   │   ├── AgentStore.java           # NEW — agents/{id}.json CRUD (R-001)
│   │   ├── AgentService.java         # NEW — validation + agentName uniqueness (FR-018/019)
│   │   ├── AtelierStore.java         # MODIFIED — persists agentSnapshots
│   │   ├── AtelierService.java       # MODIFIED — resolve(agentIds), shuffle (FR-020)
│   │   ├── llm/
│   │   │   ├── AgentConfigResolver.java  # REWORKED — agentIds → Participants + snapshots
│   │   │   ├── AgentSnapshot.java        # NEW — persisted per-player agent config (FR-017)
│   │   │   ├── AgentLlmConfig.java       # legacy, kept readable (FR-014), no longer written
│   │   │   ├── LlmBrainFactory.java      # MODIFIED — snapshot in, ACP branch removed (FR-011),
│   │   │   │                             #   system prompt = base + personalityPrompt (FR-012),
│   │   │   │                             #   apiKey resolved via System.getenv(apiKeyVar) (FR-006/013)
│   │   │   ├── SystemPrompts.java        # MODIFIED — forArchetype removed from game flow
│   │   │   └── AcpCliBrain.java          # REMOVED (FR-011)
│   │   ├── model/
│   │   │   ├── Agent.java                # NEW — id, agentName, openAiCompatibleUrl, apiKeyVar, modelName, personalityPrompt
│   │   │   ├── Participant.java          # MODIFIED — (name, agentId); archetype legacy-only
│   │   │   ├── Archetype.java            # kept, test-only (deterministic agents)
│   │   │   └── Atelier.java              # MODIFIED — agentSnapshots[]
│   │   └── agent/                        # deterministic agents: unchanged, test-only
│   └── resources/application.properties  # MODIFIED — + cornucopai.agents.root-dir=${AGENTS_ROOT_DIR:agents}
└── src/test/java/com/cornucopai/...      # NEW tests first (TDD)

frontend/
├── src/
│   ├── pages/
│   │   ├── AgentsPage.vue            # NEW — route /agents: list/create/edit/delete
│   │   └── AtelierDetailPage.vue     # unchanged
│   ├── components/
│   │   └── ConfigureGameDialog.vue   # MODIFIED — v-autocomplete of agents replaces playerCount
│   ├── services/
│   │   ├── agentsApi.ts              # NEW — CRUD /api/agents
│   │   └── atelierApi.ts             # MODIFIED — CreateAtelierRequest.agentIds
│   ├── router.ts                     # MODIFIED — + { path: '/agents', component: AgentsPage }
│   └── HomePage.vue                  # MODIFIED — nav button "Gérer les agents" + new launch payload
└── src/**/*.spec.ts                  # NEW vitest specs (TDD)
```

**Structure Decision**: existing two-top-level-dir layout (`backend/` + `frontend/`) is
kept, per constitution. New code goes into the existing packages (`api`, `game`,
`game.llm`, `game.model`) — no new top-level source directory. `agents/` is a data
directory (like `ateliers/`), gitignored, not source.

## Complexity Tracking

No constitution violations — this table intentionally stays empty. The feature adds
one CRUD resource and one store, following the exact pattern already established by
`AtelierStore`/`AtelierResource` (spec 002/004); no extra abstraction is introduced.

---

## Phase 0 / Phase 1 recap (details in their own artifacts)

- Phase 0 (`research.md`): storage layout (R-001), API shape (R-002), field validation
  rules (R-003), launch-request shape (R-004), snapshot semantics (R-005), API key
  resolution (R-006), archetype/ACP removal (R-007), frontend page + selector (R-008,
  R-009), random order (R-010), legacy compat (R-011), contract versioning (R-012).
- Phase 1 (`data-model.md`, `contracts/`, `quickstart.md`): `Agent`, `AgentSnapshot`,
  `Participant`, `Atelier` entities; `/api/agents` + `/api/ateliers` v0.8.0 contracts;
  runnable validation scenarios.

**Constitution Check re-evaluated after Phase 1 design**: still OK — no new violation
introduced by the data model or contracts (filesystem JSON, no new dependency, existing
dirs only). Gates pass; ready for `/speckit-tasks`.
