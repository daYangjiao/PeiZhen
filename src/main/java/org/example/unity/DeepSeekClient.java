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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DeepSeekClient {

    private final ObjectMapper objectMapper;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.model:deepseek-reasoner}")
    private String model;

    public DeepSeekClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String chatCompletion(List<Map<String, String>> messages) {
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

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("messages", messages);
            payload.put("temperature", 0.2);
            payload.put("max_tokens", 1200);

            httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (statusCode < 200 || statusCode >= 300) {
                    throw new IllegalStateException("DeepSeek API 调用失败，状态码=" + statusCode);
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
