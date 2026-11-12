# Contracts: build commands exposed by feature 008-java-code-style

Phase 1 output. This feature's external interface is the set of Maven commands that
developers and CI run. They are the contract: their behavior is asserted by the
spec's success criteria (SC-001…SC-005) and exercised in `quickstart.md`.

All commands run from `backend/` and use the Maven wrapper (`./mvnw`).

## `./mvnw verify` (CI and local gate)

- **Behavior**: runs the full lifecycle including `checkstyle:check` and
  `spotless:check` (both bound to `verify`).
- **Success**: exit code 0, no Checkstyle violations, all files Spotless-clean,
  existing tests pass.
- **Failure**: non-zero exit if any style violation exists; console/report names
  file, line, and rule for each Checkstyle violation (FR-009); Spotless lists
  unformatted files.

## `./mvnw spotless:apply`

- **Behavior**: reformats every Java file under `src/main/java` and `src/test/java`
  with google-java-format 2.0.0 (Google Java Style). Idempotent (FR-010).
- **Success**: exit 0; working tree contains the reformatted files.
- **Scope**: never touches `target/`.

## `./mvnw spotless:check`

- **Behavior**: like `verify`'s Spotless gate but standalone; exit non-zero if any
  file would be reformatted by `spotless:apply`.

## `./mvnw checkstyle:check`

- **Behavior**: standalone Checkstyle run against `src/main/java` + `src/test/java`
  with `backend/config/checkstyle/checkstyle.xml`; exit non-zero on violations
  (severity ≥ warning).

## `./mvnw quarkus:dev`

- **Unchanged contract (constitution)**: debug mode with JDWP on port 5005 must
  remain available; this feature must not break it.
