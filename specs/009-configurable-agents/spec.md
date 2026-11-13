# Feature Specification: Agents configurables (objets JSON) avec page de gestion dédiée

**Feature Branch**: `009-configurable-agents`

**Created**: 2026-11-12

**Status**: Draft

**Input**: User description: "Nous allons changer le concept d'archetype pour en faire quelque chose de plus 'configurable'. L'idée sera de faire des objets JSON qui sont configurables dans une nouvelle page de l'application. Globalement un 'agent' aura désormais forcément LLM (on reviendra plus tard à la notion d'ACP, mais on peut déjà supprimer le fait de lancer les choses comme aujourd'hui). Un 'agent' aura donc : openAiCompatibleUrl (une URL qui va vers le LLM — locale, API Mistral, Azure Foundry...), apiKeyVar (le nom de la variable d'environnement qui stockera l'API key ; si vide on considère que l'API n'a pas de key, sinon on fait getEnv(apiKeyVar)), modelName, personalityPrompt (texte libre qui donne des instructions supplémentaires et définit ce qu'on appelait avant l'archetype, qui n'est plus une valeur d'enum), agentName (nom lisible par les humains), id (identifiant technique autogénéré, uuid). Dans la nouvelle page on peut créer des agents, les lister, les supprimer et les mettre à jour. Sur le formulaire de création d'atelier, au lieu d'un nombre d'agents, on a un moyen moderne et accessible de sélectionner des agents. Deux participants peuvent être liés au même agent, ce sont juste deux joueurs différents."

## Clarifications

### Session 2026-11-12

