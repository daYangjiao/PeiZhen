package org.example.unity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DeepSeekClient {

    private final ObjectMapper objectMapper;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.model:deepseek-v4-flash}")
    private String model;

    @Value("${deepseek.max-tokens:1200}")
    private Integer maxTokens;

    @Value("${deepseek.thinking-type:disabled}")
    private String thinkingType;

    @Value("${deepseek.reasoning-effort:high}")
    private String reasoningEffort;

    public DeepSeekClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String chatCompletion(List<Map<String, String>> messages) {
        return chatCompletion(messages, null);
    }

    public String chatCompletion(List<Map<String, String>> messages, String modelOverride) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("DeepSeek API Key 未配置");
        }

        String endpoint = resolveEndpoint();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10000)
                .setConnectionRequestTimeout(10000)
                .setSocketTimeout(60000)
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {
            HttpPost httpPost = new HttpPost(endpoint);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + apiKey.trim());

            Map<String, Object> payload = buildPayload(messages, modelOverride);

            httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (statusCode < 200 || statusCode >= 300) {
                    throw new IllegalStateException("DeepSeek API 调用失败，状态码="
                            + statusCode + "，响应=" + summarizeResponseBody(responseBody));
                }

                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode messageNode = root.path("choices").path(0).path("message");
                String content = messageNode.path("content").asText("");
                if (StringUtils.hasText(content)) {
                    return content.trim();
                }

                String reasoningContent = messageNode.path("reasoning_content").asText("");
                if (StringUtils.hasText(reasoningContent)) {
                    return reasoningContent.trim();
                }

                throw new IllegalStateException("DeepSeek 返回内容为空");
            }
        } catch (Exception e) {
            throw new IllegalStateException("DeepSeek 调用失败：" + e.getMessage(), e);
        }
    }

    private String resolveModel(String modelOverride) {
        String requestedModel = resolveRequestedModel(modelOverride);
        if ("deepseek-chat".equals(requestedModel) || "deepseek-reasoner".equals(requestedModel)) {
            return "deepseek-v4-flash";
        }
        return requestedModel;
    }

    private String resolveRequestedModel(String modelOverride) {
        if (StringUtils.hasText(modelOverride)) {
            return modelOverride.trim();
        }
        return StringUtils.hasText(model) ? model.trim() : "deepseek-v4-flash";
    }

    private Map<String, Object> buildPayload(List<Map<String, String>> messages, String modelOverride) {
        String requestedModel = resolveRequestedModel(modelOverride);
        String thinking = resolveThinkingType(requestedModel);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", resolveModel(modelOverride));
        payload.put("messages", messages);
        payload.put("thinking", Map.of("type", thinking));
        if ("enabled".equals(thinking)) {
            payload.put("reasoning_effort", resolveReasoningEffort());
        }
        payload.put("max_tokens", maxTokens == null || maxTokens <= 0 ? 1200 : maxTokens);
        return payload;
    }

    private String resolveThinkingType(String requestedModel) {
        if ("deepseek-reasoner".equals(requestedModel)) {
            return "enabled";
        }
        String normalized = StringUtils.hasText(thinkingType) ? thinkingType.trim().toLowerCase() : "disabled";
        return "enabled".equals(normalized) ? "enabled" : "disabled";
    }

    private String resolveReasoningEffort() {
        String normalized = StringUtils.hasText(reasoningEffort) ? reasoningEffort.trim().toLowerCase() : "high";
        return "max".equals(normalized) || "xhigh".equals(normalized) ? "max" : "high";
    }

    private String summarizeResponseBody(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return "空";
        }
        String compact = responseBody.replaceAll("\\s+", " ").trim();
        return compact.length() <= 300 ? compact : compact.substring(0, 300) + "...";
    }

    private String resolveEndpoint() {
        String normalized = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "https://api.deepseek.com";
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/chat/completions")) {
            return normalized;
        }
        return normalized + "/chat/completions";
    }
}
