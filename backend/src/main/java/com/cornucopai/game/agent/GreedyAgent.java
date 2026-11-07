package com.cornucopai.game.agent;

import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.PlayedCard;
import com.cornucopai.game.model.Reaction;
import com.cornucopai.game.model.Vote;

/**
 * Greedy (spec US-4): gives an excessively long explanation, adds excessively
 * long precisions, votes YES for Happy and NO for Grumpy.
 */
public class GreedyAgent extends AbstractAgent {

    private static final String LONG_FILLER = "bla bla bla bla bla bla bla bla bla bla ";

    public GreedyAgent(String name) {
        super(name);
    }

    @Override
    public Archetype archetype() {
        return Archetype.GREEDY;
    }

    @Override
    public PlayedCard playCard(String card, String featureHtml) {
        String longExplanation = "Explication beaucoup trop longue : " + LONG_FILLER.repeat(30);
        return new PlayedCard(card, "Un scénario qui en dit long", longExplanation);
    }

    @Override
    public Reaction react(Leg leg, Participant cardPlayer, String featureHtml, boolean questionsAllowed) {
        return new Reaction.Precision("Précision excessivement longue : " + LONG_FILLER.repeat(30));
    }

    @Override
    public Vote vote(Leg leg, Participant cardPlayer) {
        // Happy votes for him, so he votes YES for Happy; he votes NO for Grumpy
        return cardPlayer.archetype() == Archetype.GRUMPY ? Vote.NO : Vote.YES;
    }
}
