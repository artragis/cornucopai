# Implementation Plan: Entrées structurées du tour (Q/R) et optimisation des prompts agents

**Branch**: `005-leg-qa-structure` | **Date**: 2026-11-08 | **Spec**: [specs/005-leg-qa-structure/spec.md](spec.md)

**Input**: Feature specification from `/specs/005-leg-qa-structure/spec.md`

## Summary

Three fixes from the real-LLM smoke test of spec 004. (1) The persisted leg goes v1 →
v2: `precisions: [string]` is replaced by `entries: [{kind, author, text, knows?,
answer?}]` so questions and their answers are first-class (legacy v1 files stay
readable via conversion, author "inconnu"); contract `leg-schema.json` v2.0.0. (2) A new
per-game toolbox tool `get_tour_info` returns the current leg's structured entries —
the react prompt no longer inlines the leg content and points to the tool instead.
(3) Prompt rewrites in French with « tour » (never « leg »): the react prompt builds
its status list conditionally (3 statuses when questions are allowed, PRECISION/PASS
only otherwise), and the vote prompt demands the bare JSON with reasoning forbidden
plus a hard 16-token cap via a dedicated capped model in the same brain. GameMaster
records precisions/questions as entries; the vote/re-ask rules and the spec-004
fallback are unchanged. Decisions: `research.md` (R-035…R-044); entities:
`data-model.md`; contracts: `contracts/`; validation: `quickstart.md`.

## Technical Context

**Language/Version**: Java 25 (backend); frontend unchanged (reads legs via API)

**Primary Dependencies**: unchanged from spec 004 — Quarkus 3.40.1.1,
`quarkus-langchain4j-core` 1.15.0, `dev.langchain4j:langchain4j-open-ai` 1.23.0,
Jackson (incl. YAML). No new dependency. Verified in the 1.23.0 sources:
`OpenAiChatModelBuilder.maxTokens(Integer)` (vote cap, R-040); no per-method request
parameters exist, hence the dedicated capped vote model.

**Storage**: Filesystem JSON, unchanged. Leg files go v2 (`entries`); v1 read
compatibility via an explicit `@JsonProperty("precisions")` setter in `Leg` (R-036).

**Testing**: JUnit 5 + AssertJ + REST Assured, hermetic fixture, fake brain
(`cornucopai.llm.brain=fake`), ONE family per game test (spec 004 FR-014). TDD per
constitution: new/updated tests first. A v1 leg JSON is written in a @TempDir to test
legacy read (R-043).

**Target Platform**: Linux server (backend port 8080); any modern browser

**Project Type**: web application (backend REST API + frontend SPA)

**Performance Goals**: vote LLM call capped at 16 output tokens (R-040) and
prompt-forbidden reasoning — target: vote call < 5 s on the local model instead of
minutes; react prompt no longer carries the leg content (its size is independent of
the number of entries); the full test suite stays < 2 min with fake brains.

**Constraints**: leg schema v2 is a breaking WRITE change (contract v2.0.0) with
tolerant READ of v1 (FR-003); 5 top-level fields kept (`cardName`, `scenario`,
`explanation`, `entries`, `validation`); `kind` is a String so unknown values never
break a read (R-035); GameMaster keeps enforcing questionsAllowed server-side;
fallback behavior (spec 004 R-030) unchanged; JDWP 5005 stays available; only
`backend/` + `frontend/` top-level dirs; CI never calls a real LLM; one family per
game test.

**Scale/Scope**: one atelier at a time; 2–6 players; entries per leg are bounded by
player count (≤ 2 entries per other player); no migration of existing ateliers on
disk (read-time conversion only).

## Constitution Check

*GATE: passed before Phase 0; re-checked after Phase 1 design — still passing.*

| Principle | Status | Evidence |
|---|---|---|
| I. Specification-Driven Development | PASS | `spec.md` written + checklist all green before this plan |
| II. Test-First (non-negotiable) | PASS (enforced downstream) | R-043: updated + new tests first; fake brain; one family per game; legacy-read test with a real v1-shaped file |
| III. Modularity / SRP | PASS | `LegEntry` in `game.model`; `LegContext`+`TourInfoTool` in `game.llm` (per-game wiring in `AgentFactory`); prompt building stays in `LlmPlayerAgent`; `GameMaster` only changes entry recording (R-037) |
| IV. Documentation & Transparency | PASS | contracts v2.0.0/v0.5.0, data-model.md, quickstart.md, decisions R-035…R-044; structured logs kept (tool calls incl. `get_tour_info`, LLM calls, fallbacks) |
| V. Simplicity (YAGNI) | PASS | No new dependency; read-time legacy conversion instead of a migration; one new tool; vote cap via a second model in the same brain, not a framework feature |
| Tech standards (Quarkus/Java 25/LangChain4j) | PASS | Stack unchanged |
| Repo layout | PASS | All changes under `backend/` (fixture/test files under `backend/src/test`) |
| Debug mode (JDWP 5005) | PASS | Unchanged |
| CI = local commands | PASS | CI stays LLM-free (fake brain) |
| Security-sensitive review (prompts) | PASS (task) | DONNÉES labeling kept on all external texts (spec 004 T045 test still applies); `PromptsTest` covers it |
| Diary | PASS (task) | tasks.md includes a diary entry for the perf findings |

