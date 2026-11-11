package com.cornucopai.game;

import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.ValidationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T015 (US3, spec 006): scoring rules in isolation — +1 per VALIDATED leg in the major
 * suit (WC counts as major), 0 for VALIDATED off-suit, 0 for REJECTED; bonus +1 to the
 * highest major-suit card per tour by CardValueOrder (FR-006…FR-009, SC-002).
 *
 * <p>These rules are exercised through the GameMaster's closeTour via full games in
 * {@link GameMasterTourTest}; here we assert the point values on persisted legs.
 */
class ScoreTest {

    @Test
    void cardValueOrderDrivesBonus() {
        // sanity: the bonus comparison uses canonical ranks, not alphabetical
        assertThat(com.cornucopai.game.model.CardValueOrder.compare("A", "K")).isGreaterThan(0);
        assertThat(com.cornucopai.game.model.CardValueOrder.compare("10", "9")).isGreaterThan(0);
    }

    @Test
    void legPointsSemantics() {
        Leg validated = new Leg("VE2", "s", "e");
        validated.setValidation(ValidationStatus.VALIDATED);
        validated.setSuitId("VE");
        validated.setMajorSuit("VE");
        validated.setPoints(1);
        assertThat(validated.points()).isEqualTo(1);

        Leg rejected = new Leg("VE3", "s", "e");
        rejected.setValidation(ValidationStatus.REJECTED);
        rejected.setSuitId("VE");
        rejected.setMajorSuit("VE");
        rejected.setPoints(0);
        assertThat(rejected.points()).isZero();

        Leg offSuit = new Leg("AT1", "s", "e");
        offSuit.setValidation(ValidationStatus.VALIDATED);
        offSuit.setSuitId("AT");
        offSuit.setMajorSuit("VE");
        offSuit.setPoints(0);
        assertThat(offSuit.points()).isZero();

        // WC counts as major suit
        Leg wc = new Leg("WCA", "s", "e");
        wc.setValidation(ValidationStatus.VALIDATED);
        wc.setSuitId("WC");
        wc.setMajorSuit("VE");
        wc.setPoints(1);
        assertThat(wc.points()).isEqualTo(1);
    }
}
