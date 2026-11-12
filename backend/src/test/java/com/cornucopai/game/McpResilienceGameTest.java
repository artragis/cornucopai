package com.cornucopai.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.cornucopai.game.agent.AgentFactory;
import com.cornucopai.game.card.CardCatalogService;
import com.cornucopai.game.card.CardDeckService;
import com.cornucopai.game.llm.AgentLlmConfig;
import com.cornucopai.game.llm.LlmBrainFactory;
import com.cornucopai.game.llm.LlmDefaults;
import com.cornucopai.game.mcp.McpIdeConfig;
import com.cornucopai.game.mcp.McpToolProviderFactory;
import com.cornucopai.game.mcp.MockMcpClient;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Atelier;
import com.cornucopai.game.model.Leg;
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
 * T014 (spec 007 US1): a full game runs end-to-end even when the MCP server is unreachable — the
 * provider degrades to zero tools and never blocks the game (SC-007, FR-004, R-047). One single
 * family, fake brain (FR-014 spec 004, R-055).
 */
class McpResilienceGameTest {

  @TempDir Path tempDir;

  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  void gameCompletesWithMcpServerDown() {
    AtelierStore store = new AtelierStore(tempDir, mapper);
    CardCatalogService catalog =
        new CardCatalogService(Path.of("src/test/resources/cornucopia-fixture"));
    CardDeckService deckService = new CardDeckService(catalog);
    Random random = new Random(42);

    CardDeckService.DeckResult deckResult =
        deckService.buildDeck(List.of("webapp:VE"), "fr", random);

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
    // MCP provider built from a FAILING mock client — must not break the game
    MockMcpClient failingClient = new MockMcpClient("ide-mcp", List.of(), true);
    var provider = McpToolProviderFactory.createWithClient(McpIdeConfig.defaults(), failingClient);
    AgentFactory agentFactory = new AgentFactory(brainFactory, provider);

    String id = store.createAtelierDir(store.newAtelierId());
    List<PlayerAgent> agents = agentFactory.createAgents(players, configs, id);

    Atelier atelier =
        new Atelier(
            id, Instant.now(), "<p>feature test</p>", 2, players, List.of(), deckResult.deck());
    atelier.setAgentConfigs(configs);
    Atelier result = new GameMaster(store, random).runGame(atelier, agents);

    assertThat(result.finishedAt()).isNotNull();
    List<Leg> legs = store.loadLegs(id);
    assertThat(legs).isNotEmpty();
  }
}
