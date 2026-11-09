# Feature Specification: Sélection des familles de cartes et distribution des vraies cartes Cornucopia

**Feature Branch**: `003-card-set-selection`

**Created**: 2026-11-08

**Status**: Draft

**Input**: User description: "Passer à l'itération suivante, plus simple : choisir les sets de cartes. Trois temps : lister les familles disponibles, permettre de sélectionner celles qu'on veut au démarrage du jeu, puis distribuer les vraies cartes aux participants. L'application reçoit en variable d'environnement le chemin vers la racine du dépôt Cornucopia (ex. /Users/francois/cornucopia) ; les fichiers YAML du dossier `source/` servent à tout mettre en place. Une fois le jeu initialisé, le moteur utilise les vraies cartes."

## Clarifications

### Session 2026-11-08

- Q: Quelles versions des sets de cartes faut-il exposer quand plusieurs versions d'une même édition existent (ex. webapp 2.2 et 3.0, mobileapp 1.0/1.1/2.0) ? → A: Une seule version par édition : la plus récente (version sémantique la plus haute). Les anciennes versions ne sont ni listées ni sélectionnables. Au moment de la rédaction, 7 sets sont exposés : companion 1.0, cumulus 1.1, dbd 1.0, eop 5.0, mlsec 1.0, mobileapp 2.0, webapp 3.0.
- Q: Quelle est l'unité sélectionnable dans le créateur de partie — le set entier ou ses familles internes ? → A: Ce sont les **familles** (suits) à l'intérieur de chaque set qui sont sélectionnables (ex. dans le set `webapp` : `VE` « Data Validation & Encoding », `AT` « Authentication », etc.). Le sélecteur est **hiérarchisé par set** (set → familles) : cocher un set sélectionne d'un coup toutes ses familles, et chaque famille reste cochable individuellement. Une famille est identifiée par la paire `setId:familleId` (ex. `webapp:VE`).
- Q: Comment gérer la langue des cartes, sachant que tous les sets n'ont pas toutes les langues ? → A: La langue est choisie dans le créateur de partie via un sélecteur, **par défaut français (`fr`)**. L'API expose, pour chaque set (version la plus récente), la liste de ses langues disponibles. Lors de la création, chaque set est lu dans la langue choisie si elle existe, sinon on rebascule sur l'anglais (`en`, présent dans tous les sets) pour ce set, sans jamais revenir à une ancienne version, et la langue réellement utilisée est signalée par set (badge) dans l'interface. Au moment de la rédaction, `fr` n'existe que dans le set `webapp` 3.0 ; les 6 autres sets passent donc en `en` par défaut. Le code langue est normalisé en minuscules (`FR` → `fr`) ; `fa` désigne le persan, pas le français.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Lister les sets et leurs familles disponibles (Priority: P1)

En tant qu'utilisateur humain, je veux que l'application me présente, dès l'ouverture du formulaire de configuration, les sets de cartes disponibles (une seule version par édition, la plus récente) organisés hiérarchiquement avec leurs familles, afin de savoir quelles familles de menaces je peux mobiliser pour ma partie.

**Why this priority**: Sans cette visibilité, l'utilisateur ne peut pas choisir ; c'est le prérequis de toute la fonctionnalité.

**Independent Test**: Ouvrir le formulaire « Configurer une partie » et vérifier que les sets s'affichent avec leurs familles, chaque set indiquant son édition, sa version, sa langue, et chaque famille son identifiant, son nom et son nombre de cartes.

**Acceptance Scenarios**:

1. **Given** l'application est démarrée avec la variable d'environnement pointant vers la racine du dépôt Cornucopia, **When** j'ouvre le formulaire de configuration, **Then** les sets disponibles s'affichent sous forme hiérarchique (set → familles), une seule version par édition (la plus récente).
2. **Given** la liste est affichée, **When** je la consulte, **Then** chaque set indique au minimum son édition (ex. `eop`, `webapp`), sa version (ex. `5.0`, `3.0`) et la liste de ses langues disponibles, et chaque famille indique son identifiant (ex. `VE`), son nom (ex. « Data Validation & Encoding ») et son nombre de cartes.
3. **Given** plusieurs versions d'une même édition existent dans le dépôt (ex. `webapp` 2.2 et 3.0), **When** la liste est calculée, **Then** seule la version la plus récente (3.0) est exposée ; l'ancienne version n'apparaît nulle part.
4. **Given** la variable d'environnement est absente ou invalide, **When** j'ouvre le formulaire, **Then** la liste est vide et un message clair indique que le dépôt Cornucopia n'est pas configuré.
5. **Given** le formulaire est ouvert, **When** je regarde le sélecteur de langue, **Then** il propose les langues disponibles dans le catalogue et est positionné par défaut sur « Français (`fr`) » ; les sets qui n'ont pas le français sont marqués comme disponibles dans une autre langue (ex. badge « EN »).

