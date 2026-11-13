package com.cornucopai.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.llm.AgentSnapshot;
import com.cornucopai.game.llm.TestAgents;
import com.cornucopai.game.model.Atelier;
import com.cornucopai.game.model.Participant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * T020 (spec 009, US2): atelier.json persists {@code agentSnapshots[]} index-aligned with {@code
 * players[]} (FR-017) — full agent config but NEVER the API key value (SC-005); legacy atelier.json
 * with {@code archetype}/{@code agentConfigs} still reads (FR-014, SC-010).
 */
class AtelierAgentConfigsPersistenceTest {

  @TempDir Path tempDir;

  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  void agentSnapshotsRoundTripThroughAtelierJsonIndexAlignedWithPlayers() throws Exception {
    AtelierStore store = new AtelierStore(tempDir, mapper);
    String id = store.createAtelierDir(store.newAtelierId());
    List<Participant> players =
        List.of(new Participant("Joueur-1", "agent-1"), new Participant("Joueur-2", "agent-1"));
    List<AgentSnapshot> snapshots =
        List.of(
            TestAgents.snapshotWithKeyVar("agent-1", "MISTRAL_API_KEY"),
            TestAgents.snapshotWithKeyVar("agent-1", "MISTRAL_API_KEY"));
    Card card =
        new Card(
            "VE2", "2", "webapp", "VE", "DATA VALIDATION & ENCODING", "en", "desc", null, null);
    Atelier atelier =
        new Atelier(
            id,
            Instant.now(),
            "<p>f</p>",
            2,
            players,
            List.of("Joueur-1", "Joueur-2"),
            List.of(card));
    atelier.setAgentSnapshots(snapshots);

    store.saveAtelier(id, atelier);

    JsonNode json = mapper.readTree(tempDir.resolve(id).resolve("atelier.json").toFile());
    assertThat(json.has("agentSnapshots")).isTrue();
    assertThat(json.get("agentSnapshots")).hasSize(2);
    assertThat(json.get("agentSnapshots").get(0).get("apiKeyVar").asText())
        .isEqualTo("MISTRAL_API_KEY");
    // FR-017/SC-005: the key VALUE is never persisted — only the variable name
    assertThat(json.get("agentSnapshots").toString()).doesNotContain("\"apiKey\"");
    assertThat(json.get("players").get(0).get("agentId").asText()).isEqualTo("agent-1");

    Atelier loaded = store.loadAtelier(id).orElseThrow();
    assertThat(loaded.agentSnapshots()).isEqualTo(snapshots);
    assertThat(loaded.agentSnapshots()).hasSameSizeAs(loaded.players());
    for (int i = 0; i < loaded.players().size(); i++) {
      assertThat(loaded.agentSnapshots().get(i).id()).isEqualTo(loaded.players().get(i).agentId());
    }
  }

  @Test
  void legacyAtelierJsonWithArchetypeAndAgentConfigsStillReads() throws Exception {
    AtelierStore store = new AtelierStore(tempDir, mapper);
    String id = store.createAtelierDir(store.newAtelierId());
    // pre-009 atelier.json: players with archetype, agentConfigs (spec 004), no agentSnapshots
    String legacy =
        """
        {
          "id": "%s", "createdAt": "2026-11-08T09:15:00Z", "featureHtml": "<p>old</p>",
          "playerCount": 2,
          "players": [{"name": "Joueur-1", "archetype": "GRUMPY"}, {"name": "Joueur-2", "archetype": "HAPPY"}],
          "turnOrder": ["Joueur-1", "Joueur-2"], "deck": [], "language": "fr",
          "selectedFamilies": ["webapp:VE"], "sets": [],
          "agentConfigs": [{"providerType": "OPENAI_COMPATIBLE", "baseUrl": "http://localhost:1235/v1", "apiKey": null, "modelName": null, "acpProvider": null, "command": null}],
          "tours": [], "scores": {}, "winner": [], "projectFacts": []
        }
        """
            .formatted(id);
    Files.createDirectories(tempDir.resolve(id));
    Files.writeString(tempDir.resolve(id).resolve("atelier.json"), legacy);

    Atelier loaded = store.loadAtelier(id).orElseThrow();
    assertThat(loaded.players()).hasSize(2);
    assertThat(loaded.players().get(0).name()).isEqualTo("Joueur-1");
    // legacy archetype is tolerated on read (FR-014)
    assertThat(loaded.players().get(0).archetype())
        .isEqualTo(com.cornucopai.game.model.Archetype.GRUMPY);
    assertThat(loaded.agentConfigs()).hasSize(1);
    // new field defaults to empty
    assertThat(loaded.agentSnapshots()).isEmpty();
  }
}
