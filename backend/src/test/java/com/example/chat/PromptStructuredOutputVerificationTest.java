package com.example.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.chat.dto.ChatResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.core.io.ByteArrayResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runtime verification harness against a loopback Ollama-compatible API, not a real model daemon. */
class PromptStructuredOutputVerificationTest {

    private static final String VALID_JSON = "{\"title\":\"Dune\",\"summary\":\"A desert planet\"}";
    private static final String INVALID_JSON = "{\"title\":\"Dune\"}";

    private HttpServer server;
    private final ConcurrentLinkedQueue<String> queuedResponses = new ConcurrentLinkedQueue<>();
    private final List<String> requests = new CopyOnWriteArrayList<>();

    @BeforeEach
    void startLoopbackOllamaApi() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            requests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String content = queuedResponses.poll();
            if (content == null) content = VALID_JSON;
            String response = """
                    {"model":"llama3.2:3b","created_at":"2026-10-15T00:00:00Z",
                     "message":{"role":"assistant","content":%s},"done_reason":"stop","done":true,
                     "total_duration":1000000,"load_duration":100000,"prompt_eval_count":35,
                     "prompt_eval_duration":400000,"eval_count":8,"eval_duration":500000}
                    """.formatted(JsonMapper.builder().build().writeValueAsString(content));
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
    }

    @AfterEach
    void stopLoopbackOllamaApi() {
        if (server != null) server.stop(0);
    }

    @Test
    void capturesDefaultDelimiterJsonFailureAndRendersWithAngleDelimiters() {
        String template = "Return literal JSON: {\"title\":\"example\",\"summary\":\"text\"}. Topic: {topic}";
        RuntimeException failure = null;
        try {
            StTemplateRenderer.builder().build().apply(template, Map.of("topic", "books"));
        }
        catch (RuntimeException ex) {
            failure = ex;
        }
        assertThat(failure).as("default-delimiter JSON collision").isNotNull();
        System.out.println("DEFAULT_RENDERER_LITERAL_ERROR=" + failure.getClass().getName() + ": " + failure.getMessage());

        String rendered = StTemplateRenderer.builder()
                .startDelimiterToken('<')
                .endDelimiterToken('>')
                .build()
                .apply("Return literal JSON: {\"title\":\"example\",\"summary\":\"text\"}. Topic: <topic>",
                        Map.of("topic", "books"));
        assertThat(rendered).isEqualTo("Return literal JSON: {\"title\":\"example\",\"summary\":\"text\"}. Topic: books");
    }

    @Test
    void samplingOptionsRemainOnRequestWhenEntityConversionIsUsed() {
        queuedResponses.add(VALID_JSON);
        ExtractedItem result = client().prompt()
                .user("Extract the title and summary")
                .options(requestOptions().mutate())
                .call()
                .entity(ExtractedItem.class);

        assertThat(result.title()).isEqualTo("Dune");
        String request = requests.getFirst();
        var json = JsonMapper.builder().build().readTree(request);
        assertThat(json.path("options").path("temperature").asDouble()).isEqualTo(0.35);
        assertThat(json.path("options").path("top_p").asDouble()).isEqualTo(0.82);
        assertThat(json.path("options").path("top_k").asInt()).isEqualTo(31);
        assertThat(json.path("options").path("num_predict").asInt()).isEqualTo(123);
        assertThat(json.path("options").path("seed").asInt()).isEqualTo(77);
    }

    @Test
    void requestSelectedSystemResourceOverridesBuilderDefaultSystemResource() {
        queuedResponses.add(VALID_JSON);
        ChatClient selectedClient = ChatClient.builder(model())
                .defaultSystem(new ByteArrayResource("BASE SYSTEM".getBytes(StandardCharsets.UTF_8)))
                .defaultTemplateRenderer(StTemplateRenderer.builder()
                        .startDelimiterToken('<')
                        .endDelimiterToken('>')
                        .build())
                .build();
        selectedClient.prompt()
                .system(system -> system.text(new ByteArrayResource("SELECTED <language>".getBytes(StandardCharsets.UTF_8)))
                        .param("language", "English"))
                .user("Extract")
                .call()
                .content();

        var messageArray = JsonMapper.builder().build().readTree(requests.getFirst()).path("messages");
        var systemMessages = new java.util.ArrayList<String>();
        for (var message : messageArray) {
            if (message.path("role").asText().equals("system")) systemMessages.add(message.path("content").asText());
        }
        assertThat(systemMessages).containsExactly("SELECTED English");
    }

    @Test
    void frameworkValidationRetriesWhileProviderModeSendsFormatWithoutFrameworkRetry() {
        queuedResponses.add(INVALID_JSON);
        queuedResponses.add(VALID_JSON);
        ExtractedItem frameworkResult = client().prompt()
                .user("Extract the title and summary")
                .options(requestOptions().mutate())
                .call()
                .entity(ExtractedItem.class, spec -> spec.validateSchema());

        assertThat(frameworkResult.summary()).isEqualTo("A desert planet");
        assertThat(requests).hasSize(2);
        String retryRequest = requests.get(1);
        System.out.println("FRAMEWORK_RETRY_REQUEST=" + retryRequest);

        requests.clear();
        queuedResponses.add(INVALID_JSON);
        assertThatThrownBy(() -> client().prompt()
                .user("Extract the title and summary")
                .options(requestOptions().mutate())
                .call()
                .entity(ExtractedItem.class, spec -> spec.useProviderStructuredOutput()))
                .hasMessageContaining("Missing required creator property 'summary'");
        assertThat(requests).hasSize(1);
        var providerRequest = JsonMapper.builder().build().readTree(requests.getFirst());
        assertThat(providerRequest.path("format").isObject()).isTrue();
        System.out.println("PROVIDER_FORMAT=" + providerRequest.path("format"));
    }

    private ChatClient client() {
        return ChatClient.builder(model())
                .defaultTemplateRenderer(StTemplateRenderer.builder()
                        .startDelimiterToken('<')
                        .endDelimiterToken('>')
                        .build())
                .build();
    }

    private OllamaChatModel model() {
        OllamaApi api = OllamaApi.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build();
        return OllamaChatModel.builder()
                .ollamaApi(api)
                .options(OllamaChatOptions.builder().model("llama3.2:3b").build())
                .build();
    }

    private OllamaChatOptions requestOptions() {
        return OllamaChatOptions.builder()
                .model("llama3.2:3b")
                .temperature(0.35)
                .topP(0.82)
                .topK(31)
                .numPredict(123)
                .seed(77)
                .build();
    }

    public record ExtractedItem(
            @JsonProperty(required = true) String title,
            @JsonProperty(required = true) String summary) {}
}