- Q: D'où provient le nom de chaque joueur (participant) affiché dans la partie quand on sélectionne des agents dans le formulaire de création d'atelier ? → A: Nom généré par défaut sous la forme « Joueur-N » (N = position dans la sélection), indépendant du nom lisible de l'agent sélectionné.
- Q: Au moment de lancer un atelier, la configuration de l'agent associé à chaque participant est-elle copiée (instantané) dans l'atelier, ou l'atelier conserve-t-il seulement une référence vers l'agent ? → A: Instantané complet de la configuration de l'agent copié dans l'atelier au lancement ; la valeur de la clé d'API n'est jamais copiée, seul le nom `apiKeyVar` l'est.
- Q: Deux agents peuvent-ils porter le même nom lisible (`agentName`) ? → A: Non, le nom lisible doit être unique parmi tous les agents, à la création comme à la modification.
- Q: Quels champs du formulaire de création d'un agent sont obligatoires et quelles règles de validation s'appliquent ? → A: Obligatoires : `agentName`, `openAiCompatibleUrl` (URL valide requise), `modelName`, `personalityPrompt` (non vide). Facultatif : `apiKeyVar` (vide = aucune clé requise ; sinon nom de variable d'environnement valide).
- Q: Dans un atelier créé par sélection d'agents, quel ordre détermine la liste des participants et le premier meneur de partie ? → A: Tirage aléatoire au moment du lancement de l'atelier : l'association participant ↔ agent sélectionné est conservée, mais l'ordre des participants (et donc le premier meneur) est mélangé aléatoirement.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Gérer des agents configurables depuis une page dédiée (Priority: P1)

En tant qu'utilisateur humain, je veux disposer d'une page dédiée dans l'application pour créer, lister, modifier et supprimer des agents configurables, afin de préparer mes configurations d'agents indépendamment de la création d'un atelier.

Chaque agent que je crée est défini par :

- un **nom lisible** (`agentName`) qui l'identifie dans l'interface ;
- un **identifiant technique unique** (`id`) généré automatiquement à la création, qui ne change jamais ensuite ;
- l'**URL du fournisseur de modèle de langage** (`openAiCompatibleUrl`) : une URL locale, l'API Mistral, une instance Azure Foundry, etc. ;
- le **nom de la variable d'environnement contenant la clé d'API** (`apiKeyVar`) : si ce champ est vide, le fournisseur est considéré comme n'exigeant aucune clé ; sinon la clé est lue dans cette variable d'environnement au moment de l'utilisation ;
- le **nom du modèle** (`modelName`) à utiliser chez ce fournisseur ;
- un **prompt de personnalité** (`personalityPrompt`) en texte libre, qui donne à l'agent ses instructions supplémentaires et définit sa personnalité — ce qui remplace l'ancien concept d'archetype prédéfini.

**Why this priority**: Sans page de gestion des agents, il n'existe aucun moyen de définir les agents utilisés ensuite dans les ateliers. C'est le prérequis de toute la feature.

**Independent Test**: Depuis la page dédiée, créer un agent en remplissant tous les champs, le retrouver dans la liste, modifier son prompt de personnalité, vérifier que l'identifiant est inchangé, puis le supprimer et constater qu'il n'apparaît plus dans la liste.

**Acceptance Scenarios**:

1. **Given** la page de gestion des agents, **When** j'y accède, **Then** je vois la liste de tous les agents existants avec leur nom lisible.
2. **Given** le formulaire de création d'agent, **When** je remplis tous les champs et valide, **Then** l'agent est créé avec un identifiant unique généré automatiquement, et il apparaît dans la liste.
3. **Given** un agent existant, **When** je modifie son nom, son URL, sa variable de clé, son modèle ou son prompt de personnalité et valide, **Then** les modifications sont enregistrées et l'identifiant de l'agent reste le même.
4. **Given** un agent existant, **When** je le supprime, **Then** il disparaît de la liste et n'est plus sélectionnable nulle part.
5. **Given** le formulaire de création, **When** je laisse le champ `apiKeyVar` vide, **Then** l'agent est enregistré comme n'exigeant aucune clé d'API.
6. **Given** le formulaire de création, **When** j'indique un nom de variable d'environnement dans `apiKeyVar`, **Then** c'est ce nom — et non la clé elle-même — qui est enregistré dans la configuration de l'agent.
7. **Given** le formulaire, **When** je soumets un formulaire invalide (champs obligatoires manquants, URL mal formée), **Then** la création est refusée et un message indique les champs à corriger.

---

### User Story 2 - Sélectionner des agents dans le formulaire de création d'atelier (Priority: P1)

En tant qu'utilisateur humain, je veux que le formulaire de création d'atelier me propose de sélectionner des agents dans la liste de ceux que j'ai configurés, au lieu de saisir un simple nombre de joueurs, afin que chaque participant de la partie soit associé à un agent concret.

Le sélecteur doit être moderne et accessible (utilisable au clavier, avec un lecteur d'écran, et compréhensible visuellement). Plusieurs participants d'un même atelier peuvent être liés au même agent : ce sont simplement des joueurs différents partageant la même configuration d'agent.

**Why this priority**: C'est le point d'entrée de toute partie — sans sélection d'agents, aucun atelier ne peut être lancé avec la nouvelle configuration.

**Independent Test**: Ouvrir le formulaire de création d'atelier, constater que le champ « nombre de joueurs » n'existe plus, sélectionner deux agents (éventuellement le même deux fois) dans le sélecteur, puis lancer l'atelier et vérifier que les deux participants sont bien associés à l'agent choisi.

**Acceptance Scenarios**:

1. **Given** le formulaire de création d'atelier, **When** je l'ouvre, **Then** je ne vois plus de champ « nombre d'agents/joueurs » : à la place, un sélecteur me permet de choisir des agents parmi ceux définis dans la page de gestion.
2. **Given** le sélecteur d'agents, **When** je sélectionne un agent pour un participant, **Then** cet agent est associé à ce participant et le nom lisible de l'agent est visible dans le formulaire.
3. **Given** le sélecteur d'agents, **When** je sélectionne le même agent pour deux participants différents, **Then** les deux participants sont acceptés et partagent la même configuration d'agent.
4. **Given** le formulaire de création d'atelier, **When** aucun agent n'a encore été créé, **Then** le sélecteur est vide et un message invite à créer un agent depuis la page dédiée avant de pouvoir lancer un atelier.
5. **Given** le formulaire de création d'atelier, **When** je tente de lancer l'atelier sans avoir sélectionné d'agent pour au moins deux participants, **Then** le lancement est refusé et un message indique qu'il faut au moins deux participants.
6. **Given** le sélecteur d'agents, **When** je navigue au clavier ou avec un lecteur d'écran, **Then** chaque option est focusable, annoncé avec le nom lisible de l'agent, et sélectionnable sans souris.

---

### User Story 3 - Lancer des parties avec des agents 100 % pilotés par LLM (Priority: P2)

En tant qu'utilisateur humain, je veux que chaque participant d'un atelier soit piloté exclusivement par un modèle de langage via la configuration de son agent (URL, modèle, clé d'API, personnalité), sans aucun mécanisme de lancement alternatif, afin que le comportement de chaque joueur soit entièrement défini par sa configuration d'agent.

**Why this priority**: C'est la conséquence directe du remplacement de l'archetype : l'ancien mécanisme de lancement (agents déterministes, fournisseurs alternatifs de type ACP) est supprimé du déroulement des parties. Reste en place uniquement ce qui est strictement nécessaire aux tests unitaires existants.

**Independent Test**: Lancer un atelier avec deux participants associés à des agents configurés, et vérifier que chaque décision des joueurs provient du modèle de langage indiqué dans la configuration de l'agent de ce participant.

**Acceptance Scenarios**:

1. **Given** un atelier lancé avec des agents configurés, **When** la partie se déroule, **Then** chaque participant est piloté par le modèle de langage de son agent (URL + nom de modèle + clé d'API lue via la variable d'environnement).
2. **Given** la configuration d'un agent, **When** son `apiKeyVar` est vide, **Then** les appels au fournisseur de LLM sont effectués sans clé d'API.
3. **Given** la configuration d'un agent, **When** son `apiKeyVar` est renseigné, **Then** la clé d'API est résolue au moment de l'appel à partir de la variable d'environnement nommée, et jamais stockée en clair dans la configuration de l'agent.
4. **Given** le prompt de personnalité d'un agent, **When** la partie se déroule, **Then** ce texte est transmis au modèle de langage comme instructions de personnalité de l'agent.
5. **Given** l'ancien concept d'archetype (valeurs prédéfinies), **When** on consulte la configuration d'un agent ou d'un atelier, **Then** il n'apparaît plus comme champ obligatoire : la personnalité est exprimée uniquement par le prompt de personnalité.

---

### Edge Cases

- **Suppression d'un agent encore référencé** : que se passe-t-il si l'utilisateur supprime un agent encore utilisé par un atelier existant ou sélectionné dans le formulaire ? Par défaut : la suppression est autorisée pour les ateliers passés (qui conservent leur historique tel quel), mais l'agent supprimé n'est plus sélectionnable pour un nouvel atelier.
- **Variable d'environnement absente au lancement** : si `apiKeyVar` est renseigné mais que la variable d'environnement n'existe pas au moment de lancer la partie, le lancement échoue avec un message clair indiquant la variable manquante (pas de clé en dur en repli).
- **URL de fournisseur injoignable** : si l'URL `openAiCompatibleUrl` est injoignable au moment de la partie, la partie applique le repli déjà prévu en cas d'indisponibilité du LLM (journalisation + valeur par défaut pour le tour), sans crash.
- **Édition d'un agent utilisé par un atelier en cours** : modifier un agent ne change pas les ateliers déjà lancés avec cet agent (chaque atelier conserve la configuration qu'il avait au lancement).
- **Deux participants, même agent** : deux joueurs d'un même atelier peuvent partager le même `id` d'agent ; ils restent des participants distincts (noms de joueur distincts, tours distincts).
- **Agent sans prompt de personnalité** : ce cas n'est plus possible à la création (le prompt est obligatoire, FR-019) ; un prompt reste néanmoins copiable vide dans un instantané d'atelier si la règle évolue.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Le système MUST fournir une page dédiée permettant de lister tous les agents configurables existants, identifiés par leur nom lisible.
- **FR-002**: Le système MUST permettre de créer un nouvel agent en renseignant : nom lisible, URL du fournisseur compatible OpenAI, nom de la variable d'environnement de la clé d'API (facultatif), nom du modèle, et prompt de personnalité.
- **FR-003**: À la création d'un agent, le système MUST générer automatiquement un identifiant technique unique et stable (UUID) ; cet identifiant ne MUST pas être modifiable ni régénéré lors des mises à jour.
- **FR-004**: Le système MUST permettre de mettre à jour un agent existant (nom, URL, variable de clé, modèle, prompt de personnalité) sans changer son identifiant.
- **FR-005**: Le système MUST permettre de supprimer un agent existant ; un agent supprimé ne MUST plus être sélectionnable pour un nouvel atelier.
- **FR-006**: Si le champ `apiKeyVar` d'un agent est vide, le système MUST considérer que le fournisseur de LLM n'exige aucune clé d'API. Sinon, le système MUST résoudre la clé au moment de l'appel en lisant la variable d'environnement dont le nom est donné par `apiKeyVar` ; la valeur de la clé ne MUST jamais être stockée dans la configuration de l'agent.
- **FR-007**: Le formulaire de création d'atelier MUST remplacer le champ « nombre de joueurs » par un sélecteur d'agents permettant d'associer chaque participant à un agent de la liste.
- **FR-008**: Le sélecteur d'agents du formulaire MUST être accessible : navigable au clavier, chaque option annoncée par le nom lisible de l'agent aux technologies d'assistance, et utilisable sans souris.
- **FR-009**: Plusieurs participants d'un même atelier MUST pouvoir être associés au même agent (même identifiant) ; ce sont des joueurs distincts partageant la même configuration.
- **FR-010**: Un atelier MUST comporter au moins deux participants, chacun associé à un agent ; le lancement sans au moins deux agents sélectionnés MUST être refusé avec un message explicite.
- **FR-011**: Chaque participant d'un atelier MUST être piloté exclusivement par un modèle de langage, selon la configuration de son agent (URL, modèle, clé, personnalité). L'ancien mécanisme de lancement alternatif (fournisseurs de type ACP, agents déterministes hors tests) MUST être retiré du déroulement des parties.
- **FR-012**: Le prompt de personnalité de l'agent MUST être transmis au modèle de langage comme instructions de personnalité lors de la partie, en remplacement de toute valeur d'archetype prédéfinie.
- **FR-013**: Si la variable d'environnement nommée par `apiKeyVar` est absente au lancement d'une partie, le système MUST refuser de lancer la partie avec un message indiquant la variable manquante.
- **FR-014**: Les ateliers existants (créés avant cette feature) MUST rester consultables tels quels ; la nouvelle configuration par agents s'applique aux nouveaux ateliers.
- **FR-015**: Les agents MUST être persistés de façon durable (objets JSON) afin d'être retrouvés à chaque ouverture de l'application, indépendamment des ateliers.
- **FR-016**: Le nom de chaque participant d'un atelier MUST être généré par défaut sous la forme « Joueur-N » (N = position dans l'ordre des participants déterminé au lancement), indépendamment du nom lisible de l'agent qui lui est associé.
- **FR-017**: Au lancement d'un atelier, le système MUST copier dans l'atelier un instantané complet de la configuration de l'agent de chaque participant (`id`, nom lisible, URL, `apiKeyVar`, modèle, prompt de personnalité) ; la valeur de la clé d'API ne MUST jamais faire partie de cet instantané. Modifier ou supprimer un agent ensuite ne MUST pas modifier les ateliers déjà lancés.
- **FR-018**: Le nom lisible (`agentName`) d'un agent MUST être unique parmi tous les agents existants ; la création ou la modification qui produirait un doublon MUST être refusée avec un message explicite.
- **FR-019**: Dans le formulaire d'agent, les champs `agentName`, `openAiCompatibleUrl`, `modelName` et `personalityPrompt` MUST être obligatoires (`openAiCompatibleUrl` doit être une URL valide, `personalityPrompt` non vide) ; seul `apiKeyVar` est facultatif (vide = aucune clé d'API requise ; renseigné = nom de variable d'environnement valide). Toute soumission invalide MUST être refusée avec un message indiquant le champ à corriger.
- **FR-020**: Au lancement d'un atelier, le système MUST tirer aléatoirement l'ordre des participants, qui détermine le premier meneur de partie ; l'association entre chaque participant et l'agent sélectionné dans le formulaire MUST être conservée telle quelle, seule la séquence des participants est mélangée.

### Key Entities *(include if feature involves data)*

- **Agent** : configuration nommée d'un joueur IA. Attributs : `id` (identifiant unique autogénéré, stable), `agentName` (nom lisible), `openAiCompatibleUrl` (URL du fournisseur de LLM), `apiKeyVar` (nom de la variable d'environnement de la clé d'API, vide = aucune clé requise), `modelName` (nom du modèle chez le fournisseur), `personalityPrompt` (texte libre définissant la personnalité et les instructions de l'agent). Un agent n'est pas un joueur : plusieurs joueurs peuvent partager le même agent.
- **Participant** (existant, modifié) : un joueur d'un atelier. Attributs : nom du joueur (généré par défaut sous la forme « Joueur-N », indépendant de l'agent), et référence vers l'`id` de l'agent qui le pilote (au lieu d'une valeur d'archetype prédéfinie).
- **Atelier** (existant, étendu) : persistait déjà les legs, le deck et la liste des participants ; persiste désormais, pour chaque participant, un instantané de la configuration de l'agent utilisé au lancement (y compris l'`id` de l'agent, mais jamais la valeur de la clé d'API).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100 % des agents créés depuis la page dédiée possèdent un identifiant unique généré automatiquement, retrouvable inchangé après modification de l'agent.
- **SC-002**: Un utilisateur peut créer, lister, modifier et supprimer un agent en moins de 2 minutes depuis la page dédiée, sans quitter la page pour la liste.
- **SC-003**: 100 % des nouveaux ateliers sont créés avec au moins deux participants chacun associé à un agent sélectionné dans le sélecteur ; aucun nouveau atelier n'est créé via un champ « nombre de joueurs ».
- **SC-004**: Dans un échantillon de nouveaux ateliers, 100 % des participants partagent ou non le même agent conformément à ce qui a été sélectionné (y compris le cas « deux participants, même agent »), vérifiable dans les données persistées de l'atelier.
- **SC-005**: Pour 100 % des agents dont `apiKeyVar` est renseigné, la clé d'API utilisée lors d'une partie provient de la variable d'environnement nommée, et aucune valeur de clé n'apparaît en clair dans la configuration persistée de l'agent.
- **SC-006**: Pour 100 % des agents dont `apiKeyVar` est vide, les appels au fournisseur de LLM sont effectués sans clé d'API.
- **SC-007**: 100 % des nouveaux ateliers lancés sont pilotés exclusivement par des modèles de langage selon la configuration des agents ; aucun lancement via l'ancien mécanisme (ACP / agents déterministes) n'est possible hors tests unitaires.
- **SC-008**: Le sélecteur d'agents du formulaire passe un audit d'accessibilité clavier et lecteur d'écran sur 100 % de ses options (navigation, focus, annonce du nom lisible).
- **SC-009**: Si la variable d'environnement nommée par `apiKeyVar` est absente, 100 % des tentatives de lancement échouent avec un message nommant la variable manquante, sans lancer de partie.
- **SC-010**: 100 % des ateliers créés avant cette feature restent consultables sans modification de leurs données.

