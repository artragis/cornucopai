# Implementation Plan: Rendu markdown des textes des agents

**Branch**: `010-markdown-agent-texts` | **Date**: 2026-11-13 | **Spec**: [specs/010-markdown-agent-texts/spec.md](spec.md)

**Input**: Feature specification from `/specs/010-markdown-agent-texts/spec.md`

## Summary

Les textes libres produits par les agents (scénario, explication, précisions, questions, réponses)
sont en **markdown** (correction du 2026-11-13 : parseur markdown, pas JSON). La page de résultat
des ateliers les affichait brut : le JSON embarqué était illisible (les `_` des clés). La feature
ajoute côté **front** un composant `AgentText.vue` qui parse le markdown avec une bibliothèque
dédiée (`marked`, épinglé) et injecte le HTML **sanitizé** (`DOMPurify`) avec les bons styles —
titres, gras, listes, et blocs de code stylés (```json → `language-json`). Côté **back**, les
prompts (système + actions de jeu) demandent explicitement : textes libres en markdown, données
structurées dans un bloc de code ```json (JSON valide). Aucun changement de contrat
API ni de persistance. Décisions : `research.md` (R-001…R-005) ; entités : `data-model.md` ;
validation : `quickstart.md`. Pas de `contracts/` : aucune interface externe modifiée (R-005).

## Technical Context

**Language/Version**: Java 25 (backend, unchanged); TypeScript 5.9 (frontend, Node.js ≥ 24)

**Primary Dependencies**: Frontend — Vue 3.5.45 + Vuetify 4.3.2 (existants) ; **nouvelles
dépendances épinglées** : `marked` `18.2.0` (parseur markdown, MIT) + `dompurify` `3.4.17`
(sanitization, MPL-2.0 OR Apache-2.0) — R-001. La dépendance `jsonrepair` de la première piste
est retirée. Backend — aucun ajout (prompts = littéraux Java).

**Storage**: Aucune modification. Ateliers et agents restent des JSON fichiers tels quels
(FR-010) ; le rendu markdown se fait à l'affichage.

**Testing**: TDD (constitution, Principe II). Frontend — Vitest 5.1.0 + jsdom + @vue/test-utils :
`src/utils/markdown.spec.ts` (parseur : gras/titre/listes, bloc ```json → `language-json` sans
`<em>`, `breaks` conservé, `<script>`/onerror sanitizés, texte vide), `src/components/AgentText.spec.ts`
(rendu via le composant), mise à jour de `src/pages/AtelierDetailPage.spec.ts` (intégration).
Backend — JUnit 5 + AssertJ : `SystemPromptsTest` (règle markdown + ```json  dans le
contrat de sortie), `PromptsTest` (rappel dans play/react/answer/vote, en respectant les interdits
« leg »/« QUESTION » existants), `LlmPlayerAgentTest` (prompt de débat).

**Target Platform**: navigateur evergreen (front) ; serveur Linux port 8080 (back, inchangé)

**Project Type**: web application (backend REST Quarkus + frontend SPA Vue/Vuetify)

**Performance Goals**: le parsing markdown d'un texte d'agent (quelques Ko) doit rester < 5 ms ;
aucun impact réseau (rendu client).

**Constraints**: FR-001…FR-010 de la spec ; sanitization systématique avant `v-html` sur les
textes d'agents (FR-005) ; compatibilité visuelle legacy (FR-003/FR-007/FR-010) ; prompts : ne
pas contenir le mot « leg » ni « QUESTION » dans la variante react sans question (tests existants) ;
JDWP 5005 toujours disponible (constitution) ; seules `backend/` et `frontend/` comme répertoires
source (constitution) ; CI GitHub Actions = mêmes commandes que localement.

## Constitution Check

*GATE: passed before Phase 0; re-checked after Phase 1 design (end of file).*

- **I. Spec-driven** : spec 010 révisée (markdown) et checklist validée avant plan. ✔
- **II. Test-first** : tasks.md ordonnera tests avant implémentation (Red-Green-Refactor). ✔
- **III. Modularity** : un utilitaire pur `markdown.ts` (parse+sanitize), un composant
  `AgentText.vue` (rendu) — responsabilités uniques, testables isolément. ✔
- **IV. Documentation** : spec/plan/research/data-model/quickstart ; dépendances épinglées et
  licences tracées en R-001 ; la correction de format (markdown, pas JSON) est consignée. ✔
- **V. Simplicity (YAGNI)** : pas de parseur maison, pas de coloration syntaxique JSON maison
  (les blocs ```json sont stylés en CSS via `language-json`), pas de changement d'API, pas de
  migration. ✔
- **Quality standards** : dépendances épinglées + licences revues (R-001) ; code sécurité
  (sanitization FR-005) testé contre l'injection ; lint/build/tests front et `mvnw verify` back
  en CI. ✔

## Project Structure

### Documentation (this feature)

```text
specs/010-markdown-agent-texts/
├── spec.md               # feature specification (révisée : markdown)
├── plan.md               # this file
├── research.md           # Phase 0 — décisions R-001..R-005
├── data-model.md         # Phase 1 — TexteMarkdownAgent / HtmlSanitise / BlocDeCode
├── quickstart.md         # validation guide
├── checklists/requirements.md
└── tasks.md              # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
frontend/
├── src/
│   ├── utils/
│   │   ├── markdown.ts           # NEW — renderAgentMarkdown(): marked (gfm, breaks) + DOMPurify
│   │   └── markdown.spec.ts      # NEW — tests TDD
│   ├── components/
│   │   ├── AgentText.vue         # NEW — rend un texte d'agent en markdown sanitizé + styles
│   │   └── AgentText.spec.ts     # NEW — tests TDD
│   └── pages/
│       ├── AtelierDetailPage.vue      # MOD — <AgentText> pour scenario/explanation/entries
│       └── AtelierDetailPage.spec.ts # MOD — scénarios markdown dans les textes
│   └── services/atelierApi.ts    # inchangé (pas de changement de contrat)
└── package.json                  # MOD — marked + dompurify épinglés (jsonrepair retiré)

backend/
└── src/main/java/com/cornucopai/game/
    ├── llm/SystemPrompts.java            # MOD — règle markdown + bloc ```json
    └── agent/LlmPlayerAgent.java         # MOD — rappel dans play/react/answer/vote/débat
└── src/test/java/com/cornucopai/game/
    ├── llm/SystemPromptsTest.java        # MOD — assertions règle markdown/```json
    └── agent/PromptsTest.java            # MOD — assertions rappel par prompt
    └── agent/LlmPlayerAgentTest.java     # MOD — assertion prompt de débat
```

**Structure Decision**: arborescence existante conservée (constitution : seulement `backend/` et
`frontend/`) ; la logique front est isolée en utilitaire pur + composant, la logique back reste
dans les classes de prompts existantes.

## Complexity Tracking

Aucune violation de la constitution — aucun tableau requis.

## Phase 0 — Research

Voir `research.md` : R-001 (choix `marked` 18.2.0 + `DOMPurify` 3.4.17, licences MIT /
MPL-2.0-or-Apache-2.0 ; piste jsonrepair abandonnée après correction de format), R-002 (options
`gfm`/`breaks`, styles des blocs de code dont `language-json`), R-003 (champs concernés de la page
détail), R-004 (règle prompt système + rappels utilisateur), R-005 (aucun contrat API modifié).

## Phase 1 — Design

Voir `data-model.md` (TexteMarkdownAgent → HtmlSanitise → BlocDeCode + règles de validation) et
`quickstart.md` (scénarios de validation). **Contrats** : inchangés — `GET /api/ateliers/{id}`
renvoie déjà les textes bruts ; le front les met en forme localement (R-005).

## Constitution Check (post-design)

Tous les principes restent satisfaits après design : la spec révisée reste la source de vérité,
le TDD est planifié tâche par tâche, chaque module a une responsabilité unique (parse+sanitize /
rendu), les dépendances sont épinglées et justifiées (R-001), la sanitization (FR-005) est testée
contre l'injection, et aucune complexité non requise n'est introduite (pas de migration, pas de
nouveau endpoint).
