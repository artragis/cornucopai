# Feature Specification: Style de code Java — lint (Checkstyle) et formatage automatique (Google Java Style)

**Feature Branch**: `008-java-code-style`

**Created**: 2026-11-12

**Status**: Draft

**Input**: User description: "On va ajouter un linter java (checkstyle) et un formatteur java. On va suivre le code de style de Google. Quelques éléments intéressants dans cet article : https://aru-sha4.medium.com/java-check-style-and-formatting-using-maven-a1a1b4e6e10a"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Vérification automatique du style de code à chaque build (Priority: P1)

En tant que développeur sur le projet CornucopAI, je veux que le build Maven échoue si mon code Java ne respecte pas le style Google, afin de garantir une base de code uniforme sans revue manuelle de style.

**Why this priority**: C'est le socle de la feature. Sans vérification bloquante dans le build, le style ne sera pas respecté de manière fiable. La constitution du projet exige déjà que « tout code passe le linting et l'analyse statique configurés au niveau du dépôt avant merge ».

**Independent Test**: Introduire volontairement une violation de style (ex. une accolade mal placée ou un nom de variable non conforme) dans un fichier Java, lancer `./mvnw verify` dans `backend/`, et constater que le build échoue avec un message indiquant le fichier et la règle violée.

**Acceptance Scenarios**:

1. **Given** un fichier Java conforme au style Google, **When** on lance `./mvnw verify` dans `backend/`, **Then** le build réussit et l'étape de linting passe sans erreur.
2. **Given** un fichier Java avec une violation de style (ex. indentation incorrecte, `import` inutilisé, nom de variable en `snake_case`), **When** on lance `./mvnw verify`, **Then** le build échoue et le rapport indique précisément le fichier, la ligne et la règle Checkstyle violée.
3. **Given** le projet en l'état actuel, **When** on applique le lint pour la première fois, **Then** soit le code existant est déjà conforme, soit les violations sont corrigées (via le formatteur automatique, voir US2) avant que le lint ne devienne bloquant.
4. **Given** un rapport de violations, **When** le développeur corrige les problèmes, **Then** un nouveau `./mvnw verify` passe avec succès.

---

### User Story 2 - Formatage automatique du code Java (Priority: P1)

En tant que développeur, je veux pouvoir lancer une commande qui reformate automatiquement tout le code Java selon le style Google, afin de ne plus avoir à me soucier manuellement de la mise en forme (indentation, retours à la ligne, espacement).

**Why this priority**: Le formatage automatique élimine les débats de style en revue de code et prépare le code à passer le lint. C'est le complément indispensable du linter.

**Independent Test**: Modifier manuellement la mise en forme d'un fichier Java (mauvaise indentation, espaces parasites), lancer la commande de formatage, et constater que le fichier est automatiquement remis en conformité Google Java Style.

**Acceptance Scenarios**:

1. **Given** un fichier Java mal formaté, **When** on lance la commande de formatage (ex. `./mvnw fmt:format` ou `./mvnw spotless:apply`), **Then** le fichier est automatiquement reformaté selon Google Java Style.
2. **Given** l'ensemble du code source du backend, **When** on lance le formatage, **Then** tous les fichiers sont traités et le résultat est conforme au style Google (vérifiable ensuite par le lint).
3. **Given** un fichier déjà conforme, **When** on relance le formatage, **Then** le fichier n'est pas modifié (idempotence).
4. **Given** le formatage appliqué, **When** on lance `./mvnw verify`, **Then** le lint passe sans erreur de formatage.

---

### User Story 3 - Intégration dans la CI GitHub Actions (Priority: P2)

En tant que mainteneur du projet, je veux que la CI vérifie automatiquement le style de code à chaque push/PR, afin qu'aucun code non conforme ne soit mergé sur `main`.

**Why this priority**: La constitution exige que « les quality gates (tests, lint, type checks, couverture) passent en CI avant qu'un changement puisse être mergé ». La CI existe déjà (`.github/workflows/ci.yml`) et lance `./mvnw verify` — le lint y sera donc automatiquement inclus s'il est branché sur le cycle de build.

**Independent Test**: Ouvrir une PR avec une violation de style et constater que la CI échoue ; ouvrir une PR avec un code conforme et constater que la CI passe.

**Acceptance Scenarios**:

1. **Given** une pull request contenant une violation de style Java, **When** la CI s'exécute, **Then** le job backend échoue à l'étape de vérification du style.
2. **Given** une pull request avec un code conforme, **When** la CI s'exécute, **Then** le job backend passe intégralement (lint + tests + build).
3. **Given** le workflow CI existant, **When** on ajoute le lint au build Maven, **Then** aucune modification du fichier `.github/workflows/ci.yml` n'est nécessaire (le lint est exécuté via `./mvnw verify`).

---

### User Story 4 - Configuration centralisée et versionnée du style (Priority: P2)

En tant que développeur, je veux que la configuration du style (règles Checkstyle + formatteur) soit versionnée dans le dépôt et centralisée, afin que tous les contributeurs et tous les environnements (local, CI, IDE) appliquent exactement les mêmes règles.

**Why this priority**: Une configuration dispersée ou non versionnée conduit à des divergences entre développeurs. La constitution exige des règles configurées « au niveau du dépôt ».

**Independent Test**: Cloner le dépôt sur une machine neuve, lancer `./mvnw verify`, et constater que les mêmes règles de style sont appliquées sans configuration manuelle.

**Acceptance Scenarios**:

