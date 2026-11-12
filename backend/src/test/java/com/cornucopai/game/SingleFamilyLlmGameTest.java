package com.cornucopai.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.cornucopai.game.agent.AgentFactory;
import com.cornucopai.game.agent.LlmPlayerAgent;
import com.cornucopai.game.card.CardCatalogService;
import com.cornucopai.game.card.CardDeckService;
import com.cornucopai.game.llm.AgentLlmConfig;
import com.cornucopai.game.llm.LlmBrainFactory;
import com.cornucopai.game.llm.LlmDefaults;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Atelier;
import com.cornucopai.game.model.Participant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * T042 (US5): a full game played by LLM agents (fake brain, R-031) on ONE single family ({@code
 * webapp:VE} in the fixture = 2 cards) → at most 2 legs, all played by {@link LlmPlayerAgent},
 * agentConfigs persisted (FR-003, FR-012, FR-014, SC-005).
 */
class SingleFamilyLlmGameTest {

  @TempDir Path tempDir;

  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  void fullLlmGameOnOneSingleFamilyIsBoundedAndPersisted() {
    AtelierStore store = new AtelierStore(tempDir, mapper);
    CardCatalogService catalog =
        new CardCatalogService(Path.of("src/test/resources/cornucopia-fixture"));
    CardDeckService deckService = new CardDeckService(catalog);
    Random random = new Random(42);

    // ONE family only (FR-014): webapp:VE -> 2 cards in the fixture
    CardDeckService.DeckResult deckResult =
        deckService.buildDeck(List.of("webapp:VE"), "fr", random);
    assertThat(deckResult.deck()).hasSize(2);

    List<Participant> players =
        List.of(
            new Participant("Joueur-1", Archetype.GRUMPY),
            new Participant("Joueur-2", Archetype.HAPPY));
    List<AgentLlmConfig> configs =
        List.of(
            AgentLlmConfig.defaultOpenAiCompatible("http://localhost:1235/v1"),
            AgentLlmConfig.defaultOpenAiCompatible("http://localhost:1235/v1"));
    LlmBrainFactory brainFactory =
        new LlmBrainFactory(
            "fake",
            new LlmDefaults("http://localhost:1235/v1", null, null, Duration.ofSeconds(5)),
            List.of());
    AgentFactory agentFactory = new AgentFactory(brainFactory);

    String id = store.createAtelierDir(store.newAtelierId());
    List<PlayerAgent> agents = agentFactory.createAgents(players, configs, id);
    assertThat(agents).allSatisfy(a -> assertThat(a).isInstanceOf(LlmPlayerAgent.class));

    Atelier atelier =
        new Atelier(id, Instant.now(), "<p>feature</p>", 2, players, List.of(), deckResult.deck());
    atelier.setAgentConfigs(configs);
    Atelier result = new GameMaster(store, random).runGame(atelier, agents);

    // bounded: 2 cards -> at most 2 legs (FR-014, SC-005)
    List<com.cornucopai.game.model.Leg> legs = store.loadLegs(id);
    assertThat(legs).hasSizeLessThanOrEqualTo(2);
    assertThat(legs).isNotEmpty();
    assertThat(legs)
        .allSatisfy(l ->
            assertThat(l.validation())
                .isIn(
                    com.cornucopai.game.model.ValidationStatus.VALIDATED,
                    com.cornucopai.game.model.ValidationStatus.REJECTED));
    // LLM-written content came through the fake brain (scenario/explanation non-blank, not
    // deterministic filler)
    assertThat(legs)
        .allSatisfy(l -> {
          assertThat(l.scenario()).isNotBlank();
          assertThat(l.explanation()).isNotBlank();
        });
    // spec 005 v2: entries are structured objects with kind + author (fake brain reacts PRECISION)
    assertThat(legs).allSatisfy(l -> assertThat(l.entries()).isNotEmpty());
    assertThat(legs)
        .flatExtracting(com.cornucopai.game.model.Leg::entries)
        .allSatisfy(e -> {
          assertThat(e.kind()).isIn("PRECISION", "QUESTION");
          assertThat(e.author()).isNotBlank();
        });
    // agentConfigs persisted with the atelier (FR-012)
    Atelier persisted = store.loadAtelier(id).orElseThrow();
    assertThat(persisted.agentConfigs()).isEqualTo(configs);
    assertThat(result.finishedAt()).isNotNull();
  }
}
