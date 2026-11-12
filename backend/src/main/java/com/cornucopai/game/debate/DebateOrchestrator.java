package com.cornucopai.game.debate;

import com.cornucopai.game.PlayerAgent;
import com.cornucopai.game.llm.LegContext;
import com.cornucopai.game.model.DebateTurn;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Runs the bounded pre-vote debate (spec 007 US5, FR-013, R-050/R-052). Each voter agent emits one
 * free-text argument per round, up to {@value #MAX_ROUNDS} rounds. The result is stored in the
 * {@link LegContext} so the vote prompts can include it. Enabled by default (clarification
 * 2026-11-11), disableable via {@code cornucopai.debate.enabled=false}.
 *
 * <p>Implemented as a plain bounded loop over the voter agents — the observable behavior is
 * identical to an agentic workflow and far simpler (YAGNI, constitution V; the {@code
 * langchain4j-agentic} {@code DebatePlanner} was studied in research R-050 and remains available if
 * a richer orchestration is needed later).
 */
public class DebateOrchestrator {

  private static final Logger LOG = Logger.getLogger(DebateOrchestrator.class);

  /** Max debate rounds per leg (bounded exchange, US5 scenario 1). */
  public static final int MAX_ROUNDS = 2;

  /**
   * Runs the bounded pre-vote debate among the voters.
   *
   * @param leg the leg on the table
   * @param voters the voter agents (all players except the card player)
   * @param cardPlayer the card player
   * @param legContext per-game context where the turns are stored
   * @return the debate turns (one per voter per round)
   */
  public List<DebateTurn> runDebate(
      Leg leg, List<PlayerAgent> voters, Participant cardPlayer, LegContext legContext) {
    List<DebateTurn> turns = new ArrayList<>();
    for (int round = 1; round <= MAX_ROUNDS; round++) {
      final int currentRound = round;
      for (PlayerAgent voter : voters) {
        String argument;
        try {
          argument = voter.debateArgument(leg, cardPlayer);
        } catch (Exception e) {
          LOG.warnf(e, "Debate argument failed for %s, skipped", voter.name());
          continue;
        }
        if (argument != null && !argument.isBlank()) {
          turns.add(new DebateTurn(voter.name(), argument, currentRound));
          LOG.infof(
              "Débat tour %d — %s : %s",
              currentRound,
              voter.name(),
              argument.length() > 80 ? argument.substring(0, 80) + "…" : argument);
        }
      }
    }
    legContext.setDebateTurns(turns);
    return turns;
  }
}
