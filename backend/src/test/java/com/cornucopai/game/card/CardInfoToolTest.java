package com.cornucopai.game.card;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** T030 (US3): the LangChain4j @Tool renders card info as Markdown; unknown id is explicit (FR-005, FR-009, R-023). */
class CardInfoToolTest {

    private final CardInfoTool tool = new CardInfoTool(new CardInfoService(Path.of("src/test/resources/cornucopia-fixture")));

    @Test
    void getCardInfoReturnsMarkdownWithAllSources() {
        String md = tool.getCardInfo("VE2");
        assertThat(md).contains("VE2");
        assertThat(md).contains("DATA VALIDATION & ENCODING");
        assertThat(md).contains("Robert");                    // YAML description
        assertThat(md).contains("CAPEC").contains("54");       // mappings
        assertThat(md).contains("ASVS").contains("2.4.1");
        assertThat(md).contains("STRIDE").contains("Information Disclosure");
        assertThat(md).contains("Brian");                     // explanation.md
        assertThat(md).contains("verbose error messages");     // technical-note.md
    }

    @Test
    void unknownCardIdIsExplicit() {
        assertThat(tool.getCardInfo("XX9")).isEqualTo("Carte inconnue : XX9");
    }

    @Test
    void toolAnnotationIsPresentForLangChain4jDiscovery() throws Exception {
        var m = CardInfoTool.class.getMethod("getCardInfo", String.class);
        var toolAnn = m.getAnnotation(dev.langchain4j.agent.tool.Tool.class);
        assertThat(toolAnn).isNotNull();
        assertThat(toolAnn.name()).isEqualTo("get_card_info");
    }
}
