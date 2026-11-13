# Feature Specification: Rendu markdown des textes des agents dans le résultat de l'atelier

**Feature Branch**: `010-markdown-agent-texts`

**Created**: 2026-11-13

**Status**: Draft

**Input**: User description: "Il faudrait faire une petite amélioration dans l'interface qui décrit le résultat du jeu car les agents aiment mettre du JSON dans leurs textes et donc _a rend tout peu lisible. On va donc importer un parseur côté front pour tout mettre comme il faut et les afficher avec les bons styles. Tu pourras légèrement modifier les prompts qu'on envoie aux agents pour leur dire de faire du JSON dans leur texte comme ça ça sera toujours le cas, on ne risque pas d'un coup d'avoir du XML." — puis correction : "le format qu'envoient les agents doit être **markdown** : le parseur côté front doit être un parseur **markdown**, pas un parseur JSON. Le JSON embarqué dans les textes passe dans des blocs de code ```json."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Lire les textes markdown des agents joliment dans le résultat de l'atelier (Priority: P1)

En tant qu'utilisateur humain qui consulte la page de résultat d'un atelier, je veux que les textes produits par les agents (scénario, explication, précisions, questions, réponses) — qui sont en **markdown** — soient parsés et rendus avec les bons styles (titres, gras, listes, blocs de code), afin que le JSON que les agents incluent dans des blocs ```json apparaisse en bloc de code lisible (les `_` des clés ne sont plus interprétés comme de l'italique) au lieu d'un texte brut illisible.

**Why this priority**: C'est le cœur de la demande — aujourd'hui les textes markdown des agents sont affichés brut, ce qui rend le JSON embarqué illisible ; c'est le problème constaté concrètement.

**Independent Test**: Ouvrir la page de détail d'un atelier dont un texte d'agent contient du markdown (gras, liste, bloc ```json) : le rendu applique les styles markdown et le bloc ```json apparaît en bloc de code stylé ; un texte sans markdown reste un simple paragraphe lisible.

**Acceptance Scenarios**:

1. **Given** un texte d'agent contenant du markdown (`**gras**`, `# titre`, `- item`), **When** la page de résultat s'affiche, **Then** le rendu applique les styles correspondants (strong, h1, liste).
2. **Given** un texte d'agent contenant un bloc de code ```json avec un objet JSON (ex. `{"card_id": "VE2"}`), **When** la page s'affiche, **Then** le bloc est rendu comme un bloc de code stylé (`pre`/`code` avec classe `language-json`), le JSON reste lisible et les `_` ne produisent aucun `<em>`.
3. **Given** un texte d'agent contenant un JSON brut non fenced (ex. `{"card_id": "VE2"}` en ligne), **When** la page s'affiche, **Then** le texte reste lisible : aucun `_` intra-mot ne crée d'italique.
4. **Given** un texte d'agent sans aucun markdown, **When** la page s'affiche, **Then** il est rendu comme un simple paragraphe (les sauts de ligne simples sont conservés), comme avant.
5. **Given** un texte d'agent contenant du HTML dangereux (`<script>`, `onerror=…`), **When** la page s'affiche, **Then** le HTML dangereux est neutralisé (sanitization) — aucun script ne s'exécute.
6. **Given** un atelier ancien (données legacy) dont les textes sont de la prose simple, **When** la page s'affiche, **Then** le rendu reste visuellement identique à l'actuel.

---

### User Story 2 - Des prompts qui garantissent du markdown (JSON fenced,) dans les textes des agents (Priority: P1)

En tant qu'utilisateur qui lance des ateliers, je veux que les prompts envoyés aux agents leur demandent explicitement d'écrire leurs textes libres en **markdown**, et que toute donnée structurée (JSON) soit placée dans un bloc de code ```json — jamais dans un bloc XML ou autre balisage — afin que le rendu markdown du front s'applique toujours et que le JSON soit toujours bien délimité.

**Why this priority**: Le rendu front repose sur le parsing markdown ; sans consigne explicite, un agent peut produire du XML ou un format brut et casser l'affichage. C'est le complément indispensable de l'US1.

**Independent Test**: Inspecter les prompts (système et messages utilisateur) construits par le backend pour chaque action de jeu (jouer une carte, réagir, répondre, voter, débattre) et vérifier qu'ils contiennent tous la règle « textes libres en markdown, données structurées en bloc ```json uniquement, ».

**Acceptance Scenarios**:

