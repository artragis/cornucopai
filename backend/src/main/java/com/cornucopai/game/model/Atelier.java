package com.cornucopai.game.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.time.Instant;
import java.util.List;

/**
 * A game session (workshop). Persisted as {@code atelier.json} inside its
 * {@code atelier{yyyyMMdd-HHmmss}} folder (research R-002).
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Atelier {

    private String id;
    private Instant createdAt;
    private String featureHtml;
    private int playerCount;
    private List<Participant> players;
    private List<String> turnOrder;
    private List<String> deck;
    private Instant finishedAt;

    /** Jackson deserialization constructor. */
    public Atelier() {
    }

    public Atelier(String id, Instant createdAt, String featureHtml, int playerCount,
                   List<Participant> players, List<String> turnOrder, List<String> deck) {
        this.id = id;
        this.createdAt = createdAt;
        this.featureHtml = featureHtml;
        this.playerCount = playerCount;
        this.players = players;
        this.turnOrder = turnOrder;
        this.deck = deck;
    }

    public String id() {
        return id;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String featureHtml() {
        return featureHtml;
    }

    public int playerCount() {
        return playerCount;
    }

    public List<Participant> players() {
        return players;
    }

    public List<String> turnOrder() {
        return turnOrder;
    }

    public List<String> deck() {
        return deck;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setFeatureHtml(String featureHtml) {
        this.featureHtml = featureHtml;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public void setPlayers(List<Participant> players) {
        this.players = players;
    }

    public void setTurnOrder(List<String> turnOrder) {
        this.turnOrder = turnOrder;
    }

    public void setDeck(List<String> deck) {
        this.deck = deck;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
