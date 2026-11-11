# Diary entry — spec 006 (scoreboard Cornucopia), 2026-11-11

Constitution "Project Diary" passage for the billet.

## What was decided

- **Tours (tricks) with rotating leader** (clarification 2026-11-10): tour 1 leader is
  drawn at random; the winner of each tour (highest card of the couleur majeure) leads
  the next; if every leg of a tour is REJECTED, the next leader is drawn at random.
  Invariant: every player still holding cards plays exactly once per tour. This is the
  authentic Cornucopia rule and it makes the score strategic.
- **Couleur majeure** = suitId of the first non-WC card of the tour; a WC led shifts the
  decision to the next non-WC card (cascade); a WC played later is always assimilated to
  the couleur majeure (joker) for scoring.
- **Scoring**: +1 per VALIDATED leg in the major suit, +1 bonus per tour to the highest
  major-suit card (canonical order 2…10 < J < Q < K < A, never alphabetical), 0 for
  REJECTED. Winner = max score, ties listed. No bonus when all legs of a tour REJECTED.
- **Card values are unique within a suit** → the bonus always has exactly one winner
  (clarification: the equal-value case is impossible).
- **Schemas are additive** (leg v3.0.0, API v0.6.0): legacy ateliers read with null/empty
  tour fields, the UI shows "données partielles" — no migration (YAGNI).
- **Frontend**: dedicated route `/ateliers/:id` via vue-router (the only new dependency,
  recorded per constitution); scoreboard above all legs; polling every 5 s while the
  atelier is unfinished, stops when `finishedAt` is set.
- Side fix during baseline: the vote token cap was raised from 16 to 100 (user decision —
  16 broke real LLM answers too often); `VoteCapTest` updated to match.

## Lessons

- Grouping legs into tours inside the GameMaster required finalizing legs at tour close
  (majorSuit/points are only known once the tour ends) — legs are persisted twice: once
  at play time (progress visible to polling) and once at tour close (final score fields).
- The "all REJECTED → random next leader" rule only concerns the leader election; the
  bonus is skipped too when no leg is VALIDATED, keeping "winner" and "bonus" consistent.
