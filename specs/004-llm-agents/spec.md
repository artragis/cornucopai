# Feature Specification: Agents joueurs pilotés par LLM, configurables par agent, avec toolbox carte

**Feature Branch**: `004-llm-agents`

**Created**: 2026-11-08

**Status**: Draft

**Input**: User description: "Passer aux agents qui répondent avec de l'IA. Dans la vision finale, chaque agent pourra utiliser soit un agent ACP (type GitHub Copilot ou Mistral Vibe), soit une URL compatible OpenAI — configurable au niveau de chaque agent. Dans cet incrément : retirer les agents déterministes des parties (sauf tests unitaires) et n'avoir qu'une seule implémentation d'agent basée sur l'IA. Par défaut, pour limiter les coûts, l'URL OpenAI utilisée est le serveur bionic local (http://localhost:1235/v1, sans authentification). Mettre en place un premier outil dans la toolbox LangChain4j : obtenir toutes les infos intéressantes d'une carte à partir de son identifiant (YAML source, mappings, et surtout le dossier cornucopia.owasp.org/data/cards). Pour les tests, ne choisir à chaque fois qu'une seule famille de cartes pour limiter le nombre de tours."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Configurer le fournisseur d'IA de chaque agent (Priority: P1)

En tant qu'utilisateur humain, je veux pouvoir choisir, pour chaque joueur de la partie, le fournisseur d'intelligence artificielle qu'il utilisera : soit un agent ACP nommé (par exemple `github-copilot` ou `mistral-vibe`), soit une URL de base compatible OpenAI accompagnée d'un nom de modèle et d'une clé d'API facultative. Par défaut, sans configuration manuelle, les agents utilisent le serveur local `http://localhost:1235/v1` sans authentification, afin de limiter les coûts.

**Why this priority**: Sans fournisseur d'IA configurable, aucun agent ne peut répondre ; c'est le prérequis de toute la feature. Le défaut local garantit que la partie démarre sans configuration et sans coût externe.

**Independent Test**: Lancer une partie sans toucher à la configuration et vérifier que les agents contactent bien l'URL locale par défaut ; puis configurer un agent avec une URL externe fictive et vérifier que la configuration est prise en compte et persistée.

**Acceptance Scenarios**:

1. **Given** aucune configuration n'est fournie, **When** une partie est lancée, **Then** chaque agent utilise par défaut l'URL de base `http://localhost:1235/v1` sans clé d'authentification.
2. **Given** le formulaire ou la requête de création de partie, **When** je définis pour un joueur une URL compatible OpenAI (base + modèle + clé optionnelle), **Then** ce joueur utilise cette configuration pour toute la partie.
3. **Given** le formulaire ou la requête de création de partie, **When** je définis pour un joueur un agent ACP nommé (ex. `github-copilot`, `mistral-vibe`), **Then** ce joueur est configuré avec ce fournisseur ACP.
4. **Given** deux joueurs d'une même partie, **When** je leur attribue des fournisseurs différents, **Then** chaque agent utilise bien son propre fournisseur, indépendamment des autres.
5. **Given** la configuration des agents, **When** la partie est lancée, **Then** la configuration de chaque agent est persistée avec l'atelier et reste consultable après la partie.
6. **Given** une configuration invalide (fournisseur inconnu, URL vide), **When** je tente de lancer la partie, **Then** la partie ne démarre pas et un message indique la configuration à corriger.

---

### User Story 2 - Jouer une partie complète avec des agents IA (Priority: P1)

En tant qu'utilisateur humain, je veux que tous les joueurs d'une partie soient des agents pilotés par un modèle de langage, afin d'obtenir des scénarios, précisions, questions, réponses et votes réalistes et non prédictibles. Les anciens agents déterministes (Grumpy, Happy, Greedy, Prof) ne servent plus dans les parties : ils restent disponibles uniquement pour les tests unitaires.

