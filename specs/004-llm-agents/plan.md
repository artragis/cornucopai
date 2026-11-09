# Implementation Plan: Agents joueurs pilotés par LLM, configurables par agent, avec toolbox carte

**Branch**: `004-llm-agents` | **Date**: 2026-11-08 | **Spec**: [specs/004-llm-agents/spec.md](spec.md)

**Input**: Feature specification from `/specs/004-llm-agents/spec.md`

## Summary

Replace the deterministic player agents in games with a single LLM-backed
implementation (`LlmPlayerAgent`) whose LLM provider is configured per agent:
either an ACP agent (`github-copilot`, `mistral-vibe`) or an OpenAI-compatible URL
(default `http://localhost:1235/v1`, no auth — the local bionic server, FR-002).
Each agent gets its own LangChain4j `AiServices` proxy with its own sliding-window
chat memory (per agent, per atelier) and a toolbox whose first tool
`get_card_info(cardId)` returns everything interesting about a Cornucopia card:
YAML card entry, `*-mappings-*.yaml` (STRIDE/CAPEC/ASVS/DevGuide/SAFECode), and the
rich markdown from `cornucopia.owasp.org/data/cards/<set>/<family>/<id>/`
(explanation + technical note). LLM answers come back as DTOs mapped to the domain
types, with a non-blocking fallback on any failure. Resolved per-agent configs are
persisted in `atelier.json` (`agentConfigs`). Deterministic archetype agents stay
in the codebase for unit tests only. Tests use one single card family per game and
fake LLM brains (CI-safe). Decisions: `research.md` (R-021…R-034); entities:
`data-model.md`; contracts: `contracts/`; validation: `quickstart.md`.

## Technical Context

**Language/Version**: Java 25 (backend); TypeScript 5.9 on Node.js ≥ 24 (frontend, unchanged this increment)

**Primary Dependencies**: Backend — Quarkus 3.40.1.1 (`quarkus-rest`,
`quarkus-rest-jackson`, `quarkus-smallrye-health` already declared),
`io.quarkiverse.langchain4j:quarkus-langchain4j-core` 1.15.0 already declared and
pulling `dev.langchain4j:langchain4j` + `langchain4j-core` **1.23.0** (verified in
the local Maven repo; all LangChain4j APIs used are checked against the 1.23.0
sources). **New deliberate dependency** (R-022, recorded per constitution):
`dev.langchain4j:langchain4j-open-ai` with `<langchain4j.version>1.23.0</langchain4j.version>`
(same version as the transitive one — no drift) for `OpenAiChatModel` with custom
`baseUrl`. LangChain4j is used programmatically per agent (R-021): `AiServices`,
`@Tool` (`dev.langchain4j.agent.tool.Tool`), `MessageWindowChatMemory`
(`dev.langchain4j.memory.chat`), `ChatModel`. Frontend — unchanged (no per-agent
UI this increment, R-032).

**Storage**: Filesystem only, unchanged (R-002). Ateliers under
`cornucopai.ateliers.root-dir` (default `ateliers/`, gitignored); `atelier.json`
gains `agentConfigs` (FR-012). Read-only inputs: Cornucopia repo at
`cornucopai.cornucopia-home=${CORNUCOPIA_HOME:}` — `source/*-cards-*.yaml`,
`source/*-mappings-*.yaml`, `cornucopia.owasp.org/data/cards/**` (R-024). LLM
defaults: `cornucopai.llm.base-url=${LLM_BASE_URL:http://localhost:1235/v1}`,
`cornucopai.llm.api-key=${LLM_API_KEY:}`, `cornucopai.llm.model=${LLM_MODEL:}`,
`cornucopai.llm.timeout-seconds=${LLM_TIMEOUT_SECONDS:120}` (R-028).

**Testing**: Backend — JUnit 5 (`quarkus-junit`) + AssertJ + REST Assured, hermetic
fixture `backend/src/test/resources/cornucopia-fixture/` **extended** with a mini
`cornucopia.owasp.org/data/cards/...` tree + `webapp-mappings-3.0.yaml` (R-031);
`LlmPlayerAgent` tested with a scripted fake `LlmBrain` (no HTTP/model in CI);
each game test selects **one single family** (`webapp:VE` in the fixture = 2 cards
→ ≤ 2 legs, FR-014); deterministic agent tests kept unchanged (SC-006). TDD per
constitution: tests first in `/speckit-tasks`. Frontend — no change, no new tests.

**Target Platform**: Linux server (backend port 8080); any modern evergreen browser

**Project Type**: web application (backend REST API + frontend SPA)

**Performance Goals**: Card catalog + card-info index cached after first scan
(< 200 ms per `get_card_info` call after warm-up, R-024); LLM calls dominate game
time — each call bounded by `timeout-seconds` (default 120 s) with `maxRetries(1)`
(R-030); a single-family fixture game (2 legs, fake brains) runs in well under 5 s;
a real 2-player game on one family against the local server is expected to complete
in a few minutes (LLM-bound); Quarkus startup < 2 s.

