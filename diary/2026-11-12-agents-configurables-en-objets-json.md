# 2026-11-12 — Les archétypes deviennent des agents configurables en objets JSON

Feature `specs/009-configurable-agents`. Le concept d'« archétype » (enum
`GRUMPY/HAPPY/GREEDY/PROF`) laisse place à des **agents configurables** : des objets
JSON persistés un par un dans `agents/{id}.json`, gérables (créer/lister/modifier/
supprimer) depuis une page dédiée `/agents` et une API `CRUD /api/agents`.

## Les décisions qui ont structuré l'implémentation

**Un agent est forcément LLM.** Chaque agent embarque `openAiCompatibleUrl`,
`modelName`, `apiKeyVar`, `personalityPrompt`, `agentName` et un `id` UUID généré à
la création. La personnalité (l'ancien archétype) devient un texte libre transmis
dans le prompt système — l'enum n'est plus dans le chemin de lancement (les agents
déterministes restent, tests unitaires uniquement). Le chemin ACP est supprimé du
lancement, comme prévu : on y reviendra plus tard.

**La clé d'API n'est jamais persistée.** Seul le *nom* de la variable d'environnement
(`apiKeyVar`) est stocké ; la valeur est résolue au lancement via `System.getenv`.
Si la variable est absente, le lancement est refusé avec un 400 qui la nomme (FR-013).
Côté test, une seam `EnvResolver` rend ce comportement testable sans toucher à l'env
du process.

**Instantané au lancement.** Au lieu de référencer l'agent courant, l'atelier copie
la config complète de chaque agent dans `agentSnapshots[]` (FR-017) : modifier ou
supprimer un agent après coup ne change pas les parties lancées, et les anciens
ateliers restent consultables tels quels (les champs legacy `archetype` /
`agentConfigs` sont lus mais plus écrits).

**Deux joueurs peuvent partager un agent.** Le formulaire d'atelier remplace le
« nombre de joueurs » par un sélecteur multi-agents (`v-autocomplete` à chips,
accessible clavier/lecteur d'écran) : `agentIds[]`, minimum 2, doublons autorisés.
L'ordre des participants — et donc le premier meneur — est tiré au hasard au
lancement, les noms `Joueur-N` étant attribués après le mélange.

## Ce que le TDD a attrapé en chemin

- Un `Location` de réponse absolutisé par Quarkus (`http://localhost:8081/...`) :
  l'assertion de test passait en `containsString` plutôt qu'en `startsWith`.
- Un bug de formatage Java dans un helper de test (`.formatted()` appliqué au
  mauvais littéral) : tous les agents de test s'appelaient `%s`, provoquant des 409
  en cascade. Les text blocks ont réglé ça.
- La validation de la variable d'env devait s'exécuter même en mode `fake` (CI sans
  LLM) : la résolution de la clé a été déplacée avant le branchement de mode dans
  `LlmBrainFactory`.

## Bilan

233 tests backend + 35 tests frontend, `mvnw verify` (checkstyle bloquant) et
`npm run lint`/`build` passent. La feature tient en un incrément parce que la spec
avait tranché les vraies décisions (instantané, unicité du nom, champs obligatoires,
ordre aléatoire) lors de la clarification — le plan n'a plus eu qu'à dérouler.