---

### User Story 2 - Sélectionner des familles au démarrage du jeu (Priority: P1)

En tant qu'utilisateur humain, je veux cocher, dans un sélecteur hiérarchisé par set, les familles de cartes que je veux inclure dans la partie au moment de la configurer, afin de ne jouer qu'avec les familles de menaces adaptées au contexte de mon atelier.

**Why this priority**: C'est le cœur de la feature : le choix des familles détermine quelles cartes seront distribuées.

**Independent Test**: Cocher exactement deux familles (appartenant éventuellement à des sets différents) dans le formulaire, lancer la partie, puis vérifier que seules les cartes de ces deux familles ont été distribuées et jouées.

**Acceptance Scenarios**:

1. **Given** le formulaire affiche le sélecteur hiérarchisé set → familles, **When** je coche certaines familles et clique sur « Lancer », **Then** seules les familles cochées (identifiées par `setId:familleId`) sont transmises au démarrage de la partie.
2. **Given** je coche un set entier dans le sélecteur hiérarchisé, **When** je clique sur « Lancer », **Then** toutes les familles de ce set sont sélectionnées d'un coup.
3. **Given** aucune famille n'est cochée, **When** je clique sur « Lancer », **Then** la partie ne démarre pas et un message indique qu'au moins une famille doit être sélectionnée.
4. **Given** je coche plusieurs familles réparties sur plusieurs sets, **When** la partie démarre, **Then** le jeu de cartes est constitué de l'union des cartes de toutes les familles cochées, quel que soit leur set d'origine.
5. **Given** je choisis une langue (ex. français) dans le sélecteur, **When** je clique sur « Lancer », **Then** la langue choisie est transmise avec la requête de création de partie (par défaut `fr`).

---

### User Story 3 - Distribuer les vraies cartes aux participants (Priority: P1)

En tant que participant (agent IA), je veux recevoir au démarrage de la partie de vraies cartes Cornucopia (identifiant + description) issues des familles sélectionnées, afin de jouer des tours réalistes ; le moteur de jeu utilise ensuite ces vraies cartes.

**Why this priority**: C'est la finalité : remplacer les cartes placeholders par les vraies cartes dans le moteur existant.

**Independent Test**: Lancer une partie en sélectionnant une famille, dérouler au moins un tour, et vérifier que la carte jouée (persistée dans le fichier JSON du leg) est une vraie carte Cornucopia (identifiant et description réels) appartenant à la famille choisie, et non une carte placeholder.

**Acceptance Scenarios**:

