package com.cornucopai.game.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.cornucopai.game.model.Leg;
import com.cornucopai.game.model.LegEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * T011 (US2): get_tour_info returns the current leg's structured entries, incl. questions with
 * knows/answer (FR-004, R-038).
 */
class TourInfoToolTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final LegContext context = new LegContext("atelierTest");
  private final TourInfoTool tool = new TourInfoTool(context);

  @Test
  void noLegSetReturnsExplicitMessage() {
    assertThat(tool.getTourInfo()).isEqualTo("Aucun tour en cours");
  }

  @Test
  void returnsCurrentLegEntriesAsJson() throws Exception {
    Leg leg = new Leg("VE9", "scénario", "explication");
    leg.addEntry(LegEntry.precision("Joueur-1", "ma précision"));
    leg.addEntry(LegEntry.question("Joueur-2", "Et si la validation échoue ?", false, null));
    leg.addEntry(LegEntry.question("Joueur-3", "Où est le code ?", true, "Dans le repo"));
    context.setCurrent(leg);

    String out = tool.getTourInfo();
    JsonNode json = mapper.readTree(out);
    assertThat(json.get("cardName").asText()).isEqualTo("VE9");
    JsonNode entries = json.get("entries");
    assertThat(entries).hasSize(3);
    assertThat(entries.get(1).get("kind").asText()).isEqualTo("QUESTION");
    assertThat(entries.get(1).get("knows").asBoolean())
        .isFalse(); // the model can see this question already got "Je sais pas"
    assertThat(entries.get(2).get("knows").asBoolean()).isTrue();
    assertThat(entries.get(2).get("answer").asText()).isEqualTo("Dans le repo");
  }

  @Test
  void toolIsNamedGetTourInfoForLangChain4jDiscovery() throws Exception {
    var ann =
        TourInfoTool.class
            .getMethod("getTourInfo")
            .getAnnotation(dev.langchain4j.agent.tool.Tool.class);
    assertThat(ann).isNotNull();
    assertThat(ann.name()).isEqualTo("get_tour_info");
  }
}
