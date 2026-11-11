package com.cornucopai.game.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T003 (Phase 2, spec 006): the {@link Tour} record round-trips through Jackson with
 * fields index, leader, majorSuit, legIndices, winner, bonusAwarded (R-048).
 */
class TourModelTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void tourRoundTripsThroughJackson() throws Exception {
        Tour tour = new Tour(1, "Joueur-2", "VE", List.of(1, 2, 3, 4), "Joueur-3", true);
        String json = mapper.writeValueAsString(tour);
        Tour read = mapper.readValue(json, Tour.class);
        assertThat(read.index()).isEqualTo(1);
        assertThat(read.leader()).isEqualTo("Joueur-2");
        assertThat(read.majorSuit()).isEqualTo("VE");
        assertThat(read.legIndices()).containsExactly(1, 2, 3, 4);
        assertThat(read.winner()).isEqualTo("Joueur-3");
        assertThat(read.bonusAwarded()).isTrue();
    }

    @Test
    void tourAllowsNullMajorSuitAndWinner() throws Exception {
        Tour tour = new Tour(2, "Joueur-1", null, List.of(5, 6), null, false);
        String json = mapper.writeValueAsString(tour);
        Tour read = mapper.readValue(json, Tour.class);
        assertThat(read.majorSuit()).isNull();
        assertThat(read.winner()).isNull();
        assertThat(read.bonusAwarded()).isFalse();
    }
}
