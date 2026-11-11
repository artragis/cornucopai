package com.cornucopai.api.dto;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.llm.AgentLlmConfig;
import com.cornucopai.game.model.Atelier;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.SetResolution;
import com.cornucopai.game.model.Tour;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Full atelier detail returned by POST /api/ateliers and GET /api/ateliers/{id}.
 *  v0.6.0 (spec 006): + tours, scores, winner. v0.7.0 (spec 007): + projectFacts. */
public record AtelierResponse(String id, Instant createdAt, String featureHtml, int playerCount,
                              List<Participant> players, List<String> turnOrder, Instant finishedAt,
                              String language, List<String> selectedFamilies, List<Card> deck,
                              List<SetResolution> sets, List<AgentLlmConfig> agentConfigs, List<Leg> legs,
                              List<Tour> tours, Map<String, Integer> scores, List<String> winner,
                              List<com.cornucopai.game.memory.ProjectFact> projectFacts) {

    public static AtelierResponse from(Atelier atelier, List<Leg> legs) {
        return new AtelierResponse(atelier.id(), atelier.createdAt(), atelier.featureHtml(),
                atelier.playerCount(), atelier.players(), atelier.turnOrder(), atelier.finishedAt(),
                atelier.language(), atelier.selectedFamilies(), atelier.deck(), atelier.sets(),
                atelier.agentConfigs(), legs, atelier.tours(), atelier.scores(), atelier.winner(),
                atelier.projectFacts());
    }
}
