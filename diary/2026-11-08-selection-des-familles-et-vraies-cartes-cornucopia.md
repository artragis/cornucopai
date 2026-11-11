# 2026-11-08 — Sélection des familles et vraies cartes Cornucopia : le catalogue vivant

Troisième incrément : `specs/003-card-set-selection`. Jusqu'ici, les agents jouaient
avec des cartes placeholder (`Carte-Placeholder-1..8`). Cette fois, on branche le
vrai dépôt Cornucopia et les vraies cartes.

## Les décisions qui ont structuré l'implémentation

1. **Le catalogue se lit dans `CORNUCOPIA_HOME`, pas dans le classpath.** Toute la
   logique de scan repose sur la variable d'environnement `CORNUCOPIA_HOME`
   (R-012), résolue via MicroProfile Config (`cornucopai.cornucopia-home=${CORNUCOPIA_HOME:}`).
   Le scan de `<home>/source/*-cards-*.yaml` est fait une seule fois, en lazy, et mis
   en cache dans un `@ApplicationScoped` (R-019). Pas d'endpoint de refresh — YAGNI.
   Les tests injectent un fixture hermétique sous `backend/src/test/resources/` (R-020).

2. **« Dernière version » par édition, versionnage sémantique numérique.** Pour chaque
   édition (`meta.edition`), on garde uniquement la version maximale selon une
   comparaison numérique de `x.y.z` (R-013). Résultat : `webapp 3.0` écrase `2.2`,
   `mobileapp 2.0` écrase `1.0`/`1.1`.

3. **Le repli de langue est par set, pas global.** Quand on demande `fr` mais qu'un
   set n'a pas de fichier `fr`, on retombe sur `en` pour ce set uniquement (R-015) —
   et on le consigne dans `sets: [{id, resolvedLanguage}]` pour que l'UI puisse
   afficher un badge `EN`. `eop` n'a pas de `fr`, donc `eop:SP` en `fr` donne des
   cartes `en`. C'est documenté, testé, et visible dans la réponse API.

4. **L'identité de carte embarque sa description.** Une vraie carte est un value
   object `Card = {id, value, setId, suitId, suitName, language, description, …}`
   (R-016). Le deck complet est persisté dans `atelier.json`, donc relire une
   partie ne dépend pas du dépôt Cornucopia. Les legs référencent le vrai `cardName`
   (ex: `VE9`), plus jamais de `Carte-Placeholder-*`.

5. **Une incohérence dans le dépôt réel : `webapp-cards-2.2-hu.yaml` déclare
   `edition: "ecommerce"`.** Le scan lit `meta.edition` et le prend au sérieux, donc
   ce fichier forme un set `ecommerce 2.2` (langue `hu` seule) au lieu de `webapp 2.2`.
   Ce n'est pas un bug de notre code — c'est une incohérence du dépôt Cornucopia. Le
   quickstart attendait 7 sets, on en obtient 8. Noté dans les résultats de validation.

## Ce que ça change pour le jeu

Avant : 8 cartes placeholder, quel que soit le choix. Après : l'utilisateur coche
les familles dans l'arbre `set → familles`, choisit la langue (défaut `fr`), et le
deck est construit uniquement from those families — 39 vraies cartes pour
`webapp:VE + webapp:AT + eop:SP`. Le moteur (`GameMaster`) n'a pas changé : il
travaille sur des `Card` au lieu de `String`, c'est tout.
