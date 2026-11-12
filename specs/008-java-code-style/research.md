# Research: Style de code Java — lint (Checkstyle) et formatage automatique

Phase 0 output. Each unknown from the Technical Context of `plan.md` is resolved
below with a decision, a rationale, and the alternatives considered. Binding for
Phase 1 and `/speckit-tasks`. Decision numbering continues after spec 007
(R-062), so it starts at **R-063**. Versions verified against Maven Central
`maven-metadata.xml` on 2026-11-12.

## R-063 — Formatter plugin: **Spotless 3.11.0** (not fmt-maven-plugin)

- **Decision**: Use `com.diffplug.spotless:spotless-maven-plugin:3.11.0` (latest
  release, 2026-11-02) with the `<java><googleJavaFormat/></java>` step, which
  defaults to **google-java-format 2.0.0**. Wire `spotless:check` into the
  `verify` phase (fails the build on unformatted code) and expose `spotless:apply`
  for developers.
- **Rationale**: Spotless is the actively maintained industry standard; it offers
  both `apply` (reformat) and `check` (CI gate) goals, supports pinning the GJF
  version, and its 3.11.0 release explicitly supports google-java-format 2.0.0
  (CHANGES.md: "Bump default google-java-format 1.36.1 -> 2.0.0"). GJF 2.0.0 runs
  on JDK 21+ (we run JDK 25) and can format Java 25 sources because the running
  JDK (25) ≥ source level (25). Spotless's default Java target covers
  `src/main/java` and `src/test/java` and excludes `target/`.
- **Alternatives considered**: (a) `com.spotify.fmt:fmt-maven-plugin:2.29` —
  simpler but less maintained; its `format` goal needs GJF pinned as a plugin
  dependency and its check goal is less convenient to bind. Rejected in favor of
  Spotless's richer, better-supported integration. (b) google-java-format CLI
  only (no Maven plugin) — rejected: not repo-level, not enforced in CI.

## R-064 — Checkstyle config: **vendor `google_checks.xml` from Checkstyle 14.4.0** under `backend/config/checkstyle/`

- **Decision**: Use `org.apache.maven.plugins:maven-checkstyle-plugin:3.6.0` with
  plugin dependency `com.puppycrawl.tools:checkstyle:14.4.0` (latest; confirmed
  buildable on JDK 21–25 and parsing all Java 25 features). Copy the official
  `google_checks.xml` shipped inside `checkstyle-14.4.0.jar` into
  `backend/config/checkstyle/checkstyle.xml`, add
  `backend/config/checkstyle/checkstyle-suppressions.xml`, and point the plugin at
  them via `<configLocation>` + `<propertyExpansion>` for the suppression filter.
  Bind `checkstyle:check` to the `verify` phase with `consoleOutput=true`,
  `failsOnError=true`, `includeTestSourceDirectory=true`.
- **Rationale**: Vendoring makes the exact ruleset version-controlled in the repo
  (FR-008) and lets us attach project suppressions — referencing `google_checks.xml`
  from the JAR would leave the rules outside the repo and harder to suppress. The
  plugin checks `src/main/java` (+ `src/test/java` when
  `includeTestSourceDirectory=true`) and never `target/`.
- **Alternatives considered**: (a) `<configLocation>google_checks.xml</configLocation>`
  resolving from the Checkstyle JAR — simplest but not versioned in-repo and its
  `SuppressionFilter` property (`org.checkstyle.google.suppressionfilter.config`)
  is awkward to feed from the Maven plugin; rejected for FR-008 compliance.
  (b) `sun_checks.xml` — rejected: the user explicitly wants Google style.

## R-065 — google-java-format version: **2.0.0 (Spotless 3.11.0 default)**

- **Decision**: Do not pin a custom GJF version; rely on Spotless 3.11.0's default
  `googleJavaFormat` = **2.0.0**. Keep the default `<cleanup>` (enabled): GJF 2.0.0's
  non-whitespace cleanups (redundant semicolons, empty annotation parens, lambda
  parens) are Google-style compliant.
- **Rationale**: GJF 2.0.0 (released 2026-10-30) is the current Google style engine,
  fully supported by Spotless 3.11.0, and runs on JDK 25. Pinning 1.37.0 would
  also work but forgoes 2.0.0's layout improvements.
- **Alternatives considered**: pin `googleJavaFormat('1.37.0')` — viable fallback if
  a Spotless/GJF-2.0 incompatibility ever appears on JDK 25; not needed now.

## R-066 — Recorded trade-off: relax `MissingJavadocMethod` / `MissingJavadocType`

- **Decision**: In the vendored `checkstyle.xml`, disable the `MissingJavadocMethod`
  and `MissingJavadocType` modules (via `checkstyle-suppressions.xml`). All other
  ~106 google_checks modules stay active (line length 100, import order, naming,
  whitespace, braces, etc.).
- **Rationale**: The existing ~125-file codebase has no Javadoc baseline; enforcing
  missing-Javadoc now would produce hundreds of violations and block this feature.
  Formatting/style consistency is the goal of this spec; Javadoc coverage is a
  separate, later effort. Per constitution principle IV, this significant trade-off
  is recorded here and in the suppressions file.
- **Alternatives considered**: keep the rules and write Javadoc everywhere —
  rejected as out of scope for this feature (violates YAGNI, principle V).

## R-068 — Suppress `TextBlockGoogleStyleFormattingCheck` for `LlmPlayerAgent.java`

- **Decision**: add `<suppress checks="TextBlockGoogleStyleFormattingCheck" files="LlmPlayerAgent\.java"/>`
  to `backend/config/checkstyle/checkstyle-suppressions.xml`. The Google text-block
  rule (opening/closing `"""` alone on their line, vertically aligned) is incompatible
  with preserving the exact prompt strings: those text blocks are prompt DATA sent
  verbatim to the LLM, and reformatting them would change leading whitespace per line.
  google-java-format (Spotless) already leaves these blocks untouched, so there is no
  formatter/linter conflict — only this one check conflicts with prompt preservation.
  Existing prompt tests use substring assertions (`contains`), not exact equality.
- **Rationale**: code style applies to code; prompt templates are data. Recorded per
  constitution principle IV.
- **Alternatives considered**: reformat the text blocks to satisfy the check —
  rejected because it silently changes the prompts sent to the LLM.

## R-067 — CI integration: **no workflow change needed**

- **Decision**: `.github/workflows/ci.yml` already runs `./mvnw verify` in the
  `backend` job (Temurin 25). Binding both `checkstyle:check` and `spotless:check`
  to the `verify` phase makes CI enforce style automatically (FR-007, SC-004).
- **Rationale**: Constitution requires CI to run the same commands as local dev;
  `verify` is already that command.
- **Alternatives considered**: add a separate CI job/step for lint — rejected:
  duplicate execution, slower feedback, and the constitution says workflows MUST
  run the same commands as local development.