1. **Given** des familles sont sélectionnées et la partie est lancée, **When** le jeu est initialisé, **Then** un jeu de cartes réel est construit à partir de toutes les cartes des familles sélectionnées, mélangé, puis distribué aux participants (distribution round-robin équivalente à l'existante).
2. **Given** le jeu est distribué, **When** un joueur joue une carte, **Then** le leg persisté contient l'identifiant réel de la carte (ex. `VE2`) et sa description textuelle, et non plus un nom de carte placeholder.
3. **Given** la partie est en cours, **When** les agents jouent, **Then** ils disposent de la description réelle de leur carte et de leur famille pour construire scénario et explication.
4. **Given** une partie est lancée avec des familles sélectionnées, **When** la partie se termine, **Then** 100 % des cartes jouées proviennent des familles sélectionnées et aucune carte placeholder n'a été utilisée.
5. **Given** la langue choisie est le français et qu'un set sélectionné n'existe pas en français, **When** le jeu est initialisé, **Then** ce set est lu en anglais (repli `en`) et sa langue réelle (`en`) est consignée et signalée ; la partie démarre quand même, sans erreur.
6. **Given** la langue choisie est le français, **When** le jeu est initialisé, **Then** chaque carte distribuée provient de la version française de son set si elle existe, sinon de la version anglaise, et chaque set de l'atelier indique la langue réellement utilisée.

---

### User Story 4 - Gérer les cas d'erreur de configuration des cartes (Priority: P2)

En tant qu'utilisateur, je veux obtenir un message clair quand le dépôt Cornucopia est mal configuré ou qu'une famille demandée est inconnue, afin de corriger rapidement sans que la partie ne démarre dans un état incohérent.

**Why this priority**: Garantit la robustesse de l'initialisation sans bloquer le flux nominal.

**Independent Test**: Démarrer l'application sans la variable d'environnement, puis tenter de lancer une partie ; vérifier le message d'erreur. Envoyer une requête avec une paire `setId:familleId` inconnue et vérifier le rejet.

**Acceptance Scenarios**:

1. **Given** la variable d'environnement vers le dépôt Cornucopia est absente, **When** l'application démarre, **Then** elle signale clairement que le dépôt n'est pas configuré et la liste des sets/familles est vide.
2. **Given** une requête de création de partie contient une paire `setId:familleId` inconnue, **When** le serveur la reçoit, **Then** il répond une erreur 400 indiquant la famille inconnue et ne crée aucun atelier.
3. **Given** aucune famille n'est sélectionnée dans la requête, **When** le serveur la reçoit, **Then** il répond une erreur 400 demandant au moins une famille.

---

### Edge Cases

- Que se passe-t-il si le dossier `source/` ne contient aucun fichier de cartes anglais (`*-cards-*-en.yaml`) ? → La liste des sets est vide ; le lancement d'une partie est refusé avec un message explicite.
- Que se passe-t-il si plusieurs versions d'une même édition existent (ex. `webapp` 2.2 et 3.0, `mobileapp` 1.0/1.1/2.0) ? → Seule la version sémantique la plus haute est retenue et exposée ; les autres sont ignorées (jamais listées, jamais sélectionnables).
- Que se passe-t-il si deux sets différents contiennent une famille de même nom (ex. `WC` « Wild Card » dans plusieurs éditions) ? → Les familles sont identifiées par la paire `setId:familleId` ; il n'y a donc pas de collision, et sélectionner `webapp:WC` n'entraîne pas `eop:WC`.
- Que se passe-t-il si un set sélectionné contient un fichier YAML illisible ou invalide ? → Le set est ignoré et journalisé, ou la requête est rejetée si aucune famille valide ne reste ; aucun atelier n'est créé dans un état partiel.
- Que se passe-t-il si le nombre de cartes distribuées est inférieur au nombre de joueurs ? → La distribution round-robin attribue au moins une carte au premier joueur ; la partie se déroule tant qu'au moins un joueur a des cartes (comportement existant du moteur).
- Que se passe-t-il si l'utilisateur resélectionne une famille déjà cochée ? → Aucun doublon : chaque paire `setId:familleId` n'apparaît qu'une seule fois dans le jeu.
- Que se passe-t-il si la langue choisie (ex. `fr`) n'existe pas pour un set sélectionné (dans sa version la plus récente) ? → Le set est lu en anglais (`en`, toujours présent), la langue réelle `en` est consignée pour ce set et signalée dans l'interface ; on ne revient jamais à une ancienne version du set pour obtenir la langue.
- Que se passe-t-il si la langue choisie n'existe pour aucun set (ex. une langue disponible nulle part) ? → Tous les sets passent en anglais (`en`) ; la partie démarre quand même et le repli est signalé.
- Que se passe-t-il si le code langue est saisi en majuscules (ex. `FR`) ? → Il est normalisé en minuscules (`fr`) avant comparaison ; `fa` (persan) n'est jamais confondu avec `fr` (français).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Le système MUST lire le chemin vers la racine du dépôt Cornucopia dans une variable d'environnement (par défaut `CORNUCOPIA_HOME`).
- **FR-002**: Le système MUST découvrir les sets de cartes disponibles en listant les fichiers de cartes anglais (`*-cards-*-en.yaml`) du dossier `<racine>/source/`, puis ne retenir, pour chaque édition, que la version la plus récente (comparaison sémantique) ; les anciennes versions MUST être ignorées.
- **FR-003**: Le système MUST exposer, via une API consultable par le frontend, la liste des sets retenus, chacun avec son édition, sa version, sa langue et la liste de ses familles (identifiant, nom, nombre de cartes), ainsi que le nombre total de cartes du set.
- **FR-004**: Le formulaire « Configurer une partie » MUST afficher un sélecteur hiérarchisé set → familles : une case par famille, et une case par set qui sélectionne/désélectionne d'un coup toutes les familles de ce set.
- **FR-005**: La requête de création de partie MUST véhiculer la liste des familles sélectionnées, identifiées chacune par la paire `setId:familleId` (ex. `webapp:VE`).
- **FR-006**: Au moins une famille MUST être sélectionnée pour qu'une partie puisse être lancée ; sinon la requête est rejetée (400).
- **FR-007**: Le système MUST charger et parser le fichier YAML du set de chaque famille sélectionnée, et n'extraire que les cartes appartenant aux familles sélectionnées (identifiant, valeur, famille d'appartenance, description).
- **FR-008**: Le système MUST constituer un jeu de cartes combiné à partir de toutes les cartes des familles sélectionnées (quels que soient leurs sets), le mélanger, et le distribuer aux participants (round-robin), en remplacement du jeu placeholder.
- **FR-009**: Chaque carte jouée dans un leg MUST être persistée avec son identifiant réel et sa description textuelle, afin que le moteur et les agents disposent des vraies cartes.
- **FR-010**: Le jeu de cartes distribué MUST être persisté dans l'atelier (champ `deck`) sous forme de vraies cartes.
- **FR-011**: Si la variable d'environnement est absente ou invalide, le système MUST signaler clairement l'absence du dépôt et refuser le lancement d'une partie (liste vide + erreur explicite).
- **FR-012**: Si une paire `setId:familleId` demandée est inconnue, le système MUST rejeter la requête (400) en nommant la famille inconnue, sans créer d'atelier.
- **FR-013**: Une famille sélectionnée ne MUST apparaître qu'une seule fois dans le jeu combiné (pas de doublons de cartes).
- **FR-014**: L'API de liste des sets MUST indiquer, pour chaque set, la liste de ses langues disponibles (codes normalisés en minuscules).
- **FR-015**: Le formulaire « Configurer une partie » MUST proposer un sélecteur de langue positionné par défaut sur le français (`fr`), et la requête de création MUST véhiculer la langue choisie.
- **FR-016**: Lors de la création, chaque set MUST être lu dans la langue choisie si un fichier existe pour cette langue dans la version la plus récente du set ; sinon il MUST rebasculer sur l'anglais (`en`) pour ce set, sans jamais utiliser une ancienne version du set.
- **FR-017**: La langue réellement utilisée pour chaque set MUST être consignée dans l'atelier et exposée (badge) dans l'interface.
- **FR-018**: Les codes langue MUST être normalisés en minuscules avant comparaison (`FR` → `fr`) ; `fa` (persan) ne MUST pas être confondu avec `fr` (français).

