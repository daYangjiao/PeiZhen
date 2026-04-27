package org.example.unity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeepSeekClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void chatCompletionShouldUseV4FlashNonThinkingPayloadByDefault() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, """
                    {"choices":[{"message":{"content":"好的"}}]}
                    """);
        });

        DeepSeekClient client = configuredClient("");

        String answer = client.chatCompletion(List.of(Map.of("role", "user", "content", "你好")));

        assertThat(answer).isEqualTo("好的");
        JsonNode payload = objectMapper.readTree(requestBody.get());
        assertThat(payload.path("model").asText()).isEqualTo("deepseek-v4-flash");
        assertThat(payload.path("thinking").path("type").asText()).isEqualTo("disabled");
        assertThat(payload.has("temperature")).isFalse();
    }

    @Test
    void chatCompletionShouldHonorModelOverrideAndKeepThinkingDisabled() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, """
                    {"choices":[{"message":{"content":"已完成"}}]}
                    """);
        });

        DeepSeekClient client = configuredClient("deepseek-v4-flash");

        String answer = client.chatCompletion(
                List.of(Map.of("role", "user", "content", "生成推荐")),
                "deepseek-v4-pro"
        );

        assertThat(answer).isEqualTo("已完成");
        JsonNode payload = objectMapper.readTree(requestBody.get());
        assertThat(payload.path("model").asText()).isEqualTo("deepseek-v4-pro");
        assertThat(payload.path("thinking").path("type").asText()).isEqualTo("disabled");
    }

    @Test
    void chatCompletionShouldMapDeprecatedChatAliasToV4Flash() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, """
                    {"choices":[{"message":{"content":"已切换"}}]}
                    """);
        });

        DeepSeekClient client = configuredClient("deepseek-chat");

        String answer = client.chatCompletion(List.of(Map.of("role", "user", "content", "你好")));

        assertThat(answer).isEqualTo("已切换");
        JsonNode payload = objectMapper.readTree(requestBody.get());
        assertThat(payload.path("model").asText()).isEqualTo("deepseek-v4-flash");
        assertThat(payload.path("thinking").path("type").asText()).isEqualTo("disabled");
    }

    @Test
    void chatCompletionShouldMapDeprecatedReasonerAliasToV4FlashThinking() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, """
                    {"choices":[{"message":{"content":"已思考"}}]}
                    """);
        });

        DeepSeekClient client = configuredClient("deepseek-reasoner");

        String answer = client.chatCompletion(List.of(Map.of("role", "user", "content", "你好")));

        assertThat(answer).isEqualTo("已思考");
        JsonNode payload = objectMapper.readTree(requestBody.get());
        assertThat(payload.path("model").asText()).isEqualTo("deepseek-v4-flash");
        assertThat(payload.path("thinking").path("type").asText()).isEqualTo("enabled");
        assertThat(payload.path("reasoning_effort").asText()).isEqualTo("high");
    }

    @Test
    void chatCompletionShouldIncludeResponseBodySummaryWhenDeepSeekRejectsRequest() {
        server.createContext("/chat/completions", exchange -> respond(exchange, 400, """
                {"error":{"message":"Model Not Exist"}}
                """));
        DeepSeekClient client = configuredClient("unknown-model");

        assertThatThrownBy(() -> client.chatCompletion(List.of(Map.of("role", "user", "content", "你好"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("状态码=400")
                .hasMessageContaining("Model Not Exist");
    }

    private DeepSeekClient configuredClient(String model) {
        DeepSeekClient client = new DeepSeekClient(objectMapper);
        ReflectionTestUtils.setField(client, "baseUrl", baseUrl);
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
        ReflectionTestUtils.setField(client, "model", model);
        ReflectionTestUtils.setField(client, "maxTokens", 1200);
        return client;
    }

    private void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
