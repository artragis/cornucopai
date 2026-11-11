# Feature Specification: Entrées structurées du tour (questions/réponses) et optimisation des prompts agents

**Feature Branch**: `005-leg-qa-structure`

**Created**: 2026-11-08

**Status**: Draft

**Input**: User description: "Problèmes de performances observés sur les vraies parties LLM : (1) le modèle produit trop de tokens pour la phase de validation — revoir le prompt pour être plus clair, on ne cherche pas son raisonnement dans ce cas ; (2) le prompt de demande de précision/question est mal fait — il énonce inconditionnellement 3 statuts (précision, question, passer) puis ajoute ensuite que la question n'est possible que si un booléen l'autorise — il faut faire autrement ; (3) erreur probable de conception du résultat du tour : plutôt qu'une liste plate de strings (les précisions), avoir une liste d'objets avec des champs permettant de lister les questions ET leur réponse — ce qui permettrait de délocaliser la lecture de ces questions/réponses dans un outil, pour que le modèle sache à moindre frais si une question ayant reçu 'je sais pas' a déjà été posée. accessoirement, le prompt parle de 'leg' alors qu'il est en français."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Persister le tour sous forme d'entrées structurées (Priority: P1)

En tant que participant, je veux que le résultat d'un tour de jeu soit persisté comme une liste d'**entrées structurées** — chaque entrée ayant un type (`PRECISION` ou `QUESTION`), un auteur, un texte, et pour les questions un indicateur `knows` plus la `answer` quand le joueur de la carte a su répondre — afin que les questions posées et leurs réponses soient lisibles comme telles, et non noyées dans une liste plate de chaînes.

**Why this priority**: C'est la refonte de conception demandée : sans entrées structurées, impossible de distinguer une question d'une précision, ni de savoir quelles questions ont reçu « Je sais pas ».

**Independent Test**: Jouer un tour (test à une seule famille) avec une question et une précision ; relire le fichier JSON du tour et vérifier le schéma `entries: [{kind, author, text, ...}]` avec la question portant `knows`/`answer`.

**Acceptance Scenarios**:

1. **Given** un tour en cours, **When** un joueur ajoute une précision, **Then** une entrée `{kind: "PRECISION", author, text}` est ajoutée à `entries` et persistée.
2. **Given** un joueur pose une question, **When** le joueur de la carte répond « Je sais » + réponse, **Then** une entrée `{kind: "QUESTION", author, text, knows: true, answer: "..."}` est persistée.
3. **Given** un joueur pose une question, **When** le joueur de la carte répond « Je sais pas », **Then** une entrée `{kind: "QUESTION", author, text, knows: false}` est persistée (sans `answer`).
4. **Given** le schéma de persistance, **When** un tour est écrit, **Then** le fichier contient toujours 5 champs de premier niveau : `cardName`, `scenario`, `explanation`, `entries`, `validation`.
5. **Given** un ancien atelier (schéma v1, `precisions: [string]`), **When** on le relit, **Then** chaque chaîne est convertie en entrée `PRECISION` (auteur inconnu) et l'atelier reste lisible sans erreur.

---

### User Story 2 - Lire les questions/réponses du tour via un outil (Priority: P1)

En tant qu'agent IA, je veux pouvoir appeler un outil `get_tour_info` qui me retourne les entrées structurées du tour en cours — notamment les questions déjà posées et leur `knows`/réponse — afin de savoir à moindre frais si une question a déjà reçu « Je sais pas », sans que tout le contenu du tour soit recopié dans mon prompt.

**Why this priority**: C'est le levier de performance principal : le prompt de réaction n'embarque plus tout l'historique du tour ; le modèle le consulte par outil quand il en a besoin.

**Independent Test**: Pendant un tour de test, appeler l'outil avec le tour courant renseigné et vérifier que la réponse liste les entrées structurées, questions avec `knows` inclus ; appeler l'outil avant tout tour → réponse « aucun tour en cours ».

**Acceptance Scenarios**:

1. **Given** un tour en cours avec des entrées, **When** l'agent appelle `get_tour_info`, **Then** la réponse contient les entrées structurées (kind, author, text, knows, answer) du tour courant.
2. **Given** une question ayant reçu « Je sais pas », **When** l'agent appelle l'outil, **Then** cette question apparaît avec `knows: false`, permettant au modèle de ne pas la reposer.
3. **Given** aucun tour en cours, **When** l'outil est appelé, **Then** il répond explicitement « aucun tour en cours » sans erreur.
4. **Given** la toolbox de chaque agent, **When** un agent est créé pour une partie, **Then** `get_tour_info` (lié à cette partie) et `get_card_info` sont tous deux enregistrés.

---

### User Story 3 - Vote minimal sans raisonnement, tokens capés (Priority: P1)

En tant qu'utilisateur, je veux que la phase de validation produise le moins de tokens possible : le prompt demande UNIQUEMENT le JSON du vote, sans aucun raisonnement ni explication, et la génération est capée à quelques tokens.

**Why this priority**: Sur les vraies parties, le vote coûtait plusieurs centaines de secondes de génération pour un simple oui/non.

