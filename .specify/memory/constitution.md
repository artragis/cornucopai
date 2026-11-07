<!--
Sync Impact Report:
- Version change: 1.1.0 → 1.2.0 (MINOR: repository layout pinned in Technology &
  Quality Standards; new "Project Diary" section added; no principles removed or
  redefined)
- Modified principles: none (all five core principles preserved unchanged)
- Added sections: Project Diary — Zeste de Savoir Billet
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
  change can be merged.

## Project Diary — Zeste de Savoir Billet
As decisions are made and lessons are learned throughout the project (LangChain4j
usage, agent orchestration, spec-kit workflow, secure development), the team MUST
incrementally draft passages of a billet (blog post) intended for publication on
Zeste de Savoir. The billet MUST follow the zmarkdown norms and the billet's
`manifest.json` with `type=OPINION`.

The billet is built around the following sources and goals:
- Sources: the Zeste de Savoir article on running a threat-modeling workshop with
  Cornucopia (articles/5088); the real difficulties of organizing threat-modeling
  workshops; noteworthy critical vulnerabilities in the news (e.g. the Linux
  kernel flaw present since 2016); and the debate on AI in publishing that took
  place on Zeste de Savoir.
- Goals: improve mastery of LangChain concepts; test a spec-kit-style workflow
  end-to-end starting from a fresh project; compare without prejudice a
  human-written billet with an AI-written one; observe the evolution of AI in
  secure development beyond the spectacular vulnerability discovery/fixing demos.

Each significant decision or learning MUST be captured as a diary passage at the
time it happens, so the billet is assembled progressively rather than written
retrospectively.

## Governance
This constitution supersedes all other practices and conventions in the project.
Amendments require: a written proposal, review by all active contributors, a
migration plan for any change that affects existing workflows, and a semantic
version bump of this document. All pull requests and reviews MUST verify
compliance with this constitution. Complexity beyond what the constitution
requires MUST be justified. Runtime development guidance lives in the
templates and workflows under `.specify/`.

**Version**: 1.2.0 | **Ratified**: 2026-11-07 | **Last Amended**: 2026-11-07
