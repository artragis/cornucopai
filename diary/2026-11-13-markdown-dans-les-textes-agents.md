# 2026-11-13 — Les textes des agents passent en markdown rendu côté front

Feature `specs/010-markdown-agent-texts`. Constat : les agents aiment embarquer du JSON dans
leurs textes libres (scénario, explication, précisions, réponses), et affiché brut c'est
illisible — les `_` des clés JSON (`card_id`, `threat_actor`…) rendent tout « peu lisible ».

## La fausse piste évitée

Premier réflexe : importer un parseur **JSON** côté front (`jsonrepair`) pour détecter et
pretty-printer les blocs JSON embarqués. Erreur de format : ce que les agents envoient, c'est du
**markdown**, pas du JSON brut. Le parseur JSON a été retiré (`npm uninstall jsonrepair`) avant
toute commit de la feature — la spec a été entièrement réécrite en `010-markdown-agent-texts`.

## La décision retenue

- **Front** : un composant `AgentText.vue` parse les textes d'agents avec `marked` (épinglé
  `18.2.0`, MIT, options `gfm` + `breaks` pour garder les sauts de ligne simples des ateliers
  legacy) et injecte le HTML **sanitizé par `DOMPurify`** (épinglé `3.4.17`) — indispensable car
  ces textes viennent de LLM (injection possible) ; testé contre `<script>`/`onerror`/`javascript:`.
  Les blocs ```json rendent en `<pre><code class="language-json">` stylé : le JSON y est enfin
  lisible, et les `_` intra-mot ne produisent plus d'italique parasite (CommonMark). Le composant
  est branché sur `scenario`, `explanation`, `entries[].text`, `entries[].answer` de la page de
  résultat d'atelier.
- **Back (prompts)** : règle ajoutée au contrat de sortie du prompt système + rappel léger dans
  chaque prompt d'action (play/react/answer/vote/débat) : les champs texte libre sont en
  **markdown**, et toute donnée structurée y va dans un bloc de code ```json.
  Comme ça le rendu front s'applique toujours, et on ne risque pas un jour de recevoir du XML.

## Bilan

TDD strict (tests écrits avant, vus rouges, puis verts) : front 51 tests vitest, 0 erreur eslint,
build OK ; back SystemPromptsTest/PromptsTest/LlmPlayerAgentTest verts avec les nouvelles
assertions. Aucun changement d'API ni de persistance : les ateliers legacy restent tels quels.
