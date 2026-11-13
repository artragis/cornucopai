# Implementation Plan: Agents conscients du projet — MCP IDE, mémoire partagée et vote par arbre de décision

**Branch**: `007-contextual-game-turns` | **Date**: 2026-11-11 | **Spec**: [specs/007-contextual-game-turns/spec.md](spec.md)

**Input**: Feature specification from `/specs/007-contextual-game-turns/spec.md`

## Summary

Give every LLM player agent contextual awareness of the software project under
analysis by connecting a streamable-HTTP MCP server (the user's IDE, default
`http://127.0.0.1:64462/stream`, no auth) and exposing a whitelist of 7 IDE tools
(`get_project_dependencies`, `get_project_modules`, `list_directory_tree`,
`read_file`, `search_file`, `search_regex`, `search_text`) in each agent's
toolbox via a LangChain4j `McpToolProvider` with a name filter. Add a per-atelier
shared project memory (`ProjectFact` journal) that agents read/write through two
`@Tool`s (`get_project_facts` / `add_project_fact`), persisted in `atelier.json`,
with optional inheritance from a source atelier via a UI dropdown. Replace the
bare YES/NO vote with a two-branch decision tree (explanation sufficient? +
applicable to the real project, verified factually via MCP?) whose short
justification is persisted with each vote, and optionally precede each vote with a
bounded multi-agent debate (**disabled by default** since the 2026-11-11 review,
R-061; enable via `cornucopai.debate.enabled=true`). MCP failures are non-blocking
(FR-004). Tests stay on one card family and use a deterministic MCP mock or the
real local server. Decisions: `research.md` (R-045…R-065); entities:
`data-model.md`; contracts: `contracts/`; validation: `quickstart.md`.

## Technical Context

**Language/Version**: Java 25 (backend); TypeScript 5.9 on Node.js ≥ 24 (frontend, one new dropdown in the create-atelier form)

**Primary Dependencies**: Backend — Quarkus 3.40.1.1, `io.quarkiverse.langchain4j:quarkus-langchain4j-core` 1.15.0 (pulls `dev.langchain4j:langchain4j` + `langchain4j-core` **1.23.0**), `dev.langchain4j:langchain4j-open-ai` 1.23.0 (already declared). **New deliberate dependencies** (recorded per constitution, R-045/R-050) — note: `langchain4j-mcp` / `langchain4j-agentic` exist only on a **separate beta line**, so they are pinned via a dedicated property `langchain4j-beta.version=1.23.0-beta33` while core stays at 1.23.0:
- `dev.langchain4j:langchain4j-mcp:${langchain4j-beta.version}` — MCP client: `StreamableHttpMcpTransport`, `DefaultMcpClient`, `McpToolProvider` (with `.filter(...)` for the tool whitelist), wired into `AiServices` via `.toolProvider(...)` (R-046).
- `dev.langchain4j:langchain4j-agentic:${langchain4j-beta.version}` + `dev.langchain4j:langchain4j-agentic-patterns:${langchain4j-beta.version}` — agentic workflows considered for the bounded debate (R-052); in the end a plain in-code loop in `DebateOrchestrator` was enough (alternative recorded in research.md), the artifacts are kept for future orchestration experiments.

**Storage**: Filesystem only, unchanged (R-002). Ateliers under `cornucopai.ateliers.root-dir` (default `ateliers/`); `atelier.json` gains `projectFacts: List<ProjectFact>` (FR-005/FR-007). Leg files gain a `votes: List<VoteDecision>` field (leg-schema v4.0.0, FR-010). Read-only inputs: Cornucopia repo at `CORNUCOPIA_HOME`. New config: `cornucopai.mcp.ide.url=${MCP_IDE_URL:http://127.0.0.1:64462/stream}`, `cornucopai.mcp.ide.tools=get_project_dependencies,get_project_modules,list_directory_tree,read_file,search_file,search_regex,search_text`, `cornucopai.mcp.ide.timeout-seconds=${MCP_IDE_TIMEOUT_SECONDS:30}`, `cornucopai.mcp.ide.enabled=${MCP_IDE_ENABLED:true}`, `cornucopai.mcp.ide.auto-health-check=${MCP_IDE_AUTO_HEALTH_CHECK:false}` and `cornucopai.mcp.ide.protocol-version=2025-11-25` (R-059: no reconnection log spam, detection probe skipped), `cornucopai.debate.enabled=${DEBATE_ENABLED:false}` (**OFF by default** since the 2026-11-11 review, R-061 — was `true` at plan time), `cornucopai.memory.prompt-window=${MEMORY_PROMPT_WINDOW:20}` (FR-013, FR-014, R-048).

**Testing**: Backend — JUnit 5 + AssertJ + REST Assured, unchanged. MCP in tests: a deterministic in-process mock `McpTransport` (scripted tool responses) OR the real local IDE server when available; CI must stay hermetic → mock by default, real server behind a profile (FR-015, R-055). Fake `LlmBrain` for unit tests (R-031 of spec 004). Each game test selects one single family (FR-014 spec 004). Frontend — Vitest for the new dropdown component.

**Target Platform**: Linux server (backend port 8080); any modern evergreen browser

**Project Type**: web application (backend REST API + frontend SPA)

**Performance Goals**: MCP tool calls bounded by `cornucopai.mcp.ide.timeout-seconds` (default 30 s); tool list cached (`cacheToolList(true)`) so the MCP handshake happens once per server, not per agent (R-046). Shared-memory prompt injection capped at the 20 most recent facts (clarification 2026-11-11). Vote prompt stays token-capped (spec 005 FR-006, SC-008). Debate (when enabled) adds at most 2 argument exchanges per voter (bounded, R-052).

