package com.cornucopai.game.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

/**
 * One trick (tour) of a Cornucopia game — spec 006 (R-045, R-048). A tour is a full
 * pass where every player still holding cards plays exactly once. Persisted inside
 * {@code atelier.json}.
 *
 * <p>{@code majorSuit} is the suitId of the tour's couleur majeure (first non-WC card);
 * null when every card of the tour was WC. {@code winner} is the player who won the
 * +1 bonus (highest major-suit card); null when no bonus was awarded.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Tour {

    private int index;
    private String leader;
    private String majorSuit;
    private List<Integer> legIndices;
    private String winner;
    private boolean bonusAwarded;

    /** Jackson deserialization constructor. */
    public Tour() {
    }

    public Tour(int index, String leader, String majorSuit, List<Integer> legIndices,
                String winner, boolean bonusAwarded) {
        this.index = index;
        this.leader = leader;
        this.majorSuit = majorSuit;
        this.legIndices = legIndices;
        this.winner = winner;
        this.bonusAwarded = bonusAwarded;
    }

    public int index() { return index; }
    public String leader() { return leader; }
    public String majorSuit() { return majorSuit; }
    public List<Integer> legIndices() { return legIndices; }
    public String winner() { return winner; }
    public boolean bonusAwarded() { return bonusAwarded; }

    public void setIndex(int index) { this.index = index; }
    public void setLeader(String leader) { this.leader = leader; }
    public void setMajorSuit(String majorSuit) { this.majorSuit = majorSuit; }
    public void setLegIndices(List<Integer> legIndices) { this.legIndices = legIndices; }
    public void setWinner(String winner) { this.winner = winner; }
    public void setBonusAwarded(boolean bonusAwarded) { this.bonusAwarded = bonusAwarded; }
}
