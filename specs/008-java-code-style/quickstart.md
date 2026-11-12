# Quickstart: validation du style de code Java (Checkstyle + Spotless)

Runnable validation scenarios for feature `008-java-code-style`. They prove the
feature end-to-end: format command works and is idempotent, lint fails on a
deliberate violation and passes on clean code, and the full `verify` (what CI
runs) goes green. Decisions in `research.md` (R-063…R-067); entities in
`data-model.md`; command contract in `contracts/build-commands.md`.

Prerequisites:

- JDK 25 on PATH (same as CI's Temurin 25).
- From the repo root, all commands run in `backend/` via the wrapper: `cd backend`.

## 1. Initial format pass (one-time, before gates go live)

```bash
cd backend && ./mvnw spotless:apply
```

Expected: exit 0; `git status` shows the reformatted Java files under
`src/main/java` and `src/test/java` (Google Java Style). Re-running it changes
nothing (idempotence, FR-010 / SC-003: completes < 60 s).

## 2. Lint fails on a deliberate violation

Introduce a violation (e.g. a trailing whitespace or a `snake_case` variable) in
any file under `src/main/java`, then:

```bash
cd backend && ./mvnw checkstyle:check
```

Expected: non-zero exit; console names the file, line, and violated rule (FR-009,
SC-002: fails within 30 s of the check starting). Revert the violation.

## 3. Format gate fails on unformatted code

Manually break the formatting of one file (bad indentation), then:

```bash
cd backend && ./mvnw spotless:check
```

Expected: non-zero exit listing the file. Restore with `./mvnw spotless:apply`.

## 4. Full build gate (what CI runs) is green

```bash
cd backend && ./mvnw verify
```

Expected: exit 0 — `checkstyle:check` and `spotless:check` pass, all existing
tests pass, Quarkus build succeeds (SC-001: 100 % of Java files lint-clean).
This is exactly the command `.github/workflows/ci.yml` runs in the `backend` job,
so CI enforces style with no workflow change (FR-007, SC-004).

## 5. Debug mode still available (constitution)

```bash
cd backend && ./mvnw quarkus:dev
```

Expected: app starts on 8080 with JDWP on 5005; style plugins do not interfere.
Stop it after confirmation.