**Independent Test**: Inspecter le prompt de vote et la configuration de l'appel (test unitaire) : le prompt interdit explicitement tout raisonnement, et la sortie est capée (paramètre de requête ou modèle dédié avec `maxTokens` faible).

**Acceptance Scenarios**:

1. **Given** la phase de vote, **When** le prompt est construit, **Then** il demande UNIQUEMENT `{"vote": "YES"}` ou `{"vote": "NO"}` et interdit explicitement raisonnement/explication.
2. **Given** l'appel LLM de vote, **When** il est émis, **Then** le nombre maximal de tokens de sortie est capé à une petite valeur (ex. 16) — via paramètres de requête par appel si la librairie le permet, sinon via un modèle dédié capé.
3. **Given** un modèle qui déborde malgré tout, **When** la réponse est parsée, **Then** tout texte parasite autour du JSON est toléré (extraction du JSON) et un échec de parsing retombe sur le repli NO (FR-013 de la spec 004).

---

### User Story 4 - Prompt de réaction avec statuts conditionnels propres (Priority: P1)

En tant qu'agent IA, je veux que le prompt de réaction (précision/question/passer) présente les statuts de manière cohérente : quand les questions sont autorisées, les 3 statuts sont listés avec la règle « une question maximum » ; quand elles sont interdites (après un « Je sais pas »), seuls PRÉCISION et PASSER sont proposés et la question est explicitement interdite — sans phrase inconditionnelle suivie d'une exception contradictoire.

**Why this priority**: Le prompt actuel dit « 3 statuts possibles » puis « sauf si... » : le modèle hésite et produit des réponses invalides.

**Independent Test**: Construire les deux variantes du prompt (questions autorisées / interdites) et vérifier par test unitaire la structure de chaque variante.

**Acceptance Scenarios**:

