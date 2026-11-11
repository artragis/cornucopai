# 2026-11-08 — Entrées structurées du tour et prompts allégés : leçons du smoke test réel

Cinquième incrément : `specs/005-leg-qa-structure`. Le smoke test réel contre bionic
(spec 004) a montré trois problèmes de performance/qualité, traités ici.

## Ce que le smoke test a révélé

1. **Le vote coûtait des minutes de génération** (251 s sur un seul vote) pour un
   oui/non : le modèle produisait tout son raisonnement avant le JSON.
2. **Le prompt de réaction était contradictoire** : « 3 statuts possibles » puis
   « sauf si un booléen l'interdit » — le modèle hésitait.
3. **Le résultat du tour était une liste plate de strings** : impossible de distinguer
   une question d'une précision, ni de savoir quelles questions avaient reçu
   « Je sais pas ».

## Les décisions (R-035…R-044)

1. **Schéma du tour v1 → v2** : `precisions: [string]` → `entries: [{kind, author,
   text, knows?, answer?}]`. Une question et sa réponse forment UNE entrée `QUESTION`
   avec `knows`/`answer`. Cinq champs de premier niveau conservés. Lecture legacy :
   un fichier v1 (`precisions`) est converti à la lecture en entrées PRECISION
   (author `"inconnu"`) — vérifié sur le vrai atelier du smoke test
   `atelier20261108-232136`. Piège rencontré : la méthode `isQuestion()` du record
   était sérialisée comme une propriété `question` par Jackson → `@JsonIgnore`, et
   `@JsonInclude(NON_NULL)` pour omettre `knows`/`answer` quand absents.
2. **Q/R délocalisées dans un outil** : nouveau tool `get_tour_info` (par partie, via
   un `LegContext` partagé) qui retourne le tour courant en JSON, entrées structurées
   incluses. Le prompt de réaction n'embarque PLUS le contenu du tour : il nomme le
   tour (carte + joueur) et pointe vers l'outil. Gain de tokens à chaque réaction.
3. **Vote capé à 16 tokens** : langchain4j 1.23.0 n'a pas de paramètres par méthode,
   donc un second proxy AiServices sur un modèle dédié `maxTokens(16)`, même mémoire
   et même prompt système, sans outils. Le prompt de vote interdit tout raisonnement.
   `JsonAnswers.stripToJson` (fences markdown + premier bloc `{...}`) rend le parsing
   tolérant, réutilisé par le brain ACP.
4. **Statuts conditionnels propres** : `questionsAllowed=true` → `PRECISION|QUESTION|
   PASS` (une seule question) ; `false` → `PRECISION|PASS` seuls, « tu ne peux pas
   poser de question », et le mot QUESTION n'apparaît plus comme statut proposé.
5. **Terminologie française** : plus aucun prompt ne contient le mot « leg » ; le tour
   en cours est désigné par « tour » / « la carte posée par <joueur> » (test
   `PromptsTest` qui scanne les 4 prompts).

## Résultat

`./mvnw test` : **132 tests, 0 échec** (114 baseline + 18 nouveaux/mis à jour). Les
prochaines parties réelles devraient voir le vote passer de minutes à quelques
secondes et les réactions ne plus recopier tout le tour dans le prompt.
