package com.cornucopai.game.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** T018: tolerant JSON extraction for chatty local models (R-042). */
class JsonAnswersTest {

    @Test
    void plainJsonPassesThrough() {
        assertThat(JsonAnswers.stripToJson("{\"vote\":\"YES\"}")).isEqualTo("{\"vote\":\"YES\"}");
    }

    @Test
    void markdownFencesAreStripped() {
        assertThat(JsonAnswers.stripToJson("```json\n{\"vote\":\"NO\"}\n```")).isEqualTo("{\"vote\":\"NO\"}");
        assertThat(JsonAnswers.stripToJson("```\n{\"knows\":false}\n```")).isEqualTo("{\"knows\":false}");
    }

    @Test
    void surroundingTextIsDroppedAndFirstObjectExtracted() {
        String messy = "Voici ma réponse : {\"type\":\"PASS\",\"text\":null} et bonne partie !";
        assertThat(JsonAnswers.stripToJson(messy)).isEqualTo("{\"type\":\"PASS\",\"text\":null}");
    }
}
