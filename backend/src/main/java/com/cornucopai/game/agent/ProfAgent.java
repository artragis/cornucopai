package com.cornucopai.game.agent;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.PlayedCard;
import com.cornucopai.game.model.Reaction;
import com.cornucopai.game.model.Vote;

/**
 * Prof (spec US-4): short precisions to Happy, a question before a precision against Grumpy, passes
 * against Greedy; votes NO to Grumpy, YES to the others. . " *
 *
 * <p>Spec 004 (FR-003): deterministic agent kept for UNIT TESTS ONLY — games use LlmPlayerAgent. "
 * *
 *
 * <p>Spec 004 (FR-003): deterministic agent kept for UNIT TESTS ONLY — games use LlmPlayerAgent.
 */
public class ProfAgent extends AbstractAgent {

  public ProfAgent(String name) {
    super(name);
  }

  @Override
  public Archetype archetype() {
    return Archetype.PROF;
  }

  @Override
  public PlayedCard playCard(Card card, String featureHtml) {
    return new PlayedCard(
        card.id(),
        "Scénario analysé en cours (" + card.suitName() + " " + card.value() + ")",
        "Explication structurée et concise. " + card.description());
  }

  @Override
  public Reaction react(
      Leg leg, Participant cardPlayer, String featureHtml, boolean questionsAllowed) {
    return switch (cardPlayer.archetype()) {
      case HAPPY -> new Reaction.Precision("Précision courte.");
      case GRUMPY ->
          questionsAllowed
              ? new Reaction.Question("Prof demande : quelle est la cause racine exacte ?")
              : new Reaction.Precision("Précision courte.");
      case GREEDY -> new Reaction.Pass();
      case PROF -> new Reaction.Precision("Précision courte.");
    };
  }

  @Override
  public Vote vote(Leg leg, Participant cardPlayer) {
    return cardPlayer.archetype() == Archetype.GRUMPY ? Vote.NO : Vote.YES;
  }
}
