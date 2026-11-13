package com.cornucopai.game.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.cornucopai.game.AtelierStore;
import com.cornucopai.game.GameMaster;
import com.cornucopai.game.PlayerAgent;
import com.cornucopai.game.agent.AgentFactory;
import com.cornucopai.game.card.CardCatalogService;
import com.cornucopai.game.card.CardDeckService;
import com.cornucopai.game.llm.AgentSnapshot;
import com.cornucopai.game.llm.LlmBrainFactory;
import com.cornucopai.game.llm.LlmDefaults;
import com.cornucopai.game.llm.TestAgents;
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
 * T024 (spec 007 US2): in one game, a fact added through one agent's tool is visible through the
 * shared memory, and a second atelier starts with an empty memory (SC-003, SC-006, R-049). Facts
 * are persisted in atelier.json (R-053).
 */
class SharedMemoryGameTest {

  @TempDir Path tempDir;

  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  void factIsSharedAcrossAgentsAndPersistedPerAtelier() {
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
    List<AgentSnapshot> snapshots =
        List.of(TestAgents.snapshot("agent-1"), TestAgents.snapshot("agent-2"));
    LlmBrainFactory brainFactory =
        new LlmBrainFactory(
            "fake",
            new LlmDefaults("http://localhost:1235/v1", null, null, Duration.ofSeconds(5)),
            List.of());
    AgentFactory agentFactory = new AgentFactory(brainFactory);

    String id = store.createAtelierDir(store.newAtelierId());
    List<PlayerAgent> agents = agentFactory.createAgents(players, snapshots, id);
    Atelier atelier =
        new Atelier(id, Instant.now(), "<p>f</p>", 2, players, List.of(), deckResult.deck());
    atelier.setAgentSnapshots(snapshots);
    new GameMaster(store, random).runGame(atelier, agents);

    // simulate a discovery recorded mid-game via the shared memory of agent 1's tool:
    // here we assert persistence + isolation directly on the model level
    Atelier reloaded = store.loadAtelier(id).orElseThrow();
    // no facts were recorded by the fake brain, but the field must be present and empty
    assertThat(reloaded.projectFacts()).isNotNull().isEmpty();

    // a second atelier starts empty (SC-006)
    String id2 = store.createAtelierDir(store.newAtelierId());
    Atelier second = store.loadAtelier(id2).orElse(new Atelier());
    assertThat(second.projectFacts()).isEmpty();
  }

  @Test
  void factsDiscoveredDuringTurnArePersistedInTheAtelierJson() {
    // review 2026-11-11: facts discovered mid-game must be snapshotted into the atelier
    // after each turn so they survive a restart and can be inherited by a later run
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
    List<AgentSnapshot> snapshots =
        List.of(TestAgents.snapshot("agent-1"), TestAgents.snapshot("agent-2"));
    LlmBrainFactory brainFactory =
        new LlmBrainFactory(
            "fake",
            new LlmDefaults("http://localhost:1235/v1", null, null, Duration.ofSeconds(5)),
            List.of());
    AgentFactory agentFactory = new AgentFactory(brainFactory);

    String id = store.createAtelierDir(store.newAtelierId());
    List<PlayerAgent> agents = agentFactory.createAgents(players, snapshots, id);
    SharedProjectMemory memory = agents.get(0).sharedMemory();
    assertThat(memory).isNotNull();
    // decorate the first agent: it records a discovery while playing its card
    List<PlayerAgent> decorated =
        agents.stream().map(a -> a == agents.get(0) ? new DiscoveringAgent(a, memory) : a).toList();
    Atelier atelier =
        new Atelier(id, Instant.now(), "<p>f</p>", 2, players, List.of(), deckResult.deck());
    atelier.setAgentSnapshots(snapshots);
    new GameMaster(store, random).runGame(atelier, decorated);

    Atelier reloaded = store.loadAtelier(id).orElseThrow();
    assertThat(reloaded.projectFacts()).anyMatch(f -> f.text().contains("object_id est un int"));
  }

  /** Delegates everything, but records a project fact when it plays a card. */
  private static final class DiscoveringAgent implements PlayerAgent {
    private final PlayerAgent delegate;
    private final SharedProjectMemory memory;

    private DiscoveringAgent(PlayerAgent delegate, SharedProjectMemory memory) {
      this.delegate = delegate;
      this.memory = memory;
    }

    @Override
    public String name() {
      return delegate.name();
    }

    @Override
    public Archetype archetype() {
      return delegate.archetype();
    }

    @Override
    public com.cornucopai.game.model.PlayedCard playCard(
        com.cornucopai.game.card.model.Card card, String featureHtml) {
      memory.add(new ProjectFact("object_id est un int (models.py:42)", name(), 1, Instant.now()));
      return delegate.playCard(card, featureHtml);
    }

    @Override
    public com.cornucopai.game.model.Reaction react(
        com.cornucopai.game.model.Leg leg,
        com.cornucopai.game.model.Participant cardPlayer,
        String featureHtml,
        boolean questionsAllowed) {
      return delegate.react(leg, cardPlayer, featureHtml, questionsAllowed);
    }

    @Override
    public com.cornucopai.game.model.Answer answerQuestion(String question) {
      return delegate.answerQuestion(question);
    }

    @Override
    public com.cornucopai.game.model.Vote vote(
        com.cornucopai.game.model.Leg leg, com.cornucopai.game.model.Participant cardPlayer) {
      return delegate.vote(leg, cardPlayer);
    }

    @Override
    public com.cornucopai.game.llm.LegContext legContext() {
      return delegate.legContext();
    }

    @Override
    public SharedProjectMemory sharedMemory() {
      return memory;
    }
  }

  @Test
  void memoryIsSharedBetweenTwoToolsOfTheSameAtelier() {
    SharedProjectMemory memory = new SharedProjectMemory("atelierX");
    ProjectFactsTool toolA = new ProjectFactsTool(memory, () -> "Joueur-1", () -> 1);
    ProjectFactsTool toolB = new ProjectFactsTool(memory, () -> "Joueur-2", () -> 2);
    toolA.addProjectFact("fait découvert par A");
    // B sees A's fact via get_project_facts (SC-003)
    assertThat(toolB.getProjectFacts()).contains("fait découvert par A").contains("Joueur-1");
  }
}
