package com.cornucopai.game;

import com.cornucopai.api.dto.AtelierResponse;
import com.cornucopai.api.dto.AtelierSummary;
import com.cornucopai.api.dto.CreateAtelierRequest;
import com.cornucopai.game.agent.AgentFactory;
import com.cornucopai.game.card.CardCatalogService;
import com.cornucopai.game.card.CardDeckService;
import com.cornucopai.game.llm.AgentConfigResolver;
import com.cornucopai.game.llm.LlmDefaults;
import com.cornucopai.game.model.Atelier;
import com.cornucopai.game.model.Participant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.jboss.logging.Logger;

/**
 * Creates an atelier (folder + players + deck), runs the whole game through the {@link GameMaster}
 * and returns the persisted result (research R-003, R-009).
 */
@ApplicationScoped
public class AtelierService {

  private static final Logger LOG = Logger.getLogger(AtelierService.class);

  private final AtelierStore store;
  private final AgentFactory agentFactory;
  private final CardDeckService cardDeckService;
  private final AgentConfigResolver agentConfigResolver;
  private final Random random;

  /**
   * Spec 007 (FR-013): pre-vote debate, OFF by default since 2026-11-11 (see
   * application.properties).
   */
  private final boolean debateEnabled;

  @Inject
  public AtelierService(
      AtelierStore store,
      AgentFactory agentFactory,
      CardCatalogService catalogService,
      LlmDefaults llmDefaults,
      @org.eclipse.microprofile.config.inject.ConfigProperty(
              name = "cornucopai.debate.enabled",
              defaultValue = "false")
          boolean debateEnabled) {
    this(
        store,
        agentFactory,
        new CardDeckService(catalogService),
        new AgentConfigResolver(llmDefaults),
        new Random(),
        debateEnabled);
  }

  /** Test-friendly constructor with a seeded random. */
  public AtelierService(
      AtelierStore store,
      AgentFactory agentFactory,
      CardDeckService cardDeckService,
      AgentConfigResolver agentConfigResolver,
      Random random) {
    this(store, agentFactory, cardDeckService, agentConfigResolver, random, false);
  }

  /** Test-friendly constructor with a seeded random + debate flag (spec 007 US5). */
  public AtelierService(
      AtelierStore store,
      AgentFactory agentFactory,
      CardDeckService cardDeckService,
      AgentConfigResolver agentConfigResolver,
      Random random,
      boolean debateEnabled) {
    this.store = store;
    this.agentFactory = agentFactory;
    this.cardDeckService = cardDeckService;
    this.agentConfigResolver = agentConfigResolver;
    this.random = random;
    this.debateEnabled = debateEnabled;
  }

  public AtelierResponse createAndRun(CreateAtelierRequest request) {
    String id = store.createAtelierDir(store.newAtelierId());
    // Spec 004 (R-028/R-032): resolve per-player participants + LLM configs (defaults when no
    // agents array)
    AgentConfigResolver.Resolved resolved =
        agentConfigResolver.resolve(request.playerCount(), request.agents());
    List<Participant> players = resolved.players();
    // Spec 003 (FR-005/FR-008/FR-015/FR-016): build the real deck from the selected families
    String language =
        request.language() == null || request.language().isBlank()
            ? "fr"
            : request.language().toLowerCase();
    List<String> families =
        request.families() == null ? List.of() : request.families().stream().distinct().toList();
    CardDeckService.DeckResult deckResult = cardDeckService.buildDeck(families, language, random);
    Atelier atelier =
        new Atelier(
            id,
            Instant.now(),
            request.featureHtml(),
            request.playerCount(),
            players,
            List.of(),
            deckResult.deck());
    atelier.setLanguage(language);
    atelier.setSelectedFamilies(families);
    atelier.setSets(deckResult.sets());
    atelier.setAgentConfigs(resolved.configs()); // FR-012: persisted with the atelier
    // Spec 007 (FR-007, R-053, clarification 2026-11-11): optional inheritance of the
    // source atelier's shared project memory (chosen via the UI dropdown)
    if (request.inheritFactsFrom() != null && !request.inheritFactsFrom().isBlank()) {
      Atelier source =
          store
              .loadAtelier(request.inheritFactsFrom())
              .orElseThrow(() ->
                  new IllegalArgumentException(
                      "unknown inheritFactsFrom: " + request.inheritFactsFrom()));
      atelier.setProjectFacts(source.projectFacts());
      LOG.infof(
          "Atelier %s inherits %d project facts from %s",
          id, source.projectFacts().size(), source.id());
    }
    // Spec 004 (FR-003): games run on the single LLM agent implementation
    List<PlayerAgent> agents = agentFactory.createAgents(players, resolved.configs(), id);
    // Spec 007 (review 2026-11-11): seed the agents' shared memory with the atelier's
    // facts (possibly inherited) so they start the game with previous runs' knowledge
    agents.stream()
        .map(PlayerAgent::sharedMemory)
        .filter(java.util.Objects::nonNull)
        .findFirst()
        .ifPresent(memory -> memory.setAll(atelier.projectFacts()));
    LOG.infof(
        "Launching atelier %s with %d players, %d real cards from %s (%s)",
        id, request.playerCount(), deckResult.deck().size(), families, language);
    Atelier result = new GameMaster(store, random, debateEnabled).runGame(atelier, agents);
    return AtelierResponse.from(result, store.loadLegs(id));
  }

  public List<AtelierSummary> list() {
    return store.listAteliers().stream()
        .map(a ->
            new AtelierSummary(
                a.id(),
                a.createdAt(),
                a.playerCount(),
                a.projectFacts() != null && !a.projectFacts().isEmpty())) // spec 007 v0.7.0 (R-056)
        .toList();
  }

  public Optional<AtelierResponse> get(String id) {
    return store.loadAtelier(id).map(a -> AtelierResponse.from(a, store.loadLegs(id)));
  }
}
