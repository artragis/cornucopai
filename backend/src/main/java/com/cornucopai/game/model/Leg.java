package com.cornucopai.game.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.List;

/**
 * One played card in a Cornucopia game. Persisted as its own JSON file with
 * exactly 5 fields (contracts/leg-schema.json, FR-007): cardName, scenario,
 * explanation, precisions, validation.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Leg {

    private String cardName;
    private String scenario;
    private String explanation;
    private List<String> precisions;
    private ValidationStatus validation;

    /** J Jackson deserialization constructor. */
    public Leg() {
        this.precisions = new ArrayList<>();
        this.validation = ValidationStatus.PENDING;
    }

    public Leg(String cardName, String scenario, String explanation) {
        if (cardName == null || cardName.isBlank()) {
            throw new IllegalArgumentException("cardName must be non-empty");
        }
        if (scenario == null || scenario.isBlank()) {
            throw new IllegalArgumentException("scenario must be non-empty");
        }
        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException("explanation must be non-empty");
        }
        this.cardName = cardName;
        this.scenario = scenario;
        this.explanation = explanation;
        this.precisions = new ArrayList<>();
        this.validation = ValidationStatus.PENDING;
    }

    public void addPrecision(String precision) {
        if (precision == null || precision.isBlank()) {
            throw new IllegalArgumentException("precision must be non-empty");
        }
        this.precisions.add(precision);
    }

    public String cardName() {
        return cardName;
    }

    public String scenario() {
        return scenario;
    }

    public String explanation() {
        return explanation;
    }

    public List<String> precisions() {
        return precisions;
    }

    public ValidationStatus validation() {
        return validation;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public void setPrecisions(List<String> precisions) {
        this.precisions = precisions;
    }

    public void setValidation(ValidationStatus validation) {
        this.validation = validation;
    }
}
