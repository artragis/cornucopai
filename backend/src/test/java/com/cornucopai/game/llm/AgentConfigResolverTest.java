package com.cornucopai.game.llm;

import com.cornucopai.api.dto.AgentConfigDto;
import com.cornucopai.game.model.Archetype;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** T013: resolution of per-agent LLM configs (FR-001, FR-002, R-028/R-032). */
class AgentConfigResolverTest {

    private final LlmDefaults defaults = new LlmDefaults("http://localhost:1235/v1", "", "", java.time.Duration.ofSeconds(120));
    private final AgentConfigResolver resolver = new AgentConfigResolver(defaults);

    @Test
    void withoutAgentsAllPlayersGetDefaultLocalConfigAndRoundRobinArchetypes() {
        AgentConfigResolver.Resolved r = resolver.resolve(4, null);
        assertThat(r.players()).extracting("name")
                .containsExactly("Joueur-1", "Joueur-2", "Joueur-3", "Joueur-4");
        assertThat(r.players()).extracting("archetype")
                .containsExactly(Archetype.GRUMPY, Archetype.HAPPY, Archetype.GREEDY, Archetype.PROF);
        assertThat(r.configs()).hasSize(4);
        for (AgentLlmConfig cfg : r.configs()) {
            assertThat(cfg.providerType()).isEqualTo(AgentLlmConfig.ProviderType.OPENAI_COMPATIBLE);
            assertThat(cfg.baseUrl()).isEqualTo("http://localhost:1235/v1");
            assertThat(cfg.apiKey()).isNull();
        }
    }

    @Test
    void withAgentsEachPlayerGetsItsOwnConfig() {
        AgentConfigDto openAi = new AgentConfigDto("Alice", Archetype.HAPPY, "OPENAI_COMPATIBLE",
                "http://example:9999/v1", "secret", "my-model", null, null);
        AgentConfigDto acp = new AgentConfigDto(null, null, "ACP", null, null, null, "mistral-vibe", null);
        AgentConfigResolver.Resolved r = resolver.resolve(2, List.of(openAi, acp));
        assertThat(r.players().get(0).name()).isEqualTo("Alice");
        assertThat(r.players().get(1).name()).isEqualTo("Joueur-2");
        assertThat(r.configs().get(0).baseUrl()).isEqualTo("http://example:9999/v1");
        assertThat(r.configs().get(0).apiKey()).isEqualTo("secret");
        assertThat(r.configs().get(0).modelName()).isEqualTo("my-model");
        assertThat(r.configs().get(1).providerType()).isEqualTo(AgentLlmConfig.ProviderType.ACP);
        assertThat(r.configs().get(1).acpProvider()).isEqualTo("mistral-vibe");
    }

    @Test
    void openAiCompatibleWithoutBaseUrlFallsBackToDefaultButBlankIsRejected() {
        AgentConfigDto absent = new AgentConfigDto(null, null, "OPENAI_COMPATIBLE", null, null, null, null, null);
        assertThat(resolver.resolve(1, List.of(absent)).configs().get(0).baseUrl())
                .isEqualTo("http://localhost:1235/v1");
        AgentConfigDto blank = new AgentConfigDto(null, null, "OPENAI_COMPATIBLE", "  ", null, null, null, null);
        assertThatThrownBy(() -> resolver.resolve(1, List.of(blank)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseUrl");
    }

    @Test
    void unknownProviderTypeAndTooManyAgentsAreRejected() {
        AgentConfigDto bad = new AgentConfigDto(null, null, "FOO", "http://x", null, null, null, null);
        assertThatThrownBy(() -> resolver.resolve(1, List.of(bad)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown providerType");
        assertThatThrownBy(() -> resolver.resolve(2, List.of(
                        new AgentConfigDto(null, null, "ACP", null, null, null, "github-copilot", null),
                        new AgentConfigDto(null, null, "ACP", null, null, null, "github-copilot", null),
                        new AgentConfigDto(null, null, "ACP", null, null, null, "mistral-vibe", null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("playerCount");
    }
}