No violations → Complexity Tracking below lists only justified choices.

## Project Structure

### Documentation (this feature)

```text
specs/005-leg-qa-structure/
├── spec.md                    # Feature specification (/speckit-specify)
├── plan.md                    # This file (/speckit-plan)
├── research.md                # Phase 0 output — decisions R-035..R-044
├── data-model.md              # Phase 1 output — LegEntry / Leg v2 / LegContext / TourInfoTool / prompts
├── quickstart.md              # Phase 1 output — validation scenarios
├── contracts/
│   ├── leg-schema.json        # v2.0.0 — entries[] of LegEntry (breaking write, tolerant read)
│   └── ateliers-api.yaml      # v0.5.0 — Leg/LegEntry schemas updated; rest unchanged from v0.4.0
├── checklists/
│   └── requirements.md        # Spec quality checklist (all green)
└── tasks.md                   # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cornucopai/
│   ├── game/
│   │   ├── model/
│   │   │   ├── LegEntry.java        # NEW record: kind(String), author, text, knows?, answer? + factories (R-035)
│   │   │   └── Leg.java             # MODIFY: precisions -> entries (List<LegEntry>), addEntry(), @JsonProperty("precisions") legacy setter (R-036)
│   │   ├── GameMaster.java          # MODIFY: record PRECISION/QUESTION entries with author + knows/answer (R-037)
│   │   ├── agent/
│   │   │   ├── LlmPlayerAgent.java  # MODIFY: prompts réécrits FR (tour, statuts conditionnels, vote minimal, react allégé) + setCurrent(leg) (R-041, R-038)
│   │   │   └── AgentFactory.java    # MODIFY: crée LegContext + TourInfoTool par partie, passe gameTools au factory (R-038/R-039)
│   │   └── llm/
│   │       ├── LegContext.java      # NEW: AtomicReference<Leg> currentLeg par atelier (R-038)
│   │       ├── TourInfoTool.java    # NEW: @Tool get_tour_info -> entrées du tour courant en JSON (R-038)
│   │       ├── LlmBrainFactory.java # MODIFY: base tools (CardInfoTool) + gameTools param (R-039)
│   │       ├── LangChain4jBrain.java# MODIFY: second proxy "vote" sur modèle capé maxTokens=16 (R-040); tools = base + game
│   │       ├── PlayerAiService.java # MODIFY: vote() reste; (pas de params par méthode en 1.23.0)
│   │       └── JsonAnswers.java     # NEW: strip fences / extract first {...} (R-042) — utilisé par AcpCliBrain + fallback
│   │       └── AcpCliBrain.java     # MODIFY: réutilise JsonAnswers.stripToJson
│   └── (api unchanged: legs are serialized from Leg automatically)
└── src/test/…                     # MODIFY AtelierStoreTest, ModelContractTest, GameMasterTest, AtelierResourceTest (entries)
                                   # NEW LegEntryTest (v2 + legacy + unknown kind), TourInfoToolTest, PromptsTest, VoteCapTest
```

**Structure Decision**: Same web-app split as before. The change is deliberately
confined to the model (`Leg`/`LegEntry`), the engine's entry recording
(`GameMaster`), and the LLM layer (`LlmPlayerAgent` prompts, `LangChain4jBrain` vote
cap, new per-game tool). The API layer needs no code change — legs serialize from the
model — only the contract files move to v2.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Leg schema v1 → v2 (breaking write) | The flat string list cannot carry question+answer pairing (FR-001) | Keeping `precisions` and adding a parallel `questions` array would duplicate and could diverge |
| Second capped ChatModel for the vote | 1.23.0 has no per-method request parameters; only a model-level `maxTokens` caps reliably (R-040) | Prompt-only "don't reason" is exactly what failed in the smoke test |
| Per-game LegContext + TourInfoTool | The tool must see the current leg without it being in the prompt (US-2) | Singleton tool would leak state across concurrent ateliers |
| `kind` as String (not enum) in LegEntry | Unknown kinds in persisted files must not break reads (edge case) | Enum would make `readValue` throw and the whole leg file get skipped |

No constitution principle violations; the rows above are justified design choices.
