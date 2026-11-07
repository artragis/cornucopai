# Feature Specification: Flux complet d'une partie de Cornucopia jouée par des agents IA

**Feature Branch**: `002-cornucopia-game-flow`

**Created**: 2026-11-07

**Status**: Draft

**Input**: User description: "Générer le flux complet d'une partie de Cornucopia où des agents IA jouent, le programme faisant office de maître du jeu : configuration de la partie (nombre de joueurs + description WYSIWYG de la feature analysée), ordre des participants tiré au sort, boucle de tours (carte + scénario + explication, précisions/questions des autres joueurs, vote de validation), persistance JSON par atelier, et quatre archétypes de joueurs simples (Grumpy, Happy, Greedy, Prof)."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Configurer et lancer une partie (Priority: P1)

En tant qu'utilisateur humain, je veux cliquer sur « Configurer une partie », indiquer le nombre de joueurs et décrire dans une zone de texte riche (WYSIWYG) la feature que nous allons analyser en threat modeling, puis appuyer sur « Lancer » pour que la partie démarre immédiatement avec les agents IA.

**Why this priority**: Sans cette étape de configuration, aucune partie ne peut exister. C'est le point d'entrée unique de toute la feature.

**Independent Test**: Depuis l'application, remplir le formulaire de configuration (nombre de joueurs + texte de la feature) et cliquer sur « Lancer » ; la partie démarre et les agents commencent à jouer sans autre action manuelle.

**Acceptance Scenarios**:

1. **Given** l'application est ouverte sur l'écran d'accueil, **When** je clique sur « Configurer une partie », **Then** un formulaire s'affiche avec un champ « nombre de joueurs » et une zone de texte WYSIWYG pour la description de la feature.
2. **Given** le formulaire est rempli (nombre de joueurs valide, description non vide), **When** je clique sur « Lancer », **Then** la partie démarre : un dossier d'atelier est créé et le premier tour commence automatiquement.
3. **Given** le formulaire est vide ou le nombre de joueurs est invalide, **When** je clique sur « Lancer », **Then** la partie ne démarre pas et un message indique les champs à corriger.

---

### User Story 2 - Déroulement orchestré d'un tour par le maître du jeu (Priority: P1)

En tant que participant (agent IA), je veux que le maître du jeu (le programme) me demande, à mon tour, ma carte, mon scénario et mon explication, puis recueille un par un les précisions et questions des autres joueurs, et enfin leur vote de validation, en persistants chaque leg dans un fichier JSON sur le disque.

**Why this priority**: C'est le cœur du jeu Cornucopia ; sans cette boucle de tour, il n'y a pas de partie.

**Independent Test**: Lancer une partie à 2 joueurs minimum et observer le déroulement complet d'au moins un tour : tirage de carte, collecte des avis, vote, et présence du fichier JSON correspondant dans le dossier de l'atelier.

**Acceptance Scenarios**:

1. **Given** une partie est lancée, **When** le premier tour démarre, **Then** le maître du jeu tire au sort l'ordre des participants et le conserve pour toute la partie.
2. **Given** c'est à un joueur de jouer, **When** le maître du jeu lui demande sa carte, **Then** le joueur fournit une carte, un scénario et une explication, et un fichier JSON à 5 champs est écrit sur le disque (nom de la carte, scénario, explication, précisions = liste vide, validation = PENDING).
3. **Given** le leg est enregistrée, **When** le maître du jeu interroge chaque autre joueur un par un, **Then** chaque joueur peut ajouter une précision, poser une question ou passer son tour.
4. **Given** un joueur pose une question, **When** le joueur qui a posé la carte répond, **Then** la réponse est « Je sais » + une réponse (ajoutée aux précisions) ou « Je sais pas » (aucun ajout) ; dans ce dernier cas, le questionneur doit redonner son avis sans pouvoir reposer de question.
5. **Given** tous les autres joueurs ont répondu, **When** le maître du jeu demande à chacun « valides-tu ? », **Then** chaque joueur répond oui ou non ; si strictement plus de la moitié a voté oui, le champ validation passe à VALIDATED, sinon à REJECTED.
6. **Given** un tour est terminé, **When** des cartes restent aux joueurs, **Then** le maître du jeu passe au joueur suivant dans l'ordre tiré au sort.

