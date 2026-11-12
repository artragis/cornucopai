# 2026-11-12 — Style de code Java : Checkstyle + Spotless (Google Java Style)

Mise en place du lint et du formatage automatiques du backend Java, en suivant le
Google Java Style Guide (spec `008-java-code-style`).

## Choix techniques

- **Checkstyle 14.4.0** via `maven-checkstyle-plugin 3.6.0`, bindé à la phase
  `verify` : le lint bloque donc le build (et la CI) sur toute violation. Le
  ruleset est le `google_checks.xml` officiel, **vendu dans le dépôt** sous
  `backend/config/checkstyle/` (plutôt que référencé depuis le jar) pour être
  versionné et pouvoir porter des suppressions projet.
- **Spotless 3.11.0** avec `googleJavaFormat` (google-java-format 2.0.0 par défaut,
  compatible JDK 25) : `spotless:apply` reformate, `spotless:check` est bindé à
  `verify` pour bloquer le code non formaté. Les deux outils couvrent
  `src/main/java` + `src/test/java`, jamais `target/`.
- **Aucun changement de CI** : `.github/workflows/ci.yml` lançait déjà
  `./mvnw verify` dans le job backend, donc les deux gates y sont automatiquement
  bloquants sur chaque PR.

## Ce qui a changé dans le code existant

La passe initiale `spotless:apply` a reformaté 122 fichiers sur 125 (indentation
2 espaces, wrapping, etc.), ramenant les violations Checkstyle d'environ 5000 à 80.
Les résidus ont été corrigés à la main : accolades manquantes, imports en étoile,
noms de tests avec majuscules consécutives, déclarations de variables déplacées au
plus près de leur usage, javadoc résumés, `default` de switch manquant, etc.

## Deux suppressions enregistrées (trade-offs)

- `MissingJavadocMethod` / `MissingJavadocType` désactivés (R-066) : la codebase
  n'a pas de baseline Javadoc, l'imposer maintenant bloquerait la feature.
- `TextBlockGoogleStyleFormattingCheck` désactivé pour `LlmPlayerAgent.java`
  (R-068) : les blocs de texte y sont des **prompts envoyés verbatim au LLM** ;
  la règle Google sur les guillemets de text block changerait les espaces du prompt.

## Leçon retenue

Vendored config + suppressions explicites > config embarquée dans un jar : on
sait exactement quelles règles sont actives et pourquoi chaque exception existe.
Et un linter n'est utile que s'il est bloquant dans le même `verify` que les
tests — sinon il finit ignoré.
