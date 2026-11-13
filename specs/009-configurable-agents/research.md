# Research: Agents configurables (objets JSON) — décisions Phase 0

Derived from `spec.md` (FR-001…FR-020 + Clarifications session 2026-11-12) and the
existing codebase (specs 002–007). Format: Decision / Rationale / Alternatives considered.

## R-001 — Stockage des agents : un fichier JSON par agent

- **Decision**: chaque agent est persisté dans son propre fichier
  `{agents.root-dir}/{id}.json` (défaut `agents/`, gitignored comme `ateliers/`).
  Config: `cornucopai.agents.root-dir=${AGENTS_ROOT_DIR:agents}`.
- **Rationale**: cohérent avec le stockage filesystem des ateliers (R-002 de la spec
  002) ; écriture atomique par agent (rename après écriture complète) ; pas de fichier
  index à maintenir ; suppression = supprimer un fichier. Le listing = lister le
  répertoire et parser chaque fichier (volume faible : quelques dizaines d'agents).
- **Alternatives considered**: un fichier index unique `agents.json` (rejeté : écriture
  non atomique sur édition concurrente, corruption possible du fichier entier) ; une base
  de données (rejeté : YAGNI, la constitution impose filesystem pour les ateliers et aucun
  moteur de BD n'existe dans le projet).

## R-002 — API REST dédiée aux agents : `/api/agents`

- **Decision**: nouvelle ressource `AgentResource` `@Path("/api/agents")` :
  `GET /api/agents` (liste), `POST /api/agents` (création, 201 + body créé),
  `GET /api/agents/{id}`, `PUT /api/agents/{id}` (mise à jour, `id` inchangé),
  `DELETE /api/agents/{id}` (204). Contrat complet dans `contracts/agents-api.yaml`
  v1.0.0. Erreurs: 400 validation (FR-019), 404 id inconnu, 409 nom lisible déjà pris
  (FR-018).
- **Rationale**: CRUD symétrique, testable via REST Assured, consommable par le frontend
  via `fetch` comme `atelierApi.ts`.
- **Alternatives considered**: étendre `/api/ateliers` avec des sous-routes (rejeté :
  mélange deux responsabilités, moins clair) ; GraphQL (rejeté : pas dans le projet).

## R-003 — Règles de validation des champs (FR-019)

- **Decision**: obligatoires et non vides après trim : `agentName`,
  `openAiCompatibleUrl`, `modelName`, `personalityPrompt`. `apiKeyVar` facultatif ; si
  renseigné, doit matcher `^[A-Za-z_][A-Za-z0-9_]*$` (nom de variable d'environnement
  POSIX valide). `openAiCompatibleUrl` doit parser comme URL absolue avec schéma
  `http` ou `https`. `agentName` unique parmi tous les agents (comparaison sur la valeur
  trimmée, sensible à la casse).
- **Rationale**: directement issu de la clarification 2026-11-12 (Q4) ; un nom de
  variable invalide échouerait de toute façon au `System.getenv` ; une URL sans schéma
  casserait le client LLM.
- **Alternatives considered**: rendre `personalityPrompt` facultatif (rejeté : la
  clarification Q4 l'a rendu obligatoire) ; autoriser n'importe quel schéma d'URL (rejeté
  : le client OpenAI-compatible exige http/https).

## R-004 — Forme de la requête de création d'atelier (v0.8.0)

- **Decision**: `CreateAtelierRequest` remplace `playerCount` (integer) et `agents`
  (array, spec 004) par **`agentIds: string[]`** — requis, minimum 2 éléments (FR-010),
  chaque élément = un participant associé à cet agent. `featureHtml`, `language`,
  `families`, `inheritFactsFrom` inchangés. Le nombre de participants = `agentIds.size()`.
- **Rationale**: le formulaire envoie directement les agents sélectionnés ; le backend
  n'a plus à deviner un nombre de joueurs. Le même `id` peut apparaître deux fois
  (FR-009).
- **Alternatives considered**: garder `playerCount` + `agents` (rejeté : la spec dit
  explicitement que le champ « nombre de joueurs » disparaît) ; un tableau d'objets
  `{agentId}` (rejeté : inutile, un id suffit).

## R-005 — Instantané (snapshot) de l'agent au lancement (FR-017)

- **Decision**: au lancement, pour chaque participant, `atelier.json` persiste un
  `agentSnapshots[]` indexé comme `players[]`, contenant `id`, `agentName`,
  `openAiCompatibleUrl`, `apiKeyVar`, `modelName`, `personalityPrompt` — **jamais la
  valeur de la clé d'API**. `players[]` gagne `agentId`. L'ancien champ `agentConfigs[]`
  (spec 004) reste lisible mais n'est plus écrit.
- **Rationale**: clarification Q2 (option A) ; rend FR-014 (ateliers existants consultables)
  et « modifier/supprimer un agent ne change pas les ateliers lancés » vrais par
  construction.
- **Alternatives considered**: référence seule vers l'agent courant (option B, rejetée par
  Q2) ; id + nom uniquement (option C, rejetée par Q2).

## R-006 — Résolution de la clé d'API (FR-006, FR-013)

- **Decision**: au moment de créer le brain LLM de chaque participant (lancement de
  l'atelier), si `apiKeyVar` est vide → appel sans clé ; sinon
  `System.getenv(apiKeyVar)` — si la variable n'existe pas, le lancement est refusé
  (400) avec un message nommant la variable manquante. La valeur de la clé n'est jamais
  persistée ni loggée.
- **Rationale**: lecture de l'env côté serveur au moment de l'appel, conforme à la
  description initiale (« sinon on fait getEnv(apiKeyVar) ») ; FR-013.
- **Alternatives considered**: résoudre la clé à la création de l'agent (rejeté : la spec
  dit que seule le nom de la variable est stocké) ; repli sur une clé par défaut (rejeté :
  FR-013 et edge case « pas de clé en dur en repli »).

## R-007 — Suppression de l'archetype et de l'ACP du déroulement (FR-011, FR-012)

- **Decision**: `Participant` devient `(String name, String agentId)` ; l'enum
  `Archetype` et les agents déterministes (Grumpy/Happy/Greedy/Prof) restent dans le
  codebase **uniquement pour les tests unitaires** (comme en spec 004, SC-006). Le prompt
  système du brain = prompt de base du jeu + `personalityPrompt` de l'agent (FR-012).
  `LlmBrainFactory` perd sa branche `ACP` ; `AcpCliBrain` et `providerType` sont retirés
  du chemin de lancement. Lecture des anciens `atelier.json` : OK car Jackson (Quarkus)
  ignore les propriétés inconnues par défaut (`archetype`, `agentConfigs`).
- **Rationale**: FR-011/FR-012 + « supprimer le fait de lancer les choses comme
  aujourd'hui » ; la personnalité passe exclusivement par le prompt.
- **Alternatives considered**: garder l'enum comme champ optionnel des nouveaux ateliers
  (rejeté : la spec dit qu'il n'apparaît plus comme champ obligatoire ; gardé uniquement
  en lecture legacy) ; garder ACP dormant (rejeté : YAGNI, « on reviendra plus tard à la
  notion d'ACP » signifie hors du chemin de lancement, pas code mort).

## R-008 — Page frontend de gestion des agents

- **Decision**: nouvelle page `frontend/src/pages/AgentsPage.vue` sur la route
  `/agents`, ajoutée au routeur, avec un bouton « Gérer les agents » sur `HomePage`.
  Liste des agents (nom + résumé), création/édition dans une `v-dialog` avec
  `v-form`, suppression avec confirmation. Service `frontend/src/services/agentsApi.ts`
  (fetch vers `/api/agents`, proxy Vite `/api` → 8080 déjà en place).
- **Rationale**: US-1 (FR-001…FR-005) ; séparation page dédiée / formulaire d'atelier.
- **Alternatives considered**: intégrer la gestion dans le dialogue de création
  d'atelier (rejeté : la spec demande une page dédiée indépendante).

## R-009 — Sélecteur d'agents dans le formulaire d'atelier (FR-007, FR-008)

- **Decision**: dans `ConfigureGameDialog.vue`, le champ « nombre de joueurs » est
  remplacé par un **`v-autocomplete` multiple avec chips** (`items` = agents du backend,
  `item-title="agentName"`, `item-value="id"`, `multiple`, `chips`, `closable-chips`),
  minimum 2 sélections (FR-010), label explicite. Vuetify fournit navigation clavier,
  focus visible et annonces ARIA nativement. État vide (aucun agent créé) : message
  invitant à aller sur `/agents` (scénario US-2.4). Le même agent peut être sélectionné
  plusieurs fois (FR-009).
- **Rationale**: composant Vuetify standard = accessible sans travail custom (FR-008) ;
  chips = sélection visible et supprimable au clavier.
- **Alternatives considered**: cases à cocher par agent (rejeté : peu scalable et moins
  clair pour « même agent deux fois ») ; saisie libre de noms (rejeté : la sélection doit
  venir de la liste persistée).

## R-010 — Ordre aléatoire des participants au lancement (FR-020)

- **Decision**: `AtelierService` mélange la liste des participants avec
  `Collections.shuffle(list, random)` (le `Random` déjà injecté, seedé dans les tests)
  **après** avoir associé chaque participant à son agent ; le premier de la liste
  mélangée est le premier meneur. Les noms `Joueur-N` sont attribués après le mélange
  (FR-016 : N = position dans l'ordre déterminé au lancement).
- **Rationale**: clarification Q5 (option C) ; déterministe en test grâce au seed.
- **Alternatives considered**: ordre de sélection (option A) ou alphabétique (option B),
  rejetés par Q5.

## R-011 — Compatibilité legacy (FR-014)

- **Decision**: les lectures d'anciens `atelier.json` (champs `archetype`,
  `agentConfigs`, `playerCount`) continuent de fonctionner (Jackson ignore les champs
  inconnus ; les champs legacy restent dans les DTO de lecture marqués `@deprecated`).
  Aucune migration de données. Les nouveaux ateliers écrivent `agentId` +
  `agentSnapshots`. Le frontend affiche les anciens ateliers tels quels (l'archetype
  legacy reste visible en lecture seule si présent).
- **Rationale**: FR-014 + edge cases ; zéro migration = simplicité (YAGNI).
- **Alternatives considered**: migration à la volée des anciens ateliers (rejeté : non
  exigé, risque de perte de données).

## R-012 — Versioning des contrats

- **Decision**: `contracts/agents-api.yaml` = v1.0.0 (nouveau). `contracts/ateliers-api.yaml`
  passe en **v0.8.0** (supersede v0.7.0 de la spec 007) : `POST /api/ateliers` prend
  `agentIds[]` à la place de `playerCount` + `agents[]` ; schémas `Atelier.players[]`
  (ajout `agentId`) et `agentSnapshots[]` ; `GET` inchangés. `agent-schema.json` décrit
  un fichier agent ; `atelier-schema.json` v0.8.0 est le delta.
- **Rationale**: convention du projet (chaque spec bump la version du contrat, cf. v0.2.0
  → v0.7.0).
- **Alternatives considered**: garder v0.7.0 et surcharger les champs (rejeté : rupture
  claire documentée, le frontend est le seul consommateur).