### Key Entities *(include if feature involves data)*

- **Set (édition / jeu de cartes)** : représente un set de cartes Cornucopia découvert dans le dépôt, en version la plus récente uniquement ; attributs : édition (ex. `webapp`), version (ex. `3.0`), langues disponibles (ex. `[en, fr, es]`), langue choisie pour la partie, langue réellement utilisée (après repli éventuel), fichier source, liste des familles, nombre total de cartes. Identifiant = édition (une seule version par édition est exposée).
- **Famille (suit)** : représente une famille de menaces à l'intérieur d'un set ; c'est l'unité sélectionnable dans le créateur de partie ; attributs : identifiant (ex. `VE`), nom (ex. « Data Validation & Encoding »), set d'appartenance, langue, liste des cartes, nombre de cartes. Identifiant unique = paire `setId:familleId` (ex. `webapp:VE`).
- **Card (carte réelle)** : représente une vraie carte Cornucopia ; attributs : identifiant (ex. `VE2`), valeur (ex. `2`), famille d'appartenance, langue, description textuelle (`desc`), et éventuellement une URL ou un champ divers (`misc`). C'est l'unité distribuée aux participants et jouée dans les legs.
- **Atelier (partie)** : étendu par rapport à l'itération précédente — son champ `deck` contient désormais les vraies cartes (identifiants) issues des familles sélectionnées, et chaque leg référence une vraie carte.
- **Leg (tour de jeu)** : le champ « nom de la carte » contient désormais l'identifiant réel de la carte jouée, et la description de la carte est disponible pour le scénario et l'explication.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: L'API de liste retourne exactement les 7 sets en version la plus récente présents dans `source/` (companion 1.0, cumulus 1.1, dbd 1.0, eop 5.0, mlsec 1.0, mobileapp 2.0, webapp 3.0), chacun avec édition, version, langue et la liste de ses familles (identifiant, nom, nombre de cartes) ; aucune ancienne version n'est retournée.
- **SC-002**: Une partie lancée avec un sous-ensemble de familles ne contient que des cartes appartenant aux familles sélectionnées (100 % des cartes distribuées et jouées proviennent des paires `setId:familleId` cochées).
- **SC-003**: 100 % des legs persistés référencent une vraie carte (identifiant + description) des familles sélectionnées ; aucune carte placeholder n'est utilisée dès qu'au moins une famille est sélectionnée.
- **SC-004**: La distribution round-robin attribue les vraies cartes aux participants de manière déterministe et reproductible (graine fixée en test).
- **SC-005**: Une requête sans aucune famille sélectionnée, ou avec une paire `setId:familleId` inconnue, est rejetée par une erreur 400 explicite ; aucun dossier d'atelier n'est créé dans ces cas.
- **SC-006**: Le formulaire de configuration affiche le sélecteur hiérarchisé set → familles et bloque le bouton « Lancer » tant qu'aucune famille n'est cochée (en plus des règles existantes sur le nombre de joueurs et la feature).
- **SC-007**: L'API de liste indique pour chaque set ses langues disponibles ; avec la langue par défaut `fr`, seul le set `webapp` 3.0 est en français, les 6 autres sont signalés en `en` (repli), sans erreur.
- **SC-008**: 100 % des cartes d'une partie en français distribuées depuis un set disponible en `fr` proviennent du fichier `fr` ; 100 % des cartes d'un set absent en `fr` proviennent du fichier `en` (repli), et chaque set de l'atelier indique sa langue réellement utilisée.

