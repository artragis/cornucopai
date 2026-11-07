package com.cornucopai.game;

import com.cornucopai.api.dto.AtelierResponse;
import com.cornucopai.api.dto.AtelierSummary;
import com.cornucopai.api.dto.CreateAtelierRequest;
import com.cornucopai.game.agent.AgentFactory;
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
    private final Random random;

    @Inject
    public AtelierService(AtelierStore store, AgentFactory agentFactory) {
        this(store, agentFactory, new Random());
    }

    /** Test-friendly constructor with a seeded random. */
    public AtelierService(AtelierStore store, AgentFactory agentFactory, Random random) {
        this.store = store;
        this.agentFactory = agentFactory;
        this.random = random;
    }

    public AtelierResponse createAndRun(CreateAtelierRequest request) {
        String id = store.createAtelierDir(store.newAtelierId());
        List<PlayerAgent> agents = agentFactory.createAgents(request.playerCount());
        List<Participant> players = agents.stream()
                .map(a -> new Participant(a.name(), a.archetype()))
                .toList();
        List<String> deck = new PlaceholderCards(random).shuffledDeck();
        Atelier atelier = new Atelier(id, Instant.now(), request.featureHtml(),
                request.playerCount(), players, List.of(), deck);
        LOG.infof("Launching atelier %s with %d players", id, request.playerCount());
        Atelier result = new GameMaster(store, new PlaceholderCards(random), random).runGame(atelier, agents);
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
