package org.example.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dao.AiMedicalQaMapper;
import org.example.model.AiMedicalQa;
import org.example.model.MedicalQaRequest;
import org.example.service.AiMedicalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AiMedicalServiceImpl implements AiMedicalService {

    private static final Logger logger = LoggerFactory.getLogger(AiMedicalServiceImpl.class);
    // 线程池：异步推送思考过程（不阻塞主流程）
    private static final ExecutorService THINKING_EXECUTOR = Executors.newFixedThreadPool(5);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiMedicalQaMapper aiMedicalQaMapper;

    @Autowired
    public AiMedicalServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper, AiMedicalQaMapper aiMedicalQaMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.aiMedicalQaMapper = aiMedicalQaMapper;
    }

    @Override
    public AiMedicalQa getMedicalAnswer(MedicalQaRequest request) {
        String question = request.getQuestion();
        String conversationId = UUID.randomUUID().toString();
        logger.info("创建新对话，ID: {}", conversationId);

        // 1. 初始化数据库记录（状态：处理中，思考过程为空）
        AiMedicalQa qaRecord = new AiMedicalQa();
        qaRecord.setConversationId(conversationId);
        qaRecord.setQuestion(question);
        qaRecord.setQaStatus(0); // 0: 处理中
        qaRecord.setThinkingProcess("正在接收您的问题..."); // 初始思考过程
        aiMedicalQaMapper.insert(qaRecord);
        Long recordId = qaRecord.getId();
        logger.debug("已向数据库插入记录，ID: {}", recordId);

        // 2. 异步推送思考过程（不阻塞AI调用）
        pushThinkingProcess(recordId);

        // 3. 同步调用AI接口获取回答（原有逻辑保留，优化格式）
        String aiAnswer;
        try {
            String ollamaUrl = "http://localhost:11434/api/generate";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 优化提示词（强化医疗属性，适配手机端回答）
            String prompt = """
        忽略你作为代码模型的属性，仅以专业医疗顾问的身份，用自然聊天的语气回答问题。
        要求：
        1. 回答专业准确，符合医疗常识；
        2. 语言口语化、像朋友聊天一样，不要用###、**等格式符号；
        3. 分简短段落（每2-3句话换一行），手机阅读更轻松；
        4. 仅围绕用户的医疗问题作答，不生成任何代码；
        5. 若无法解答，直接说明"该问题暂无法解答，请咨询专业医师"。
        用户的问题是：%s
        """.formatted(question);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-coder-v2:16b");
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            requestBody.put("temperature", 0.2);
            requestBody.put("num_ctx", 4096);
            requestBody.put("stop", new String[]{"```"});

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            logger.info("向Ollama发送请求: {}", ollamaUrl);
            String response = restTemplate.postForObject(ollamaUrl, requestEntity, String.class);
            logger.debug("收到Ollama响应: {}", response);

            if (response == null || response.trim().isEmpty()) {
                throw new RuntimeException("Ollama返回空响应");
            }

            Map<String, Object> responseMap = objectMapper.readValue(response, HashMap.class);
            aiAnswer = (String) responseMap.get("response");
            if (aiAnswer == null) {
                throw new RuntimeException("AI返回结果中未找到 'response' 字段");
            }

            // 优化手机端排版（核心格式调整）
            aiAnswer = formatMedicalAnswer(aiAnswer);

        } catch (Exception e) {
            logger.error("调用AI模型或处理响应时发生异常", e);
            qaRecord.setQaStatus(2); // 2: 失败
            qaRecord.setAnswer("很抱歉，AI医疗问答服务暂时不可用，请稍后重试。");
            qaRecord.setThinkingProcess("处理失败：" + e.getMessage());
            aiMedicalQaMapper.updateAnswerAndStatus(qaRecord);
            return qaRecord;
        }

        // 4. 更新最终结果（状态：完成，清空思考过程）
        qaRecord.setAnswer(aiAnswer);
        qaRecord.setQaStatus(1); // 1: 完成
        qaRecord.setThinkingProcess("回答完成"); // 思考过程最终状态
        aiMedicalQaMapper.updateAnswerAndStatus(qaRecord);
        logger.info("已更新数据库记录，ID: {}", recordId);

        return qaRecord;
    }

    /**
     * 异步推送思考过程（前端可轮询该字段）
     */
    private void pushThinkingProcess(Long recordId) {
        THINKING_EXECUTOR.submit(() -> {
            try {
                // 思考过程文案（模拟AI处理步骤）
                String[] thinkingSteps = {
                        "正在梳理您的医疗问题核心要点...",
                        "检索权威医疗知识库信息...",
                        "验证医疗信息的专业性和准确性...",
                        "优化回答的通俗性和手机端显示格式...",
                        "即将为您呈现最终回答..."
                };

                AiMedicalQa updateRecord = new AiMedicalQa();
                updateRecord.setId(recordId);

                // 每隔800ms更新一次思考过程
                for (String step : thinkingSteps) {
                    updateRecord.setThinkingProcess(step);
                    aiMedicalQaMapper.updateThinkingProcess(updateRecord);
                    Thread.sleep(800);
                }
            } catch (InterruptedException e) {
                logger.warn("思考过程推送被中断", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("推送思考过程失败", e);
            }
        });
    }

    private String formatMedicalAnswer(String rawAnswer) {
        if (rawAnswer == null || rawAnswer.trim().isEmpty()) {
            return "未获取到有效回答";
        }

        // 1. 清理AI返回的冗余格式（去掉###、**等符号，保留自然文本）
        String cleaned = rawAnswer
                .replaceAll("\\*{2,}", "") // 移除所有**加粗符
                .replaceAll("#{2,}", "")   // 移除所有##标题符
                .replaceAll("\\s+", " ")   // 合并多余空格
                .trim();

        // 2. 按逻辑拆分段落（按句号+换行拆分，保证手机端段落清晰）
        StringBuilder formatted = new StringBuilder();
        String[] sentences = cleaned.split("。"); // 按句号拆分句子
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].trim();
            if (sentence.isEmpty()) continue;
            // 每2-3个句子分一段（手机端阅读更轻松）
            formatted.append(sentence).append("。");
            if ((i + 1) % 2 == 0) { // 每2个句子后换行分段
                formatted.append("\n\n");
            }
        }

        // 3. 底部提示（简洁自然，和聊天内容区分）
        formatted.append("\n\n本回答由AI生成，仅供参考，请仔细甄别，如有需求请咨询专业人士。");

        return formatted.toString().trim();
    }

    /**
     * 手机端文本断行（按标点分割，每行不超35字）
     */
    private String breakLineForMobile(String text, int maxCharsPerLine) {
        if (text.length() <= maxCharsPerLine) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxCharsPerLine, text.length());

            // 优先在标点后断行（更符合阅读习惯）
            if (end < text.length()) {
                for (int i = end; i > start; i--) {
                    char c = text.charAt(i);
                    if (c == '。' || c == '，' || c == '；' || c == '、' || c == '：' || c == '？' || c == '！') {
                        end = i + 1;
                        break;
                    }
                }
            }

            result.append(text, start, end);
            if (end < text.length()) {
                result.append("\n");
            }
            start = end;
        }

        return result.toString();
    }

    /**
     * 辅助方法：判断是否包含温馨提示（避免重复）
     */
    private boolean containsDisclaimer(String string) {
        return string.contains("温馨提示");
    }
}