**Why this priority**: C'est le cœur de l'incrément : remplacer les comportements codés en dur par une seule implémentation d'agent basée sur l'IA.

**Independent Test**: Lancer une partie à plusieurs joueurs et vérifier, sur les fichiers JSON des legs et les logs, que 100 % des décisions (carte jouée, réactions, réponses, votes) proviennent de l'implémentation IA unique, et qu'aucun agent déterministe n'a participé au déroulement.

**Acceptance Scenarios**:

1. **Given** une partie est lancée, **When** le maître du jeu demande à un joueur de jouer une carte, **Then** le scénario et l'explication produits par l'agent IA sont persistés dans le leg.
2. **Given** un leg est posé par un joueur, **When** les autres joueurs réagissent, **Then** leurs précisions, questions ou pass proviennent de l'agent IA.
3. **Given** un joueur IA pose une question, **When** le joueur de la carte répond, **Then** la réponse est « Je sais » + une réponse ou « Je sais pas », produite par l'agent IA.
4. **Given** la phase de vote, **When** chaque joueur vote, **Then** le vote provient de l'agent IA.
5. **Given** la partie est terminée, **When** on consulte les legs, **Then** aucun comportement déterministe codé en dur n'est identifiable dans le déroulement ; les tests unitaires des anciens agents déterministes continuent seuls de les couvrir.

---

### User Story 3 - Disposer d'un premier outil « infos carte » dans la toolbox de l'agent (Priority: P1)

