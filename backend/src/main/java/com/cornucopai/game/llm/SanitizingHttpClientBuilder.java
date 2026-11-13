package com.cornucopai.game.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Workaround for OpenAI-compatible servers that return the assistant message content as an ARRAY of
 * content parts — observed 2026-11-11 with LM Studio (localhost:1235, gemma): {@code "content":
 * [{"type":"text","text":"..."}]}. langchain4j-open-ai 1.23.0 deserializes {@code
 * AssistantMessage.content} as a plain {@code String} and then fails with {@code
 * MismatchedInputException: Cannot deserialize value of type String from Array value}.
 *
 * <p>This builder wraps the default JDK HTTP client and rewrites array {@code content} into the
 * joined text of its text parts, only for {@code /chat/completions} responses. Sanitization never
 * breaks the call: any parsing problem returns the original response untouched.
 */
public final class SanitizingHttpClientBuilder implements HttpClientBuilder {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final JdkHttpClientBuilder delegate = new JdkHttpClientBuilder();

  @Override
  public Duration connectTimeout() {
    return delegate.connectTimeout();
  }

  @Override
  public HttpClientBuilder connectTimeout(Duration timeout) {
    delegate.connectTimeout(timeout);
    return this;
  }

  @Override
  public Duration readTimeout() {
    return delegate.readTimeout();
  }

  @Override
  public HttpClientBuilder readTimeout(Duration timeout) {
    delegate.readTimeout(timeout);
    return this;
  }

  @Override
  public HttpClient build() {
    return new SanitizingHttpClient(delegate.build());
  }

  /** Package-visible for tests. */
  static final class SanitizingHttpClient implements HttpClient {

    private final HttpClient delegate;

    SanitizingHttpClient(HttpClient delegate) {
      this.delegate = delegate;
    }

    @Override
    public SuccessfulHttpResponse execute(HttpRequest request) {
      return sanitize(request, delegate.execute(request));
    }

    @Override
    public void execute(
        HttpRequest request, ServerSentEventParser parser, ServerSentEventListener listener) {
      // SSE path: content arrives as text events, nothing to rewrite
      delegate.execute(request, parser, listener);
    }

    @Override
    public CompletableFuture<SuccessfulHttpResponse> executeAsync(HttpRequest request) {
      return delegate.executeAsync(request).thenApply(response -> sanitize(request, response));
    }

    private static SuccessfulHttpResponse sanitize(
        HttpRequest request, SuccessfulHttpResponse response) {
      try {
        String body = response.body();
        if (body == null
            || !body.contains("\"content\"")
            || !request.url().contains("chat/completions")) {
          return response;
        }
        JsonNode root = MAPPER.readTree(body);
        JsonNode choices = root.path("choices");
        boolean changed = false;
        if (choices.isArray()) {
          for (JsonNode choice : choices) {
            JsonNode message = choice.path("message");
            JsonNode content = message.path("content");
            if (content.isArray()) {
              StringBuilder sb = new StringBuilder();
              for (JsonNode part : content) {
                JsonNode text = part.path("text");
                if (text.isTextual()) {
                  sb.append(text.asText());
                }
              }
              ((ObjectNode) message).set("content", new TextNode(sb.toString()));
              changed = true;
            }
          }
        }
        if (!changed) {
          return response;
        }
        return SuccessfulHttpResponse.builder()
            .statusCode(response.statusCode())
            .headers(response.headers())
            .body(MAPPER.writeValueAsString(root))
            .build();
      } catch (Exception e) {
        return response; // sanitization must never break the LLM call
      }
    }
  }
}
