# Data Model: Style de code Java — fichiers de configuration

Phase 1 output. This feature introduces no runtime data; its "entities" are the
versioned build-configuration files and the properties they expose. Validation
rules come from the spec's FR-001…FR-010.

## Entity: `CheckstyleConfig` — `backend/config/checkstyle/checkstyle.xml`

- **Represents**: the exact Checkstyle ruleset applied to the backend.
- **Source**: vendored copy of `google_checks.xml` from `com.puppycrawl.tools:checkstyle:14.4.0`.
- **Key attributes**: 108 modules (Google Java Style, line length 100); severity
  `warning` overridable via `${org.checkstyle.google.severity}`; `SuppressionFilter`
  pointing at `checkstyle-suppressions.xml`.
- **Validation rules**: MUST be parseable by Checkstyle 14.4.0; MUST reference the
  sibling suppressions file; MUST NOT be edited without a recorded decision in
  `research.md`.
- **Relationships**: consumed by `maven-checkstyle-plugin` via `<configLocation>`.

## Entity: `CheckstyleSuppressions` — `backend/config/checkstyle/checkstyle-suppressions.xml`

- **Represents**: project-level exceptions to the Google ruleset.
- **Key attributes**: `<suppress checks="MissingJavadocMethod" .../>`,
  `<suppress checks="MissingJavadocType" .../>` (R-066); placeholder patterns for
  any future generated-code exclusion.
- **Validation rules**: every entry MUST carry a comment citing the research
  decision (R-xxx) that justifies it (constitution principle IV).
- **Relationships**: referenced from `checkstyle.xml` (`SuppressionFilter`).

## Entity: `SpotlessConfig` — `<plugin>` block in `backend/pom.xml`

- **Represents**: the formatter configuration.
- **Key attributes**: `com.diffplug.spotless:spotless-maven-plugin:3.11.0`;
  `<java><googleJavaFormat/></java>` (GJF 2.0.0 default, R-065); default target =
  `src/main/java`, `src/test/java` (excludes `target/`); `spotless:check` bound to
  `verify`.
- **Validation rules**: `spotless:check` MUST fail the build on unformatted files;
  `spotless:apply` MUST be idempotent (FR-010).
- **Relationships**: sibling of the Checkstyle plugin in `<build><plugins>`.

## Entity: `CheckstylePlugin` — `<plugin>` block in `backend/pom.xml`

- **Represents**: the lint gate.
- **Key attributes**: `maven-checkstyle-plugin:3.6.0`; plugin dependency
  `checkstyle:14.4.0`; `configLocation` → `config/checkstyle/checkstyle.xml`;
  `propertyExpansion` for the suppression filter; `consoleOutput=true`;
  `failsOnError=true`; `violationSeverity=warning`; `includeTestSourceDirectory=true`;
  execution `check` bound to `verify`.
- **Validation rules**: `./mvnw verify` MUST fail on any violation (FR-001);
  report MUST name file + line + rule (FR-009).
- **Relationships**: consumes `CheckstyleConfig` and `CheckstyleSuppressions`.

## State transitions (build lifecycle)

`compile` → `test` → **`checkstyle:check` + `spotless:check`** (both in `verify`) → `package`.
A violation at either gate → build FAILURE with a readable report. Developer runs
`./mvnw spotless:apply` → files reformatted → gates pass on next `verify`.
