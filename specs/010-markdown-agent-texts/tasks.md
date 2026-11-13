---

description: "Task list for feature 010-markdown-agent-texts — rendu markdown des textes d'agents dans la page de résultat + règle « markdown + bloc ```json, » dans les prompts"
---

# Tasks: Rendu markdown des textes des agents + prompts markdown/```json

**Input**: Design documents from `/specs/010-markdown-agent-texts/`

**Prerequisites**: spec.md (FR-001…FR-010, US1/US2 — révisée : format markdown), plan.md, research.md (R-001…R-005), data-model.md, quickstart.md

**Tests**: INCLUDED and MANDATORY — constitution principle II (Test-First, non-negotiable): every test task is written and MUST FAIL before its implementation task runs (Red-Green-Refactor). Frontend tests use Vitest (jsdom) ; backend tests stay LLM-free (scripted/fake brains).

**Organization**: grouped by user story. US1 = front (parseur markdown + rendu stylé sur la page de résultat) ; US2 = back (prompts). US2 is independent of US1.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on incomplete tasks)
- **[Story]**: user story from spec.md (US1/US2)
- Exact file paths in every description

## Path Conventions

- Frontend: `frontend/src/…`, tests colocated (`*.spec.ts`), run with `cd frontend && npm test`
- Backend main: `backend/src/main/java/com/cornucopai/…`, tests: `backend/src/test/java/com/cornucopai/…`, run with `cd backend && ./mvnw test -Dtest=…`

---

## Phase 1: Setup

- [x] T001 Confirm green baseline before any change: `cd backend && ./mvnw test -Dtest='SystemPromptsTest,PromptsTest,LlmPlayerAgentTest'` and `cd frontend && npm test` pass; then swap the pinned dependencies: `cd frontend && npm uninstall jsonrepair && npm install marked@18.2.0 dompurify@3.4.17 --save-exact` (R-001, licences MIT / MPL-2.0-or-Apache-2.0, recorded in research.md) and verify `npm ls marked dompurify`

---

## Phase 2: US1 — Front, tests first (parseur markdown + rendu)

**Story goal**: les textes markdown des agents de la page de résultat sont parsés et rendus avec les bons styles ; les blocs ```json deviennent des blocs de code stylés `language-json` sans italique parasite ; le HTML est sanitizé ; les textes legacy restent de simples paragraphes (FR-001…FR-007).

**Independent test criteria**: `cd frontend && npm test` green with the new specs; a plain text renders as a simple paragraph with line breaks kept; a ```json block renders as `pre code.language-json`; `<script>` never survives.

- [x] T002 [US1] Write `frontend/src/utils/markdown.spec.ts` — TDD for `renderAgentMarkdown(text)`: (a) `**gras**` → `<strong>`, `# Titre` → `<h1>`, `- a\n- b` → `<ul><li>`; (b) fenced ```json block → `<pre><code class="language-json">` containing the escaped JSON and NO `<em>`; (c) bare inline JSON `{"card_id": "VE2"}` → no `<em>` (intraword `_` not emphasis); (d) single newlines kept (`breaks: true` → `<br>`); (e) `<script>alert(1)</script>` and `<img src=x onerror=…>` sanitized away; (f) `javascript:` link href removed; (g) empty/undefined → `''`; (h) plain prose → single `<p>` with the text
- [x] T003 [US1] Write `frontend/src/components/AgentText.spec.ts` — TDD for the component (`mount`): renders `props.text` through the markdown pipeline into `.agent-text` (v-html of sanitized HTML); markdown styles present (`<strong>`, `<h1>`); ```json fence → `pre code.language-json`; script injection stripped; plain text → paragraph with `<br>`; empty text → empty
- [x] T004 [US1] Extend `frontend/src/pages/AtelierDetailPage.spec.ts` — add a leg whose `explanation` contains markdown + a ```json block and an `entry.text` with `**gras**`: after expanding the panel, `.agent-text` renders them (`.language-json` present, `<strong>` present); the existing legacy tests (plain prose) still pass unchanged (FR-007/FR-010)

## Phase 3: US1 — Front, implementation

- [x] T005 [US1] Implement `frontend/src/utils/markdown.ts` — `renderAgentMarkdown(text: string): string` = `DOMPurify.sanitize(marked.parse(text, { async: false }))` with `marked.setOptions({ gfm: true, breaks: true })`; empty input → `''`
- [x] T006 [US1] Implement `frontend/src/components/AgentText.vue` — props `{ text?: string }`; computed sanitized HTML; root `<span class="agent-text markdown-body" v-html="html" />`; scoped styles for p/h1..h6/ul/ol/strong/em/a/pre/code and a distinct style for `pre code.language-json` (surface-variant background, monospace) using Vuetify theme CSS vars
- [x] T007 [US1] Wire into `frontend/src/pages/AtelierDetailPage.vue` — replace `{{ leg.scenario }}`, `{{ leg.explanation }}`, `{{ entry.text }}`, `{{ entry.answer }}` with `<AgentText :text="…" />` (R-003); keep everything else identical
- [x] T008 [US1] Remove the abandoned JSON-parser artifacts if present (`frontend/src/utils/jsonText.ts`, `frontend/src/utils/jsonText.spec.ts`, `frontend/src/components/JsonBlock.vue`) — the format is markdown now
- [x] T009 Checkpoint US1: `cd frontend && npm test && npm run lint && npm run build` all green (T002–T004 now pass)

---

## Phase 4: US2 — Back, tests first (prompts markdown + ```json,)

