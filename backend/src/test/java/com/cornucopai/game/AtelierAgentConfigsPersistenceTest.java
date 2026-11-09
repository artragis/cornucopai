package com.cornucopai.game;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.llm.AgentLlmConfig;
import com.cornucopai.game.model.Archetype;
import com.cornucopai.game.model.Atelier;
import com.cornucopai.game.model.Participant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** T006: atelier.json persists agentConfigs (FR-012), index-aligned with players. */
class AtelierAgentConfigsPersistenceTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void agentConfigsRoundTripThroughAtelierJson() throws Exception {
        AtelierStore store = new AtelierStore(tempDir, mapper);
        String id = store.createAtelierDir(store.newAtelierId());
        List<Participant> players = List.of(
                new Participant("Joueur-1", Archetype.GRUMPY),
                new Participant("Joueur-2", Archetype.HAPPY));
        List<AgentLlmConfig> configs = List.of(
                AgentLlmConfig.defaultOpenAiCompatible("http://localhost:1235/v1"),
                new AgentLlmConfig(AgentLlmConfig.ProviderType.ACP, null, null, null, "mistral-vibe", null));
        Card card = new Card("VE2", "2", "webapp", "VE", "DATA VALIDATION & ENCODING", "en", "desc", null, null);
        Atelier atelier = new Atelier(id, Instant.now(), "<p>f</p>", 2, players, List.of("Joueur-1", "Joueur-2"), List.of(card));
        atelier.setAgentConfigs(configs);

        store.saveAtelier(id, atelier);

        JsonNode json = mapper.readTree(tempDir.resolve(id).resolve("atelier.json").toFile());
        assertThat(json.has("agentConfigs")).isTrue();
        assertThat(json.get("agentConfigs")).hasSize(2);
        assertThat(json.get("agentConfigs").get(0).get("providerType").asText()).isEqualTo("OPENAI_COMPATIBLE");
        assertThat(json.get("agentConfigs").get(0).get("baseUrl").asText()).isEqualTo("http://localhost:1235/v1");
        assertThat(json.get("agentConfigs").get(1).get("providerType").asText()).isEqualTo("ACP");
        assertThat(json.get("agentConfigs").get(1).get("acpProvider").asText()).isEqualTo("mistral-vibe");

        Atelier loaded = store.loadAtelier(id).orElseThrow();
        assertThat(loaded.agentConfigs()).isEqualTo(configs);
        assertThat(loaded.agentConfigs()).hasSameSizeAs(loaded.players());
    }
}