**Constraints**: one atelier at a time (R-034); 2–6 players; per-agent provider
config (FR-001) with default local URL and no auth (FR-002); single LLM agent
implementation in games, deterministic agents test-only (FR-003); memory isolated
per agent per atelier, window of 40 messages, nothing persisted across games
(FR-010, R-025); LLM failure → logged fallback for that decision only, game
continues (FR-013, R-030); invalid agent config → 400, no half-configured atelier
(R-028); leg keeps its 5-field schema (leg-schema.json unchanged); JDWP debug on
5005 must remain available (constitution); only `backend/` and `frontend/` as
top-level source dirs (constitution); game engine (`GameMaster`, turn order, votes)
unchanged — only the `PlayerAgent` implementations behind it change.

**Scale/Scope**: one atelier at a time; 2–6 players; deck = cards of the selected
families (spec 003); 7 sets in the catalog; rich card content read from `data/cards`
in `en` (only language available there); LLM calls only within a game.

## Constitution Check

*GATE: passed before Phase 0; re-checked after Phase 1 design — still passing.*

| Principle | Status | Evidence |
|---|---|---|
| I. Specification-Driven Development | PASS | `spec.md` written and checklist all green before this plan; no clarification needed (decisions delegated by the user are recorded in spec Assumptions + research.md) |
| II. Test-First (non-negotiable) | PASS (enforced downstream) | R-031: fake `LlmBrain`, extended fixture, one family per game test; deterministic agent tests kept; tests first in tasks.md; coverage gates in quickstart §1 |
| III. Modularity / SRP | PASS | New `game.llm` package (config, brain interface + 2 impls, AiService + DTOs, prompts), `LlmPlayerAgent` in `game.agent`, `CardInfoService`/`CardInfoTool` in `game.card`; `GameMaster`/`PlayerAgent` contract untouched (R-033) |
| IV. Documentation & Transparency | PASS | `contracts/` (v0.4.0 API + atelier schema), `data-model.md`, `quickstart.md`, this plan; decisions R-021…R-034 with rationale in `research.md`; structured logging required in tasks (LLM calls, tool calls, fallbacks) |
| V. Simplicity (YAGNI) | PASS | One new backend dep (`langchain4j-open-ai`, R-022); no frontend change (R-032); ACP brain is a thin one-shot CLI adapter, full ACP protocol deferred (R-029); tool returns Markdown string (R-023); filesystem JSON, no DB; synchronous POST kept |
| Tech standards (Quarkus/Java 25/LangChain4j; Vue 3/Vuetify) | PASS | Stack unchanged; LangChain4j used as mandated; new dep pinned to the already-used 1.23.0 (R-022); no new frontend dep |
| Repo layout (`backend/`, `frontend/` only) | PASS | New code under `backend/src/main/java/com/cornucopai/...`; fixture under `backend/src/test/resources` (test resource, not a top-level source dir) |
| Debug mode (JDWP 5005) | PASS | Unchanged; quickstart prerequisites restate it |
| CI = local commands | PASS | Existing GitHub Actions run the same Maven commands; CI stays LLM-free (fake brains, R-031) |
| Security-sensitive code review (LLM prompt handling) | PASS (task) | tasks.md includes prompt-injection-aware handling (card text/feature HTML treated as data, never as instructions), focused tests for malformed LLM output (R-027/R-030), and no logging of apiKey |
| Diary / billet | PASS (task) | tasks.md includes a diary passage capturing the LangChain4j/toolbox/config decisions, per constitution |

No violations → Complexity Tracking below lists only justified design choices.

## Project Structure

### Documentation (this feature)