---

### User Story 3 - Fin de partie quand les cartes sont épuisées (Priority: P2)

En tant qu'utilisateur, je veux que la partie se termine automatiquement lorsque les joueurs n'ont plus aucune carte, afin de consulter le récapitulatif des legs jouées dans le dossier de l'atelier.

**Why this priority**: La clôture propre de la partie rend le résultat exploitable et consultable.

**Independent Test**: Lancer une partie avec un petit nombre de cartes ; vérifier que la partie s'arrête seule à l'épuisement des cartes et que tous les fichiers JSON des legs sont présents.

**Acceptance Scenarios**:

1. **Given** tous les joueurs ont joué toutes leurs cartes, **When** le dernier tour se termine, **Then** la partie se termine et le dossier de l'atelier contient un fichier JSON par leg jouée.

---

### User Story 4 - Archétypes de joueurs simples pour tester le flux (Priority: P2)

En tant qu'utilisateur, je veux disposer de quatre archétypes d'agents « bêtes » (Grumpy, Happy, Greedy, Prof) aux comportements déterministes, afin de tester le flux complet de la partie sans dépendre de la qualité des réponses d'un vrai modèle.

**Why this priority**: Ces archétypes permettent de valider le moteur de jeu de façon reproductible avant d'introduire de vrais agents LLM.

**Independent Test**: Lancer une partie en composant une équipe avec chaque archétype et vérifier que chacun adopte bien le comportement attendu (votes, questions, précisions).

**Acceptance Scenarios**:

1. **Given** un joueur de type Grumpy, **When** il participe à un tour, **Then** il vote toujours non et pose une question avant de passer son tour.
2. **Given** un joueur de type Happy, **When** il participe à un tour, **Then** il vote toujours oui et n'a jamais ni question ni précision.
3. **Given** un joueur de type Greedy, **When** il joue une carte, **Then** il donne une explication trop longue et ajoute des précisions excessivement longues ; il vote oui pour Happy et non pour Grumpy.
4. **Given** un joueur de type Prof, **When** il participe à un tour, **Then** il donne des précisions courtes à Happy, pose une question avant de donner une précision à Grumpy, passe son tour face à Greedy, et vote non à Grumpy mais oui aux autres.

---

### Edge Cases

- Que se passe-t-il quand un joueur tente de poser une question alors qu'il lui est interdit d'en poser (après un « Je sais pas ») ? → Le maître du jeu refuse la question et redemande uniquement son avis (précision ou passage).
- Que se passe-t-il en cas d'égalité stricte des votes (autant de oui que de non) ? → La majorité strictement supérieure n'est pas atteinte : le leg est REJECTED.
- Que se passe-t-il avec un nombre de joueurs pair et la moitié exacte de votes oui ? → Idem : REJECTED, car il faut strictement plus de la moitié.
- Que se passe-t-il si le nombre de joueurs saisi est inférieur au minimum requis pour jouer (ex. 1 seul joueur) ? → Le formulaire bloque le lancement.
- Que se passe-t-il si deux ateliers sont lancés à la même seconde ? → Le nom de dossier `atelier{date}{heure}` intègre les secondes pour éviter les collisions.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Le système MUST permettre à un utilisateur de cliquer sur « Configurer une partie » pour ouvrir le formulaire de configuration.
- **FR-002**: Le formulaire MUST comporter un champ pour le nombre de joueurs et une zone de texte WYSIWYG pour décrire la feature à analyser.
- **FR-003**: Le système MUST utiliser des cartes placeholders fixes pour cette itération (pas de choix de familles de cartes).
- **FR-004**: À l'appui sur « Lancer », le système MUST créer un dossier d'atelier nommé `atelier{date}{heure}` (tirets à la place des deux-points) et y persister les legs en fichiers JSON.
- **FR-005**: Au premier tour, le maître du jeu MUST tirer au sort l'ordre des participants et le conserver en mémoire pour toute la partie.
- **FR-006**: À chaque tour, le maître du jeu MUST demander au joueur courant sa carte, son scénario et son explication.
- **FR-007**: Le système MUST stocker chaque leg dans un fichier JSON à 5 champs : nom de la carte, scénario évoqué, explication du joueur, précisions (liste initialement vide), validation (PENDING initialement).
- **FR-008**: Après l'enregistrement du leg, le maître du jeu MUST interroger un par un chaque autre joueur pour savoir s'il veut ajouter une précision, poser une question ou passer.
- **FR-009**: Une précision ajoutée MUST être stockée dans la liste « précisions » du JSON de le leg courante.
- **FR-010**: Une question posée MUST être adressée au joueur ayant joué la carte, qui répond soit « Je sais » + une réponse, soit « Je sais pas » ; si « Je sais », la réponse est ajoutée aux précisions, sinon le questionneur redonne son avis sans pouvoir poser de nouvelle question.
- **FR-011**: Une fois tous les autres joueurs passés, le maître du jeu MUST demander à chacun « valides-tu ? » (oui/non).
- **FR-012**: Si strictement plus de la moitié des votants a répondu oui, le champ validation MUST passer à VALIDATED, sinon à REJECTED.
- **FR-013**: Le maître du jeu MUST enchaîner les tours dans l'ordre tiré au sort jusqu'à épuisement des cartes de tous les joueurs, puis terminer la partie.
- **FR-014**: Le système MUST fournir quatre archétypes de joueurs déterministes : Grumpy (vote non, question avant de passer), Happy (vote oui, jamais de question ni précision), Greedy (explication et précisions trop longues, vote oui à Happy / non à Grumpy), Prof (précision courte à Happy, question puis précision à Grumpy, passe face à Greedy, vote non à Grumpy / oui aux autres).
- **FR-015**: Les agents MUST pouvoir discuter entre eux via le maître du jeu (questions, précisions, réponses) pendant la partie.