1. **Given** un nouveau clone du dépôt, **When** on lance `./mvnw verify` sans configuration manuelle, **Then** le lint s'exécute avec les règles Google configurées dans le projet.
2. **Given** la configuration Checkstyle, **When** on l'inspecte, **Then** elle référence explicitement le style Google (fichier de config `google_checks.xml` ou équivalent embarqué dans le projet).
3. **Given** un développeur utilisant un IDE, **When** il importe le projet, **Then** il peut utiliser la même configuration de formatage (Google Java Format) que le build Maven.

---

### Edge Cases

- Que se passe-t-il si un fichier généré (ex. code généré par Quarkus) ne respecte pas le style ? → Les répertoires de code généré (`target/`) sont exclus du lint et du formatage.
- Que se passe-t-il si le formatage entre en conflit avec une règle Checkstyle ? → Le formatage (Google Java Format) est appliqué en premier, puis le lint vérifie les règles que le formatteur ne couvre pas (imports, javadoc, etc.). Les deux sont cohérents car tous deux suivent Google Java Style.
- Comment gérer la transition du code existant ? → Une passe de formatage automatique est appliquée sur tout le code existant avant que le lint ne devienne bloquant ; les violations résiduelles (non couvertes par le formatteur) sont corrigées manuellement.
- Le lint s'applique-t-il aussi aux tests ? → Oui, les fichiers de test (`src/test/`) sont inclus dans le périmètre du lint et du formatage.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Le build Maven du backend (`backend/pom.xml`) MUST intégrer un linter Checkstyle exécuté pendant la phase `verify` (ou une phase antérieure), de sorte que `./mvnw verify` échoue en cas de violation.
- **FR-002**: La configuration Checkstyle MUST suivre le code style Google (basée sur `google_checks.xml` du projet Checkstyle, éventuellement avec des ajustements minimes documentés).
- **FR-003**: Le build Maven MUST intégrer un plugin de formatage automatique (ex. `spotless-maven-plugin` avec `googleJavaFormat`, ou `fmt-maven-plugin`) permettant de reformater le code via une commande Maven dédiée (ex. `./mvnw spotless:apply`).
- **FR-004**: Le formatage automatique MUST couvrir l'ensemble des sources Java du backend (`src/main/java` et `src/test/java`), à l'exclusion du code généré (`target/`).
- **FR-005**: Le lint et le formatage MUST être configurés de manière cohérente (tous deux selon Google Java Style) pour éviter les conflits.
- **FR-006**: Une passe de formatage initiale MUST être appliquée sur tout le code Java existant afin que le lint puisse devenir bloquant sans violations préexistantes.
- **FR-007**: La vérification du style MUST faire partie du job CI backend existant (qui exécute déjà `./mvnw verify`), sans nécessiter de job CI séparé.
- **FR-008**: Les règles de style MUST être documentées dans le dépôt (fichier de configuration versionné + éventuellement une courte section dans le README ou le plan) pour que tout contributeur puisse les consulter.
- **FR-009**: Le lint MUST produire un rapport lisible indiquant le fichier, la ligne et la règle violée pour chaque infraction.
- **FR-010**: Le plugin de formatage MUST être idempotent (relancer le formatage sur du code déjà conforme ne produit aucun changement).

### Key Entities *(include if feature involves data)*

- **Configuration Checkstyle**: fichier de règles (XML) définissant les conventions Google appliquées au code Java ; versionné dans le dépôt (ex. `backend/config/checkstyle/google_checks.xml` ou embarqué via la dépendance Checkstyle officielle).
- **Configuration Format**: paramétrage du plugin de formatage (version de Google Java Format, périmètre des fichiers) dans `backend/pom.xml`.
- **Rapport de lint**: sortie du build indiquant les violations détectées (fichier, ligne, règle, sévérité).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100 % du code Java du backend passe le lint Checkstyle (Google Java Style) sans aucune violation après la passe de formatage initiale.
- **SC-002**: Tout commit introduisant une violation de style fait échouer `./mvnw verify` en moins de 30 secondes après le début de la phase de lint.
- **SC-003**: La commande de formatage automatique retraite l'ensemble des fichiers Java du backend en moins de 60 secondes sur une machine de développement standard.
- **SC-004**: La CI GitHub Actions (job backend) intègre la vérification de style sans modification du workflow (via `./mvnw verify`) et échoue sur toute PR contenant une violation.
- **SC-005**: Aucun débat de revue de code sur la mise en forme (indentation, espacement, retours à la ligne) n'est nécessaire après l ado ption du formatage automatique.

## Assumptions

- Le style de code retenu est le **Google Java Style Guide** (https://google.github.io/styleguide/javaguide.html), comme demandé par l'utilisateur.
- Le backend reste un projet **Maven** (`pom.xml`) — aucun passage à Gradle n'est envisagé.
- Le plugin de formatage retenu sera **Spotless** (`spotless-maven-plugin`) avec `googleJavaFormat`, ou alternativement **`fmt-maven-plugin`** (Spotify) — le choix définitif sera tranché dans le plan en fonction de la compatibilité avec Java 25 et Quarkus.
- Checkstyle utilisera la configuration officielle `google_checks.xml` fournie par le projet Checkstyle, éventuellement surchargée par un fichier local pour des ajustements mineurs (ex. longueur de ligne).
- Le code généré par Quarkus (dans `target/`) est exclu du lint et du formatage.
- Les fichiers de test sont inclus dans le périmètre.
- La CI existante (`.github/workflows/ci.yml`) n'a pas besoin d'être modifiée car elle exécute déjà `./mvnw verify`.
