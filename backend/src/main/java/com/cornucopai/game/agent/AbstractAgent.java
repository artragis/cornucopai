package com.cornucopai.game.agent;

import com.cornucopai.game.PlayerAgent;
import com.cornucopai.game.model.Answer;

/** Shared base: name handling and the default "Je sais" answer. */
abstract class AbstractAgent implements PlayerAgent {

    private final String name;

    protected AbstractAgent(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Answer answerQuestion(String question) {
        return new Answer.Knows("Je sais : la réponse à « " + question + " » est documentée dans l'analyse de risques.");
    }
}
