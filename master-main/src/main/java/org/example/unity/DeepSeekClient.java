package org.example.unity;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DeepSeekClient {
    // 本地Ollama部署的DeepSeek地址（默认端口11434）
    @Value("${deepseek.base-url:http://localhost:11434/api/generate}")
    private String baseUrl;

    @Value("${deepseek.model-name:deepseek-r1:14b}")
    private String modelName;

    /**
     * 医疗问答：单轮对话获取AI回答（原有功能）
     */
    public String getSingleResponse(String prompt) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(baseUrl);

        // 设置请求头（Ollama不需要Authorization）
        httpPost.setHeader("Content-Type", "application/json");

        // 构建Ollama请求参数（stream=false关闭流模式）
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", modelName);
        requestMap.put("prompt", prompt);
        requestMap.put("stream", false);
        requestMap.put("temperature", 0.3); // 降低随机性

        try {
            StringEntity entity = new StringEntity(JSON.toJSONString(requestMap), StandardCharsets.UTF_8);
            httpPost.setEntity(entity);
            CloseableHttpResponse response = httpClient.execute(httpPost);

            // 解析Ollama响应
            HttpEntity responseEntity = response.getEntity();
            String responseStr = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
            JSONObject responseJson = JSON.parseObject(responseStr);
            return responseJson.getString("response");
        } catch (Exception e) {
            throw new RuntimeException("DeepSeek医疗问答调用失败：" + e.getMessage(), e);
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * AI匹配陪诊师：生成专业领域标签（核心方法）
     */
    public List<String> getAttendantTags(String symptoms, String surgeryName, String emergencyLevel) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(baseUrl);
        httpPost.setHeader("Content-Type", "application/json");

        // 构建标签生成提示词
        StringBuilder prompt = new StringBuilder();
        prompt.append("请生成3-5个医疗陪诊师专业领域标签，用于筛选陪诊师：\n");
        prompt.append("用户症状：").append(symptoms).append("\n");
        if (surgeryName != null && !surgeryName.isEmpty()) {
            prompt.append("手术名称：").append(surgeryName).append("\n");
        }
        prompt.append("紧急程度：").append(emergencyLevel).append("\n");
        prompt.append("要求：标签具体（如神经内科陪诊、术后护理），仅返回JSON数组，无其他内容");

        // 构建Ollama请求
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", modelName);
        requestMap.put("prompt", prompt.toString());
        requestMap.put("stream", false);
        requestMap.put("temperature", 0.2);

        try {
            StringEntity entity = new StringEntity(JSON.toJSONString(requestMap), StandardCharsets.UTF_8);
            httpPost.setEntity(entity);
            CloseableHttpResponse response = httpClient.execute(httpPost);

            // 解析标签数组
            String responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JSONObject responseJson = JSON.parseObject(responseStr);
            String tagsStr = responseJson.getString("response");
            return JSON.parseArray(tagsStr, String.class);
        } catch (Exception e) {
            throw new RuntimeException("DeepSeek标签生成失败：" + e.getMessage(), e);
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}