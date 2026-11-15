<!--
Sync Impact Report:
- Version change: 1.2.0 → 1.3.0 (MINOR: two standing rules added to Development
  Workflow — CI on GitHub Actions, Quarkus debug mode always available; no
  principles removed or redefined)
- Modified principles: none (all five core principles preserved unchanged)
- Added sections: none
- Removed sections: none
- Follow-up TODOs: none
-->

# CornucopAI Constitution

CornucopAI is a playground where AI agents play games of Cornucopia, the card game
used for threat modeling. The backend runs on Quarkus with Java 25 and LangChain4j
to orchestrate the agents and game logic. The frontend, built with Vue 3 and Vuetify,
lets users view games in progress and create new workshops (ateliers).

## Core Principles

### I. Specification-Driven Development
Every feature MUST begin with a written specification (spec.md) approved before any
implementation starts. Specifications are the single source of truth for what the
system does; code, tests, and docs MUST trace back to them. Ambiguities in a spec
MUST be resolved via clarification before planning begins.

### II. Test-First (NON-NEGOTIABLE)
TDD is mandatory: tests are written, reviewed, and approved before implementation.
The Red-Green-Refactor cycle is strictly enforced. No production code may be merged
without passing tests that were written first. Test coverage gates MUST be defined
per feature in the plan and MUST be met before the feature is considered done.

### III. Modularity and Single Responsibility
Every module, function, and component MUST have a single, clearly stated purpose.
Modules MUST be self-contained, independently testable, and loosely coupled.
Organizational-only abstractions without a concrete use case are prohibited.

### IV. Documentation and Transparency
All public APIs, modules, and non-trivial logic MUST be documented. Decisions with
significant trade-offs MUST be recorded with rationale in the relevant spec, plan,
or an ADR-style note. Structured logging MUST be used so behaviour is observable
and debuggable in any environment.

### V. Simplicity (YAGNI)
Start with the simplest solution that satisfies the specification. Do not add
features, abstractions, or optimizations that are not required by the current spec.
Complexity MUST be justified in writing when it is introduced.

## Technology & Quality Standards
- Repository layout: the backend lives in `backend/` and the frontend lives in
  `frontend/` at the repository root. No other top-level source directories are
  permitted without an approved amendment.
- Backend: Quarkus on Java 25, with LangChain4j for LLM/agent orchestration. These
  are the mandated backend technologies; any deviation requires an approved
  migration plan recorded in the constitution.
- Frontend: Vue 3 with Vuetify as the component library, used for viewing games
  and creating workshops. No other UI framework may be introduced without an
  approved amendment.
- All code MUST pass linting and static analysis configured at the repo level before
  merge.
- Dependencies MUST be pinned and their licenses reviewed; adding a dependency is a
  deliberate decision recorded in the plan.
- Security-sensitive code (auth, data handling, network I/O, LLM prompt handling)
  MUST receive focused review and MUST include tests for failure and abuse cases.

## Development Workflow
- Work proceeds in phases: Specify → Clarify → Plan → Tasks → Implement → Validate.
  Each phase MUST produce its artifact (spec.md, plan.md, tasks.md) before the next
  begins.
- Every change MUST be traceable to a task in tasks.md; ad-hoc changes outside the
  task list are not permitted.
- Code review is required for every merge. Reviewers MUST verify compliance with
  this constitution, not just correctness of the code.
- Quality gates (tests, lint, type checks, coverage) MUST pass in CI before a
  change can be merged. CI runs on GitHub Actions (the project is exposed on
  GitHub); workflows live in `.github/workflows/` and MUST run the same commands
  as local development.
- The Quarkus application MUST always remain launchable in debug mode (JDWP
  debugger, e.g. port 5005 via `./mvnw quarkus:dev`) to facilitate development
  and debugging. No build or configuration change may remove this ability.

## Governance
This constitution supersedes all other practices and conventions in the project.
Amendments require: a written proposal, review by all active contributors, a
migration plan for any change that affects existing workflows, and a semantic
version bump of this document. All pull requests and reviews MUST verify
compliance with this constitution. Complexity beyond what the constitution
requires MUST be justified. Runtime development guidance lives in the
templates and workflows under `.specify/`.

**Version**: 1.3.0 | **Ratified**: 2026-11-07 | **Last Amended**: 2026-11-07
