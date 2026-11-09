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
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Creates an atelier (folder + players + deck), runs the whole game through the
 * {@link GameMaster} and returns the persisted result (research R-003, R-009).
 */
@ApplicationScoped
public class AtelierService {

    private static final Logger LOG = Logger.getLogger(AtelierService.class);

    private final AtelierStore store;
    private final AgentFactory agentFactory;
    private final CardDeckService cardDeckService;
    private final AgentConfigResolver agentConfigResolver;
    private final Random random;

    @Inject
    public AtelierService(AtelierStore store, AgentFactory agentFactory, CardCatalogService catalogService,
                          LlmDefaults llmDefaults) {
        this(store, agentFactory, new CardDeckService(catalogService), new AgentConfigResolver(llmDefaults), new Random());
    }

    /** Test-friendly constructor with a seeded random. */
    public AtelierService(AtelierStore store, AgentFactory agentFactory, CardDeckService cardDeckService,
                          AgentConfigResolver agentConfigResolver, Random random) {
        this.store = store;
        this.agentFactory = agentFactory;
        this.cardDeckService = cardDeckService;
        this.agentConfigResolver = agentConfigResolver;
        this.random = random;
    }

    public AtelierResponse createAndRun(CreateAtelierRequest request) {
        String id = store.createAtelierDir(store.newAtelierId());
        // Spec 004 (R-028/R-032): resolve per-player participants + LLM configs (defaults when no agents array)
        AgentConfigResolver.Resolved resolved = agentConfigResolver.resolve(request.playerCount(), request.agents());
        List<Participant> players = resolved.players();
        // Spec 004 (FR-003): games run on the single LLM agent implementation
        List<PlayerAgent> agents = agentFactory.createAgents(players, resolved.configs(), id);
        // Spec 003 (FR-005/FR-008/FR-015/FR-016): build the real deck from the selected families
        String language = request.language() == null || request.language().isBlank()
                ? "fr" : request.language().toLowerCase();
        List<String> families = request.families() == null
                ? List.of() : request.families().stream().distinct().toList();
        CardDeckService.DeckResult deckResult = cardDeckService.buildDeck(families, language, random);
        Atelier atelier = new Atelier(id, Instant.now(), request.featureHtml(),
                request.playerCount(), players, List.of(), deckResult.deck());
        atelier.setLanguage(language);
        atelier.setSelectedFamilies(families);
        atelier.setSets(deckResult.sets());
        atelier.setAgentConfigs(resolved.configs()); // FR-012: persisted with the atelier
        LOG.infof("Launching atelier %s with %d players, %d real cards from %s (%s)",
                id, request.playerCount(), deckResult.deck().size(), families, language);
        Atelier result = new GameMaster(store, random).runGame(atelier, agents);
        return AtelierResponse.from(result, store.loadLegs(id));
    }

    public List<AtelierSummary> list() {
        return store.listAteliers().stream()
                .map(a -> new AtelierSummary(a.id(), a.createdAt(), a.playerCount()))
                .toList();
    }

    public Optional<AtelierResponse> get(String id) {
        return store.loadAtelier(id)
                .map(a -> AtelierResponse.from(a, store.loadLegs(id)));
    }
}
