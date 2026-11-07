package com.cornucopai.game.agent;

import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.PlayedCard;
import com.cornucopai.game.model.Reaction;
import com.cornucopai.game.model.Vote;

/**
 * Happy (spec US-4): always votes YES, never has a question nor a precision.
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
    public PlayedCard playCard(String card, String featureHtml) {
        return new PlayedCard(card, "Un scénario sans gravité", "Tout va bien se passer, je suis sûr.");
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
