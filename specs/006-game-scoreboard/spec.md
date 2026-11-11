# Feature Specification: Scoreboard Cornucopia — couleur majeure, points et page de résultats

**Feature Branch**: `006-game-scoreboard`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "On va implémenter le scoreboard de cornucopia. Au début de chaque tour, le premier joueur, en jouant sa carte, décide de la famille 'majeure' et les autres essaient de suivre s'ils peuvent, sinon ils doivent se donner une autre carte. Si leur scénario est accepté ET qu'ils sont dans la couleur majeure du tour, alors ils gagnent un point. Une fois que tous les joueurs ont joué, celui qui a la carte la plus élevée gagne un point bonus. Une fois que toutes les cartes sont jouées, on regarde celui qui a le plus de points. Si la famille 'cornucopia' est jouée en premier, c'est le second joueur qui décide de la couleur majeure, sinon elle est assimilable à la couleur majeure (famille joker). Cela implique d'ajouter le concept de 'couleur majeure' dans le flux, puis de compter les points. Il faudra une interface pour voir tous les résultats d'un jeu en cliquant dans l'historique. Le scoreboard apparaîtra en plus de tous les scénarios dans cette page. La page aura aussi un polling pour afficher les scénarios au fur et à mesure qu'ils sont produits."

## Clarifications

### Session 2026-11-10

- Q: Quel mécanisme désigne le premier joueur de chaque nouveau tour ? → A: Règle Cornucopia authentique : le premier joueur du tour 1 est tiré au sort ; ensuite c'est le gagnant du tour précédent (carte la plus forte de la couleur majeure) qui ouvre le tour suivant ; si toutes les cartes d'un tour ont été REJECTED, le premier joueur du tour suivant est tiré au sort. Invariant : un tour impose que tous les joueurs encore en lice aient joué exactement une fois.
- Q: En cas d'égalité de valeur entre deux cartes de la couleur majeure lors du point bonus, comment attribue-t-on le point ? → A: Cas impossible — les valeurs de cartes sont uniques au sein d'une même famille (suit), donc deux cartes de la couleur majeure ne peuvent pas avoir la même valeur.
- Q: Comment la page de détail d'un atelier s'ouvre-t-elle depuis l'historique ? → A: Par une route dédiée (ex. `/ateliers/:id`) gérée par le routeur Vue, avec URL propre permettant de consulter un atelier par lien direct et de rafraîchir la page sans perdre le contexte.
- Q: À quel intervalle exact la page de détail interroge-t-elle le serveur en polling ? → A: Toutes les 5 secondes (valeur fixe), le polling s'arrête quand l'atelier est terminé.
- Q: Sans couleur majeure déterminée (tour 100 % WC), qui ouvre le tour suivant ? → A: Le gagnant du point bonus (carte WC la plus forte) ouvre le tour suivant ; le tirage au sort ne s'applique que quand tous les legs du tour sont REJECTED (aucun gagnant).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Déterminer la couleur majeure à chaque tour (Priority: P1)

En tant que maître du jeu, je veux qu'au début de chaque tour (une passe complète où chaque joueur encore en lice joue exactement une fois), la carte jouée par le premier joueur du tour définisse la **couleur majeure** du tour, afin que les autres joueurs sachent quelle famille ils doivent essayer de suivre. Le premier joueur du tour 1 est tiré au sort ; ensuite c'est le gagnant du tour précédent qui ouvre (ou tirage au sort si tous les legs du tour précédent ont été REJECTED).

**Why this priority**: La couleur majeure est le cœur du mécanisme de score ; sans elle, aucun point ne peut être attribué correctement.

**Independent Test**: Lancer une partie à 3 joueurs avec au moins 2 tours complets ; vérifier que chaque tour possède bien une couleur majeure renseignée, égale à la famille de la première carte jouée dans ce tour.

**Acceptance Scenarios**:

1. **Given** un nouveau tour commence (premier joueur désigné selon FR-001), **When** ce joueur joue une carte d'une famille F, **Then** la couleur majeure du tour est F.
2. **Given** la couleur majeure d'un tour est F, **When** les autres joueurs jouent à leur tour dans ce même tour, **Then** leur carte est comparée à F pour déterminer s'ils « suivent la couleur ».
3. **Given** un tour en cours avec couleur majeure F, **When** le tour se termine (tous les joueurs ont joué), **Then** la couleur majeure est archivée avec le tour et un nouveau tour démarre avec une nouvelle couleur majeure.

