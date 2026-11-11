package com.cornucopai.game.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The shared project memory of one atelier (spec 007 US2, R-049/R-053, FR-005..FR-008).
 * A thread-safe ordered journal of {@link ProjectFact}s: any agent of the atelier can
 * read all facts and append its own discoveries. Persisted as {@code projectFacts} in
 * {@code atelier.json}; isolated per atelier, except for the explicit UI-dropdown
 * inheritance at atelier creation (FR-007, clarification 2026-11-11).
 *
 * <p>Turns are sequential in {@code GameMaster}, so a {@link CopyOnWriteArrayList} is
 * more than enough and keeps reads lock-free.
 */
public class SharedProjectMemory {

    private final String atelierId;
    private final CopyOnWriteArrayList<ProjectFact> facts = new CopyOnWriteArrayList<>();

    public SharedProjectMemory(String atelierId) {
        this.atelierId = atelierId;
    }

    public String atelierId() {
        return atelierId;
    }

    public void add(ProjectFact fact) {
        facts.add(fact);
    }

    /** All facts, in insertion order. */
    public List<ProjectFact> facts() {
        return List.copyOf(facts);
    }

    /** The {@code n} most recent facts, in insertion order (for prompt injection, FR-008). */
    public List<ProjectFact> recent(int n) {
        int size = facts.size();
        if (n >= size) {
            return facts();
        }
        return List.copyOf(new ArrayList<>(facts).subList(size - n, size));
    }

    /** Replaces the whole journal (used when inheriting facts from a source atelier, FR-007). */
    public void setAll(List<ProjectFact> inherited) {
        facts.clear();
        if (inherited != null) {
            facts.addAll(inherited);
        }
    }
}