**Constraints**: one atelier at a time; 2–6 players; MCP server is the user's IDE project (no project path configured by CornucopAI — clarification 2026-11-11); MCP results treated as trusted content, NOT wrapped in DONNÉES anti-injection sections (clarification 2026-11-11, revisit if MCP ever exposed off-localhost); MCP read-only tools only; MCP failure → non-blocking error string returned to the agent (FR-004); shared memory isolated per atelier except explicit UI-dropdown inheritance (FR-007), and snapshotted into `atelier.json` after every turn (FR-016, R-062); debate **disabled by default**, enableable via config (FR-013, R-061); vote fields are enums, not strings (R-060); leg schema v3 → v4 additive (votes array), legacy read tolerant; JDWP debug on 5005 must remain available (constitution); only `backend/` and `frontend/` top-level source dirs (constitution).

**Scale/Scope**: one atelier at a time; 2–6 players; deck = cards of selected families; up to ~20 facts injected per prompt, all facts persisted; 7 MCP tools exposed per agent.

## Constitution Check

*GATE: passed before Phase 0; re-checked after Phase 1 design — still passing.*

| Principle | Status | Evidence |
|---|---|---|
| I. Specification-Driven Development | PASS | `spec.md` written, checklist all green, 7 clarifications/revisions integrated (session 2026-11-11, incl. post-review R-060/R-061/R-062) before this plan |
| II. Test-First (non-negotiable) | PASS (enforced downstream) | Deterministic MCP mock for CI (R-055), fake `LlmBrain`, one family per game test; tests first in `/speckit-tasks` |
| III. Modularity / SRP | PASS | New `game.mcp` package (config, transport/client/provider factory), `game.memory` package (SharedProjectMemory, ProjectFact, tools), vote decision tree in `game.llm` (VoteDecision DTO + prompt builder), debate orchestration in `game.debate`; `GameMaster` extended but `PlayerAgent` contract unchanged |
| IV. Documentation & Transparency | PASS | `contracts/` (API v0.7.0, leg-schema v4.0.0, atelier schema), `data-model.md`, `quickstart.md`, this plan; decisions R-045…R-065 in `research.md`; structured logging of MCP calls, tool calls, debate turns, fallbacks |
| V. Simplicity (YAGNI) | PASS | Two new deps only (`langchain4j-mcp`, `langchain4j-agentic`), both pinned to 1.23.0; if agentic is overkill for the debate, plain loop fallback (research R-052); filesystem JSON, no DB; no MCP auth (per user) |
| Tech standards (Quarkus/Java 25/LangChain4j; Vue 3/Vuetify) | PASS | Stack unchanged; new deps pinned to the already-used 1.23.0; frontend: one Vuetify `v-select` dropdown |
| Repo layout (`backend/`, `frontend/` only) | PASS | New code under `backend/src/main/java/com/cornucopai/game/{mcp,memory,debate}`; frontend component under `frontend/src/components/` |
| Debug mode (JDWP 5005) | PASS | Unchanged; quickstart prerequisites restate it |
| CI = local commands | PASS | CI stays LLM-free and MCP-free (mock transport + fake brains, R-055) |
| Security-sensitive code review (LLM prompt handling) | PASS (task) | Clarification 2026-11-11 recorded: MCP results trusted (localhost only); featureHtml/cards stay in DONNÉES sections; shared-memory facts injected as DONNÉES (FR-008); no apiKey logging; if MCP URL ever non-local, auth + encapsulation must be added |

## Project Structure

### Documentation (this feature)

```text
specs/007-contextual-game-turns/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   ├── ateliers-api.yaml
│   ├── leg-schema.json
│   └── atelier-schema.json
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cornucopai/
│   ├── game/
│   │   ├── mcp/                    # NEW: McpIdeConfig, McpToolProviderFactory, MockMcpClient (src/main: test profile sets mock=true, R-055)
│   │   ├── memory/                 # NEW: ProjectFact, SharedProjectMemory, ProjectFactsTool
│   │   ├── debate/                 # NEW: DebateOrchestrator (bounded argument exchange, OFF by default R-061)
│   │   ├── llm/                    # EXTENDED: dto/VoteDto (enum-typed, R-060), vote prompt w/ decision tree
│   │   ├── agent/LlmPlayerAgent.java  # EXTENDED: vote() uses decision tree + debate; prompts as text blocks (R-061)
│   │   ├── model/                  # EXTENDED: Atelier (+projectFacts), Leg (+votes), VoteDecision + enums (R-060)
│   │   └── GameMaster.java         # EXTENDED: runs debate before vote (when enabled), persists VoteDecision, snapshots facts each turn (FR-016)
│   └── api/                        # EXTENDED: CreateAtelierRequest (+inheritFactsFrom)
└── src/test/java/com/cornucopai/...

frontend/
├── src/
│   ├── components/ConfigureGameDialog.vue  # EXTENDED: dropdown "réutiliser les faits projet"
│   ├── ConfigureGameDialog.spec.ts         # Vitest spec for the dropdown
│   └── ...
└── tests/
```

**Structure Decision**: Web application layout (backend REST + frontend SPA), unchanged from spec 004. New backend code grouped in three new packages under `com.cornucopai.game` (`mcp`, `memory`, `debate`) to keep SRP; existing `llm`, `agent`, `model`, `api` packages extended in place. No new top-level source dirs.

## Complexity Tracking

No constitution violations. No complexity tracking needed.
