package com.cornucopai.game.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpMethod;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/**
 * Observed 2026-11-11 with a real run (LM Studio localhost:1235, gemma): the server returns {@code
 * choices[].message.content} as an ARRAY of parts and langchain4j-open-ai 1.23.0 fails with "Cannot
 * deserialize value of type String from Array value". The sanitizing client must rewrite that array
 * into the joined text before Jackson parses the response.
 */
class SanitizingHttpClientBuilderTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static HttpRequest chatRequest() {
    return HttpRequest.builder()
        .method(HttpMethod.POST)
        .url("http://localhost:1235/v1/chat/completions")
        .addHeader("Content-Type", "application/json")
        .body("{}")
        .build();
  }

  private static SuccessfulHttpResponse jsonResponse(String body) {
    return SuccessfulHttpResponse.builder()
        .statusCode(200)
        .headers(Map.of("Content-Type", List.of("application/json")))
        .body(body)
        .build();
  }

  private static HttpClient fakeReturning(String body) {
    return new HttpClient() {
      @Override
      public SuccessfulHttpResponse execute(HttpRequest request) {
        return jsonResponse(body);
      }

      @Override
      public void execute(
          HttpRequest request, ServerSentEventParser parser, ServerSentEventListener listener) {
        throw new UnsupportedOperationException();
      }

      @Override
      public CompletableFuture<SuccessfulHttpResponse> executeAsync(HttpRequest request) {
        return CompletableFuture.completedFuture(jsonResponse(body));
      }
    };
  }

  @Test
  void arrayContentIsRewrittenToJoinedText() throws Exception {
    String body =
        """
        {"id":"x","choices":[{"index":0,"message":{"role":"assistant",
          "content":[{"type":"text","text":"Hello "},{"type":"text","text":"world"}]},
          "finish_reason":"stop"}]}
        """;
    var client = new SanitizingHttpClientBuilder.SanitizingHttpClient(fakeReturning(body));
    SuccessfulHttpResponse response = client.execute(chatRequest());
    JsonNode content =
        MAPPER.readTree(response.body()).path("choices").get(0).path("message").path("content");
    assertThat(content.isTextual()).isTrue();
    assertThat(content.asText()).isEqualTo("Hello world");
  }

  @Test
  void stringContentIsLeftUntouched() throws Exception {
    String body =
        """
        {"choices":[{"message":{"role":"assistant","content":"plain"}}]}
        """;
    var client = new SanitizingHttpClientBuilder.SanitizingHttpClient(fakeReturning(body));
    String out = client.execute(chatRequest()).body();
    assertThat(MAPPER.readTree(out).path("choices").get(0).path("message").path("content").asText())
        .isEqualTo("plain");
  }

  @Test
  void nonChatCompletionResponsesAreNotTouched() throws Exception {
    String body =
        """
        {"content":[{"type":"text","text":"not a chat response"}]}
        """;
    var client = new SanitizingHttpClientBuilder.SanitizingHttpClient(fakeReturning(body));
    HttpRequest other =
        HttpRequest.builder().method(HttpMethod.GET).url("http://localhost:1235/v1/models").build();
    SuccessfulHttpResponse response = client.execute(other);
    assertThat(MAPPER.readTree(response.body()).path("content").isArray()).isTrue();
  }

  @Test
  void brokenJsonFallsBackToOriginalResponse() {
    var client =
        new SanitizingHttpClientBuilder.SanitizingHttpClient(
            fakeReturning("{\"choices\":[{\"message\":{\"content\":[not json"));
    SuccessfulHttpResponse response = client.execute(chatRequest());
    assertThat(response.body()).contains("not json"); // unchanged
  }

  @Test
  void asyncExecuteIsSanitizedToo() throws Exception {
    String body =
        """
        {"choices":[{"message":{"role":"assistant","content":[{"type":"text","text":"ok"}]}}]}
        """;
    var client = new SanitizingHttpClientBuilder.SanitizingHttpClient(fakeReturning(body));
    SuccessfulHttpResponse response = client.executeAsync(chatRequest()).join();
    JsonNode content =
        MAPPER.readTree(response.body()).path("choices").get(0).path("message").path("content");
    assertThat(content.isTextual()).isTrue();
    assertThat(content.asText()).isEqualTo("ok");
  }
}
