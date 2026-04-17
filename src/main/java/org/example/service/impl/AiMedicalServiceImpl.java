package org.example.service.impl;

import org.example.dao.AiMedicalQaMapper;
import org.example.model.AiMedicalQa;
import org.example.model.MedicalQaRequest;
import org.example.model.MedicalQaResponse;
import org.example.service.AiMedicalService;
import org.example.unity.DeepSeekClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class AiMedicalServiceImpl implements AiMedicalService {

    private static final Logger logger = LoggerFactory.getLogger(AiMedicalServiceImpl.class);
    private static final ExecutorService AI_EXECUTOR = Executors.newFixedThreadPool(2);

    private static final String PHASE_THINKING = "thinking";
    private static final String PHASE_ANSWERING = "answering";
    private static final String PHASE_COMPLETED = "completed";
    private static final String PHASE_FAILED = "failed";

    private static final String THINKING_MESSAGE = "正在分析症状与就诊方向...";
    private static final String ANSWERING_MESSAGE = "正在生成导诊建议...";
    private static final int MAX_HISTORY_ROUNDS = 2;

    private static final String SYSTEM_PROMPT = """
            你现在是愈安伴平台的 AI 导诊助手。
            你的职责是根据用户描述的症状，帮用户梳理症状方向和可能的就诊科室，仅供参考。
            回答要求：
            1. 用简短自然语言回答，适合手机端阅读，不要使用 Markdown 标题、编号或代码块。
            2. 优先包含三类信息：症状方向判断、建议就诊科室、是否需要尽快线下就医或急诊。
            3. 可补充 1 到 2 条基础注意事项，但不要展开长篇病因分析，不要重复用户原话。
            4. 若出现胸痛、呼吸困难、持续高热、意识异常、明显出血等风险信号，要直接建议尽快急诊。
            5. 不要给出处方、药量、检查结果结论，不能替代医生面诊。
            6. 总长度尽量控制在 2 到 3 小段，保持简洁。
            7. 结尾必须保留一句简短免责声明：仅供参考，不能替代医生面诊。
            """;

    private final AiMedicalQaMapper aiMedicalQaMapper;
    private final DeepSeekClient deepSeekClient;

    public AiMedicalServiceImpl(AiMedicalQaMapper aiMedicalQaMapper, DeepSeekClient deepSeekClient) {
        this.aiMedicalQaMapper = aiMedicalQaMapper;
        this.deepSeekClient = deepSeekClient;
    }

    @Override
    public MedicalQaResponse submitQuestion(Integer userId, MedicalQaRequest request) {
        requireUserId(userId);
        String question = normalizeQuestion(request.getQuestion());
        String conversationId = resolveConversationId(userId, request.getConversationId());

        AiMedicalQa record = new AiMedicalQa();
        record.setUserId(userId);
        record.setConversationId(conversationId);
        record.setQuestion(question);
        record.setQaStatus(0);
        record.setThinkingProcess(THINKING_MESSAGE);
        aiMedicalQaMapper.insert(record);

        CompletableFuture.runAsync(() -> generateAnswer(record.getId()), AI_EXECUTOR);

        return toResponse(record);
    }

    @Override
    public MedicalQaResponse getRecord(Integer userId, Long recordId) {
        requireUserId(userId);
        AiMedicalQa record = aiMedicalQaMapper.selectByIdAndUserId(recordId, userId);
        return record == null ? null : toResponse(record);
    }

    @Override
    public List<MedicalQaResponse> getConversation(Integer userId, String conversationId) {
        requireUserId(userId);
        if (!StringUtils.hasText(conversationId)) {
            return List.of();
        }
        String trimmedConversationId = conversationId.trim();
        List<AiMedicalQa> records = aiMedicalQaMapper.selectByUserIdAndConversationId(userId, trimmedConversationId);
        if (records.isEmpty() && aiMedicalQaMapper.countLegacyConversation(trimmedConversationId) > 0) {
            int claimedRows = aiMedicalQaMapper.claimLegacyConversation(userId, trimmedConversationId);
            logger.info("AI 导诊旧会话已归属当前用户, userId={}, conversationId={}, rows={}",
                    userId, trimmedConversationId, claimedRows);
            records = aiMedicalQaMapper.selectByUserIdAndConversationId(userId, trimmedConversationId);
        }

        return records
                .stream()
                .sorted(Comparator.comparing(AiMedicalQa::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AiMedicalQa::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalQaResponse> getLatestConversation(Integer userId) {
        requireUserId(userId);
        String conversationId = aiMedicalQaMapper.selectLatestConversationIdByUserId(userId);
        if (!StringUtils.hasText(conversationId)) {
            return List.of();
        }
        return getConversation(userId, conversationId);
    }

    private void generateAnswer(Long recordId) {
        AiMedicalQa record = aiMedicalQaMapper.selectById(recordId);
        if (record == null) {
            return;
        }

        try {
            updateThinkingProcess(recordId, THINKING_MESSAGE);

            List<AiMedicalQa> conversationRecords = aiMedicalQaMapper.selectByUserIdAndConversationId(
                    record.getUserId(), record.getConversationId());
            List<Map<String, String>> messages = buildMessages(conversationRecords, record);

            updateThinkingProcess(recordId, ANSWERING_MESSAGE);
            String answer = deepSeekClient.chatCompletion(messages);

            AiMedicalQa update = new AiMedicalQa();
            update.setId(recordId);
            update.setQaStatus(1);
            update.setThinkingProcess("回答完成");
            update.setAnswer(formatMedicalAnswer(answer));
            aiMedicalQaMapper.updateAnswerAndStatus(update);
        } catch (Exception e) {
            logger.error("生成 AI 导诊回答失败, recordId={}", recordId, e);
            AiMedicalQa failed = new AiMedicalQa();
            failed.setId(recordId);
            failed.setQaStatus(2);
            failed.setThinkingProcess("服务暂不可用，请稍后重试");
            failed.setAnswer("抱歉，当前 AI 导诊服务暂不可用，请稍后重试。");
            aiMedicalQaMapper.updateAnswerAndStatus(failed);
        }
    }

    private List<Map<String, String>> buildMessages(List<AiMedicalQa> conversationRecords, AiMedicalQa currentRecord) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", SYSTEM_PROMPT));

        List<AiMedicalQa> recentHistory = conversationRecords.stream()
                .filter(item -> item != null && item.getId() != null && !item.getId().equals(currentRecord.getId()))
                .filter(item -> StringUtils.hasText(item.getQuestion()))
                .sorted(Comparator.comparing(AiMedicalQa::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AiMedicalQa::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        int historyStart = Math.max(0, recentHistory.size() - MAX_HISTORY_ROUNDS);
        for (AiMedicalQa item : recentHistory.subList(historyStart, recentHistory.size())) {
            if (item == null || item.getId() == null || item.getId().equals(currentRecord.getId())) {
                continue;
            }
            if (!StringUtils.hasText(item.getQuestion())) {
                continue;
            }
            messages.add(message("user", item.getQuestion().trim()));
            if (item.getQaStatus() != null && item.getQaStatus() == 1 && StringUtils.hasText(item.getAnswer())) {
                messages.add(message("assistant", item.getAnswer().trim()));
            }
        }

        messages.add(message("user", currentRecord.getQuestion().trim()));
        return messages;
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("role", role);
        payload.put("content", content);
        return payload;
    }

    private void updateThinkingProcess(Long recordId, String value) {
        AiMedicalQa update = new AiMedicalQa();
        update.setId(recordId);
        update.setThinkingProcess(value);
        aiMedicalQaMapper.updateThinkingProcess(update);
    }

    private String normalizeQuestion(String question) {
        return StringUtils.hasText(question) ? question.trim() : "";
    }

    private void requireUserId(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("请先登录后再使用 AI 导诊");
        }
    }

    private String resolveConversationId(Integer userId, String requestedConversationId) {
        if (!StringUtils.hasText(requestedConversationId)) {
            return UUID.randomUUID().toString();
        }

        String trimmedConversationId = requestedConversationId.trim();
        int ownedCount = aiMedicalQaMapper.countByUserIdAndConversationId(userId, trimmedConversationId);
        if (ownedCount <= 0) {
            if (aiMedicalQaMapper.countLegacyConversation(trimmedConversationId) > 0) {
                int claimedRows = aiMedicalQaMapper.claimLegacyConversation(userId, trimmedConversationId);
                logger.info("AI 导诊提交时归属旧会话, userId={}, conversationId={}, rows={}",
                        userId, trimmedConversationId, claimedRows);
                return trimmedConversationId;
            }
            logger.warn("AI 导诊会话归属校验失败，已创建新会话, userId={}, conversationId={}", userId, trimmedConversationId);
            return UUID.randomUUID().toString();
        }
        return trimmedConversationId;
    }

    private MedicalQaResponse toResponse(AiMedicalQa record) {
        MedicalQaResponse response = new MedicalQaResponse();
        response.setRecordId(record.getId());
        response.setConversationId(record.getConversationId());
        response.setQuestion(record.getQuestion());
        response.setAnswer(record.getAnswer());
        response.setQaStatus(record.getQaStatus());
        response.setThinkingProcess(resolveThinkingText(record));
        response.setProcessingPhase(resolveProcessingPhase(record));
        response.setCreateTime(record.getCreateTime());
        response.setUpdateTime(record.getUpdateTime());
        return response;
    }

    private String resolveProcessingPhase(AiMedicalQa record) {
        Integer status = record.getQaStatus();
        if (status != null) {
            if (status == 1) return PHASE_COMPLETED;
            if (status == 2) return PHASE_FAILED;
        }

        String thinking = record.getThinkingProcess();
        if (ANSWERING_MESSAGE.equals(thinking)) {
            return PHASE_ANSWERING;
        }
        return PHASE_THINKING;
    }

    private String resolveThinkingText(AiMedicalQa record) {
        String phase = resolveProcessingPhase(record);
        if (PHASE_COMPLETED.equals(phase)) {
            return "已完成";
        }
        if (PHASE_FAILED.equals(phase)) {
            return StringUtils.hasText(record.getThinkingProcess()) ? record.getThinkingProcess() : "服务暂不可用，请稍后重试";
        }
        return StringUtils.hasText(record.getThinkingProcess()) ? record.getThinkingProcess() : THINKING_MESSAGE;
    }

    private String formatMedicalAnswer(String rawAnswer) {
        String answer = StringUtils.hasText(rawAnswer) ? rawAnswer.trim() : "暂未生成有效回答。";
        answer = answer
                .replaceAll("\\*{1,2}", "")
                .replaceAll("#{1,6}", "")
                .replaceAll("`{1,3}", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        answer = answer
                .replace("不能替代专业医生面诊", "不能替代医生面诊")
                .replace("不能替代专业医生的面诊", "不能替代医生面诊")
                .replace("不能替代专业医师面诊", "不能替代医生面诊");

        String[] paragraphs = answer.split("\\n\\n+");
        if (paragraphs.length > 3) {
            answer = String.join("\n\n", List.of(paragraphs).subList(0, 3)).trim();
        }

        if (!answer.contains("仅供参考") && !answer.contains("不能替代医生面诊")) {
            answer = answer + "\n\n仅供参考，不能替代医生面诊。";
        }
        return answer;
    }
}
