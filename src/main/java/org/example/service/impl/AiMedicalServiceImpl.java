package org.example.service.impl;

import org.example.dao.AiMedicalQaMapper;
import org.example.dao.UserMapper;
import org.example.model.AiMedicalQa;
import org.example.model.MedicalQaRequest;
import org.example.model.MedicalQaResponse;
import org.example.model.User;
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
            8. 只能基于当前会话已经提供的信息回答，不要假设用户延续了上一条未明确提到的病情背景。
            9. 如果信息不足，直接基于当前问题说明还需要补充什么，不要借用其他会话信息补全。
            10. 用户询问陪诊、陪诊师、预约陪诊、帮忙找人陪诊时，不得推荐外部平台、外部机构、第三方 APP、健康服务平台、医院服务台或社工部等非愈安伴服务。
            11. 陪诊相关问题只能引导用户使用愈安伴平台内的 AI导诊或预约陪诊流程，说明可在预约页填写医院、就诊时间、就诊事项和特殊需求，由平台匹配可接单陪诊师。
            12. 当前医疗问答页没有陪诊师候选、排班、价格和服务范围数据，不能直接编造或指定某位陪诊师。
            """;
    private static final String NEW_SESSION_CONTEXT_PROMPT = """
            session_mode=new_session
            这是一个全新的导诊会话。你只能根据当前这一次提问中的内容回答，不能假设用户延续了之前窗口的病情、年龄、既往史或检查结果。
            如果当前信息不足，请直接提示用户补充关键症状、持续时间或危险信号，不要自行补全背景。
            """;
    private static final String CONTINUE_SESSION_CONTEXT_PROMPT = """
            session_mode=continue_session
            这是同一导诊会话中的继续追问。你只能参考本会话里已经出现的历史问答，不能借用其他窗口或历史会话的信息。
            如果用户本轮没有明确补充新的病情信息，也不要自行新增设定。
            """;

    private final AiMedicalQaMapper aiMedicalQaMapper;
    private final DeepSeekClient deepSeekClient;
    private final UserMapper userMapper;

    public AiMedicalServiceImpl(AiMedicalQaMapper aiMedicalQaMapper, DeepSeekClient deepSeekClient, UserMapper userMapper) {
        this.aiMedicalQaMapper = aiMedicalQaMapper;
        this.deepSeekClient = deepSeekClient;
        this.userMapper = userMapper;
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
        String userProfileContext = buildUserProfileContext(currentRecord == null ? null : currentRecord.getUserId());
        if (StringUtils.hasText(userProfileContext)) {
            messages.add(message("system", userProfileContext));
        }

        List<AiMedicalQa> recentHistory = conversationRecords.stream()
                .filter(item -> item != null && item.getId() != null && !item.getId().equals(currentRecord.getId()))
                .filter(item -> StringUtils.hasText(item.getQuestion()))
                .sorted(Comparator.comparing(AiMedicalQa::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AiMedicalQa::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        messages.add(message("system", recentHistory.isEmpty()
                ? NEW_SESSION_CONTEXT_PROMPT
                : CONTINUE_SESSION_CONTEXT_PROMPT));

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

    private String buildUserProfileContext(Integer userId) {
        if (userId == null) {
            return "";
        }
        try {
            User user = userMapper.findById(userId);
            if (user == null) {
                return "";
            }
            String name = StringUtils.hasText(user.getName()) ? user.getName().trim() : "未知";
            String sex = normalizePatientSex(user.getSex());
            String age = user.getAge() == null ? "未知" : String.valueOf(user.getAge());
            return String.format(
                    "用户资料：姓名=%s，性别=%s，年龄=%s。称谓规则：性别=男时可以称先生，性别=女时可以称女士；性别未知时不要猜测用户性别，不要使用先生或女士。",
                    name,
                    sex,
                    age
            );
        } catch (Exception e) {
            logger.warn("构建 AI 导诊用户资料上下文失败, userId={}, cause={}", userId, e.getMessage());
            return "";
        }
    }

    private String normalizePatientSex(String sex) {
        if (!StringUtils.hasText(sex)) {
            return "未知";
        }
        String normalized = sex.trim();
        if ("男".equals(normalized) || "男性".equals(normalized) || "male".equalsIgnoreCase(normalized) || "m".equalsIgnoreCase(normalized)) {
            return "男";
        }
        if ("女".equals(normalized) || "女性".equals(normalized) || "female".equalsIgnoreCase(normalized) || "f".equalsIgnoreCase(normalized)) {
            return "女";
        }
        return "未知";
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

        answer = enforcePlatformEscortReferral(answer);

        String[] paragraphs = answer.split("\\n\\n+");
        if (paragraphs.length > 3) {
            answer = String.join("\n\n", List.of(paragraphs).subList(0, 3)).trim();
        }

        if (!answer.contains("仅供参考") && !answer.contains("不能替代医生面诊")) {
            answer = answer + "\n\n仅供参考，不能替代医生面诊。";
        }
        return answer;
    }

    private String enforcePlatformEscortReferral(String answer) {
        if (!StringUtils.hasText(answer)) {
            return answer;
        }
        if (!containsEscortIntent(answer) || !containsExternalEscortReferral(answer)) {
            return answer;
        }

        String[] paragraphs = answer.split("\\n\\n+");
        List<String> safeParagraphs = new ArrayList<>();
        for (String paragraph : paragraphs) {
            String trimmed = paragraph == null ? "" : paragraph.trim();
            if (!StringUtils.hasText(trimmed) || containsExternalEscortReferral(trimmed)) {
                continue;
            }
            safeParagraphs.add(trimmed);
        }

        String platformReferral = "如果需要陪诊支持，建议在愈安伴平台预约页使用 AI导诊填写医院、就诊时间、就诊事项和特殊需求，平台会根据您的需求匹配合适的陪诊师。";
        boolean hasPlatformReferral = safeParagraphs.stream().anyMatch(item -> item.contains("愈安伴平台") || item.contains("平台会根据您的需求匹配"));
        if (!hasPlatformReferral) {
            int disclaimerIndex = -1;
            for (int i = 0; i < safeParagraphs.size(); i++) {
                String paragraph = safeParagraphs.get(i);
                if (paragraph.contains("仅供参考") || paragraph.contains("不能替代医生面诊")) {
                    disclaimerIndex = i;
                    break;
                }
            }
            if (disclaimerIndex >= 0) {
                safeParagraphs.add(disclaimerIndex, platformReferral);
            } else {
                safeParagraphs.add(platformReferral);
            }
        }

        if (safeParagraphs.isEmpty()) {
            safeParagraphs.add(platformReferral);
        }
        return String.join("\n\n", safeParagraphs).trim();
    }

    private boolean containsEscortIntent(String text) {
        return text.contains("陪诊") || text.contains("陪同就诊") || text.contains("陪护就诊");
    }

    private boolean containsExternalEscortReferral(String text) {
        return text.contains("外部平台")
                || text.contains("外部机构")
                || text.contains("第三方")
                || text.contains("线上平台")
                || text.contains("健康服务平台")
                || text.contains("健康服务APP")
                || text.contains("健康服务 App")
                || text.contains("健康服务 app")
                || text.contains("APP")
                || text.contains("App")
                || text.contains("app")
                || text.contains("医院服务台")
                || text.contains("服务台")
                || text.contains("社工部")
                || text.contains("当地大型医院");
    }
}
