package com.cornucopai.game.agent;

import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.PlayedCard;
import com.cornucopai.game.model.Reaction;
import com.cornucopai.game.model.Vote;

/**
 * Grumpy (spec US-4): always votes NO and always has a question to ask before
 * passing his turn.
 */
public class GrumpyAgent extends AbstractAgent {

    public GrumpyAgent(String name) {
        super(name);
    }

    @Override
    public Archetype archetype() {
        return Archetype.GRUMPY;
    }

    @Override
    public PlayedCard playCard(String card, String featureHtml) {
        return new PlayedCard(card, "Encore un scénario qui craint", "Franchement, cette feature est mal fichue.");
    }

    @Override
    public Reaction react(Leg leg, Participant cardPlayer, String featureHtml, boolean questionsAllowed) {
        if (questionsAllowed) {
            return new Reaction.Question("Quelle est selon toi la pire conséquence de ce scénario ?");
        }
        return new Reaction.Pass();
    }

    @Override
    public Vote vote(Leg leg, Participant cardPlayer) {
        return Vote.NO;
    }
}