### Key Entities *(include if feature involves data)*

- **Atelier (partie)** : représente une session de jeu ; nommé `atelier{date}{heure}` ; contient la description de la feature analysée, la liste des participants, l'ordre de jeu tiré au sort, et les legs jouées.
- **Participant (joueur agent)** : représente un agent IA ; possède un nom et un archétype (Grumpy, Happy, Greedy, Prof) définissant son comportement déterministe.
- **Leg (tour de jeu)** : représente une carte jouée ; persistée en JSON avec 5 champs : nom de la carte, scénario évoqué, explication du joueur, précisions (liste), validation (PENDING / VALIDATED / REJECTED).
- **Maître du jeu** : orchestrateur de la partie ; décide à qui c'est de jouer, pose les questions aux joueurs, enregistre les legs et calcule le résultat du vote.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Une partie complète (configuration → lancement → tours → fin) peut être déroulée de bout en bout sans intervention manuelle autre que le clic sur « Lancer ».
- **SC-002**: 100 % des legs jouées sont persistées dans le dossier de l'atelier sous forme de fichiers JSON respectant les 5 champs attendus.
- **SC-003**: Le vote de validation d'un leg est correctement tranché : VALIDATED si et seulement si strictement plus de la moitié des votes sont oui.
- **SC-004**: Chaque archétype (Grumpy, Happy, Greedy, Prof) adopte son comportement déterministe sur 100 % des tours auxquels il participe.
- **SC-005**: L'ordre des participants, tiré au sort au premier tour, reste stable pendant toute la partie (vérifiable sur l'ensemble des legs enregistrées).

## Assumptions

- Les familles de cartes Cornucopia ne sont pas choisies dans cette itération : des cartes placeholders fixes sont utilisées et seront remplacées dans une itération ultérieure.
- Le nombre de joueurs est saisi librement mais doit être suffisant pour que la notion de majorité stricte ait un sens (au moins 2 joueurs).
- Le backend (Quarkus / Java 25 / LangChain4j) héberge le maître du jeu et orchestre les agents ; le frontend (Vue 3 / Vuetify) fournit le formulaire de configuration et la zone WYSIWYG, conformément à la constitution du projet.
- Les dossiers d'atelier et leurs fichiers JSON sont stockés sur le disque local du serveur.
- Les quatre archétypes sont suffisants pour valider le flux dans cette première itération ; d'autres archétypes pourront être ajoutés ensuite.