En tant qu'agent IA jouant une carte, je veux pouvoir appeler un outil qui me retourne toutes les informations intéressantes d'une carte à partir de son seul identifiant (ex. `VE2`) : sa description, sa famille, son URL, ses mappings de sécurité (STRIDE, CAPEC, ASVS, guide développeur), et son contenu riche (scénario d'exemple, note technique) issu du dossier `cornucopia.owasp.org/data/cards`. Ainsi, même sans connaître la carte à l'avance, je peux construire un scénario et une explication crédibles.

**Why this priority**: Sans cet outil, l'agent LLM ne dispose que de la description courte de la carte ; avec lui, il peut raisonner sur la menace en profondeur. C'est le premier outil de la toolbox LangChain4j.

**Independent Test**: Pour un échantillon d'identifiants couvrant chaque set exposé, appeler l'outil avec le seul identifiant et vérifier que la réponse contient la description, la famille, les mappings et le contenu riche de la carte.

**Acceptance Scenarios**:

1. **Given** un identifiant de carte valide (ex. `VE2`), **When** l'agent appelle l'outil avec cet identifiant, **Then** la réponse contient l'identifiant, la valeur, la description, l'URL, la famille (identifiant + nom) et le set d'appartenance.
2. **Given** un identifiant de carte valide, **When** l'outil est appelé, **Then** la réponse contient les mappings de la carte issus des fichiers de mappings (STRIDE, CAPEC, ASVS, OWASP Dev Guide, SAFECode quand présents).
3. **Given** un identifiant de carte valide dont le contenu riche existe, **When** l'outil est appelé, **Then** la réponse contient le contenu du dossier `data/cards/<set>/<famille>/<id>/` (fichiers `explanation.md` et `technical-note.md`).
4. **Given** un identifiant présent dans plusieurs sets, **When** l'outil est appelé, **Then** la réponse signale les différentes localisations et préfère celle de l'atelier en cours si elle existe.
5. **Given** un identifiant inconnu, **When** l'outil est appelé, **Then** la réponse indique explicitement que la carte est inconnue, sans provoquer d'erreur bloquante.
6. **Given** une carte sans contenu riche dans `data/cards`, **When** l'outil est appelé, **Then** la réponse contient néanmoins les données du YAML et les mappings, et signale l'absence de contenu riche.

---

### User Story 4 - Donner à chaque agent une mémoire et un prompt système (Priority: P2)

En tant qu'agent IA, je veux disposer d'une mémoire dédiée à la partie en cours et d'un prompt système définissant ma personnalité (héritée de mon archétype : Grumpy, Happy, Greedy, Prof) ainsi que les règles du jeu Cornucopia, afin que mes réponses soient cohérentes tout au long de la partie et conformes à mon personnage.

**Why this priority**: La mémoire par agent rend les échanges crédibles (un agent se souvient des legs précédents) ; le prompt système conserve l'esprit des archétypes sans code déterministe.

**Independent Test**: Lancer une partie à 4 joueurs et vérifier qu'aucun agent ne partage sa mémoire avec un autre, que chaque agent applique la personnalité de son archétype, et qu'aucune mémoire ne persiste d'un atelier à l'autre.

**Acceptance Scenarios**:

1. **Given** une partie en cours, **When** un agent joue plusieurs tours, **Then** ses décisions ultérieures tiennent compte des legs précédents de la même partie (mémoire de la partie).
2. **Given** deux ateliers distincts, **When** le même nom de joueur participe aux deux, **Then** aucune information ne fuite d'un atelier à l'autre (mémoires isolées par atelier).
3. **Given** un agent avec l'archétype Grumpy, **When** il joue, **Then** son comportement reflète la personnalité grincheuse définie par son prompt système, sans code déterministe.
4. **Given** le démarrage d'une partie, **When** chaque agent est créé, **Then** chacun reçoit le prompt système complet (règles du jeu + personnalité + format des réponses attendues).

---

### User Story 5 - Lancer des tests avec une seule famille de cartes (Priority: P2)

En tant que développeur, je veux que chaque test d'intégration ne sélectionne qu'une seule famille de cartes, afin que le nombre de tours joués (et donc d'appels au LLM, coûteux en temps) reste borné et prévisible.

**Why this priority**: Les appels LLM sont longs ; limiter chaque test à une famille rend la suite de tests rapide et reproductible.

**Independent Test**: Inspecter les tests d'intégration et vérifier qu chacun ne déclare qu'une seule paire `setId:familleId`, et que le nombre de legs joués dans chaque test est inférieur ou égal au nombre de cartes de cette famille.

**Acceptance Scenarios**:

1. **Given** un test d'intégration d'une partie, **When** il est écrit, **Then** il ne sélectionne exactement qu'une seule famille de cartes.
2. **Given** un test sélectionnant une famille de N cartes, **When** la partie de test se déroule, **Then** au plus N legs sont jouées.

---

### Edge Cases

- Que se passe-t-il si le LLM est indisponible (endpoint éteint, timeout, réponse illisible) ? → La partie ne plante pas : l'appel en échec est journalisé et un comportement de repli défini est appliqué (valeur par défaut pour ce tour).
- Que se passe-t-il si l'outil carte reçoit un identifiant inconnu ? → Réponse explicite « carte inconnue », aucun crash, la partie continue.
- Que se passe-t-il si un identifiant de carte existe dans plusieurs sets (ex. une wild card présente dans plusieurs éditions) ? → L'outil signale les localisations multiples et préfère celle de l'atelier en cours.
- Que se passe-t-il si une carte n'a pas de dossier dans `data/cards` (ex. contenu riche absent) ? → L'outil retourne les données YAML + mappings et signale l'absence de contenu riche.
- Que se passe-t-il si le fichier de mappings d'un set est absent ou illisible ? → Les mappings sont vides pour les cartes concernées ; pas d'erreur bloquante.
- Que se passe-t-il si la configuration d'un agent est invalide (URL vide, fournisseur inconnu) ? → La partie est refusée au lancement avec un message explicite ; aucun atelier n'est créé dans un état incohérent.
- Que se passe-t-il si deux ateliers tournent en même temps ? → Les mémoires de chaque agent sont isolées par atelier ; aucune fuite entre parties.
- Que se passe-t-il si un utilisateur configure une URL externe (coûteuse) ? → La configuration est acceptée mais journalisée, le défaut restant le serveur local pour limiter les coûts.
- Que se passe-t-il si le LLM renvoie une réponse qui ne respecte pas le format attendu (ex. un vote illisible) ? → Le système applique la règle de repli prévue pour ce type de réponse et journalise l'incident.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Chaque agent joueur MUST être configurable individuellement avec un fournisseur d'IA choisi parmi : un agent ACP nommé (ex. `github-copilot`, `mistral-vibe`) ou une URL de base compatible OpenAI (avec nom de modèle et clé d'API facultative).
- **FR-002**: La configuration par défaut de tout agent MUST être l'URL de base `http://localhost:1235/v1`, sans authentification, afin de limiter les coûts.
- **FR-003**: Il MUST exister une seule implémentation d'agent joueur basée sur l'IA ; les agents déterministes (Grumpy, Happy, Greedy, Prof) MUST être exclus du déroulement des parties et conservés uniquement pour les tests unitaires.
- **FR-004**: L'agent IA MUST implémenter le contrat complet du joueur : jouer une carte (scénario + explication), réagir à un leg (précision, question ou pass), répondre aux questions (« Je sais » + réponse ou « Je sais pas »), et voter (oui/non).
- **FR-005**: L'agent IA MUST disposer d'une toolbox d'outils ; le premier outil MUST retourner toutes les informations intéressantes d'une carte à partir de son seul identifiant.
- **FR-006**: L'outil « infos carte » MUST agréger au minimum : les données du fichier YAML `source/` du set (identifiant, valeur, description, URL, champ divers, famille d'appartenance), les mappings du fichier `source/*-mappings-*.yaml` (STRIDE, CAPEC, ASVS, OWASP Dev Guide, SAFECode), et le contenu riche du dossier `cornucopia.owasp.org/data/cards/<set>/<famille-slug>/<id>/` (`explanation.md`, `technical-note.md`).
- **FR-007**: L'identifiant de la carte MUST suffire à localiser la carte dans tous les sets exposés (dernière version de chaque édition), sans nécessiter de contexte de partie.
- **FR-008**: Lorsqu'un identifiant existe dans plusieurs sets, l'outil MUST le signaler et préférer la localisation de l'atelier en cours quand elle existe.
- **FR-009**: Pour un identifiant inconnu, l'outil MUST répondre explicitement que la carte est inconnue, sans erreur bloquante.
- **FR-010**: Chaque agent MUST disposer d'une mémoire dédiée, isolée par atelier : un agent se souvient des événements de la partie en cours, et rien ne persiste entre deux ateliers.
- **FR-011**: Chaque agent MUST recevoir un prompt système définissant les règles du jeu, le format des réponses attendues et la personnalité de son archétype (Grumpy, Happy, Greedy, Prof).
- **FR-012**: La configuration de chaque agent (type de fournisseur, URL/agent ACP, modèle, clé, nom, archétype) MUST être persistée avec l'atelier et consultable après la partie.
- **FR-013**: En cas d'indisponibilité ou de réponse invalide du LLM (endpoint éteint, timeout, format illisible), le système MUST appliquer un repli non bloquant (valeur par défaut journalisée pour le tour concerné) et poursuivre la partie.
- **FR-014**: Les tests d'intégration MUST sélectionner une seule famille de cartes (`setId:familleId`) par partie de test, afin de borner le nombre de tours et donc d'appels au LLM.
- **FR-015**: Le changement de fournisseur d'un agent MUST être possible par configuration seule, sans modification du code.

### Key Entities *(include if feature involves data)*

- **ConfigurationAgent** : la configuration d'un agent joueur ; attributs : nom du joueur, archétype (personnalité), type de fournisseur (`acp` ou `openai-compatible`), nom de l'agent ACP ou URL de base + modèle + clé d'API facultative, portée de la mémoire (atelier en cours). Persistée avec l'atelier.
- **AgentJoueurIA** : l'unique implémentation d'agent joueur basée sur l'IA ; possède sa mémoire isolée par atelier, son prompt système et l'accès à la toolbox d'outils. Remplace les agents déterministes dans les parties.
- **InfoCarte** : le résultat de l'outil « infos carte » pour un identifiant donné : identifiant, valeur, description, URL, champ divers, set (édition + version), famille (identifiant + nom), langue, mappings (STRIDE, CAPEC, ASVS, Dev Guide, SAFECode), contenu riche (explication, note technique), et éventuelles localisations multiples.
- **OutilCarte** : le premier outil de la toolbox, appelable par l'agent avec un identifiant de carte et retournant un `InfoCarte` (ou « carte inconnue »).
- **Atelier** (existant, étendu) : persistait déjà les legs et le deck ; persiste désormais aussi la liste des `ConfigurationAgent` de la partie.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Une partie complète (lancement → tours → fin) peut être déroulée de bout en bout avec uniquement des agents IA, sans intervention manuelle et sans agent déterministe dans le déroulement.
- **SC-002**: 100 % des décisions des joueurs (cartes, réactions, réponses, votes) sont produites par l'implémentation IA unique, vérifiable sur l'ensemble des legs d'une partie de test.
- **SC-003**: Pour un échantillon d'identifiants couvrant 100 % des sets exposés, l'outil « infos carte » retourne la description, la famille, les mappings et le contenu riche : 100 % de succès sur l'échantillon.
- **SC-004**: Sans aucune configuration manuelle, une partie démarre en utilisant l'URL locale `http://localhost:1235/v1` sans authentification (vérifié par les logs d'appel).
- **SC-005**: Chaque test d'intégration joue au plus N legs, où N est le nombre de cartes de l'unique famille sélectionnée dans ce test.
- **SC-006**: Les tests unitaires existants des agents déterministes continuent de passer sans modification de leur code.
- **SC-007**: En cas d'indisponibilité forcée du LLM, 100 % des appels en échec appliquent le repli prévu et la partie se termine sans crash.
- **SC-008**: La configuration de chaque agent est retrouvable dans l'atelier persisté pour 100 % des parties jouées.

## Assumptions

- Le serveur local « bionic » expose une API compatible OpenAI à l'URL de base `http://localhost:1235/v1`, sans authentification (style LM Studio) ; c'est le fournisseur par défaut pour limiter les coûts.
- `CORNUCOPIA_HOME` pointe vers la racine du dépôt Cornucopia (ex. `/Users/francois/cornucopia`), qui contient à la fois `source/` (YAML des cartes et des mappings) et `cornucopia.owasp.org/data/cards/` (contenu riche par carte).
- Dans cet incrément, le chemin « URL compatible OpenAI » est celui exercé de bout en bout ; le chemin « agent ACP » (`github-copilot`, `mistral-vibe`) est prévu et sélectionnable dans la configuration de chaque agent, conformément à la vision finale.
- Les quatre archétypes existants (Grumpy, Happy, Greedy, Prof) deviennent des personnalités exprimées par le prompt système de l'agent IA ; ils ne sont plus des comportements codés.
- Granularité de mémoire retenue : une mémoire par agent et par atelier (l'agent se souvient de sa partie en cours uniquement), avec une fenêtre glissante pour borner la taille de l'historique.
- Le contenu riche du dossier `data/cards` n'existe qu'en anglais au moment de la rédaction ; la description de la carte, elle, reste dans la langue choisie via les fichiers `source/`.
- Le backend (Quarkus / Java 25) orchestre les agents avec LangChain4j, conformément à la constitution ; la toolbox des agents est la toolbox d'outils LangChain4j.
- Les tests d'intégration sélectionnent une seule famille de cartes (ex. `webapp:VE`) pour borner le nombre de tours joués, les appels LLM étant longs.
- En cas d'échec du LLM, le repli consiste à journaliser l'incident et à produire une valeur par défaut pour le tour (ex. pass / « Je sais pas » / vote non), afin de ne jamais bloquer la partie.