## Assumptions

- Les agents sont persistés sous forme d'objets JSON (un objet par agent, identifié par son `id`) ; le mécanisme de stockage exact (fichier, base de données) est un détail d'implémentation couvert par le plan.
- La résolution de la clé d'API via `getEnv(apiKeyVar)` a lieu côté serveur, au moment de l'appel au fournisseur de LLM, jamais au moment de l'enregistrement de l'agent.
- La page de gestion des agents est une nouvelle page de l'application frontend, accessible depuis la navigation principale (accueil comprise).
- La suppression d'un agent est définitive et ne supprime pas les ateliers passés qui l'ont utilisé (leurs données restent consultables).
- Modifier un agent après le lancement d'un atelier ne modifie pas cet atelier en cours ou terminé.
- Le nombre de participants d'un atelier est déterminé par le nombre d'agents sélectionnés dans le formulaire (un participant par sélection), avec un minimum de deux.
- Les agents déterministes existants (Grumpy, Happy, Greedy, Prof) et le mécanisme ACP sont retirés du déroulement des parties ; ils peuvent subsister uniquement dans les tests unitaires tant que ceux-ci ne testent pas le lancement de parties.
- Les ateliers existants avant cette feature conservent leur affichage actuel (archetype inclus) ; aucune migration de leurs données n'est exigée dans cet incrément.
- Le sélecteur d'agents réutilise les composants d'interface existants (Vuetify) pour garantir cohérence visuelle et accessibilité native.