```text
specs/004-llm-agents/
├── spec.md                    # Feature specification (/speckit-specify)
├── plan.md                    # This file (/speckit-plan)
├── research.md                # Phase 0 output — decisions R-021..R-034
├── data-model.md              # Phase 1 output — AgentLlmConfig/LlmBrain/DTOs/InfoCarte/Atelier
├── quickstart.md              # Phase 1 output — validation scenarios
├── contracts/
│   ├── ateliers-api.yaml      # OpenAPI v0.4.0: POST/GET /api/ateliers (+ optional agents[], + agentConfigs)
│   └── atelier-schema.json    # atelier.json + agentConfigs (AgentLlmConfig); leg-schema.json UNCHANGED (spec 003)
├── checklists/
│   └── requirements.md        # Spec quality checklist (all green)
└── tasks.md                   # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cornucopai/
│   ├── api/
│   │   ├── AtelierResource.java        # MODIFY: validate agents[] (400 cases, R-032)
│   │   └── dto/
│   │       ├── CreateAtelierRequest.java   # MODIFY: + optional List<AgentConfigDto> agents
│   │       ├── AgentConfigDto.java         # NEW: name?, archetype?, providerType, baseUrl?, apiKey?, modelName?, acpProvider?, command?
│   │       └── AtelierResponse.java        # MODIFY: + agentConfigs
│   └── game/
│       ├── llm/                            # NEW package (R-033)
│       │   ├── AgentLlmConfig.java         # NEW record, persisted (providerType/baseUrl/apiKey/modelName/acpProvider/command)
│       │   ├── LlmDefaults.java            # NEW: cornucopai.llm.* config props (base-url default http://localhost:1235/v1, R-028)
│       │   ├── LlmBrain.java               # NEW interface: playCard/react/answerQuestion/vote → DTOs
│       │   ├── LlmBrainFactory.java        # NEW: AgentLlmConfig → LlmBrain (LangChain4jBrain | AcpCliBrain)
│       │   ├── LangChain4jBrain.java       # NEW: per-agent AiServices proxy (ChatModel from config, system prompt, MessageWindowChatMemory(40), CardInfoTool)
│       │   ├── AcpCliBrain.java            # NEW: one-shot ACP CLI adapter (github-copilot/mistral-vibe), fake-command test seam
│       │   ├── PlayerAiService.java        # NEW: AiService interface, 4 methods, @V("input") String → DTO records
│       │   ├── SystemPrompts.java          # NEW: per-archetype system prompt + output contract (R-026)
│       │   └── dto/                        # NEW: PlayedCardDto, ReactionDto, AnswerDto, VoteDto (R-027)
│       ├── agent/
│       │   ├── LlmPlayerAgent.java         # NEW: the single PlayerAgent impl for games (LlmBrain + fallback R-030)
│       │   ├── AgentFactory.java           # MODIFY: builds LlmPlayerAgent per AgentLlmConfig (defaults when absent); no deterministic agents in games
│       │   ├── AbstractAgent.java          # KEEP, unit-tests only (javadoc updated)
│       │   └── GrumpyAgent/HappyAgent/GreedyAgent/ProfAgent.java  # KEEP, unit-tests only
│       ├── card/
│       │   ├── CardInfoService.java        # NEW: cardId → InfoCarte (cards YAML + mappings YAML + data/cards walk; cached index, R-024)
│       │   ├── CardInfoTool.java           # NEW: @Tool get_card_info(String cardId) → Markdown (R-023)
│       │   ├── CardSetFile.java            # MODIFY/EXTEND: mappings POJO (stride/capec/asvs/dev_guide/safecode) for *-mappings-*.yaml
│       │   └── model/
│       │       ├── CardInfo.java           # NEW: id, value, setId, version, suitId, suitName, language, description, url?, misc?, mappings?, explanation?, technicalNote?, locations[]
│       │       └── CardMappings.java       # NEW: stride, capec, owasp_asvs, owasp_dev_guide, safecode (+ *_print variants, optional)
│       ├── AtelierService.java             # MODIFY: resolve agent configs (request agents → defaults), build agents via AgentFactory, persist agentConfigs
│       ├── GameMaster.java                 # UNCHANGED (engine untouched)
│       ├── PlayerAgent.java                # UNCHANGED (contract)
│       └── model/Atelier.java              # MODIFY: + agentConfigs (index-aligned with players)
├── src/main/resources/application.properties  # + cornucopai.llm.base-url=${LLM_BASE_URL:http://localhost:1235/v1}, api-key, model, timeout-seconds
├── pom.xml                               # + dev.langchain4j:langchain4j-open-ai (${langchain4j.version}=1.23.0) (R-022)
└── src/test/…                            # NEW: extended cornucopia-fixture (mini data/cards tree + mappings yaml); LlmPlayerAgentTest (fake brain + fallback);
                                          # CardInfoServiceTest/CardInfoToolTest; single-family game test (webapp:VE, 2 cards); existing tests updated where signatures changed

frontend/                                 # UNCHANGED this increment (R-032): games launch with default LLM config
```

**Structure Decision**: Web application split (backend REST API + frontend SPA),
matching the existing `backend/` + `frontend/` layout mandated by the constitution.
All LLM concerns are isolated in a new `com.cornucopai.game.llm` package; card
knowledge stays in `com.cornucopai.game.card`; the game engine (`GameMaster`,
`PlayerAgent`) is untouched — only the agents behind the interface change. The test
fixture lives under `backend/src/test/resources` (a test resource, not a top-level
source dir).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| New dependency `dev.langchain4j:langchain4j-open-ai` (1.23.0) | OpenAI-compatible endpoints with a custom base URL need the official client; constitution mandates LangChain4j | Hand-rolled HTTP would lose the tool loop, memory and retries of AiServices (R-022) |
| `LlmBrain` interface + 2 implementations (LangChain4j / ACP CLI) | FR-001 requires a per-agent choice between OpenAI-compatible URLs and ACP agents behind a single agent implementation | One brain per provider type would duplicate all agent logic and violate FR-003 (R-029) |
| DTO layer between the LLM and domain sealed types | Reliable parsing of model output + fallback (FR-013) | Returning sealed interfaces directly needs polymorphic Jackson config and breaks on any model deviation (R-027) |
| Extended test fixture (mini `data/cards` tree + mappings YAML) | Card-info tool must be tested hermetically against all three data sources | Testing only against the real repo would make tests depend on `/Users/francois/cornucopia` (R-031) |

No constitution principle violations; the rows above are justified design choices,
not principle breaches.