---

### User Story 2 - Gérer la famille Cornucopia (joker) comme couleur majeure (Priority: P1)

En tant que maître du jeu, je veux que si la famille Cornucopia (Wild Card, identifiant `WC`) est jouée en premier dans un tour, ce soit le **deuxième joueur** qui décide de la couleur majeure ; sinon, une carte WC jouée par un autre joueur est assimilée à la couleur majeure du tour (elle compte comme « suivre la couleur »).

**Why this priority**: La famille WC est la règle spéciale de Cornucopia ; mal gérée, elle casse le scoring.

**Independent Test**: Lancer une partie incluant la famille WC ; vérifier (a) que WC jouée en premier reporte la décision au joueur suivant, et (b) que WC jouée par un autre joueur compte comme suivre la couleur majeure.

**Acceptance Scenarios**:

1. **Given** un tour démarre, **When** le premier joueur joue une carte de la famille WC, **Then** la couleur majeure n'est pas encore fixée et c'est le deuxième joueur du tour qui la définira en jouant sa carte.
2. **Given** un tour démarre, **When** le premier joueur joue WC et que le deuxième joueur joue une carte de la famille G, **Then** la couleur majeure du tour est G.
3. **Given** la couleur majeure d'un tour est F (F ≠ WC), **When** un joueur joue une carte WC, **Then** cette carte est considérée comme appartenant à la couleur majeure F (joker) pour le scoring.
4. **Given** un tour où le premier joueur a joué WC et le deuxième aussi, **When** le troisième joueur joue une carte de famille H, **Then** la couleur majeure du tour est H (report en cascade jusqu'à une carte non-WC).

---

### User Story 3 - Attribuer les points : scénario validé dans la couleur majeure + point bonus de la carte la plus forte (Priority: P1)

En tant que maître du jeu, je veux attribuer **1 point** à chaque joueur dont le scénario est VALIDATED et dont la carte est dans la couleur majeure du tour, et **1 point bonus** au joueur ayant joué la carte la plus forte du tour (parmi les cartes de la couleur majeure), afin de calculer un score par joueur à la fin de la partie.

**Why this priority**: C'est le mécanisme de score complet ; c'est ce qui fait le « scoreboard ».

**Independent Test**: Lancer une partie à 3 joueurs sur 2 tours ; vérifier le décompte de points : points de scénario (VALIDATED + couleur majeure) et point bonus par tour.

**Acceptance Scenarios**:

1. **Given** un leg dont le scénario est VALIDATED, **When** la carte jouée est dans la couleur majeure du tour, **Then** le joueur gagne 1 point.
2. **Given** un leg dont le scénario est VALIDATED, **When** la carte jouée n'est PAS dans la couleur majeure du tour, **Then** le joueur ne gagne aucun point pour ce leg.
3. **Given** un leg dont le scénario est REJECTED, **When** la carte jouée est dans la couleur majeure du tour, **Then** le joueur ne gagne aucun point (le scénario doit être accepté).
4. **Given** un tour terminé (tous les joueurs ont joué), **When** on détermine le gagnant du tour, **Then** c'est le joueur ayant joué la carte de valeur la plus élevée parmi les cartes de la couleur majeure (WC comprise) qui gagne 1 point bonus.
5. **Given** plusieurs cartes de la couleur majeure dans un tour, **When** on compare leurs valeurs, **Then** l'ordre est : 2 < 3 < … < 10 < J < Q < K < A (As le plus fort).
6. **Given** toutes les cartes sont jouées (fin de partie), **When** on calcule le classement final, **Then** le joueur avec le plus de points est déclaré vainqueur ; en cas d'égalité, les joueurs à égalité sont tous listés.

---

### User Story 4 - Consulter le scoreboard et tous les scénarios d'un atelier depuis l'historique (Priority: P1)

En tant qu'utilisateur humain, je veux cliquer sur un atelier dans la liste « Ateliers précédents » pour ouvrir une **page de résultats** affichant le scoreboard (points par joueur, vainqueur) et la liste de tous les scénarios (legs) joués, afin de rejouer ou analyser une partie passée.

**Why this priority**: C'est l'interface utilisateur demandée ; sans elle, le score n'est pas visible.

**Independent Test**: Depuis l'écran d'accueil, cliquer sur un atelier de l'historique ; vérifier que la page de résultats s'ouvre avec le scoreboard en haut et tous les legs listés en dessous.

**Acceptance Scenarios**:

1. **Given** l'écran d'accueil affiche la liste « Ateliers précédents », **When** je clique sur un atelier, **Then** je suis redirigé vers une route dédiée (ex. `/ateliers/:id`) affichant la page de détail de cet atelier.
2. **Given** la page de détail d'un atelier est ouverte, **When** je la consulte, **Then** le scoreboard est affiché (points par joueur, ordre de passage, vainqueur) au-dessus de la liste de tous les scénarios (legs).
3. **Given** la page de détail est ouverte, **When** je regarde la liste des legs, **Then** chaque leg affiche : le joueur, la carte (id + famille), le scénario, l'explication, les entrées (questions/précisions), le statut de validation, et les points gagnés sur ce leg.
4. **Given** un atelier terminé, **When** j'ouvre sa page de détail, **Then** le scoreboard indique le vainqueur (joueur avec le plus de points).
5. **Given** un atelier en cours (non terminé), **When** j'ouvre sa page de détail, **Then** le scoreboard affiche les points accumulés jusque-là et la partie est indiquée comme « en cours ».

---

### User Story 5 - Polling pour afficher les scénarios au fur et à mesure (Priority: P2)

En tant qu'utilisateur humain, je veux que la page de détail d'un atelier **interroge régulièrement le serveur** (polling) pour rafraîchir la liste des scénarios et le scoreboard, afin de voir la partie progresser en temps réel sans recharger manuellement la page.

**Why this priority**: Le polling rend la page vivante pendant une partie en cours ; c'est un confort, pas un bloqueur.

**Independent Test**: Ouvrir la page de détail d'un atelier en cours pendant qu'une partie se déroule ; vérifier que les nouveaux legs apparaissent automatiquement sans action de l'utilisateur.

**Acceptance Scenarios**:

1. **Given** la page de détail d'un atelier est ouverte, **When** le temps s'écoule, **Then** la page envoie une requête toutes les 5 secondes pour obtenir l'état courant de l'atelier.
2. **Given** de nouveaux legs sont apparus côté serveur, **When** le prochain polling répond, **Then** la liste des scénarios et le scoreboard sont mis à jour automatiquement.
3. **Given** l'atelier est terminé, **When** le polling continue, **Then** il peut s'arrêter (plus de mise à jour nécessaire) ou continuer sans erreur.

---

### Edge Cases

- Que se passe-t-il si tous les joueurs d'un tour jouent WC ? → La couleur majeure reste indéterminée ; aucun point de scénario couleur majeure n'est attribué pour ce tour ; le point bonus est attribué à la carte WC la plus forte, et ce gagnant ouvre le tour suivant.
- Que se passe-t-il en cas d'égalité de points en fin de partie ? → Tous les joueurs à égalité sont listés comme vainqueurs (ex aequo).
- Que se passe-t-il si un joueur n'a plus de cartes au milieu d'un tour ? → Il ne participe pas au tour ; le tour continue avec les joueurs restants (un tour impose que chaque joueur encore en lice joue exactement une fois).
- Que se passe-t-il si tous les legs d'un tour sont REJECTED ? → Aucun gagnant de tour (pas de point bonus) ; le premier joueur du tour suivant est tiré au sort.
- Que se passe-t-il si la carte la plus forte du tour est une WC (joker) ? → Elle participe à la comparaison comme carte de la couleur majeure et peut remporter le point bonus.
- Que se passe-t-il à la relecture d'un atelier ancien (sans couleur majeure ni scores) ? → L'atelier reste lisible ; le scoreboard affiche 0 point partout et « données partielles » ; aucun crash.
- Que se passe-t-il si le polling échoue (serveur indisponible) ? → La page conserve le dernier état affiché et réessaie au prochain cycle ; aucune erreur bloquante.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Le système MUST regrouper les legs en **tours** (tricks) : un tour est une passe complète où chaque joueur encore en lice joue exactement une fois. Le premier joueur du tour 1 MUST être tiré au sort ; pour chaque tour suivant, le gagnant du tour précédent (carte la plus forte de la couleur majeure, FR-009 — ou carte WC la plus forte si la couleur majeure est indéterminée) MUST ouvrir le tour ; si tous les legs d'un tour ont été REJECTED (aucun gagnant), le premier joueur du tour suivant MUST être tiré au sort.
- **FR-002**: À chaque tour, le système MUST déterminer la **couleur majeure** : la famille (suitId) de la première carte jouée dans le tour, sauf si cette carte est de la famille WC (voir FR-003).
- **FR-003**: Si la première carte d'un tour est de la famille WC (Cornucopia / Wild Card), le système MUST reporter la détermination de la couleur majeure à la première carte non-WC jouée dans ce tour (par le deuxième joueur, ou le troisième, etc.).
- **FR-004**: Une carte de la famille WC jouée par un joueur autre que le premier du tour MUST être considérée comme appartenant à la couleur majeure (joker) pour le scoring.
- **FR-005**: Le système MUST persister la couleur majeure de chaque tour (dans le fichier JSON du tour ou de l'atelier) afin qu'elle soit relisable dans l'historique.
- **FR-006**: Pour chaque leg dont le statut de validation est VALIDATED, le système MUST attribuer **1 point** au joueur si la famille de sa carte est égale à la couleur majeure du tour (WC incluse via FR-004).
- **FR-007**: Pour chaque leg dont le statut est VALIDATED mais dont la carte n'est pas dans la couleur majeure, le système MUST attribuer **0 point**.
- **FR-008**: Pour chaque leg REJECTED, le système MUST attribuer **0 point**, quelle que soit la famille de la carte.
- **FR-009**: À la fin de chaque tour (tous les joueurs ont joué), le système MUST attribuer **1 point bonus** au joueur ayant joué la carte de valeur la plus élevée parmi les cartes de la couleur majeure du tour (WC comprise).
- **FR-010**: L'ordre de comparaison des valeurs de cartes MUST être : 2 < 3 < 4 < 5 < 6 < 7 < 8 < 9 < 10 < J < Q < K < A.
- **FR-011**: Le système MUST calculer et persister le **score total par joueur** (somme des points de scénario + points bonus) à la fin de la partie, et identifier le ou les vainqueurs (joueur(s) avec le plus de points).
- **FR-012**: L'API de détail d'un atelier (`GET /api/ateliers/{id}`) MUST retourner, en plus des champs existants, la couleur majeure de chaque tour, les points par leg, le score par joueur, et le vainqueur.
- **FR-013**: L'interface MUST rendre chaque atelier de l'historique **cliquable** pour naviguer vers une route dédiée (ex. `/ateliers/:id`) affichant la page de détail de l'atelier.
- **FR-014**: La page de détail MUST afficher le **scoreboard** (score par joueur, ordre de jeu, vainqueur ou « en cours ») et la liste de **tous les scénarios** (legs) avec leurs détails (joueur, carte, famille, scénario, explication, entrées, validation, points gagnés sur le leg, couleur majeure du tour).
- **FR-015**: La page de détail MUST effectuer un **polling** périodique toutes les **5 secondes** sur l'API de détail pour rafraîchir les legs et le scoreboard tant que l'atelier n'est pas terminé.
- **FR-016**: Le polling MUST s'arrêter (ou devenir inactif) quand l'atelier est terminé (`finishedAt` non nul), sans empêcher l'affichage du résultat final.
- **FR-017**: La lecture d'un atelier ancien (antérieur à cette feature, sans couleur majeure ni scores) MUST rester possible sans erreur : le scoreboard affiche des valeurs par défaut (0 point, pas de couleur majeure) et un indicateur « données partielles ».
- **FR-018**: Chaque leg persisté MUST comporter, en plus des champs existants, la couleur majeure de son tour et les points attribués pour ce leg (0 ou 1 pour le scénario, et le bonus éventuel), afin que l'historique soit autonome.

### Key Entities *(include if feature involves data)*

- **Tour (trick)** : groupe d'un leg par joueur encore en lice, joué dans l'ordre de jeu ; possède une couleur majeure déterminée par sa première carte non-WC ; persiste la liste de ses legs et son gagnant (point bonus).
- **Couleur majeure (major suit)** : famille (suitId) d'un tour, déterminée par la première carte non-WC du tour ; une carte WC est toujours considérée comme de la couleur majeure.
- **Score** : points accumulés par un joueur = somme des points de scénario (1 si VALIDATED + couleur majeure) + points bonus (1 par tour remporté).
- **Scoreboard** : tableau récapitulatif par joueur (nom, archétype, score total, nombre de tours remportés) + vainqueur de la partie ; affiché sur la page de détail de l'atelier.
- **Leg (étendu)** : en plus des champs existants (cardName, scenario, explanation, entries, validation), chaque leg persisté comporte désormais : `turnIndex` (numéro du tour), `majorSuit` (couleur majeure du tour), `suitId` (famille de la carte jouée), `cardValue` (valeur de la carte), `points` (points gagnés sur ce leg : 0, 1, ou 1+bonus).
- **Atelier (étendu)** : en plus des champs existants, l'atelier persisté comporte la liste des tours avec leur couleur majeure et leur gagnant, le score par joueur, et le vainqueur final.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100 % des nouveaux ateliers persistés contiennent une couleur majeure par tour et un score par joueur, vérifiables sur les fichiers JSON.
- **SC-002**: Le calcul des points est correct sur 100 % des parties de test : 1 point par leg VALIDATED en couleur majeure, 1 point bonus par tour pour la carte la plus forte de la couleur majeure, 0 sinon.
- **SC-003**: La règle du joker WC est appliquée correctement dans 100 % des cas de test : WC en premier → report au joueur suivant ; WC ailleurs → assimilée à la couleur majeure.
- **SC-004**: La page de détail d'un atelier affiche le scoreboard et tous les legs en moins de 2 secondes après le clic dans l'historique.
- **SC-005**: Le polling met à jour la page de détail d'un atelier en cours en moins de 6 secondes après l'apparition d'un nouveau leg côté serveur (intervalle de 5 s + marge de traitement).
- **SC-006**: 100 % des ateliers anciens (sans couleur majeure ni scores) restent lisibles via l'API et l'interface, avec un affichage dégradé (0 point, « données partielles »).
- **SC-007**: Le vainqueur affiché en fin de partie correspond au joueur avec le plus de points dans 100 % des parties de test ; les égalités sont correctement gérées.

## Assumptions

- Un **tour** (trick) correspond à une passe complète où chaque joueur encore en lice joue exactement une fois. Le premier joueur du tour 1 est tiré au sort ; le gagnant du tour précédent ouvre le tour suivant (tirage au sort si tous les legs du tour précédent ont été REJECTED). Le regroupement en tours et la désignation du premier joueur sont de la responsabilité du maître du jeu.
- La famille **WC** (Wild Card / Cornucopia) est identifiée par le `suitId` `"WC"` dans les données des cartes (c'est la famille joker du jeu Cornucopia).
- L'ordre des valeurs de cartes est : 2 < 3 < … < 10 < J < Q < K < A (As le plus fort), conformément aux règles usuelles des jeux de levées. Les valeurs sont des chaînes (`"2"`, `"10"`, `"J"`, etc.) et la comparaison est faite sur cet ordre canonique, pas alphabétiquement.
- Les valeurs de cartes sont uniques au sein d'une même famille : deux cartes de la couleur majeure ne peuvent jamais avoir la même valeur, donc le point bonus d'un tour a toujours un seul gagnant.
- En cas d'égalité de score final, tous les joueurs à égalité sont affichés comme vainqueurs.
- La page de détail d'un atelier est une **route dédiée** (ex. `/ateliers/:id`) gérée par le routeur Vue ; après le lancement d'une partie, l'utilisateur est redirigé vers cette route, et chaque atelier de l'historique y mène par un clic.
- Le polling est un simple `setInterval` de **5 secondes** côté frontend appelant `GET /api/ateliers/{id}` ; il n'y a pas de WebSocket ni de SSE dans cette itération.
- Les ateliers existants (antérieurs à cette feature) n'ont pas de couleur majeure ni de scores persistés ; ils restent lisibles avec des valeurs par défaut (FR-017).
- Le backend (Quarkus / Java 25) calcule les points et la couleur majeure côté serveur dans le `GameMaster` ; le frontend (Vue 3 / Vuetify) affiche le scoreboard et les legs sur la page de détail, conformément à la constitution.
