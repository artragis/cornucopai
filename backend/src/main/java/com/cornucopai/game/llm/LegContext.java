package com.cornucopai.game.llm;

import com.cornucopai.game.model.DebateTurn;
import com.cornucopai.game.model.Leg;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Per-game context holding the leg (tour) currently on the table (spec 005 US-2,
 * R-038). One instance per atelier, shared by the game's agents so that
 * {@link TourInfoTool} can serve the current leg without it being inlined in prompts.
 * Turns are sequential in {@code GameMaster}, so a single reference is race-free.
 */
public class LegContext {

    private final String atelierId;
    private final AtomicReference<Leg> current = new AtomicReference<>();
    /** Spec 007 US5 (R-052): arguments exchanged during the pre-vote debate of the current leg. */
    private final AtomicReference<List<DebateTurn>> debateTurns = new AtomicReference<>(List.of());

    public LegContext(String atelierId) {
        this.atelierId = atelierId;
    }

    public void setCurrent(Leg leg) {
        current.set(leg);
    }

    public Leg current() {
        return current.get();
    }

    public void setDebateTurns(List<DebateTurn> turns) {
        debateTurns.set(turns == null ? List.of() : turns);
    }

    public List<DebateTurn> debateTurns() {
        return debateTurns.get();
    }

    public String atelierId() {
        return atelierId;
    }
}
