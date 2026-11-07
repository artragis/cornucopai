# Data Model: Project Bootstrap

The bootstrap feature introduces **no domain entities** — it wires build tooling
and a smoke endpoint/page only. There is no persistence layer at this stage
(storage: N/A in the plan).

## Artifacts (non-domain, for reference)

- **Backend build artifact**: the runnable output of `./mvnw package`
  (Quarkus fast-jar under `backend/target/quarkus-app/`).
- **Frontend build artifact**: the optimized static assets emitted by
  `npm run build` into `frontend/dist/`.

## Future entities (out of scope)

Cornucopia domain entities (workshop, game, player/agent, card, threat) will be
defined when the first game feature is specified — not here.