## Assumptions

- Un **set** correspond à un fichier deck du dépôt Cornucopia de la forme `*-cards-*-en.yaml` dans `source/` ; quand plusieurs versions d'une même édition existent, seule la version sémantique la plus haute est retenue et exposée (les autres sont ignorées).
- Une **famille** est un suit à l'intérieur d'un set (ex. `VE`, `AT`, `SP`) ; c'est l'unité sélectionnable dans le créateur de partie, identifiée par la paire `setId:familleId`. Le sélecteur est hiérarchisé par set.
- Le nom de la variable d'environnement est `CORNUCOPIA_HOME` et sa valeur est la racine du dépôt Cornucopia (ex. `/Users/francois/cornucopia`) ; le dossier des cartes est `<CORNUCOPIA_HOME>/source/`.
- La langue des cartes est choisie dans le créateur de partie, **par défaut le français (`fr`)** ; si un set (dans sa version la plus récente) n'existe pas dans la langue choisie, il rebascule sur l'anglais (`en`, présent dans tous les sets) sans revenir à une ancienne version. Au moment de la rédaction, `fr` n'est disponible que pour le set `webapp` 3.0. Les codes langue sont normalisés en minuscules (`FR` → `fr`) ; `fa` = persan, distinct de `fr`.
- Les vraies cartes remplacent entièrement les cartes placeholders dès cette itération : le jeu placeholder de l'itération 002 n'est plus utilisé lorsqu'au moins une famille est disponible et sélectionnée.
- Le backend (Quarkus / Java 25) lit les fichiers YAML au démarrage et à la création de partie ; le frontend (Vue 3 / Vuetify) affiche le sélecteur hiérarchisé dans le formulaire existant, conformément à la constitution.
- La distribution des cartes reste round-robin et le moteur de jeu (ordre des tours, legs, votes) reste inchangé : seules les cartes distribuées deviennent réelles.