**Story goal**: le prompt système et chaque prompt utilisateur d'action de jeu demandent explicitement : Rappel : text est en markdown ; si tu y inclus des données structurées, mets-les dans un
bloc de code ```json.

**Independent test criteria**: `cd backend && ./mvnw test -Dtest='SystemPromptsTest,PromptsTest,LlmPlayerAgentTest'` green with the new assertions.

- [x] T010 [US2] Extend `backend/src/test/java/com/cornucopai/game/llm/SystemPromptsTest.java` — new test: `forPersonality("…")` (and the deprecated `forArchetype` variants) contain the free-text rule: markdown for free-text fields, structured data in a ```json code fence, never XML (FR-008)
- [x] T011 [US2] Extend `backend/src/test/java/com/cornucopai/game/agent/PromptsTest.java` — new test: `playPrompt`, `reactPrompt` (both variants — the no-question one must still NOT contain `QUESTION`, nor any prompt the word `leg`), `answerPrompt`, `votePrompt` each carry the light reminder: free text in markdown, structured data in a ```json fence, never XML (FR-009)
- [x] T012 [US2] Extend `backend/src/test/java/com/cornucopai/game/agent/LlmPlayerAgentTest.java` — with a brain capturing the debate prompt: the `debateArgument` prompt also carries the markdown + ```json-never-XML reminder

## Phase 5: US2 — Back, implementation

- [x] T013 [US2] Edit `backend/src/main/java/com/cornucopai/game/llm/SystemPrompts.java` — add to the « Contrat de sortie » block of `RULES_AND_CONTRACT`: free-text fields (scenario, explanation, text, answer, justification) are markdown; structured data goes in a ```json code fence (valid JSON) — never XML or any other markup (FR-008); wording must avoid the word « leg »
- [x] T014 [US2] Edit `backend/src/main/java/com/cornucopai/game/agent/LlmPlayerAgent.java` — add one short reminder line to `playPrompt` (scenario/explanation), `reactPrompt` (text, both variants, without the word `QUESTION` in the no-question one), `answerPrompt` (answer), `votePrompt` (justification) and the `debateArgument` prompt: free text in markdown, structured data in a ```json fence, never XML (FR-009)
- [x] T015 Checkpoint US2: `cd backend && ./mvnw test -Dtest='SystemPromptsTest,PromptsTest,LlmPlayerAgentTest'` green (T010–T012 pass)

---

## Phase 6: Polish & validation globale

- [x] T016 Full validation: `cd frontend && npm run lint && npm run build && npm test` and `cd backend && ./mvnw test` (full suite) all green
- [x] T017 Add diary entry `diary/2026-11-13-markdown-dans-les-textes-agents.md` (constitution: Project Diary) — decisions captured: format markdown (correction), marked+DOMPurify pinned, sanitization tested, prompt rule markdown/```json/never-XML; billet material

---

## Dependencies

- US1 (T002–T009) depends on T001 (dependencies swapped).
- US2 (T010–T015) depends on T001 only — fully independent of US1.
- T016 depends on all previous; T017 last.

## Parallel opportunities

- T002, T003, T004 are independent test files ([P] within US1 tests).
- T010, T011, T012 are independent test files ([P] within US2 tests).
- US1 and US2 phases can be worked in parallel after T001.
