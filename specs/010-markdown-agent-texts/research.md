# Research — Rendu markdown des textes des agents (spec 010)

> Correction du 2026-11-13 : le format des textes envoyés par les agents est le **markdown**, pas
> du JSON brut. Le parseur côté front est donc un **parseur markdown** ; le JSON embarqué passe
> dans des blocs de code ```json. La première piste (parseur JSON `jsonrepair` + scanner de
> blocs) est abandonnée.

## R-001 : Parseur markdown côté front

- **Decision** : `marked` épingé à `18.2.0` (parseur markdown CommonMark/GFM, licence MIT,
  types inclus) + `DOMPurify` épingé à `3.4.17` (sanitization, licence MPL-2.0 OR Apache-2.0,
  types inclus) dans `frontend/package.json`.
- **Rationale** : `marked` est le parseur markdown de référence, configurable (`gfm: true`,
  `breaks: true` pour conserver les sauts de ligne simples des textes legacy). Les textes des
  agents étant produits par des LLM, le HTML généré DOIT être sanitizé avant injection :
  `DOMPurify` supprime `<script>`, gestionnaires d'événements et URL `javascript:` (FR-005) —
  testé unitairement. Le couple marked+DOMPurify est le standard de l'industrie pour ce cas.
- **Alternatives considérées et rejetées** :
  - parseur JSON (`jsonrepair`) + scanner de blocs maison — abandonné après correction du format
    (les agents envoient du markdown, pas du JSON brut) ;
  - `markdown-it` — équivalent à marked, mais marked a une API plus simple pour un rendu HTML
    complet et est déjà très répandu ;
  - rendu markdown → composants Vue sans HTML (ex. `marked` → VNodes) — sur-ingenierie pour ce
    besoin (YAGNI, constitution V) ; le couple marked+DOMPurify + `v-html` est déjà le pattern
    existant pour `featureHtml`.

## R-002 : Options du parseur et rendu des blocs de code

- **Decision** : `marked.setOptions({ gfm: true, breaks: true })`. Un bloc ```json produit
  `<pre><code class="language-json">…</code></pre>` ; les styles CSS scopés du composant donnent
  aux blocs `pre` un fond `surface-variant`, police monospace, et une nuance distincte pour
  `.language-json` (FR-002). Les `_` intra-mot ne produisent pas d'italique (CommonMark), et
  à l'intérieur d'un bloc de code aucun marqueur n'est interprété (FR-004).
- **Rationale** : `breaks: true` conserve les sauts de ligne simples → les textes legacy (prose
  sur plusieurs lignes) restent visuellement identiques (FR-007).

## R-003 : Zones de l'interface concernées

- **Decision** : page de détail d'un atelier (`AtelierDetailPage.vue`), champs produits par les
  agents : `leg.scenario`, `leg.explanation`, `entry.text`, `entry.answer`, via un composant
  `AgentText.vue` (`<AgentText :text="…" />`). Le `featureHtml` reste en `v-html` tel quel, hors
  périmètre.
- **Rationale** : ce sont les seuls textes d'agents affichés ; le reste de la page est inchangé.

## R-004 : Modification des prompts (backend)

- **Decision** : une règle ajoutée au « Contrat de sortie » du prompt système
  (`SystemPrompts.RULES_AND_CONTRACT`) : les champs texte libre (scenario, explanation, text,
  answer, justification, argument de débat) sont en **markdown** ; toute donnée structurée y est
  incluse dans un bloc de code ```json (JSON valide). Rappel
  léger (une ligne) dans chaque prompt utilisateur concerné de `LlmPlayerAgent` (playPrompt,
  reactPrompt, answerPrompt, votePrompt, débat). Aucun changement de DTO ni de flux de jeu.
- **Rationale** : FR-008/FR-009 ; la règle dans le système couvre tous les agents (spec 009 :
  personnalité libre), le rappel local ancre le comportement sur les champs texte.
- **Pièges repérés dans les tests existants** : `PromptsTest.reactPromptWithoutQuestions...`
  interdit le mot `QUESTION` dans la variante sans question, et `noPromptContainsTheWordLeg`
  interdit le mot « leg » — les lignes ajoutées ne contiennent ni l'un ni l'autre.

## R-005 : Contrats API

- **Decision** : aucun changement de contrat. Le rendu markdown se fait côté front sur les
  champs texte existants de `GET /api/ateliers/{id}` ; aucune modification de
  `ateliers-api.yaml` ni des schémas. Les ateliers legacy restent valides tels quels
  (FR-010).
