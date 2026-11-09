package com.cornucopai.game.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Jackson POJO mapping of a Cornucopia {@code <edition>-mappings-<version>.yaml} file
 * (R-024): same suits/cards skeleton as the cards file, plus per-card cross-references
 * {@code stride, stride_print, capec, capec_map, owasp_asvs, owasp_asvs_print,
 * owasp_dev_guide, owasp_dev_guide_print, safecode}. Parsed with the shared YAML
 * {@code ObjectMapper} (R-011).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardMappingsFile {

    private List<SuitEntry> suits;

    public List<SuitEntry> getSuits() { return suits; }
    public void setSuits(List<SuitEntry> suits) { this.suits = suits; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SuitEntry {
        private String id;
        private String name;
        private List<CardMappingEntry> cards;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<CardMappingEntry> getCards() { return cards; }
        public void setCards(List<CardMappingEntry> cards) { this.cards = cards; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CardMappingEntry {
        private String id;
        private List<Object> stride;
        private List<Object> stridePrint;
        private List<Object> capec;
        private Map<String, Object> capecMap;
        private List<Object> owaspAsvs;
        private List<Object> owaspAsvsPrint;
        private List<Object> owaspDevGuide;
        private List<Object> owaspDevGuidePrint;
        private List<Object> safecode;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public List<Object> getStride() { return stride; }
        public void setStride(List<Object> stride) { this.stride = stride; }
        @com.fasterxml.jackson.annotation.JsonProperty("stride_print")
        public List<Object> getStridePrint() { return stridePrint; }
        @com.fasterxml.jackson.annotation.JsonProperty("stride_print")
        public void setStridePrint(List<Object> stridePrint) { this.stridePrint = stridePrint; }
        public List<Object> getCapec() { return capec; }
        public void setCapec(List<Object> capec) { this.capec = capec; }
        @com.fasterxml.jackson.annotation.JsonProperty("capec_map")
        public Map<String, Object> getCapecMap() { return capecMap; }
        @com.fasterxml.jackson.annotation.JsonProperty("capec_map")
        public void setCapecMap(Map<String, Object> capecMap) { this.capecMap = capecMap; }
        @com.fasterxml.jackson.annotation.JsonProperty("owasp_asvs")
        public List<Object> getOwaspAsvs() { return owaspAsvs; }
        @com.fasterxml.jackson.annotation.JsonProperty("owasp_asvs")
        public void setOwaspAsvs(List<Object> owaspAsvs) { this.owaspAsvs = owaspAsvs; }
        @com.fasterxml.jackson.annotation.JsonProperty("owasp_asvs_print")
        public List<Object> getOwaspAsvsPrint() { return owaspAsvsPrint; }
        @com.fasterxml.jackson.annotation.JsonProperty("owasp_asvs_print")
        public void setOwaspAsvsPrint(List<Object> owaspAsvsPrint) { this.owaspAsvsPrint = owaspAsvsPrint; }
        @com.fasterxml.jackson.annotation.JsonProperty("owasp_dev_guide")
        public List<Object> getOwaspDevGuide() { return owaspDevGuide; }
        @com.fasterxml.jackson.annotation.JsonProperty("owasp_dev_guide")
        public void setOwaspDevGuide(List<Object> owaspDevGuide) { this.owaspDevGuide = owaspDevGuide; }
        @com.fasterxml.jackson.annotation.JsonProperty("owasp_dev_guide_print")
        public List<Object> getOwaspDevGuidePrint() { return owaspDevGuidePrint; }
        @com.fasterxml.jackson.annotation.JsonProperty("owasp_dev_guide_print")
        public void setOwaspDevGuidePrint(List<Object> owaspDevGuidePrint) { this.owaspDevGuidePrint = owaspDevGuidePrint; }
        public List<Object> getSafecode() { return safecode; }
        public void setSafecode(List<Object> safecode) { this.safecode = safecode; }
    }
}