1. **Given** le prompt système envoyé à chaque agent, **When** on le consulte, **Then** il contient une règle indiquant que les champs texte libre (scenario, explanation, text, answer, justification, argument de débat) sont en markdown, et que toute donnée structurée y est incluse dans un bloc de code ```json ni autre balisage.
2. **Given** les prompts utilisateur de chaque action (jouer une carte, réagir à un tour, répondre à une question, voter, argument de débat), **When** on les consulte, **Then** ils reprennent cette règle (markdown + bloc ```json,).
3. **Given** un agent qui produit un texte libre avec des données structurées, **When** la partie se déroule, **Then** ces données sont dans un bloc ```json (vérifiable sur les ateliers persistés), donc rendues en bloc de code stylé côté front.

---

### Edge Cases

- **HTML/JS injecté dans un texte d'agent** (prompt injection) : le rendu markdown est sanitizé — `<script>`, gestionnaires d'événements, `javascript:` sont supprimés.
- **Bloc de code d'un autre langage** (```xml, ```yaml) : rendu comme bloc de code standard, mais les prompts interdisent XML pour les données structurées.
- **Bloc de code non fermé** en fin de texte : le parseur markdown le rend comme un bloc de code ouvert — jamais d'erreur, jamais de HTML brut non échappé.
- **Texte très long avec du markdown** : rendu complet, sans limite arbitraire.
- **Anciens ateliers (legacy)** : prose simple → simple paragraphe, sauts de ligne conservés ; rendu visuellement identique.
- **Texte vide ou null** : rien n'est rendu, aucune erreur.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La page de résultat d'un atelier MUST rendre en markdown les textes produits par les agents : `scenario`, `explanation`, `entries[].text`, `entries[].answer`. Le `featureHtml` (HTML fourni par l'utilisateur) reste tel quel, hors périmètre.
- **FR-002**: Le rendu MUST appliquer les styles markdown : titres, gras/italique, listes, liens, et blocs de code `pre`/`code` stylés (avec une mise en évidence spécifique des blocs `language-json`).
- **FR-003**: Les sauts de ligne simples MUST être conservés dans le rendu (option `breaks`), pour préserver l'affichage des textes legacy.
- **FR-004**: Les `_` et autres marqueurs à l'intérieur des blocs de code ou en intra-mot MUST ne pas produire d'italique/emphase parasite dans le JSON.
- **FR-005**: Le HTML produit MUST être sanitizé avant injection (bibliothèque de sanitization dédiée) : aucun `<script>`, gestionnaire d'événement ou URL `javascript:` ne doit survivre.
- **FR-006**: Le front MUST importer un parseur markdown dédié (dépendance épinglée, licence vérifiée, choix et justification enregistrés dans le plan) plutôt que de réimplementer un parseur.
- **FR-007**: Un texte sans markdown MUST être rendu comme un simple paragraphe lisible (compatibilité visuelle avec les ateliers existants).
- **FR-008**: Le prompt système des agents MUST comporter une règle explicite : les champs texte libre sont en markdown ; toute donnée structurée y est incluse dans un bloc de code ```json (JSON valide) ni autre balisage.
- **FR-009**: Les prompts utilisateur des actions de jeu (jouer une carte, réagir, répondre à une question, voter, débat) MUST rester cohérents avec FR-008 (rappel léger dans les champs texte libre concernés).
- **FR-010**: Les ateliers existants (données persistées avant cette feature) MUST rester consultables sans changement de format de leurs données.

### Key Entities *(include if feature involves data)*

- **Texte markdown d'agent** (existant, rendu modifié) : champ texte libre produit par un agent (`scenario`, `explanation`, `entries[].text`, `entries[].answer`) ; c'est désormais du markdown.
- **HTML sanitizé** (nouveau, front) : résultat du parsing markdown d'un texte d'agent puis de la sanitization ; c'est ce qui est injecté dans le DOM du composant de rendu.
- **Bloc de code** : élément `pre > code` du rendu markdown ; un bloc ```json porte la classe `language-json` et reçoit un style distinct.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100 % des textes d'agents de la page de résultat sont rendus via le parseur markdown (vérifié par tests d'intégration sur la page).
- **SC-002**: 100 % des blocs ```json présents dans les textes sont rendus en bloc de code stylé `language-json`, sans `<em>` parasite (vérifié par tests).
- **SC-003**: 100 % des textes injectés contenant du HTML dangereux sont sanitizés (`<script>`/gestionnaires d'événements supprimés — tests dédiés).
- **SC-004**: 100 % des textes sans markdown sont rendus comme un simple paragraphe avec sauts de ligne conservés (ateliers legacy — tests de non-régression).
- **SC-005**: 100 % des prompts (1 prompt système + les prompts utilisateur de playCard / react / answer / vote / débat) contiennent la règle « markdown + bloc ```json, ».
- **SC-006**: Les dépendances markdown ajoutées au front sont épinglées dans `package.json`, et `npm run lint`, `npm run build` et `npm test` passent.
- **SC-007**: Les tests backend ciblant les prompts (SystemPromptsTest, PromptsTest, LlmPlayerAgentTest) passent avec les nouveaux textes de prompts.

## Assumptions

- Le format des textes libres des agents est le **markdown** (correction du 2026-11-13 : parseur markdown, pas JSON) ; le JSON embarqué est placé dans des blocs de code ```json.
- Les champs concernés sont ceux affichés sur la page de détail d'un atelier et produits par les agents : `scenario`, `explanation`, `entries[].text`, `entries[].answer`. Le `featureHtml` reste du HTML fourni par l'utilisateur, hors périmètre.
- Le parseur markdown choisi est une dépendance front dédiée (voir plan) ; le rendu passe par une sanitization (DOMPurify ou équivalent) avant injection HTML, testée contre l'injection de scripts.
- Les sauts de ligne simples sont conservés (option `breaks` du parseur) pour garder les textes legacy lisibles tels quels.
- Côté prompts, la règle « markdown + ```json, » est ajoutée au prompt système (contrat de sortie) avec un rappel léger dans chaque prompt utilisateur concerné ; aucune restructuration du flux de jeu.
- Aucun changement de format de persistance (les ateliers JSON existants restent valides tels quels).
- Le style visuel s'appuie sur le thème Vuetify existant, sans nouveau framework UI.