1. **Given** `questionsAllowed = true`, **When** le prompt de réaction est construit, **Then** il liste `PRECISION`, `QUESTION`, `PASS` avec la règle « une seule question ».
2. **Given** `questionsAllowed = false`, **When** le prompt est construit, **Then** il liste UNIQUEMENT `PRECISION` et `PASS` et mentionne explicitement « tu ne peux pas poser de question » ; le mot `QUESTION` n'apparaît pas comme statut proposé.
3. **Given** un modèle qui renvoie pourtant une question alors qu'elle est interdite, **When** l'agent la reçoit, **Then** elle est convertie en `PASS` (le GameMaster l'aurait rejetée sinon) — comportement déjà en place, à conserver.

---

### User Story 5 - Terminologie française dans tous les prompts agents (Priority: P2)

En tant qu'utilisateur lisant les logs, je veux que les prompts envoyés aux agents soient entièrement en français et utilisent le mot « tour » (ou « la carte posée ») plutôt que le jargon anglais « leg », afin que les traces soient cohérentes avec le reste de l'interface.

**Why this priority**: Les prompts mélangeaient français et termes techniques anglais ("leg"), ce qui prête à confusion pour le modèle comme pour les lecteurs de logs.

**Independent Test**: Test unitaire parcourant tous les prompts construits par l'agent : aucun ne contient le mot « leg » ; la réaction/vote/question utilisent « tour ».

**Acceptance Scenarios**:

1. **Given** n'importe quel prompt construit par l'agent (jouer, réagir, répondre, voter), **When** il est inspecté, **Then** il ne contient pas le mot « leg ».
2. **Given** le prompt de réaction, **When** il décrit le tour en cours, **Then** il parle du « tour » / de « la carte posée par <joueur> ».

---

### Edge Cases

- Que se passe-t-il à la relecture d'un atelier legacy (`precisions: ["..."]`) ? → Converti en entrées `PRECISION` (author `"inconnu"`), aucun échec de lecture.
- Que se passe-t-il si une entrée persistée a un `kind` inconnu ? → Ignorée à la lecture (log WARN), le reste du tour reste lisible.
- Que se passe-t-il si l'outil `get_tour_info` est appelé avant qu'un tour soit posé ? → « Aucun tour en cours », sans erreur.
- Que se passe-t-il si le modèle répond « Je sais » avec une réponse vide ? → Entrée `QUESTION` avec `knows: false` (comportement spec 004 conservé).
- Que se passe-t-il si le JSON du vote contient du texte autour ? → Extraction tolérante du JSON ; sinon repli NO.
- Que se passe-t-il pour la persistance entre deux tours ? → Chaque tour a son propre fichier ; `entries` part toujours de la liste vide à la création du tour.
- Les tests de partie restent à UNE seule famille de cartes (FR-014 de la spec 004).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Le tour persisté MUST comporter 5 champs de premier niveau : `cardName`, `scenario`, `explanation`, `entries`, `validation`. `entries` MUST être une liste d'objets `LegEntry` : `{kind: "PRECISION"|"QUESTION", author: string, text: string, knows: boolean (QUESTION seulement), answer: string (QUESTION seulement, présente si knows=true)}`.
- **FR-002**: Le maître du jeu MUST enregistrer chaque précision comme entrée `PRECISION` (avec l'auteur) et chaque question/réponse comme entrée `QUESTION` (`knows` + `answer` quand « Je sais »).
- **FR-003**: La lecture d'un atelier legacy (v1, champ `precisions` = liste de chaînes) MUST convertir chaque chaîne en entrée `PRECISION` avec `author: "inconnu"`, sans erreur.
- **FR-004**: Un nouvel outil `get_tour_info` MUST être disponible dans la toolbox de chaque agent : il retourne les entrées structurées du tour courant de la partie (avec `knows`/`answer` des questions), et répond « Aucun tour en cours » si aucun tour n'est posé. L'outil est lié à la partie (instance par atelier).
- **FR-005**: Le prompt de réaction MUST NOT contenir le contenu complet du tour (scénario, explication, entrées) ; il y fait référence et oriente vers l'outil `get_tour_info`. Les statuts proposés sont construits conditionnellement : `questionsAllowed=true` → `PRECISION|QUESTION|PASS` (une question max) ; `false` → `PRECISION|PASS` uniquement, question explicitement interdite.
- **FR-006**: Le prompt de vote MUST demander UNIQUEMENT le JSON `{"vote": "YES"|"NO"}` et interdire tout raisonnement/explication ; la sortie du vote MUST être capée à ≤ 16 tokens (paramètres par appel si supportés, sinon modèle dédié capé).
- **FR-007**: Tous les prompts des agents MUST être en français et ne MUST PAS contenir le mot « leg » ; le tour en cours est désigné par « tour » / « la carte posée par <joueur> ».
- **FR-008**: Le prompt de réponse à une question (« Je sais » / « Je sais pas ») MUST rester minimal mais conserve le contrat JSON `{"knows": ..., "answer": ...}`.
- **FR-009**: Le repli en cas d'échec LLM (spec 004 FR-013) MUST être conservé tel quel, y compris pour les nouvelles entrées structurées.
- **FR-010**: Les tests de partie MUST rester à une seule famille de cartes (spec 004 FR-014) ; le contrat `leg-schema.json` passe en v2.0.0 (changement cassant, repli de lecture legacy couvert par FR-003).

### Key Entities *(include if feature involves data)*

- **LegEntry (entrée de tour)** : `{kind: PRECISION|QUESTION, author, text, knows?, answer?}` — `knows`/`answer` présents uniquement pour `QUESTION` ; persistée dans le fichier du tour.
- **Leg (tour de jeu)** : 5 champs `cardName`, `scenario`, `explanation`, `entries: LegEntry[]`, `validation` — `entries` remplace `precisions` (v1).
- **LegContext (contexte de partie)** : référence vers le tour courant d'une partie, partagée par les agents de la partie pour alimenter l'outil.
- **TourInfoTool (outil get_tour_info)** : outil LangChain4j par partie, retourne les entrées du tour courant en JSON/Markdown.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100 % des tours écrits par les tests ont un champ `entries` composé d'objets `LegEntry` conformes au schéma v2 (vérifié sur les fichiers JSON persistés).
- **SC-002**: Le prompt de vote contient l'interdiction de raisonnement et l'appel de vote est configuré avec une sortie capée ≤ 16 tokens (vérifié par test unitaire sur la configuration/les paramètres).
- **SC-003**: Aucune variante du prompt de réaction ne contient à la fois la liste des 3 statuts et une exception ; la variante `questionsAllowed=false` ne propose que `PRECISION`/`PASS` (test unitaire sur les deux variantes).
- **SC-004**: Le prompt de réaction ne contient plus le contenu du tour (le scénario n'y apparaît pas) et mentionne l'outil `get_tour_info` (test unitaire).
- **SC-005**: 100 % des prompts construits par l'agent sont sans le mot « leg » (test unitaire parcourant playCard/react/answer/vote).
- **SC-006**: `get_tour_info` retourne les entrées du tour courant incluant les questions avec leur `knows` (test unitaire + test d'intégration à une seule famille).
- **SC-007**: Un atelier legacy v1 (`precisions` en chaînes) est relu sans erreur, converti en entrées `PRECISION` (test).
- **SC-008**: Suite de tests complète verte, chaque test de partie à une seule famille.

## Assumptions

- Le changement de schéma du tour est un **changement cassant accepté** (v1 → v2) : les nouveaux tours écrivent `entries` ; la lecture des anciens fichiers (`precisions`) reste supportée par conversion (FR-003). L'atelier de smoke test `atelier20261108-232136` est un cas legacy réel.
- « Tour » est la traduction retenue pour « leg » dans les prompts ; le terme de domaine `Leg` reste dans le code Java (les prompts, eux, sont en français).
- Le plafond de tokens du vote sera posé par paramètres de requête par appel si la librairie le permet (à vérifier en phase 0), sinon par un second modèle dédié avec `maxTokens` faible, construit dans le même brain.
- La règle métier du maître du jeu (une question interdite après « Je sais pas » pour ce joueur) reste appliquée côté serveur en plus de l'outil, comme garde-fou.
- Le prompt de réaction allégé suppose que le modèle appelle effectivement `get_tour_info` quand il a besoin du détail du tour ; si ce n'est pas le cas, le comportement reste correct (moins riche), jamais invalide.
