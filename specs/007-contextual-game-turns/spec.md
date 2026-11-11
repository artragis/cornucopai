# Feature Specification: Agents conscients du projet — MCP IDE, mémoire partagée et vote par arbre de décision

**Feature Branch**: `007-contextual-game-turns`

**Created**: 2026-11-11

**Status**: Draft

**Input**: User description: "Nous allons tenter d'améliorer ce qu'il se passe en général durant un tour de jeu.

## Clarifications

### Session 2026-11-11

- Q: Quel projet logiciel le serveur MCP IDE donne-t-il aux agents à analyser — chemin configurable par atelier, ou projet ouvert dans l'IDE de l'utilisateur ? → A: Option A — le MCP pointe toujours vers le projet ouvert dans l'IDE de l'utilisateur ; aucune config de chemin côté CornucopAI, l'URL du serveur MCP suffit.
- Q: Les résultats retournés par les appels MCP doivent-ils être traités comme du contenu brut « DONNÉES » (anti-injection), comme le HTML de la feature et les textes de cartes ? → A: Option B — non, les résultats MCP sont considérés comme de confiance (serveur local 127.0.0.1) et ne sont PAS encapsulés dans une section « DONNÉES » ; seuls le HTML de la feature et les textes de cartes restent encapsulés.
- Q: Les faits de la mémoire partagée sont-ils rechargés au re-démarrage d'un atelier, ou purement volatils ? → A: Persistés avec l'atelier dans son JSON ; lors de la création d'un nouvel atelier à partir d'un atelier existant, un sélecteur (dropdown) dans l'interface demande si l'on veut réutiliser les faits projet de l'atelier source — si oui ils sont copiés dans la mémoire partagée du nouvel atelier, si non la mémoire démarre vide.
- Q: Combien de faits les plus récents (N) sont injectés dans le prompt quand la mémoire partagée dépasse le seuil de fenêtrage ? → A: 20 faits les plus récents dans le prompt ; tous restent consultables via l'outil `get_project_facts`.
- Q: À quel moment les faits découverts sont-ils persistés ? → A: (révision 2026-11-11, R-062) à la fin de **chaque tour** : la mémoire partagée est instantanée dans l'atelier (`atelier.json`) après chaque tour, pas seulement en fin de partie. L'héritage via le dropdown alimente aussi la mémoire partagée vivante des agents (et pas seulement le JSON de l'atelier).
- Q: Les champs du vote (vote, explanationCheck, applicabilityCheck) sont-ils des chaînes libres ? → A: (révision 2026-11-11, R-060) non — ce sont des enums (`VoteChoice`, `ExplanationCheck`, `ApplicabilityCheck`) ; une valeur inconnue côté LLM échoue à la désérialisation et retombe sur le repli NO, sans validation de chaîne manuelle.
- Q: Le mode « débat structuré » avant le vote (US5) doit-il être activé par défaut ou désactivé par défaut ? → A: Option A — activé par défaut pour toutes les parties ; chaque vote est précédé d'un débat borné entre les agents du tour. **Révisé le même jour après revue d'une partie réelle (R-061) : désactivé par défaut** (`cornucopai.debate.enabled=false`, env `DEBATE_ENABLED`) — un argument hypothétique (« si le champ était un string… ») avait convaincu les votants sans aucune vérification factuelle du projet ; la branche (b) du vote exige désormais une vérification réelle via MCP. Pour cela nous allons fournir un MCP de type HTTP (pour l'instant sans authentification) qui sera un MCP d'IDE. Dans mon cas, pour les tests, le MCP sera à l'URL http://127.0.0.1:64462/stream. Comme c'est un MCP il faudra le configurer un peu plus, notamment sélectionner la liste des outils utilisables : get_project_dependencies, get_project_modules, list_directory_tree, read_file, search_file, search_regex, search_text. Cela permettra aux agents de faire des recherches contextuelles sur le projet dont on discute. Ensuite pour ce qui est du vote et des questions/réponses je me demande s'il ne faudrait pas utiliser d'autres outils exposés par langchain4j (exemple les Debate ou les Workflow) pour se faciliter la vie. Notamment, pour le vote, pour rejeter, il n'est pas sensé le faire au hasard — il devrait prendre sa décision selon une sorte d'arbre : (1) le scénario est-il suffisamment bien expliqué (et/ou les précisions permettent-elles d'apporter les bonnes explications) ? (2) le scénario est-il applicable à notre cas (par exemple si le scénario implique que l'application expose quelque chose en API REST mais qu'une telle API n'existe pas dans le projet, on rejette). Enfin, il faudrait qu'au fur et à mesure que les agents découvrent les caractéristiques du projet ils puissent avoir une sorte de mémoire partagée car sinon ils vont devoir faire des appels MCP à chaque fois. Bref il faut bien étudier ce que propose langchain4j pour améliorer le rendu final."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Accéder au code du projet analysé via un serveur MCP IDE (Priority: P1)

En tant qu'agent joueur, je veux pouvoir interroger le projet logiciel soumis à l'analyse (feature) via un serveur MCP de type IDE, afin de rechercher dans le code, lire des fichiers, lister l'arborescence et connaître les dépendances — sans que ce contenu soit recopié dans mon prompt.

**Why this priority**: C'est le fondement de la feature : sans accès au projet, les agents ne peuvent pas évaluer si un scénario est applicable au cas réel. C'est le prérequis de toutes les autres histoires.

**Independent Test**: Lancer une partie sur un projet connu (ex. ce dépôt CornucopAI) et vérifier qu'un agent peut appeler les outils MCP pour obtenir l'arborescence du projet, lire un fichier source, et lister les dépendances — le tout sans que le contenu du projet soit dans le prompt initial.

**Acceptance Scenarios**:

1. **Given** un serveur MCP IDE configuré (URL HTTP, sans authentification pour les tests), **When** un agent démarre une partie, **Then** les outils MCP sélectionnés sont enregistrés dans sa toolbox au même titre que `get_card_info` et `get_tour_info`.
2. **Given** la liste des outils MCP activés (`get_project_dependencies`, `get_project_modules`, `list_directory_tree`, `read_file`, `search_file`, `search_regex`, `search_text`), **When** l'agent appelle l'un d'eux, **Then** il obtient le résultat réel du serveur MCP (arborescence, contenu de fichier, résultats de recherche).
3. **Given** un agent qui n'a jamais vu le projet, **When** il appelle `list_directory_tree` puis `read_file` sur un fichier clé, **Then** il peut se faire une représentation du projet sans intervention humaine.
4. **Given** la configuration du serveur MCP, **When** le serveur est injoignable, **Then** les appels MCP échouent gracieusement (message d'erreur retourné à l'agent) et la partie continue (repli non bloquant, comme FR-013 de la spec 004).
5. **Given** plusieurs agents dans la même partie, **When** chacun appelle les outils MCP, **Then** chaque agent a accès aux mêmes outils (le serveur MCP est partagé, pas dupliqué par agent).

---

### User Story 2 - Mémoire partagée des découvertes sur le projet entre agents (Priority: P1)

En tant qu'agent joueur, je veux pouvoir écrire dans une mémoire partagée (par atelier) les caractéristiques du projet que j'ai découvertes (ex. « le projet n'expose pas d'API REST publique », « l'authentification est en session cookie », « les dépendances incluent telle librairie vulnérable connue »), afin que les autres agents — et moi-même plus tard — puissent les consulter sans re-faire les appels MCP.

**Why this priority**: Sans mémoire partagée, chaque agent devrait redécouvrir le projet à chaque tour via MCP, ce qui est coûteux en tokens et en temps. C'est le levier d'efficacité principal.

**Independent Test**: Lancer une partie à 3 joueurs ; faire découvrir par l'agent A une caractéristique du projet (via MCP) ; vérifier que l'agent B peut lire cette découverte via un outil de mémoire partagée sans nouvel appel MCP.

**Acceptance Scenarios**:

1. **Given** une partie en cours, **When** un agent découvre une caractéristique du projet (via MCP ou raisonnement), **Then** il peut l'enregistrer dans la mémoire partagée de l'atelier via un outil dédié (ex. `add_project_fact`).
2. **Given** une mémoire partagée alimentée, **When** un agent appelle l'outil de lecture (ex. `get_project_facts`), **Then** il obtient la liste de toutes les découvertes enregistrées par tous les agents de la partie.
3. **Given** deux agents dans la même partie, **When** l'agent A enregistre un fait et que l'agent B lit la mémoire, **Then** B voit le fait de A (mémoire réellement partagée, pas isolée par agent).
4. **Given** deux ateliers distincts, **When** l'atelier 1 a une mémoire pleine et l'atelier 2 démarre, **Then** la mémoire de l'atelier 2 est vide (isolement par atelier, comme la mémoire agent de la spec 004 FR-010).
5. **Given** un fait enregistré, **When** un agent Consulte la mémoire, **Then** chaque fait indique quel agent l'a découvert et à quel tour, pour la traçabilité.
6. **Given** la mémoire partagée, **When** elle est consultée dans un prompt, **Then** elle est injectée comme section « DONNÉES » (jamais dans le system prompt) pour éviter l'injection de prompt.
7. **Given** un atelier source ayant des faits persistés, **When** on crée un nouvel atelier à partir de cet atelier source, **Then** l'interface propose un dropdown demandant si l'on veut réutiliser ces faits ; si oui ils sont copiés dans la mémoire du nouvel atelier, si non la mémoire démarre vide.

---

### User Story 3 - Vote de validation par arbre de décision explicite (Priority: P1)

En tant qu'agent votant, je veux que mon vote YES/NO soit le résultat d'un raisonnement structuré en arbre de décision, et non d'un choix au hasard : d'abord évaluer si le scénario est suffisamment bien expliqué (scénario + explication + précisions), puis évaluer si le scénario est applicable au projet réel (grâce aux outils MCP et à la mémoire partagée), et rejeter (NO) si l'une de ces deux conditions échoue.

**Why this priority**: C'est l'amélioration centrale du « rendu final » : un vote motivé et justifié améliore la qualité du threat modeling. C'est ce qui distingue un vote intelligent d'un vote aléatoire.

**Independent Test**: Poser un scénario délibérément non applicable (ex. « attaque sur l'API REST publique » alors que le projet n'en a pas) et vérifier que les agents votent NO en citant l'inapplicabilité ; poser un scénario bien expliqué et applicable et vérifier des votes YES.

**Acceptance Scenarios**:

1. **Given** un tour soumis au vote, **When** l'agent construit son raisonnement de vote, **Then** il suit un arbre de décision à deux branches : (a) le scénario est-il suffisamment bien expliqué ? (b) le scénario est-il applicable au projet réel ?
2. **Given** un scénario où l'explication est absente ou incohérente avec le scénario, **When** l'agent évalue la branche (a), **Then** il conclut « explication insuffisante » et vote NO.
3. **Given** un scénario bien expliqué mais non applicable au projet (ex. mentionne une API REST inexistante, une techno non utilisée), **When** l'agent évalue la branche (b) via les outils MCP / mémoire partagée, **Then** il conclut « non applicable » et vote NO.
4. **Given** un scénario bien expliqué ET applicable au projet, **When** l'agent termine l'arbre de décision, **Then** il peut voter YES (en fonction de sa personnalité d'archetype).
5. **Given** le résultat du vote, **When** il est persisté, **Then** la justification courte (quelle branche a échoué, ou « OK des deux côtés ») est enregistrée avec le vote pour la traçabilité.
6. **Given** un vote NO motivé par l'arbre de décision, **When** le GameMaster consolide les votes, **Then** le statut du leg reflète le rejet avec la justification agrégée.

---

### User Story 4 - Questions/réponses enrichies par le contexte projet (Priority: P2)

En tant qu'agent posant une question ou répondant à une question, je veux pouvoir m'appuyer sur le contexte projet (via MCP et mémoire partagée) pour formuler des questions plus pertinentes et des réponses plus exactes — par exemple vérifier dans le code si une certaine attaque est réellement possible.

**Why this priority**: Les Q/R sont le cœur du débat de threat modeling ; les ancrer dans le réel du projet améliore la qualité des échanges. C'est P2 car la feature fonctionne déjà sans, mais gagne beaucoup à y être.

**Independent Test**: Pendant un tour, vérifier qu'un agent peut poser une question du type « le endpoint X existe-t-il vraiment ? » et qu'un autre agent peut répondre en ayant consulté le code via MCP.

**Acceptance Scenarios**:

1. **Given** un agent qui réagit à un tour (phase précision/question), **When** il construit sa question, **Then** il peut s'appuyer sur les outils MCP et la mémoire partagée pour la rendre plus pertinente.
2. **Given** un agent qui répond à une question (« Je sais » / « Je sais pas »), **When** il formule sa réponse, **Then** il peut consulter le projet via MCP pour vérifier son affirmation avant de dire « Je sais ».
3. **Given** une question portant sur le projet (ex. « cette API est-elle exposée ? »), **When** l'agent y répond, **Then** sa réponse reflète fidèlement l'état du code (vérifiable via les outils MCP).

---

### User Story 5 - Orchestration du vote via un mécanisme de débat structuré (Priority: P2)

En tant que système, je veux que le vote de validation puisse être orchestré par un mécanisme de débat structuré (plusieurs agents délibèrent avant de voter) plutôt qu'un vote individuel direct, afin d'améliorer la qualité des décisions collectives.

**Why this priority**: C'est une piste d'amélioration identifiée par l'utilisateur (« utiliser les Debate ou les Workflow de langchain4j »). **Désactivé par défaut depuis la revue du 2026-11-11 (R-061)** : lors d'une partie réelle, un argument hypothétique a convaincu les votants sans vérification factuelle ; le mode reste disponible via `cornucopai.debate.enabled=true`.

**Independent Test**: Activer le mode « débat » pour la phase de vote et vérifier que plusieurs agents échangent des arguments avant que le vote final ne soit émis.

**Acceptance Scenarios**:

1. **Given** la phase de vote d'un tour, **When** le mode débat est activé, **Then** les agents du tour échangent un nombre borné de messages d'arguments avant le vote individuel.
2. **Given** un débat en cours, **When** un agent a déjà exprimé un argument, **Then** les autres agents peuvent y répondre (tour de parole borné, ex. 1 à 2 échanges par agent).
3. **Given** le débat terminé, **When** chaque agent émet son vote, **Then** le vote reflète les arguments échangés (le résultat du débat est visible dans la trace).
4. **Given** le mode débat, **When** la phase de vote a lieu, **Then** il est désactivé par défaut (révision 2026-11-11, R-061) : par défaut les agents votent directement (US3). Une configuration (`cornucopai.debate.enabled=true`) permet de l'activer : les agents échangent d'abord leurs arguments (échange borné), puis votent individuellement.

---

### Edge Cases

- Que se passe-t-il si le serveur MCP IDE est injoignable au démarrage de la partie ? → Les outils MCP sont enregistrés mais retournent une erreur lisible à l'appel ; la partie démarre quand même (repli non bloquant).
- Que se passe-t-il si un agent tente d'écrire un fait contradictoire avec un fait existant dans la mémoire partagée ? → Les deux faits coexistent (la mémoire est un journal, pas une base cohérente) ; c'est au lecteur (agent) d'en juger. Une note de contradiction peut être ajoutée.
- Que se passe-t-il si la mémoire partagée devient très grande (beaucoup de faits) ? → Elle est fenêtrée (ex. les N faits les plus récents sont injectés dans le prompt, tous sont consultables via l'outil).
- Que se passe-t-il si le vote par arbre de décision échoue (LLM indisponible) ? → Repli NO (comportement FR-013 spec 004 conservé), avec la justification « repli LLM indisponible ».
- Que se passe-t-il si un scénario mentionne une technologie présente dans le projet mais utilisée différemment ? → C'est à l'agent d'évaluer via MCP ; l'arbre de décision ne tranche pas automatiquement, il guide.
- Que se passe-t-il entre deux ateliers pour la mémoire partagée ? → Isolation stricte : chaque atelier a sa propre mémoire partagée, vidée au démarrage — sauf héritage explicite via le dropdown à la création (FR-007).
- Faut-il une authentification sur le MCP ? → Non pour l'instant (consigne utilisateur : « pour l'instant sans authentification »).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Le système MUST pouvoir connecter un serveur MCP HTTP (sans authentification) dont l'URL est configurable (par défaut `http://127.0.0.1:64462/stream` pour les tests), et enregistrer dans la toolbox de chaque agent les outils MCP sélectionnés. Le serveur MCP pointe vers le projet actuellement ouvert dans l'IDE de l'utilisateur ; CornucopAI ne configure pas de chemin de projet.
- **FR-002**: La liste des outils MCP activés MUST être configurable et MUST inclure par défaut : `get_project_dependencies`, `get_project_modules`, `list_directory_tree`, `read_file`, `search_file`, `search_regex`, `search_text`.
- **FR-003**: Les outils MCP MUST être disponibles en plus des outils existants (`get_card_info`, `get_tour_info`) dans la toolbox de chaque agent LangChain4j. Les résultats des appels MCP sont considérés comme du contenu de confiance (serveur local) et ne sont PAS encapsulés dans une section « DONNÉES » anti-injection (clarification 2026-11-11) — seuls le HTML de la feature et les textes de cartes sont encapsulés.
- **FR-004**: Toute erreur d'appel MCP (serveur injoignable, outil inconnu, timeout) MUST être non bloquante : l'agent reçoit un message d'erreur lisible et la partie continue (repli FR-013 spec 004).
- **FR-005**: Une mémoire partagée par atelier MUST exister : chaque agent peut y lire tous les faits découverts et y écrire les siens via des outils dédiés (ex. `get_project_facts` / `add_project_fact`).
- **FR-006**: Chaque fait de la mémoire partagée MUST comporter : le texte du fait, l'agent auteur, et le numéro du tour où il a été découvert.
- **FR-007**: La mémoire partagée MUST être isolée par atelier par défaut (deux ateliers n'ont aucune mémoire en commun) et MUST être vide au démarrage d'un atelier — SAUF si, lors de la création d'un nouvel atelier à partir d'un atelier existant, l'utilisateur choisit explicitement (dropdown UI) d'hériter des faits projet de l'atelier source : dans ce cas les faits sont copiés dans la mémoire du nouvel atelier (clarification 2026-11-11).
- **FR-008**: L'injection de la mémoire partagée dans un prompt MUST se faire dans une section « DONNÉES » du message utilisateur (jamais dans le system prompt), avec un fenêtrage à 20 faits les plus récents dans le prompt (clarification 2026-11-11) ; tous les faits restent consultables via l'outil `get_project_facts`.
- **FR-009**: Le vote de validation MUST suivre un arbre de décision à deux branches : (a) le scénario est-il suffisamment bien expliqué (scénario + explication + précisions du tour) ? (b) le scénario est-il applicable au projet réel ? Un échec à l'une des branches → vote NO. Pour la branche (b), la vérification MUST s'appuyer sur le code réel via les outils MCP (citer fichier, classe, champ et type réel trouvé) — un raisonnement hypothétique (« si le champ était un string… ») ne satisfait PAS la branche : sans vérification factuelle, `applicabilityCheck=NOT_APPLICABLE` et vote NO (révision 2026-11-11, R-061).
- **FR-010**: La justification du vote (branche évaluée, OK/échec, raison courte) MUST être persistée avec le vote pour chaque agent.
- **FR-011**: Le prompt de vote MUST guider l'agent dans l'arbre de décision (énoncer les deux questions à se poser) tout en restant compatible avec le plafonnement de tokens (spec 005 FR-006) — le raisonnement interne reste capé, seule la justification courte est persistée.
- **FR-012**: Les phases de question/réponse MUST pouvoir s'appuyer sur les outils MCP et la mémoire partagée (le prompt de réaction et le prompt de réponse MUST mentionner ces outils comme disponibles).
- **FR-013**: Un mode « débat » MUST permettre aux agents d'échanger un nombre borné d'arguments avant le vote, sans modifier le contrat de vote final (toujours YES/NO + justification). Ce mode MUST être **désactivé par défaut** (révision 2026-11-11, R-061) et activable par configuration (`cornucopai.debate.enabled=true`, env `DEBATE_ENABLED`).
- **FR-014**: La configuration du serveur MCP (URL, liste des outils) MUST être fournie par configuration seule, sans modification de code (comme FR-015 de la spec 004).
- **FR-015**: Les tests d'intégration MUST rester à une seule famille de cartes (FR-014 spec 004) et le serveur MCP utilisé en test MUST être un serveur réel ou un mock déterministe.
- **FR-016**: La mémoire partagée MUST être écrite dans l'atelier (`atelier.json`, champ `projectFacts`) à la fin de chaque tour, pas seulement en fin de partie (révision 2026-11-11, R-062). Lors d'un héritage (`inheritFactsFrom`), les faits copiés MUST être chargés dans la mémoire partagée vivante des agents, pas seulement dans le JSON de l'atelier.

### Key Entities *(include if feature involves data)*

- **McpIdeConfig** : configuration du serveur MCP IDE ; attributs : URL du serveur, liste des noms d'outils activés, timeout. Configurable par propriété (ex. `cornucopai.mcp.ide.url`). Ne comporte PAS de chemin de projet : le serveur MCP sert le projet ouvert dans l'IDE de l'utilisateur.
- **ProjectFact (fait projet)** : une découverte enregistrée dans la mémoire partagée ; attributs : texte du fait, nom de l'agent auteur, numéro du tour, horodatage. Persisté avec l'atelier.
- **SharedProjectMemory (mémoire projet partagée)** : la mémoire partagée d'un atelier ; contient la liste ordonnée des `ProjectFact` ; persistée dans le JSON de l'atelier ; isolée par atelier sauf héritage explicite (dropdown UI à la création d'un nouvel atelier depuis un atelier source) ; alimentée et lue via des outils dédiés.
- **VoteDecision (décision de vote)** : le résultat structuré du vote d'un agent ; attributs : `vote` (enum `VoteChoice` : YES/NO), `explanationCheck` (enum `ExplanationCheck` : OK/INSUFFICIENT), `applicabilityCheck` (enum `ApplicabilityCheck` : OK/NOT_APPLICABLE), `justification` (texte court). Persisté avec le leg.
- **Atelier** (existant, étendu) : persiste désormais aussi la `SharedProjectMemory` et les `VoteDecision` enrichies.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100 % des agents d'une partie de test ont dans leur toolbox les 7 outils MCP configurés + `get_card_info` + `get_tour_info` + les 2 outils de mémoire partagée (vérifié par test unitaire d'inspection de la toolbox).
- **SC-002**: Lors d'un test de partie, au moins un agent appelle effectivement un outil MCP (`list_directory_tree` ou `read_file`) et obtient un résultat non vide du projet (vérifié dans les logs).
- **SC-003**: Lorsqu'un agent enregistre un fait dans la mémoire partagée, ce fait est lisible par un autre agent via l'outil de lecture, dans la même partie (test d'intégration à 2+ agents).
- **SC-004**: Sur un scénario délibérément non applicable au projet (test contrôlé), au moins 2 agents sur 3 votent NO avec une justification mentionnant l'inapplicabilité (branche b de l'arbre de décision).
- **SC-005**: 100 % des votes persistés comportent une justification structurée (branches a et b évaluées), vérifiable sur les fichiers JSON des legs.
- **SC-006**: La mémoire partagée de deux ateliers distincts est strictement isolée (test : atelier 1 a des faits, atelier 2 démarre vide).
- **SC-007**: Une partie avec le serveur MCP injoignable se déroule quand même de bout en bout (aucun blocage), les appels MCP échouant gracieusement (test de résilience).
- **SC-008**: Le prompt de vote contient les deux questions de l'arbre de décision et reste capé en tokens (hérité de spec 005 FR-006), vérifié par test unitaire.

## Assumptions

- Le serveur MCP IDE est un serveur MCP standard conforme au protocole MCP over HTTP (streamable), exposé sans authentification pour les tests. L'URL `http://127.0.0.1:64462/stream` est une valeur par défaut de test, configurable. Le serveur MCP sert le projet actuellement ouvert dans l'IDE de l'utilisateur (clarification 2026-11-11) : CornucopAI ne configure aucun chemin de projet, il se contente de se connecter à l'URL.
- La liste des 7 outils MCP (`get_project_dependencies`, `get_project_modules`, `list_directory_tree`, `read_file`, `search_file`, `search_regex`, `search_text`) est représentative ; d'autres outils pourront être ajoutés par configuration.
- La mémoire partagée est un journal ordonné de faits, pas une base de connaissances cohérente : les contradictions entre faits sont possibles et laissées à l'appréciation des agents.
- L'arbre de décision du vote guide le raisonnement de l'agent mais ne remplace pas son jugement : un agent peut voter YES même si une branche est limite, selon sa personnalité d'archetype (ex. HAPPY votera YES plus facilement).
- Le mode « débat » (US5) est désactivé par défaut (révision 2026-11-11, R-061) ; il pourra s'appuyer sur les mécanismes d'orchestration multi-agents disponibles dans l'écosystème LangChain4j (workflows, débat) sans que cela change le contrat de sortie (vote YES/NO + justification). Une propriété de configuration permet de l'activer (`cornucopai.debate.enabled=true`).
- Le fenêtrage de la mémoire partagée dans les prompts suit la même approche que la mémoire de conversation (MessageWindowChatMemory) : les 20 faits les plus récents sont injectés dans le prompt, tous sont accessibles via l'outil (clarification 2026-11-11).
- Les outils MCP sont des outils en lecture seule sur le projet (pas d'écriture dans le code du projet analysé).
- Choix de sécurité (clarification 2026-11-11) : le serveur MCP est un serveur local de confiance (`127.0.0.1`) ; ses résultats ne sont PAS traités comme du contenu brut « DONNÉES » anti-injection. Si le MCP devait un jour être exposé sur un réseau non fiable, ce choix devrait être revu (authentification + encapsulation des résultats).
- La persistance des faits projet et des décisions de vote suit le même mécanisme que les legs (fichiers JSON par atelier), avec repli de lecture pour les ateliers legacy (sans ces champs).
- Héritage optionnel de la mémoire partagée (clarification 2026-11-11) : à la création d'un nouvel atelier depuis un atelier existant, un dropdown UI permet de copier les faits projet de l'atelier source dans le nouvel atelier ; par défaut (choix « non »), la mémoire démarre vide.
