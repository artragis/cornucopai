package com.cornucopai.game.agent;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.PlayedCard;
import com.cornucopai.game.model.Reaction;
import com.cornucopai.game.model.Vote;

/**
 * Happy (spec US-4): always votes YES, never has a question nor a precision.
  . " *
 * <p>Spec 004 (FR-003): deterministic agent kept for UNIT TESTS ONLY — games use LlmPlayerAgent.
" *
 * <p>Spec 004 (FR-003): deterministic agent kept for UNIT TESTS ONLY — games use LlmPlayerAgent.
 */
public class HappyAgent extends AbstractAgent {

    public HappyAgent(String name) {
        super(name);
    }

    @Override
    public Archetype archetype() {
        return Archetype.HAPPY;
    }

    @Override
    public PlayedCard playCard(Card card, String featureHtml) {
        return new PlayedCard(card.id(), "Un scénario sans gravité (" + card.suitName() + ")",
                "Tout va bien se passer, je suis sûr. " + card.description());
    }

    @Override
    public Reaction react(Leg leg, Participant cardPlayer, String featureHtml, boolean questionsAllowed) {
        return new Reaction.Pass();
    }

    @Override
    public Vote vote(Leg leg, Participant cardPlayer) {
        return Vote.YES;
    }
}
