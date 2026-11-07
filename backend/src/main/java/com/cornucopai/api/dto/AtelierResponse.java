package com.cornucopai.api.dto;

import com.cornucopai.game.model.Atelier;
import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.Participant;

import java.time.Instant;
import java.util.List;

/** Full atelier detail returned by POST /api/ateliers and GET /api/ateliers/{id}. */
public record AtelierResponse(String id, Instant createdAt, String featureHtml, int playerCount,
                              List<Participant> players, List<String> turnOrder, Instant finishedAt,
                              List<Leg> legs) {

    public static AtelierResponse from(Atelier atelier, List<Leg> legs) {
        return new AtelierResponse(atelier.id(), atelier.createdAt(), atelier.featureHtml(),
                atelier.playerCount(), atelier.players(), atelier.turnOrder(), atelier.finishedAt(), legs);
    }
}
