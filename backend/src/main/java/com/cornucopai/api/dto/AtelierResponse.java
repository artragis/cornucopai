package com.cornucopai.api.dto;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.llm.AgentLlmConfig;
import com.cornucopai.game.model.Atelier;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;
import com.cornucopai.game.model.SetResolution;

import java.time.Instant;
import java.util.List;

/** Full atelier detail returned by POST /api/ateliers and GET /api/ateliers/{id} (v0.4.0: + agentConfigs, spec 004 FR-012). */
public record AtelierResponse(String id, Instant createdAt, String featureHtml, int playerCount,
                              List<Participant> players, List<String> turnOrder, Instant finishedAt,
                              String language, List<String> selectedFamilies, List<Card> deck,
                              List<SetResolution> sets, List<AgentLlmConfig> agentConfigs, List<Leg> legs) {

    public static AtelierResponse from(Atelier atelier, List<Leg> legs) {
        return new AtelierResponse(atelier.id(), atelier.createdAt(), atelier.featureHtml(),
                atelier.playerCount(), atelier.players(), atelier.turnOrder(), atelier.finishedAt(),
                atelier.language(), atelier.selectedFamilies(), atelier.deck(), atelier.sets(),
                atelier.agentConfigs(), legs);
    }
}
