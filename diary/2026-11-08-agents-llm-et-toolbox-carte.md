# 2026-11-08 — Agents joueurs pilotés par LLM et toolbox carte : premier vrai incrément IA

Quatrième incrément : `specs/004-llm-agents`. Jusqu'ici les parties étaient jouées
par des agents déterministes (Grumpy/Happy/Greedy/Prof). Cette fois, les agents
répondent avec un LLM, configuré **par agent**, et disposent d'un premier outil dans
leur toolbox LangChain4j.

## Les décisions qui ont structuré l'implémentation (R-021…R-034)

1. **AiServices programmatique par agent, pas la config globale Quarkus.** Les
   propriétés `quarkus.langchain4j.*` sont globales ; or la spec exige un fournisseur
   PAR agent (FR-001). Chaque agent a donc son propre proxy
   `AiServices.builder(PlayerAiService.class).chatModel(...).systemMessageProvider(...)
   .chatMemory(...).tools(...).build()` (R-021), avec un `OpenAiChatModel` construit
   from sa config (baseUrl custom, apiKey optionnelle, timeout). Nouvelle dépendance
   `dev.langchain4j:langchain4j-open-ai` épinglée à **1.23.0** — la version exacte que
   `quarkus-langchain4j-core` 1.15.0 tire déjà transitivement (R-022). Vérifié dans
   le jar : `apiKey == null` → aucun header `Authorization` (vrai « no auth » pour le
   serveur bionic local, FR-002).

2. **Un seul agent, des brains interchangeables.** `LlmPlayerAgent` est la seule
   implémentation de `PlayerAgent` en jeu (FR-003). Il parle à une interface
   `LlmBrain` ; `LangChain4jBrain` (endpoint OpenAI-compatible, toolbox + mémoire) et
   `AcpCliBrain` (agent ACP `github-copilot`/`mistral-vibe` en CLI one-shot, R-029)
   sont les deux transports. Les agents déterministes restent dans le code pour les
   tests unitaires uniquement.

3. **Sorties typées via DTO, fallback non bloquant.** Le LLM renvoie des records
   (`PlayedCardDto`, `ReactionDto`, `AnswerDto`, `VoteDto`) que l'agent mappe vers les
   types scellés du domaine (R-027). Toute erreur (endpoint down, JSON illisible,
   valeur invalide) → valeur de repli pour CETTE décision (Pass / « Je sais pas » /
   NO / scénario générique) + log WARN : la partie ne plante jamais (FR-013, R-030).
   `cardName` vient toujours de la carte distribuée, jamais du LLM. Une question n'est
   jamais renvoyée quand `questionsAllowed=false` (sinon le GameMaster lèverait une
   exception).

4. **Mémoire : une fenêtre glissante par agent et par atelier.** Chaque brain a son
   propre `MessageWindowChatMemory` (id `<atelierId>/<agentName>`, 40 messages,
   R-025) : l'isolation est structurelle (une instance AiServices par agent), rien ne
   persiste entre ateliers. Le prompt système vient de `SystemPrompts.forArchetype`
   (règles + contrat de sortie JSON + personnalité, R-026).

5. **L'outil `get_card_info` agrège les trois sources du dépôt Cornucopia** (R-024) :
   le YAML `source/*-cards-*-en.yaml` (id, valeur, description, url, famille —
   dernière version par édition), le YAML `source/<edition>-mappings-<version>.yaml`
   (STRIDE, CAPEC, ASVS, Dev Guide, SAFECode), et le contenu riche markdown de
   `cornucopia.owasp.org/data/cards/<set>/<famille>/<id>/` (`explanation.md`,
   `technical-note.md`) — localisé en PARCOURANT les dossiers, jamais en calculant un
   slug. Vérifié contre le vrai dépôt : VE2 → 4,9 ko de markdown agrégé ; JOA est
   correctement signalé présent dans deux sets (`dbd:1.0:WC`, `webapp:3.0:WC`,
   FR-008) ; id inconnu → « Carte inconnue : ZZ9 » (FR-009).

6. **Zéro config = serveur local bionic.** `cornucopai.llm.base-url` défaut
   `http://localhost:1235/v1`, sans clé (R-028). Les overrides par agent passent par
   le tableau optionnel `agents` de `POST /api/ateliers` (contrat v0.4.0), validé
   (400) et persisté dans `atelier.json` (`agentConfigs`, FR-012).

7. **CI sans LLM, une seule famille par test.** `cornucopai.llm.brain=fake` dans le
   profil de test → `FakeLlmBrain` scripté (R-031) : les `@QuarkusTest` jouent des
   parties complètes sans aucun appel réseau. Chaque test de partie ne sélectionne
   qu'une famille (`webapp:VE` = 2 cartes → ≤ 2 legs, FR-014).

## Pièges rencontrés

- `AiServices.builder` touche le container CDI Arc : construction rendue **lazy**
  dans `LangChain4jBrain` pour rester testable en unité plain-Java.
- `MessageWindowChatMemory` 1.23.0 : le builder s'appelle `maxMessages(Integer)`
  (pas `maxMessagesProvider`).
- SmallRye Config traite une valeur vide (`${LLM_API_KEY:}`) comme null pour une
  injection `String` : `api-key`/`model` sont injectés en `Optional<String>`.
- Prompt-injection : tout contenu externe (feature HTML, description de carte, legs,
  questions) est placé dans des sections « DONNÉES » étiquetées du message
  utilisateur, jamais dans le prompt système (test dédié avec texte de carte
  hostile).

## Résultat

`./mvnw test` : **114 tests, 0 échec**. Les parties tournent de bout en bout sur des
agents LLM (fake en CI, bionic local en manuel via quickstart §2).
