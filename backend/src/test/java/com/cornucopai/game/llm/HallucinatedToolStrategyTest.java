package com.cornucopai.game.llm;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import org.junit.jupiter.api.Test;

/**
 * Observed 2026-11-11 on a real run: the vote prompt tells the model to use get_tour_info / MCP
 * tools, and when a proxy lacked them langchain4j's default THROW_EXCEPTION strategy failed the
 * whole call ("The LLM is trying to execute the 'get_tour_info' tool, but no such tool exists").
 * Our lenient strategy must return an explanatory tool result instead of throwing.
 */
class HallucinatedToolStrategyTest {

  @Test
  void lenientStrategyReturnsExplanatoryResultInsteadOfThrowing() {
    ToolExecutionRequest request =
        ToolExecutionRequest.builder().id("1").name("get_tour_info").arguments("{}").build();
    ToolExecutionResultMessage result = LangChain4jBrain.LENIENT_HALLUCINATED_TOOL.apply(request);
    assertThat(result.text()).contains("get_tour_info").contains("n'existe pas");
  }
}
