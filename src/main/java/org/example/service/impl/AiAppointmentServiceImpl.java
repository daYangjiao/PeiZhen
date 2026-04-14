package org.example.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.AiAppointmentMessageMapper;
import org.example.dao.AiAppointmentSessionMapper;
import org.example.dao.GuideAppointmentMapper;
import org.example.model.AiAppointmentMessageRecord;
import org.example.model.AiAppointmentSessionRecord;
import org.example.model.AiAppointmentStructuredDemand;
import org.example.model.AiAppointmentTimeProposal;
import org.example.model.Attendant;
import org.example.model.GuideAppointment;
import org.example.model.MatchedAttendantVO;
import org.example.model.Order;
import org.example.model.User;
import org.example.model.request.AiAppointmentReplyRequest;
import org.example.model.request.AiAppointmentSessionRequest;
import org.example.model.request.AiAttendantMatchRequest;
import org.example.model.response.AiAppointmentSessionResponse;
import org.example.model.response.AiAttendantMatchResponse;
import org.example.service.AiAppointmentService;
import org.example.service.AttendantService;
import org.example.service.OrderService;
import org.example.unity.DeepSeekClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiAppointmentServiceImpl implements AiAppointmentService {

    private static final String PHASE_THINKING = "thinking";
    private static final String PHASE_ANSWERING = "answering";
    private static final String PHASE_COMPLETED = "completed";
    private static final String PHASE_FAILED = "failed";

    private static final String STATUS_COLLECTING = "COLLECTING";
    private static final String STATUS_READY = "READY";
    private static final String STATUS_MATCHING = "MATCHING";
    private static final String STATUS_MATCHED = "MATCHED";
    private static final String STATUS_FAILED = "FAILED";

    private static final String COLLECTING_MESSAGE = "正在理解您的预约需求...";
    private static final String MATCHING_MESSAGE = "AI 正在为您寻优匹配中...";
    private static final String DEGRADE_REASON = "基于您的需求，系统为您推荐当前高分优质陪诊师。";
    private static final String CONVERSATION_FALLBACK_REPLY = "我先记下这些信息，继续把日期、时间、医院和就诊情况补齐后，就能开始帮您匹配。";
    private static final int MAX_CANDIDATES = 10;
    private static final int MAX_MATCHED = 3;
    private static final int CONVERSATION_TIMEOUT_SECONDS = 18;
    private static final int MATCH_TIMEOUT_SECONDS = 30;
    private static final int MAX_CONVERSATION_HISTORY = 12;
    private static final Set<Integer> OCCUPIED_ORDER_STATUS = Set.of(2, 3, 4, 5, 8);

    private static final Pattern HOSPITAL_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z0-9（）()·]{2,30}?医院(?:[\\u4e00-\\u9fa5A-Za-z0-9（）()·]{0,10}院区)?)");
    private static final Pattern DEPARTMENT_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{1,12}(?:科|门诊))");
    private static final Pattern EXACT_DATE_PATTERN = Pattern.compile("(20\\d{2})[-/.年](\\d{1,2})[-/.月](\\d{1,2})");
    private static final Pattern MONTH_DAY_PATTERN = Pattern.compile("(\\d{1,2})[-/.月](\\d{1,2})(?:日)?");
    private static final Pattern FLEXIBLE_TIME_RANGE_PATTERN = Pattern.compile("([0-2]?\\d(?:[:：.]\\d{1,2})?|[0-2]?\\d点半?|[0-2]?\\d点\\d{1,2}分?)\\s*(?:-|到|至|~|～|—|－)\\s*([0-2]?\\d(?:[:：.]\\d{1,2})?|[0-2]?\\d点半?|[0-2]?\\d点\\d{1,2}分?)");

    private static final List<String> SYMPTOM_SEGMENT_KEYWORDS = List.of(
            "复诊", "检查", "术后", "发烧", "发热", "胸闷", "胸痛", "头晕", "头痛",
            "胃痛", "腹痛", "咳嗽", "恶心", "呕吐", "呼吸困难", "疼", "痛", "不适", "过敏"
    );
    private static final List<String> EXPLICIT_MEDICAL_SCENE_KEYWORDS = List.of(
            "复诊", "复查", "检查", "取药", "拿药", "拿结果", "取结果", "体检", "开药", "问诊", "术后", "换药"
    );
    private static final List<String> REQUIREMENT_SEGMENT_KEYWORDS = List.of(
            "轮椅", "男陪诊", "女陪诊", "急救", "护士", "熟悉医院", "熟悉流程", "耐心",
            "力气大", "跑腿", "陪老人", "陪护", "上门", "帮忙", "取号", "取药", "陪同"
    );
    private static final List<String> PREFERENCE_KEYWORDS = List.of(
            "轮椅", "急救", "护士", "老人陪护", "术后护理", "跑腿", "熟悉医院", "耐心", "力气大", "陪同挂号", "取药"
    );
    private static final List<String> PATIENT_PROFILE_KEYWORDS = List.of("爷爷", "奶奶", "老人", "老爷子", "孩子", "宝宝", "小朋友", "本人");
    private static final List<String> HOSPITAL_PREFIXES = List.of(
            "今天", "明天", "后天", "本周", "下周", "周一", "周二", "周三", "周四", "周五", "周六", "周日", "周天",
            "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日", "星期天",
            "上午", "下午", "晚上", "中午", "凌晨", "早上", "傍晚", "去", "到", "在", "陪", "带", "帮", "给", "和", "要去", "想去"
    );

    private static final String MATCH_SYSTEM_PROMPT = """
            你是一个专业的医疗陪诊派单专家。
            任务：根据用户原始需求与结构化需求，从候选陪诊师 JSON 列表中选出最合适的 1 到 3 位。
            要求：
            1. 只能从候选列表中选择，不得虚构人选。
            2. 输出必须是严格 JSON，不要输出解释性文字、不要加 markdown 代码块。
            3. JSON 顶层格式固定为：
               {"matchedList":[{"attendantId":101,"matchScore":98,"reason":"..."}]}
            4. matchScore 范围 0-100，reason 必须简洁明确，控制在 40 字以内。
            5. reason 必须结合用户的就诊时间、患者情况、医院/科室、性别偏好或技能偏好，不要写空泛结论。
            6. 优先推荐真正契合老人陪护、轮椅协助、急救经验、护士背景、医院熟悉度等具体需求的人选。
            """;

    private static final String CONVERSATION_SYSTEM_PROMPT = """
            你是愈安伴平台的商用 AI 预约助理。
            你的任务是像真实助理一样继续对话、逐步补齐预约字段，但你的输出必须是严格 JSON，不能输出 markdown、不能输出多余解释。

            系统约束：
            1. patientName 由系统提供，不要向用户追问姓名。
            2. 所有字段必须基于用户已明确表达的信息，不能猜测结束时间、医院、症状或其他需求。
            3. 如果用户只说了一个时间点，例如“八点”，不能自动补成 08:00-10:00，必须继续追问结束时间。
            4. 如果用户说的是“八点到十点”这类完整时段，可以提取为 serviceStartTime=08:00、serviceEndTime=10:00。
            5. hospital 只能保留医院本体，不要带“明天去”“想去”之类前缀。
            6. symptomDescription 只放医疗场景、症状、复诊、检查、取药、术后等信息。
            7. otherRequirement 只放陪护偏好和附加诉求，例如 推轮椅、男陪诊师、懂急救、熟悉医院。绝不能等于整段原始输入。
            8. symptomDescription 与 otherRequirement 必须分离。
            9. 一次最多只追问一个关键问题，问题要自然，不要机械列清单。
            10. 当信息已经足够确认时，assistantReply 应是确认口吻，并返回 readyForConfirm=true 与 followUpType=confirm_sheet。
            11. 当你只是建议一个时间段时，只能写入 timeProposal，不能直接把建议时间写进 fieldPatch，除非用户已经明确确认。

            目标字段：
            - hospital
            - department
            - patientProfile
            - serviceDate（YYYY-MM-DD）
            - serviceStartTime（HH:mm）
            - serviceEndTime（HH:mm）
            - symptomDescription
            - otherRequirement
            - attendantGender
            - preferenceTags（数组）
            - symptomTags（数组）

            assistantIntent 仅允许：
            - collect：继续自然收集
            - clarify：某个字段不清楚，需要追问
            - propose_time：基于上下文给出一个待确认的时间建议
            - ready_confirm：字段已足够，提示用户确认
            - acknowledge：用户刚确认了一个选择，先回应再继续

            followUpType 仅允许：
            - free_text
            - date_picker
            - time_picker
            - hospital_input
            - confirm_sheet

            输出 JSON 格式固定为：
            {
              "assistantReply": "当前要对用户说的话",
              "assistantIntent": "collect",
              "questionKey": "",
              "missingFields": [],
              "fieldPatch": {
                "hospital": "",
                "department": "",
                "patientProfile": "",
                "serviceDate": "",
                "serviceStartTime": "",
                "serviceEndTime": "",
                "symptomDescription": "",
                "otherRequirement": "",
                "attendantGender": "",
                "preferenceTags": [],
                "symptomTags": []
              },
              "followUpType": "free_text",
              "timeProposal": {
                "proposedStartTime": "",
                "proposedEndTime": "",
                "proposalText": ""
              },
              "readyForConfirm": false,
              "confirmSummary": {}
            }
            """;

    private final DeepSeekClient deepSeekClient;
    private final AiAppointmentSessionMapper aiAppointmentSessionMapper;
    private final AiAppointmentMessageMapper aiAppointmentMessageMapper;
    private final GuideAppointmentMapper guideAppointmentMapper;
    private final AttendantService attendantService;
    private final OrderService orderService;
    private final org.example.dao.UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final String appointmentModel;

    private final ConcurrentMap<String, SessionState> sessions = new ConcurrentHashMap<>();

    private static final ExecutorService SESSION_EXECUTOR = Executors.newFixedThreadPool(2);
    private static final ExecutorService CONVERSATION_AI_EXECUTOR = Executors.newFixedThreadPool(2);
    private static final ExecutorService MATCH_EXECUTOR = Executors.newFixedThreadPool(2);
    private static final ExecutorService MATCH_AI_EXECUTOR = Executors.newFixedThreadPool(2);

    public AiAppointmentServiceImpl(DeepSeekClient deepSeekClient,
                                    AiAppointmentSessionMapper aiAppointmentSessionMapper,
                                    AiAppointmentMessageMapper aiAppointmentMessageMapper,
                                    GuideAppointmentMapper guideAppointmentMapper,
                                    AttendantService attendantService,
                                    OrderService orderService,
                                    org.example.dao.UserMapper userMapper,
                                    ObjectMapper objectMapper,
                                    @Value("${deepseek.appointment-model:deepseek-chat}") String appointmentModel) {
        this.deepSeekClient = deepSeekClient;
        this.aiAppointmentSessionMapper = aiAppointmentSessionMapper;
        this.aiAppointmentMessageMapper = aiAppointmentMessageMapper;
        this.guideAppointmentMapper = guideAppointmentMapper;
        this.attendantService = attendantService;
        this.orderService = orderService;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.appointmentModel = appointmentModel;
    }

    @Override
    public AiAppointmentSessionResponse createSession(Integer userId, AiAppointmentSessionRequest request) {
        SessionState state = new SessionState();
        state.sessionId = "ai-app-" + UUID.randomUUID();
        state.userId = userId;
        state.rawDemandText = normalize(request.getDemandText());
        state.status = STATUS_COLLECTING;
        state.processingPhase = PHASE_THINKING;
        state.thinkingProcess = COLLECTING_MESSAGE;
        state.message = "";
        state.needMoreInfo = Boolean.TRUE;
        state.canMatch = Boolean.FALSE;
        state.readyForConfirm = Boolean.FALSE;
        state.followUpRound = 0;
        state.structuredDemand = new AiAppointmentStructuredDemand();
        state.structuredDemand.setRawDemandText(state.rawDemandText);
        mergeStructuredDemand(state.structuredDemand, request == null ? null : request.getStructuredDemand());
        if (userId != null) {
            User currentUser = userMapper.findById(userId);
            state.structuredDemand.setPatientName(resolvePatientName(currentUser));
        }
        normalizeStructuredDemandFields(state.structuredDemand);
        appendHistory(state.history, "user", state.rawDemandText);
        sessions.put(state.sessionId, state);
        saveSession(state);
        persistMessage(state.sessionId, "user", state.rawDemandText, null);
        log.info("AI预约会话创建 sessionId={}, userId={}, demand={}",
                state.sessionId, state.userId, buildDemandSummary(state.structuredDemand));

        CompletableFuture.runAsync(() -> analyzeSession(state.sessionId), SESSION_EXECUTOR);
        return toResponse(state);
    }

    @Override
    public AiAppointmentSessionResponse getSession(String sessionId) {
        SessionState state = sessions.get(sessionId);
        if (state == null) {
            state = loadSessionFromDb(sessionId);
            if (state != null) {
                sessions.put(sessionId, state);
            }
        }
        return state == null ? null : toResponse(state);
    }

    @Override
    public AiAppointmentSessionResponse replySession(String sessionId, AiAppointmentReplyRequest request) {
        SessionState state = requireSession(sessionId);
        String replyText = request == null ? "" : normalize(request.getReplyText());
        String syntheticReply = buildSyntheticReplyText(request);
        String mergedReplyText = StringUtils.hasText(replyText) ? replyText : syntheticReply;
        synchronized (state) {
            mergeStructuredDemand(state.structuredDemand, request == null ? null : request.getStructuredDemand());
            mergeStructuredReply(state, request);
            normalizeStructuredDemandFields(state.structuredDemand);
            if (StringUtils.hasText(mergedReplyText)) {
                state.rawDemandText = mergeDemandText(state.rawDemandText, mergedReplyText);
                state.structuredDemand.setRawDemandText(state.rawDemandText);
                appendHistory(state.history, "user", mergedReplyText);
                persistMessage(state.sessionId, "user", mergedReplyText, null);
            }
            state.processingPhase = PHASE_THINKING;
            state.thinkingProcess = COLLECTING_MESSAGE;
            state.message = "";
            state.assistantReply = "";
            state.readyForConfirm = Boolean.FALSE;
            saveSession(state);
        }
        log.info("AI预约补充回复 sessionId={}, userId={}, fieldKey={}, selectedValue={}, demand={}",
                state.sessionId,
                state.userId,
                request == null ? "" : normalize(request.getFieldKey()),
                request == null ? "" : normalize(request.getSelectedValue()),
                buildDemandSummary(state.structuredDemand));
        CompletableFuture.runAsync(() -> analyzeSession(state.sessionId), SESSION_EXECUTOR);
        return toResponse(state);
    }

    @Override
    public AiAppointmentSessionResponse startMatch(String sessionId) {
        SessionState state = requireSession(sessionId);
        synchronized (state) {
            ensureReadyForMatch(state);
            state.status = STATUS_MATCHING;
            state.processingPhase = PHASE_THINKING;
            state.thinkingProcess = "正在分析需求重点...";
            state.message = "";
            state.assistantReply = "";
            state.matchedList = new ArrayList<>();
            state.degraded = Boolean.FALSE;
            state.readyForConfirm = Boolean.FALSE;
            state.followUpType = "";
            saveSession(state);
        }
        log.info("AI预约开始匹配 sessionId={}, userId={}, demand={}",
                state.sessionId, state.userId, buildDemandSummary(state.structuredDemand));
        CompletableFuture.runAsync(() -> doMatch(state.sessionId), MATCH_EXECUTOR);
        return toResponse(state);
    }

    @Override
    public AiAttendantMatchResponse matchAttendants(AiAttendantMatchRequest request) {
        AiAppointmentStructuredDemand demand = request.getStructuredDemand() == null
                ? new AiAppointmentStructuredDemand()
                : request.getStructuredDemand();
        if (StringUtils.hasText(request.getDemandText()) && !StringUtils.hasText(demand.getRawDemandText())) {
            demand.setRawDemandText(request.getDemandText().trim());
        }
        normalizeStructuredDemandFields(demand);
        ensureReadyForMatch(demand);
        MatchBundle bundle;
        try {
            bundle = performMatch(request.getSessionId(), null, demand);
        } catch (Exception e) {
            throw new IllegalStateException("AI 匹配失败：" + e.getMessage(), e);
        }

        AiAttendantMatchResponse response = new AiAttendantMatchResponse();
        response.setSessionId(request.getSessionId());
        response.setAppointmentNo(bundle.appointmentNo);
        response.setMatchedList(bundle.matchedList);
        response.setDegraded(bundle.degraded);
        return response;
    }

    private void analyzeSession(String sessionId) {
        SessionState state = requireSession(sessionId);
        try {
            synchronized (state) {
                state.processingPhase = PHASE_THINKING;
                state.thinkingProcess = COLLECTING_MESSAGE;
            }
            normalizeStructuredDemandFields(state.structuredDemand);

            ConversationEnvelope envelope = null;
            try {
                envelope = runConversationCollection(state);
            } catch (Exception aiError) {
                log.warn("AI预约对话降级为本地追问 sessionId={}, userId={}, cause={}",
                        state.sessionId, state.userId, summarizeException(aiError), aiError);
            }

            synchronized (state) {
                if (envelope != null && envelope.fieldPatch != null) {
                    mergeStructuredDemand(state.structuredDemand, envelope.fieldPatch);
                }
                normalizeStructuredDemandFields(state.structuredDemand);

                List<String> missingFields = collectMissingFields(state.structuredDemand);
                String questionType = resolveQuestionKey(envelope, missingFields);
                String followUpType = determineFollowUpType(
                        envelope == null ? "" : envelope.followUpType,
                        questionType,
                        state.structuredDemand,
                        envelope == null ? "" : envelope.assistantIntent,
                        missingFields.isEmpty()
                );
                boolean readyForConfirm = missingFields.isEmpty();
                String assistantReply = buildAssistantReply(state, envelope, questionType, readyForConfirm);

                state.missingFields = missingFields;
                state.status = readyForConfirm ? STATUS_READY : STATUS_COLLECTING;
                state.processingPhase = PHASE_COMPLETED;
                state.needMoreInfo = !readyForConfirm;
                state.canMatch = readyForConfirm;
                state.readyForConfirm = readyForConfirm;
                state.followUpRound = readyForConfirm ? state.followUpRound : (state.followUpRound == null ? 0 : state.followUpRound) + 1;
                state.questionType = questionType;
                state.questionKey = questionType;
                state.followUpType = followUpType;
                state.options = envelope != null && envelope.options != null && !envelope.options.isEmpty()
                        ? new ArrayList<>(envelope.options)
                        : buildOptions(questionType, state.structuredDemand, state.followUpRound);
                state.thinkingProcess = "已完成";
                state.assistantReply = assistantReply;
                state.assistantIntent = envelope == null ? (readyForConfirm ? "ready_confirm" : "collect") : defaultString(envelope.assistantIntent);
                state.message = assistantReply;
                state.fieldPatch = envelope == null ? null : sanitizeFieldPatch(envelope.fieldPatch);
                state.timeProposal = sanitizeTimeProposal(envelope == null ? null : envelope.timeProposal);
                state.confirmSummary = envelope != null && envelope.confirmSummary != null && !envelope.confirmSummary.isEmpty()
                        ? new LinkedHashMap<>(envelope.confirmSummary)
                        : buildConfirmSummary(state.structuredDemand);
                appendHistory(state.history, "assistant", assistantReply);
                persistMessage(state.sessionId, "assistant", assistantReply, state.fieldPatch);
                saveSession(state);

                log.info("AI预约需求分析完成 sessionId={}, userId={}, readyForConfirm={}, assistantIntent={}, followUpType={}, missingFields={}, demand={}",
                        state.sessionId, state.userId, readyForConfirm, state.assistantIntent, followUpType, missingFields, buildDemandSummary(state.structuredDemand));
            }
        } catch (Exception e) {
            log.error("分析 AI 预约需求失败 sessionId={}, userId={}, cause={}",
                    state.sessionId, state.userId, summarizeException(e), e);
            markFailed(state, "服务暂不可用，请稍后重试");
        }
    }

    private void doMatch(String sessionId) {
        SessionState state = requireSession(sessionId);
        try {
            synchronized (state) {
                state.processingPhase = PHASE_ANSWERING;
                state.thinkingProcess = MATCHING_MESSAGE;
            }
            MatchBundle bundle = performMatch(state.sessionId, state.userId, state.structuredDemand);
            synchronized (state) {
                state.status = STATUS_MATCHED;
                state.processingPhase = PHASE_COMPLETED;
                state.thinkingProcess = "已完成";
                state.needMoreInfo = Boolean.FALSE;
                state.canMatch = Boolean.TRUE;
                state.matchedList = bundle.matchedList;
                state.appointmentNo = bundle.appointmentNo;
                state.degraded = bundle.degraded;
                state.message = bundle.degraded
                        ? "AI超时，已先为您返回高分推荐，可先选择合适的陪诊师。"
                        : "已为您匹配到更合适的陪诊师，请查看推荐结果。";
                state.assistantReply = state.message;
                state.assistantIntent = "acknowledge";
                state.questionKey = "";
                state.timeProposal = null;
                appendHistory(state.history, "assistant", state.assistantReply);
                persistMessage(state.sessionId, "assistant", state.assistantReply, null);
                saveSession(state);
            }
        } catch (Exception e) {
            log.error("AI 预约匹配失败 sessionId={}, userId={}, cause={}",
                    state.sessionId, state.userId, summarizeException(e), e);
            markFailed(state, "匹配失败，请稍后重试");
        }
    }

    private MatchBundle performMatch(String sessionId, Integer userId, AiAppointmentStructuredDemand demand) throws Exception {
        ensureReadyForMatch(demand);
        List<Attendant> roughCandidates = attendantService.findAiCandidates(20);
        List<Attendant> filtered = filterAvailableCandidates(roughCandidates, demand);
        List<Attendant> shortlist = filtered.stream().limit(MAX_CANDIDATES).collect(Collectors.toList());
        if (shortlist.isEmpty()) {
            throw new IllegalStateException("当前暂无可用陪诊师");
        }
        log.info("AI预约候选筛选完成 sessionId={}, userId={}, candidateCount={}, shortlistCount={}, demand={}",
                defaultString(sessionId), userId, roughCandidates.size(), shortlist.size(), buildDemandSummary(demand));

        String appointmentNo = createAppointmentIfNeeded(userId, demand);
        List<MatchedAttendantVO> degraded = buildMatchedVoList(shortlist.stream().limit(MAX_MATCHED).collect(Collectors.toList()), null, true);

        try {
            List<MatchedAttendantVO> aiMatched = runAiRanking(demand, shortlist);
            if (aiMatched.isEmpty()) {
                throw new IllegalStateException("DeepSeek 未返回有效推荐结果");
            }
            MatchBundle bundle = new MatchBundle();
            bundle.appointmentNo = appointmentNo;
            bundle.matchedList = aiMatched;
            bundle.degraded = Boolean.FALSE;
            log.info("AI预约匹配成功 sessionId={}, userId={}, candidateCount={}, matchedCount={}, degraded=false, demand={}",
                    defaultString(sessionId), userId, shortlist.size(), aiMatched.size(), buildDemandSummary(demand));
            return bundle;
        } catch (Exception e) {
            log.warn("AI预约匹配降级 sessionId={}, userId={}, missingFields={}, candidateCount={}, degraded=true, cause={}, demand={}",
                    defaultString(sessionId),
                    userId,
                    collectMissingFields(demand),
                    shortlist.size(),
                    summarizeException(e),
                    buildDemandSummary(demand),
                    e);
            MatchBundle bundle = new MatchBundle();
            bundle.appointmentNo = appointmentNo;
            bundle.matchedList = degraded;
            bundle.degraded = Boolean.TRUE;
            return bundle;
        }
    }

    private List<MatchedAttendantVO> runAiRanking(AiAppointmentStructuredDemand demand, List<Attendant> shortlist) throws Exception {
        Map<Integer, Attendant> candidateMap = shortlist.stream()
                .filter(item -> item.getUserId() != null)
                .collect(Collectors.toMap(Attendant::getUserId, item -> item, (left, right) -> left, LinkedHashMap::new));

        Map<String, Object> promptPayload = new LinkedHashMap<>();
        promptPayload.put("rawDemandText", defaultString(demand.getRawDemandText()));
        promptPayload.put("structuredDemand", buildStructuredPromptPayload(demand));
        promptPayload.put("candidates", shortlist.stream().map(this::buildCandidatePayload).collect(Collectors.toList()));

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", MATCH_SYSTEM_PROMPT));
        messages.add(message("user", objectMapper.writeValueAsString(promptPayload)));

        long startedAt = System.currentTimeMillis();
        CompletableFuture<String> future = CompletableFuture.supplyAsync(
                () -> deepSeekClient.chatCompletion(messages, appointmentModel),
                MATCH_AI_EXECUTOR
        );
        String raw = future.get(MATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startedAt;
        log.info("AI预约模型返回 model={}, elapsedMs={}, rawPreview={}",
                appointmentModel,
                elapsed,
                abbreviate(raw, 240));
        JsonNode root = objectMapper.readTree(extractJson(raw));
        JsonNode listNode = root.path("matchedList");
        if (!listNode.isArray()) {
            throw new IllegalStateException("DeepSeek 返回结果缺少 matchedList");
        }

        List<MatchedAttendantVO> result = new ArrayList<>();
        for (JsonNode item : listNode) {
            Integer attendantId = item.path("attendantId").isNumber() ? item.path("attendantId").asInt() : null;
            if (attendantId == null || !candidateMap.containsKey(attendantId)) {
                continue;
            }
            Attendant candidate = candidateMap.get(attendantId);
            MatchedAttendantVO vo = buildMatchedVo(candidate);
            int matchScore = item.path("matchScore").isNumber() ? item.path("matchScore").asInt() : 90;
            vo.setMatchScore(matchScore);
            vo.setReason(normalizeReason(item.path("reason").asText("")));
            result.add(vo);
            if (result.size() >= MAX_MATCHED) {
                break;
            }
        }
        if (result.isEmpty()) {
            throw new IllegalStateException("DeepSeek 未返回有效推荐结果");
        }
        return result;
    }

    private List<Attendant> filterAvailableCandidates(List<Attendant> candidates, AiAppointmentStructuredDemand demand) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<Order> occupiedOrders = orderService.findAllOrders().stream()
                .filter(order -> order.getAttendantId() != null)
                .filter(order -> OCCUPIED_ORDER_STATUS.contains(order.getOrderStatus()))
                .collect(Collectors.toList());

        return candidates.stream()
                .filter(candidate -> candidate.getUserId() != null)
                .filter(candidate -> !hasConflict(candidate.getUserId(), demand, occupiedOrders))
                .sorted(Comparator
                        .comparing((Attendant item) -> item.getScore() == null ? 0D : item.getScore()).reversed()
                        .thenComparing(item -> item.getServiceCount() == null ? 0 : item.getServiceCount(), Comparator.reverseOrder())
                        .thenComparing(item -> item.getExperienceYears() == null ? 0 : item.getExperienceYears(), Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private boolean hasConflict(Integer attendantUserId, AiAppointmentStructuredDemand demand, List<Order> occupiedOrders) {
        if (!StringUtils.hasText(demand.getServiceDate())
                || !StringUtils.hasText(demand.getServiceStartTime())
                || !StringUtils.hasText(demand.getServiceEndTime())) {
            return false;
        }
        int start = timeToMinutes(demand.getServiceStartTime());
        int end = timeToMinutes(demand.getServiceEndTime());
        for (Order order : occupiedOrders) {
            if (!attendantUserId.equals(order.getAttendantId())) {
                continue;
            }
            if (!Objects.equals(normalize(order.getServiceDate()), normalize(demand.getServiceDate()))) {
                continue;
            }
            String[] slot = splitTimeSlot(order.getServiceTimeSlot());
            if (slot == null) {
                continue;
            }
            int orderStart = timeToMinutes(slot[0]);
            int orderEnd = timeToMinutes(slot[1]);
            if (isTimeOverlap(start, end, orderStart, orderEnd)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTimeOverlap(int startA, int endA, int startB, int endB) {
        List<int[]> rangeA = normalizeRanges(startA, endA);
        List<int[]> rangeB = normalizeRanges(startB, endB);
        for (int[] a : rangeA) {
            for (int[] b : rangeB) {
                if (Math.max(a[0], b[0]) < Math.min(a[1], b[1])) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<int[]> normalizeRanges(int start, int end) {
        if (end > start) {
            return List.of(new int[]{start, end});
        }
        if (end == start) {
            return List.of(new int[]{start, start + 30});
        }
        return List.of(new int[]{start, 24 * 60}, new int[]{0, end});
    }

    private String createAppointmentIfNeeded(Integer userId, AiAppointmentStructuredDemand demand) {
        User user = userId == null ? null : userMapper.findById(userId);
        GuideAppointment appointment = new GuideAppointment();
        appointment.setAppointmentNo("APP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6));
        appointment.setUserId(userId);
        appointment.setPatientName(StringUtils.hasText(demand.getPatientName()) ? demand.getPatientName().trim() : resolvePatientName(user));
        appointment.setPatientPhone(resolvePatientPhone(user));
        appointment.setSymptoms(toSymptoms(demand));
        appointment.setHospitalName(defaultString(demand.getHospital()));
        appointment.setServiceTypeNumber(demand.getServiceTypeNumber() == null ? 1 : demand.getServiceTypeNumber());
        appointment.setServiceDate(defaultString(demand.getServiceDate()));
        appointment.setServiceStartTime(defaultString(demand.getServiceStartTime()));
        appointment.setServiceEndTime(defaultString(demand.getServiceEndTime()));
        appointment.setOtherRequirement(StringUtils.hasText(demand.getOtherRequirement()) ? demand.getOtherRequirement().trim() : "无");
        appointment.setCreateTime(new Date());
        guideAppointmentMapper.insertGuideAppointment(appointment);
        return appointment.getAppointmentNo();
    }

    private String resolvePatientName(User user) {
        if (user == null) {
            return "就诊人";
        }
        if (StringUtils.hasText(user.getName())) {
            return user.getName().trim();
        }
        return StringUtils.hasText(user.getPhone()) ? user.getPhone().trim() : "就诊人";
    }

    private String resolvePatientPhone(User user) {
        if (user == null || !StringUtils.hasText(user.getPhone())) {
            return "未填写";
        }
        return user.getPhone().trim();
    }

    private List<String> toSymptoms(AiAppointmentStructuredDemand demand) {
        LinkedHashSet<String> all = new LinkedHashSet<>();
        if (demand.getSymptomTags() != null) {
            all.addAll(demand.getSymptomTags().stream().filter(StringUtils::hasText).map(String::trim).collect(Collectors.toList()));
        }
        if (StringUtils.hasText(demand.getSymptomDescription())) {
            for (String item : splitFreeText(demand.getSymptomDescription())) {
                if (StringUtils.hasText(item)) {
                    all.add(item.trim());
                }
            }
        }
        if (StringUtils.hasText(demand.getDepartment())) {
            all.add(demand.getDepartment().trim());
        }
        return new ArrayList<>(all);
    }

    private Map<String, Object> buildCandidatePayload(Attendant item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", item.getUserId());
        payload.put("name", item.getName());
        payload.put("gender", item.getSex());
        payload.put("skills", item.getProfessionalField());
        payload.put("rating", item.getScore());
        payload.put("completedOrders", item.getServiceCount() == null ? 0 : item.getServiceCount());
        payload.put("experienceYears", item.getExperienceYears());
        payload.put("introduction", item.getIntroduction());
        payload.put("hospitalName", item.getHospitalName());
        return payload;
    }

    private List<MatchedAttendantVO> buildMatchedVoList(List<Attendant> candidates, Map<Integer, String> reasonMap, boolean degraded) {
        List<MatchedAttendantVO> list = new ArrayList<>();
        for (Attendant candidate : candidates) {
            MatchedAttendantVO vo = buildMatchedVo(candidate);
            vo.setMatchScore(degraded ? 88 : 95);
            vo.setReason(reasonMap == null ? DEGRADE_REASON : normalizeReason(reasonMap.get(candidate.getUserId())));
            list.add(vo);
        }
        return list;
    }

    private MatchedAttendantVO buildMatchedVo(Attendant candidate) {
        MatchedAttendantVO vo = new MatchedAttendantVO();
        vo.setAttendantId(candidate.getUserId());
        vo.setAttendantName(candidate.getName());
        vo.setAvatar(candidate.getAvatarUrl());
        vo.setGender(candidate.getSex());
        vo.setAttendantPhone(candidate.getPhone());
        vo.setSpecialty(candidate.getProfessionalField());
        vo.setScore(candidate.getScore());
        vo.setExperience(candidate.getExperienceYears() == null ? "经验待完善" : candidate.getExperienceYears() + "年经验");
        vo.setExperienceYears(candidate.getExperienceYears());
        vo.setProfessionalField(candidate.getProfessionalField());
        vo.setIntroduction(candidate.getIntroduction());
        vo.setCompletedOrders(candidate.getServiceCount() == null ? 0 : candidate.getServiceCount());
        return vo;
    }

    private void parseDemandTextInto(AiAppointmentStructuredDemand demand, String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        String source = text.trim();
        demand.setRawDemandText(source);
        if (!StringUtils.hasText(demand.getPatientName())) {
            demand.setPatientName("就诊人");
        }

        if (!StringUtils.hasText(demand.getHospital())) {
            String hospital = extractHospitalName(source);
            if (StringUtils.hasText(hospital)) {
                demand.setHospital(hospital);
            }
        }

        if (!StringUtils.hasText(demand.getDepartment())) {
            Matcher departmentMatcher = DEPARTMENT_PATTERN.matcher(source);
            while (departmentMatcher.find()) {
                String value = departmentMatcher.group(1);
                if (value.contains("医院")) {
                    continue;
                }
                demand.setDepartment(value);
                break;
            }
        }

        if (!StringUtils.hasText(demand.getServiceDate())) {
            String parsedDate = extractDate(source);
            if (StringUtils.hasText(parsedDate)) {
                demand.setServiceDate(parsedDate);
            }
        }

        if (!StringUtils.hasText(demand.getServiceStartTime()) || !StringUtils.hasText(demand.getServiceEndTime())) {
            TimeWindow exactWindow = extractExactTimeWindow(source);
            if (exactWindow != null) {
                demand.setTimePeriod(exactWindow.label);
                demand.setServiceStartTime(exactWindow.start);
                demand.setServiceEndTime(exactWindow.end);
            } else if (!StringUtils.hasText(demand.getTimePeriod())) {
                String quickPeriod = extractQuickTimePeriod(source);
                if (StringUtils.hasText(quickPeriod)) {
                    demand.setTimePeriod(quickPeriod);
                }
            }
        }

        if (!StringUtils.hasText(demand.getAttendantGender())) {
            if (source.contains("男陪诊") || source.contains("男陪诊师") || source.contains("男性")) {
                demand.setAttendantGender("男");
            } else if (source.contains("女陪诊") || source.contains("女陪诊师") || source.contains("女性")) {
                demand.setAttendantGender("女");
            }
        }

        if (demand.getServiceTypeNumber() == null) {
            if (source.contains("术后")) {
                demand.setServiceTypeNumber(2);
            } else if (source.contains("急诊")) {
                demand.setServiceTypeNumber(3);
            } else if (source.contains("上门")) {
                demand.setServiceTypeNumber(4);
            } else {
                demand.setServiceTypeNumber(1);
            }
        }

        if (!StringUtils.hasText(demand.getPatientProfile())) {
            demand.setPatientProfile(extractPatientProfile(source));
        }

        classifyDemandDetails(demand, source);
    }

    private String extractDate(String text) {
        LocalDate today = LocalDate.now();
        Matcher exactMatcher = EXACT_DATE_PATTERN.matcher(text);
        if (exactMatcher.find()) {
            return String.format(Locale.ROOT, "%s-%02d-%02d",
                    exactMatcher.group(1),
                    Integer.parseInt(exactMatcher.group(2)),
                    Integer.parseInt(exactMatcher.group(3)));
        }

        Matcher monthDayMatcher = MONTH_DAY_PATTERN.matcher(text);
        if (monthDayMatcher.find()) {
            int month = Integer.parseInt(monthDayMatcher.group(1));
            int day = Integer.parseInt(monthDayMatcher.group(2));
            LocalDate parsed = LocalDate.of(today.getYear(), month, day);
            if (parsed.isBefore(today)) {
                parsed = parsed.plusYears(1);
            }
            return parsed.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        if (text.contains("今天")) {
            return today.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (text.contains("明天")) {
            return today.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (text.contains("后天")) {
            return today.plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (text.contains("本周")) {
            DayOfWeek week = parseWeekday(text);
            if (week != null) {
                LocalDate target = today.with(TemporalAdjusters.nextOrSame(week));
                return target.format(DateTimeFormatter.ISO_LOCAL_DATE);
            }
        }
        if (text.contains("下周")) {
            DayOfWeek week = parseWeekday(text);
            if (week != null) {
                LocalDate target = today.with(TemporalAdjusters.next(week));
                return target.format(DateTimeFormatter.ISO_LOCAL_DATE);
            }
        }
        return "";
    }

    private DayOfWeek parseWeekday(String text) {
        if (text.contains("周一") || text.contains("星期一")) return DayOfWeek.MONDAY;
        if (text.contains("周二") || text.contains("星期二")) return DayOfWeek.TUESDAY;
        if (text.contains("周三") || text.contains("星期三")) return DayOfWeek.WEDNESDAY;
        if (text.contains("周四") || text.contains("星期四")) return DayOfWeek.THURSDAY;
        if (text.contains("周五") || text.contains("星期五")) return DayOfWeek.FRIDAY;
        if (text.contains("周六") || text.contains("星期六")) return DayOfWeek.SATURDAY;
        if (text.contains("周日") || text.contains("周天") || text.contains("星期日") || text.contains("星期天")) return DayOfWeek.SUNDAY;
        return null;
    }

    private TimeWindow extractExactTimeWindow(String text) {
        String normalizedText = normalizeChineseTimeExpression(text);
        Matcher rangeMatcher = FLEXIBLE_TIME_RANGE_PATTERN.matcher(normalizedText);
        if (rangeMatcher.find()) {
            String periodHint = resolvePeriodHint(normalizedText, rangeMatcher.start());
            String start = parseFlexibleTimeToken(rangeMatcher.group(1), periodHint);
            String end = parseFlexibleTimeToken(rangeMatcher.group(2), periodHint);
            if (StringUtils.hasText(start) && StringUtils.hasText(end)) {
                return new TimeWindow(start, end, "具体时间");
            }
        }
        return null;
    }

    private String extractQuickTimePeriod(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        if (text.contains("上午")) return "上午";
        if (text.contains("下午")) return "下午";
        if (text.contains("晚上")) return "晚上";
        if (text.contains("中午")) return "中午";
        return "";
    }

    private String extractHospitalName(String text) {
        Matcher hospitalMatcher = HOSPITAL_PATTERN.matcher(text);
        while (hospitalMatcher.find()) {
            String candidate = cleanupHospitalCandidate(hospitalMatcher.group(1));
            if (StringUtils.hasText(candidate) && candidate.endsWith("医院")) {
                return candidate;
            }
        }
        return "";
    }

    private String cleanupHospitalCandidate(String candidate) {
        String cleaned = normalize(candidate);
        boolean changed = true;
        while (changed && StringUtils.hasText(cleaned)) {
            changed = false;
            for (String prefix : HOSPITAL_PREFIXES) {
                if (cleaned.startsWith(prefix)) {
                    cleaned = cleaned.substring(prefix.length()).trim();
                    changed = true;
                }
            }
        }
        return cleaned;
    }

    private String extractPatientProfile(String source) {
        String age = "";
        Matcher ageMatcher = Pattern.compile("(\\d{1,3})岁").matcher(source);
        if (ageMatcher.find()) {
            age = ageMatcher.group(1) + "岁";
        }
        if (source.contains("爷爷") || source.contains("奶奶") || source.contains("老人") || source.contains("老爷子")) {
            return age + "老人就诊";
        }
        if (source.contains("孩子") || source.contains("宝宝") || source.contains("小朋友")) {
            return age + "儿童就诊";
        }
        if (source.contains("本人")) {
            return age + "本人就诊";
        }
        return age;
    }

    private void classifyDemandDetails(AiAppointmentStructuredDemand demand, String source) {
        LinkedHashSet<String> preferenceTags = new LinkedHashSet<>(safeList(demand.getPreferenceTags()));
        LinkedHashSet<String> symptomTags = new LinkedHashSet<>(safeList(demand.getSymptomTags()));
        LinkedHashSet<String> symptomSegments = new LinkedHashSet<>(splitFreeText(demand.getSymptomDescription()));
        LinkedHashSet<String> requirementSegments = new LinkedHashSet<>(splitFreeText(demand.getOtherRequirement()));

        for (String keyword : PREFERENCE_KEYWORDS) {
            if (source.contains(keyword.replace("陪护", "")) || source.contains(keyword)) {
                preferenceTags.add(keyword);
            }
        }

        for (String segment : splitDemandSegments(source)) {
            String cleaned = stripStructuredTokens(segment, demand);
            if (!StringUtils.hasText(cleaned)) {
                continue;
            }
            if (looksLikeSymptomSegment(cleaned)) {
                symptomSegments.add(cleaned);
            }
            if (looksLikeRequirementSegment(cleaned)) {
                requirementSegments.add(cleaned);
            }
        }

        for (String segment : symptomSegments) {
            for (String keyword : SYMPTOM_SEGMENT_KEYWORDS) {
                if (segment.contains(keyword)) {
                    symptomTags.add(keyword);
                }
            }
            if (StringUtils.hasText(demand.getDepartment()) && segment.contains(demand.getDepartment())) {
                symptomTags.add(demand.getDepartment().trim());
            }
        }

        for (String segment : requirementSegments) {
            for (String keyword : REQUIREMENT_SEGMENT_KEYWORDS) {
                if (segment.contains(keyword)) {
                    preferenceTags.add(keyword);
                }
            }
        }

        demand.setPreferenceTags(new ArrayList<>(preferenceTags));
        demand.setSymptomTags(new ArrayList<>(symptomTags));
        String symptomDescription = joinSegments(symptomSegments);
        if (!StringUtils.hasText(symptomDescription)) {
            symptomDescription = inferMedicalSceneDescription(demand, source);
        }
        demand.setSymptomDescription(symptomDescription);
        demand.setOtherRequirement(joinSegments(mergeRequirements(requirementSegments, preferenceTags, demand.getAttendantGender())));
    }

    private List<String> mergeRequirements(Set<String> requirementSegments, Set<String> preferenceTags, String attendantGender) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(requirementSegments);
        merged.addAll(preferenceTags);
        if (StringUtils.hasText(attendantGender)) {
            merged.add(attendantGender.trim() + "陪诊师");
        }
        return new ArrayList<>(merged);
    }

    private List<String> splitDemandSegments(String source) {
        return Arrays.stream(source.split("[，,。；;、\\n]|(?=需要)|(?=希望)|(?=优先)|(?=最好)"))
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private List<String> splitFreeText(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split("[，,。；;、\\n]"))
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private String stripStructuredTokens(String segment, AiAppointmentStructuredDemand demand) {
        String cleaned = normalize(segment)
                .replaceAll("\\s+", "")
                .replaceAll("(今天|明天|后天|本周[一二三四五六日天]?|下周[一二三四五六日天]?|周[一二三四五六日天]|星期[一二三四五六日天]|上午|下午|晚上|中午|凌晨|早上|傍晚)", "")
                .replaceAll("\\d{1,3}岁", "")
                .replaceAll("(\\d{1,2}(?:[:：.]\\d{1,2})?|\\d{1,2}点半?|\\d{1,2}点\\d{1,2}分?)\\s*(?:-|到|至|~|～|—|－)\\s*(\\d{1,2}(?:[:：.]\\d{1,2})?|\\d{1,2}点半?|\\d{1,2}点\\d{1,2}分?)", "")
                .replaceAll("(\\d{1,2}(?:[:：.]\\d{1,2})?|\\d{1,2}点半?|\\d{1,2}点\\d{1,2}分?)", "")
                .replaceAll("^(去|到|在|陪|带|帮|给|想|需要|希望|安排)+", "");
        if (StringUtils.hasText(demand.getHospital())) {
            cleaned = cleaned.replace(demand.getHospital(), "");
        }
        if (StringUtils.hasText(demand.getDepartment())) {
            cleaned = cleaned.replace(demand.getDepartment(), "");
        }
        for (String keyword : PATIENT_PROFILE_KEYWORDS) {
            cleaned = cleaned.replace(keyword, "");
        }
        return normalize(cleaned);
    }

    private boolean looksLikeSymptomSegment(String segment) {
        if (!StringUtils.hasText(segment)) {
            return false;
        }
        return segment.contains("复诊")
                || segment.contains("检查")
                || segment.contains("术后")
                || containsAny(segment, SYMPTOM_SEGMENT_KEYWORDS);
    }

    private boolean looksLikeRequirementSegment(String segment) {
        if (!StringUtils.hasText(segment)) {
            return false;
        }
        return segment.contains("需要")
                || segment.contains("希望")
                || segment.contains("优先")
                || segment.contains("最好")
                || segment.contains("陪诊师")
                || containsAny(segment, REQUIREMENT_SEGMENT_KEYWORDS);
    }

    private boolean hasEnoughMedicalIntent(AiAppointmentStructuredDemand demand) {
        if (demand == null) {
            return false;
        }
        if (StringUtils.hasText(demand.getSymptomDescription())) {
            return true;
        }
        if (demand.getSymptomTags() != null && !demand.getSymptomTags().isEmpty()) {
            return true;
        }
        String rawDemandText = defaultString(demand.getRawDemandText());
        for (String keyword : EXPLICIT_MEDICAL_SCENE_KEYWORDS) {
            if (rawDemandText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String inferMedicalSceneDescription(AiAppointmentStructuredDemand demand, String source) {
        if (!StringUtils.hasText(source)) {
            return "";
        }
        for (String keyword : EXPLICIT_MEDICAL_SCENE_KEYWORDS) {
            if (source.contains(keyword)) {
                if (StringUtils.hasText(demand.getDepartment())) {
                    return demand.getDepartment().trim() + keyword;
                }
                return keyword;
            }
        }
        return "";
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String joinSegments(Iterable<String> segments) {
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String segment : segments) {
            if (StringUtils.hasText(segment)) {
                cleaned.add(segment.trim());
            }
        }
        return cleaned.isEmpty() ? "" : String.join("，", cleaned);
    }

    private List<String> safeList(List<String> source) {
        return source == null ? List.of() : source.stream()
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private String parseFlexibleTimeToken(String token) {
        return parseFlexibleTimeToken(token, "");
    }

    private String parseFlexibleTimeToken(String token, String periodHint) {
        String cleaned = normalizeChineseTimeExpression(token)
                .replace('：', ':')
                .replace('．', '.')
                .replace('。', '.');
        if (!StringUtils.hasText(cleaned)) {
            return "";
        }
        String finalPeriodHint = StringUtils.hasText(periodHint) ? periodHint : resolvePeriodHint(cleaned, 0);
        cleaned = cleaned.replace("上午", "")
                .replace("下午", "")
                .replace("晚上", "")
                .replace("傍晚", "")
                .replace("中午", "")
                .replace("凌晨", "")
                .replace("早上", "");

        int hour = -1;
        int minute = 0;
        Matcher halfMatcher = Pattern.compile("^(\\d{1,2})点半$").matcher(cleaned);
        if (halfMatcher.find()) {
            hour = Integer.parseInt(halfMatcher.group(1));
            minute = 30;
            return formatTime(applyPeriodHint(hour, finalPeriodHint), minute);
        }
        Matcher minuteMatcher = Pattern.compile("^(\\d{1,2})点(\\d{1,2})分?$").matcher(cleaned);
        if (minuteMatcher.find()) {
            hour = Integer.parseInt(minuteMatcher.group(1));
            minute = Integer.parseInt(minuteMatcher.group(2));
            return formatTime(applyPeriodHint(hour, finalPeriodHint), minute);
        }
        Matcher hourMatcher = Pattern.compile("^(\\d{1,2})点$").matcher(cleaned);
        if (hourMatcher.find()) {
            hour = Integer.parseInt(hourMatcher.group(1));
            return formatTime(applyPeriodHint(hour, finalPeriodHint), 0);
        }
        Matcher separatedMatcher = Pattern.compile("^(\\d{1,2})[:.](\\d{1,2})$").matcher(cleaned);
        if (separatedMatcher.find()) {
            hour = Integer.parseInt(separatedMatcher.group(1));
            minute = Integer.parseInt(separatedMatcher.group(2));
            return formatTime(applyPeriodHint(hour, finalPeriodHint), minute);
        }
        return "";
    }

    private String normalizeChineseTimeExpression(String text) {
        String normalized = normalize(text)
                .replace('：', ':')
                .replace('．', '.')
                .replace('。', '.')
                .replace("两", "二");
        StringBuilder builder = new StringBuilder();
        int index = 0;
        while (index < normalized.length()) {
            char current = normalized.charAt(index);
            if (isChineseDigitChar(current)) {
                int end = index;
                while (end < normalized.length() && isChineseDigitChar(normalized.charAt(end))) {
                    end++;
                }
                String token = normalized.substring(index, end);
                Integer numeric = parseChineseNumber(token);
                if (numeric != null) {
                    builder.append(numeric);
                } else {
                    builder.append(token);
                }
                index = end;
                continue;
            }
            builder.append(current);
            index++;
        }
        return builder.toString();
    }

    private boolean isChineseDigitChar(char value) {
        return "零一二三四五六七八九十".indexOf(value) >= 0;
    }

    private Integer parseChineseNumber(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        if ("十".equals(token)) {
            return 10;
        }
        int tenIndex = token.indexOf('十');
        if (tenIndex >= 0) {
            String left = token.substring(0, tenIndex);
            String right = token.substring(tenIndex + 1);
            int tens = StringUtils.hasText(left) ? chineseDigitValue(left.charAt(0)) : 1;
            int units = StringUtils.hasText(right) ? chineseDigitValue(right.charAt(0)) : 0;
            if (tens < 0 || units < 0) {
                return null;
            }
            return tens * 10 + units;
        }
        if (token.length() == 1) {
            int value = chineseDigitValue(token.charAt(0));
            return value >= 0 ? value : null;
        }
        return null;
    }

    private int chineseDigitValue(char value) {
        return switch (value) {
            case '零' -> 0;
            case '一' -> 1;
            case '二' -> 2;
            case '三' -> 3;
            case '四' -> 4;
            case '五' -> 5;
            case '六' -> 6;
            case '七' -> 7;
            case '八' -> 8;
            case '九' -> 9;
            default -> -1;
        };
    }

    private String resolvePeriodHint(String text, int anchorIndex) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        int start = Math.max(0, anchorIndex - 8);
        int end = Math.min(text.length(), anchorIndex + 8);
        String scope = text.substring(start, end);
        if (scope.contains("晚上") || scope.contains("傍晚")) {
            return "晚上";
        }
        if (scope.contains("下午")) {
            return "下午";
        }
        if (scope.contains("中午")) {
            return "中午";
        }
        if (scope.contains("凌晨")) {
            return "凌晨";
        }
        if (scope.contains("早上") || scope.contains("上午")) {
            return "上午";
        }
        return "";
    }

    private int applyPeriodHint(int hour, String periodHint) {
        if (hour < 0 || hour > 23) {
            return hour;
        }
        if (!StringUtils.hasText(periodHint)) {
            return hour;
        }
        if (("下午".equals(periodHint) || "晚上".equals(periodHint) || "傍晚".equals(periodHint)) && hour < 12) {
            return hour + 12;
        }
        if ("中午".equals(periodHint) && hour < 11) {
            return hour + 12;
        }
        if ("凌晨".equals(periodHint) && hour == 12) {
            return 0;
        }
        return hour;
    }

    private String formatTime(int hour, int minute) {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            return "";
        }
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    private List<String> collectMissingFields(AiAppointmentStructuredDemand demand) {
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(demand.getPatientName())) {
            missing.add("patientName");
        }
        if (!StringUtils.hasText(demand.getServiceDate())) {
            missing.add("serviceDate");
        }
        if (!StringUtils.hasText(demand.getServiceStartTime()) || !StringUtils.hasText(demand.getServiceEndTime())) {
            missing.add("timePeriod");
        }
        if (!StringUtils.hasText(demand.getHospital())) {
            missing.add("hospital");
        }
        if (!hasEnoughMedicalIntent(demand)) {
            missing.add("symptomDescription");
        }
        return missing;
    }

    private String buildFollowUpQuestion(String questionType, Integer round, AiAppointmentStructuredDemand demand) {
        if ("serviceDate".equals(questionType)) {
            return round != null && round >= 2
                    ? "还缺少就诊日期，您可以直接选一个日期，我再继续为您匹配。"
                    : "我还需要确认就诊日期，您是今天、明天，还是下周哪一天去医院？";
        }
        if ("timePeriod".equals(questionType)) {
            boolean hasQuickTime = demand != null && StringUtils.hasText(demand.getTimePeriod());
            boolean hasExactTime = demand != null && StringUtils.hasText(demand.getServiceStartTime()) && StringUtils.hasText(demand.getServiceEndTime());
            if (hasExactTime) {
                return "就诊时段已确认，我可以继续为您匹配。";
            }
            if (hasQuickTime) {
                return "您已经选了" + demand.getTimePeriod() + "，我还需要确认具体开始和结束时间，您可以直接去选时间。";
            }
            return round != null && round >= 2
                    ? "还缺少完整就诊时段，请直接选择开始和结束时间。"
                    : "我还需要确认就诊时段，您可以先告诉我是上午、下午还是晚上，也可以直接去选择具体时间。";
        }
        if ("hospital".equals(questionType)) {
            return round != null && round >= 2
                    ? "还缺少就诊医院，您可以直接输入医院名称或在弹出的输入框里补充。"
                    : "我还需要确认就诊医院，是哪家医院呢？";
        }
        if ("symptomDescription".equals(questionType)) {
            return round != null && round >= 2
                    ? "还需要补充这次就诊的主要情况，您可以告诉我是复诊、检查、拿药，还是有具体不适症状。"
                    : "这次主要是复诊、检查、拿药，还是有具体不适症状？您简单说一句就可以。";
        }
        return "为了更准确匹配，请再补充一点信息。";
    }

    private List<String> buildOptions(String questionType, AiAppointmentStructuredDemand demand, Integer round) {
        if ("serviceDate".equals(questionType)) {
            return List.of(
                    LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
                    LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE)
            );
        }
        if ("timePeriod".equals(questionType)) {
            boolean hasQuickTime = demand != null && StringUtils.hasText(demand.getTimePeriod());
            boolean hasExactTime = demand != null && StringUtils.hasText(demand.getServiceStartTime()) && StringUtils.hasText(demand.getServiceEndTime());
            if (hasExactTime) {
                return List.of();
            }
            if (hasQuickTime || (round != null && round >= 2)) {
                return List.of("选择时间");
            }
            return List.of("上午", "下午", "晚上", "选择时间");
        }
        if ("symptomDescription".equals(questionType)) {
            return List.of("复诊", "检查", "取药", "有症状不适");
        }
        return Collections.emptyList();
    }

    private ConversationEnvelope runConversationCollection(SessionState state) throws Exception {
        Map<String, Object> contextPayload = new LinkedHashMap<>();
        contextPayload.put("today", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        contextPayload.put("patientName", defaultString(state.structuredDemand.getPatientName()));
        contextPayload.put("currentStructuredDemand", buildStructuredPromptPayload(state.structuredDemand));
        contextPayload.put("requiredFields", List.of("hospital", "serviceDate", "serviceStartTime", "serviceEndTime", "patientName", "symptomDescriptionOrScene"));
        contextPayload.put("currentMissingFields", collectMissingFields(state.structuredDemand));
        contextPayload.put("uiCapabilities", List.of(
                "free_text",
                "date_picker",
                "time_picker",
                "hospital_input",
                "confirm_sheet"
        ));

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", CONVERSATION_SYSTEM_PROMPT));
        messages.add(message("user", objectMapper.writeValueAsString(contextPayload)));

        List<Map<String, String>> history = state.history == null ? List.of() : state.history;
        int startIndex = Math.max(0, history.size() - MAX_CONVERSATION_HISTORY);
        messages.addAll(history.subList(startIndex, history.size()));

        long startedAt = System.currentTimeMillis();
        CompletableFuture<String> future = CompletableFuture.supplyAsync(
                () -> deepSeekClient.chatCompletion(messages, appointmentModel),
                CONVERSATION_AI_EXECUTOR
        );
        String raw = future.get(CONVERSATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startedAt;
        log.info("AI预约对话返回 sessionId={}, userId={}, model={}, elapsedMs={}, rawPreview={}",
                state.sessionId,
                state.userId,
                appointmentModel,
                elapsed,
                abbreviate(raw, 360));

        JsonNode root = objectMapper.readTree(extractJson(raw));
        ConversationEnvelope envelope = new ConversationEnvelope();
        envelope.assistantReply = normalize(root.path("assistantReply").asText(""));
        envelope.assistantIntent = normalize(root.path("assistantIntent").asText(""));
        envelope.questionKey = normalize(root.path("questionKey").asText(""));
        envelope.followUpType = normalize(root.path("followUpType").asText(""));
        envelope.readyForConfirm = root.path("readyForConfirm").isBoolean() && root.path("readyForConfirm").asBoolean();
        JsonNode missingNode = root.path("missingFields");
        if (missingNode.isArray()) {
            envelope.missingFields = new ArrayList<>();
            for (JsonNode item : missingNode) {
                String value = normalize(item.asText(""));
                if (StringUtils.hasText(value)) {
                    envelope.missingFields.add(value);
                }
            }
        }
        JsonNode patchNode = root.path("fieldPatch");
        if (patchNode.isObject()) {
            envelope.fieldPatch = sanitizeFieldPatch(objectMapper.treeToValue(patchNode, AiAppointmentStructuredDemand.class));
        }
        JsonNode optionsNode = root.path("options");
        if (optionsNode.isArray()) {
            envelope.options = new ArrayList<>();
            for (JsonNode item : optionsNode) {
                String value = normalize(item.asText(""));
                if (StringUtils.hasText(value)) {
                    envelope.options.add(value);
                }
            }
        }
        JsonNode summaryNode = root.path("confirmSummary");
        if (summaryNode.isObject()) {
            envelope.confirmSummary = readMap(summaryNode.toString());
        }
        JsonNode timeProposalNode = root.path("timeProposal");
        if (timeProposalNode.isObject()) {
            envelope.timeProposal = sanitizeTimeProposal(objectMapper.treeToValue(timeProposalNode, AiAppointmentTimeProposal.class));
        }
        return envelope;
    }

    private String buildSyntheticReplyText(AiAppointmentReplyRequest request) {
        if (request == null) {
            return "";
        }
        String selectedValue = normalize(request.getSelectedValue());
        String fieldKey = normalize(request.getFieldKey());
        if (!StringUtils.hasText(selectedValue)) {
            return "";
        }
        return switch (fieldKey) {
            case "serviceDate" -> "我选择的就诊日期是" + selectedValue;
            case "timePeriod" -> "我选择的就诊时间是" + selectedValue;
            case "timeRange", "timeRangeConfirmed" -> "我确认就诊时间是" + selectedValue;
            case "hospital" -> "我选择的就诊医院是" + selectedValue;
            case "symptomDescription" -> "这次就诊情况是" + selectedValue;
            case "otherRequirement" -> "我的额外需求是" + selectedValue;
            case "attendantGender" -> "我偏好" + selectedValue + "陪诊师";
            default -> selectedValue;
        };
    }

    private void appendHistory(List<Map<String, String>> history, String role, String content) {
        if (history == null || !StringUtils.hasText(content)) {
            return;
        }
        history.add(message(role, content));
    }

    private String determineFollowUpType(String aiSuggestedType,
                                         String questionType,
                                         AiAppointmentStructuredDemand demand,
                                         String assistantIntent,
                                         boolean readyForConfirm) {
        if (readyForConfirm || "ready_confirm".equals(assistantIntent) || "confirm_sheet".equals(aiSuggestedType)) {
            return "confirm_sheet";
        }
        if ("time_picker".equals(aiSuggestedType) || "date_picker".equals(aiSuggestedType)
                || "hospital_input".equals(aiSuggestedType) || "free_text".equals(aiSuggestedType)) {
            return aiSuggestedType;
        }
        if (!StringUtils.hasText(questionType)) {
            return "free_text";
        }
        if ("serviceDate".equals(questionType)) {
            return "date_picker";
        }
        if ("timePeriod".equals(questionType)) {
            return StringUtils.hasText(demand.getServiceDate()) ? "time_picker" : "free_text";
        }
        if ("hospital".equals(questionType)) {
            return "hospital_input";
        }
        return "free_text";
    }

    private String buildAssistantReply(SessionState state,
                                       ConversationEnvelope envelope,
                                       String questionType,
                                       boolean readyForConfirm) {
        String candidate = envelope == null ? "" : normalize(envelope.assistantReply);
        if (StringUtils.hasText(candidate)) {
            return candidate;
        }
        if (readyForConfirm) {
            return "我已经把预约信息整理好了。您确认后，我就开始为您智能匹配陪诊师。";
        }
        if (envelope != null && envelope.timeProposal != null && StringUtils.hasText(envelope.timeProposal.getProposalText())) {
            return envelope.timeProposal.getProposalText();
        }
        if (StringUtils.hasText(questionType)) {
            return buildFollowUpQuestion(questionType, state.followUpRound, state.structuredDemand);
        }
        return CONVERSATION_FALLBACK_REPLY;
    }

    private AiAppointmentStructuredDemand sanitizeFieldPatch(AiAppointmentStructuredDemand patch) {
        if (patch == null) {
            return null;
        }
        AiAppointmentStructuredDemand sanitized = new AiAppointmentStructuredDemand();
        mergeStructuredDemand(sanitized, patch);
        return sanitized;
    }

    private Map<String, Object> buildConfirmSummary(AiAppointmentStructuredDemand demand) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("hospital", defaultString(demand.getHospital()));
        summary.put("department", defaultString(demand.getDepartment()));
        summary.put("patientName", defaultString(demand.getPatientName()));
        summary.put("patientProfile", defaultString(demand.getPatientProfile()));
        summary.put("serviceDate", defaultString(demand.getServiceDate()));
        summary.put("serviceStartTime", defaultString(demand.getServiceStartTime()));
        summary.put("serviceEndTime", defaultString(demand.getServiceEndTime()));
        summary.put("symptomDescription", defaultString(demand.getSymptomDescription()));
        summary.put("otherRequirement", defaultString(demand.getOtherRequirement()));
        summary.put("attendantGender", defaultString(demand.getAttendantGender()));
        summary.put("preferenceTags", safeList(demand.getPreferenceTags()));
        summary.put("symptomTags", safeList(demand.getSymptomTags()));
        return summary;
    }

    private void mergeStructuredReply(SessionState state, AiAppointmentReplyRequest request) {
        if (request == null || state.structuredDemand == null) {
            return;
        }
        String fieldKey = normalize(request.getFieldKey());
        String selectedValue = normalize(request.getSelectedValue());
        if (!StringUtils.hasText(fieldKey) || !StringUtils.hasText(selectedValue)) {
            return;
        }
        switch (fieldKey) {
            case "serviceDate" -> state.structuredDemand.setServiceDate(selectedValue);
            case "hospital" -> state.structuredDemand.setHospital(selectedValue);
            case "serviceStartTime" -> state.structuredDemand.setServiceStartTime(parseFlexibleTimeToken(selectedValue));
            case "serviceEndTime" -> state.structuredDemand.setServiceEndTime(parseFlexibleTimeToken(selectedValue));
            case "timeRange", "timeRangeConfirmed" -> {
                TimeWindow exactWindow = extractExactTimeWindow(selectedValue);
                if (exactWindow != null) {
                    state.structuredDemand.setTimePeriod(exactWindow.label);
                    state.structuredDemand.setServiceStartTime(exactWindow.start);
                    state.structuredDemand.setServiceEndTime(exactWindow.end);
                }
            }
            case "timePeriod" -> {
                TimeWindow exactWindow = extractExactTimeWindow(selectedValue);
                if (exactWindow != null) {
                    state.structuredDemand.setTimePeriod(exactWindow.label);
                    state.structuredDemand.setServiceStartTime(exactWindow.start);
                    state.structuredDemand.setServiceEndTime(exactWindow.end);
                } else {
                    state.structuredDemand.setTimePeriod(selectedValue);
                }
            }
            case "symptomDescription" -> state.structuredDemand.setSymptomDescription(selectedValue);
            case "attendantGender" -> state.structuredDemand.setAttendantGender(selectedValue);
            default -> {
            }
        }
    }

    private void mergeStructuredDemand(AiAppointmentStructuredDemand target, AiAppointmentStructuredDemand incoming) {
        if (target == null || incoming == null) {
            return;
        }
        if (StringUtils.hasText(incoming.getPatientName())) {
            target.setPatientName(incoming.getPatientName().trim());
        }
        if (StringUtils.hasText(incoming.getPatientProfile())) {
            target.setPatientProfile(incoming.getPatientProfile().trim());
        }
        if (StringUtils.hasText(incoming.getServiceDate())) {
            target.setServiceDate(incoming.getServiceDate().trim());
        }
        if (StringUtils.hasText(incoming.getTimePeriod())) {
            target.setTimePeriod(incoming.getTimePeriod().trim());
        }
        if (StringUtils.hasText(incoming.getServiceStartTime())) {
            target.setServiceStartTime(parseFlexibleTimeToken(incoming.getServiceStartTime()));
        }
        if (StringUtils.hasText(incoming.getServiceEndTime())) {
            target.setServiceEndTime(parseFlexibleTimeToken(incoming.getServiceEndTime()));
        }
        if (StringUtils.hasText(incoming.getHospital())) {
            target.setHospital(cleanupHospitalCandidate(incoming.getHospital().trim()));
        }
        if (StringUtils.hasText(incoming.getDepartment())) {
            target.setDepartment(incoming.getDepartment().trim());
        }
        if (StringUtils.hasText(incoming.getSymptomDescription())) {
            target.setSymptomDescription(incoming.getSymptomDescription().trim());
        }
        if (incoming.getSymptomTags() != null && !incoming.getSymptomTags().isEmpty()) {
            target.setSymptomTags(new ArrayList<>(safeList(incoming.getSymptomTags())));
        }
        if (StringUtils.hasText(incoming.getOtherRequirement())) {
            target.setOtherRequirement(incoming.getOtherRequirement().trim());
        }
        if (incoming.getPreferenceTags() != null && !incoming.getPreferenceTags().isEmpty()) {
            target.setPreferenceTags(new ArrayList<>(safeList(incoming.getPreferenceTags())));
        }
        if (StringUtils.hasText(incoming.getAttendantGender())) {
            target.setAttendantGender(incoming.getAttendantGender().trim());
        }
        if (incoming.getServiceTypeNumber() != null) {
            target.setServiceTypeNumber(incoming.getServiceTypeNumber());
        }
        if (StringUtils.hasText(incoming.getRawDemandText())) {
            target.setRawDemandText(incoming.getRawDemandText().trim());
        }
    }

    private void ensureReadyForMatch(SessionState state) {
        if (state == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        ensureReadyForMatch(state.structuredDemand);
    }

    private void ensureReadyForMatch(AiAppointmentStructuredDemand demand) {
        if (demand == null) {
            throw new IllegalArgumentException("预约信息不能为空");
        }
        List<String> missingFields = collectMissingFields(demand);
        if (!missingFields.isEmpty()) {
            throw new IllegalStateException("预约信息还不完整");
        }
    }

    private SessionState requireSession(String sessionId) {
        SessionState state = sessions.get(sessionId);
        if (state == null) {
            state = loadSessionFromDb(sessionId);
            if (state != null) {
                sessions.put(sessionId, state);
            }
        }
        if (state == null) {
            throw new IllegalArgumentException("AI预约会话不存在");
        }
        return state;
    }

    private void markFailed(SessionState state, String message) {
        synchronized (state) {
            state.status = STATUS_FAILED;
            state.processingPhase = PHASE_FAILED;
            state.thinkingProcess = message;
            state.message = message;
            state.assistantReply = message;
            state.assistantIntent = "acknowledge";
            state.needMoreInfo = Boolean.FALSE;
            state.canMatch = Boolean.FALSE;
            state.readyForConfirm = Boolean.FALSE;
            state.followUpType = "";
            state.questionKey = "";
            state.timeProposal = null;
            appendHistory(state.history, "assistant", message);
            try {
                persistMessage(state.sessionId, "assistant", message, null);
            } catch (Exception ignore) {
                log.warn("AI预约失败消息落库失败 sessionId={}", state.sessionId);
            }
            try {
                saveSession(state);
            } catch (Exception ignore) {
                log.warn("AI预约失败状态落库失败 sessionId={}", state.sessionId);
            }
        }
    }

    private AiAppointmentSessionResponse toResponse(SessionState state) {
        AiAppointmentSessionResponse response = new AiAppointmentSessionResponse();
        response.setSessionId(state.sessionId);
        response.setStatus(state.status);
        response.setProcessingPhase(state.processingPhase);
        response.setThinkingProcess(state.thinkingProcess);
        response.setMessage(state.message);
        response.setAssistantReply(defaultString(state.assistantReply));
        response.setAssistantIntent(defaultString(state.assistantIntent));
        response.setNeedMoreInfo(Boolean.TRUE.equals(state.needMoreInfo));
        response.setMissingFields(state.missingFields == null ? List.of() : new ArrayList<>(state.missingFields));
        response.setQuestionType(defaultString(state.questionType));
        response.setQuestionKey(defaultString(state.questionKey));
        response.setOptions(state.options == null ? List.of() : new ArrayList<>(state.options));
        response.setCanMatch(Boolean.TRUE.equals(state.canMatch));
        response.setReadyForConfirm(Boolean.TRUE.equals(state.readyForConfirm));
        response.setFollowUpType(defaultString(state.followUpType));
        response.setTimeProposal(state.timeProposal);
        response.setFollowUpRound(state.followUpRound == null ? 0 : state.followUpRound);
        response.setStructuredDemand(state.structuredDemand);
        response.setFieldPatch(state.fieldPatch);
        response.setConfirmSummary(state.confirmSummary == null ? Map.of() : new LinkedHashMap<>(state.confirmSummary));
        response.setMatchedList(state.matchedList == null ? List.of() : new ArrayList<>(state.matchedList));
        response.setAppointmentNo(state.appointmentNo);
        response.setDegraded(Boolean.TRUE.equals(state.degraded));
        return response;
    }

    private void saveSession(SessionState state) {
        try {
            AiAppointmentSessionRecord record = toSessionRecord(state);
            AiAppointmentSessionRecord existing = aiAppointmentSessionMapper.selectBySessionId(state.sessionId);
            if (existing == null) {
                aiAppointmentSessionMapper.insert(record);
            } else {
                aiAppointmentSessionMapper.updateBySessionId(record);
            }
        } catch (Exception e) {
            log.error("保存 AI预约会话失败 sessionId={}, cause={}", state.sessionId, summarizeException(e), e);
            throw new IllegalStateException("AI预约会话保存失败", e);
        }
    }

    private void persistMessage(String sessionId, String role, String content, AiAppointmentStructuredDemand fieldPatch) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(content)) {
            return;
        }
        try {
            AiAppointmentMessageRecord record = new AiAppointmentMessageRecord();
            record.setSessionId(sessionId);
            record.setRole(role);
            record.setContent(content);
            record.setFieldPatchJson(writeJson(fieldPatch));
            aiAppointmentMessageMapper.insert(record);
        } catch (Exception e) {
            log.error("保存 AI预约消息失败 sessionId={}, role={}, cause={}", sessionId, role, summarizeException(e), e);
            throw new IllegalStateException("AI预约消息保存失败", e);
        }
    }

    private SessionState loadSessionFromDb(String sessionId) {
        try {
            AiAppointmentSessionRecord record = aiAppointmentSessionMapper.selectBySessionId(sessionId);
            if (record == null) {
                return null;
            }
            SessionState state = fromSessionRecord(record);
            List<AiAppointmentMessageRecord> messages = aiAppointmentMessageMapper.selectBySessionId(sessionId);
            state.history = messages.stream()
                    .filter(item -> StringUtils.hasText(item.getRole()) && StringUtils.hasText(item.getContent()))
                    .map(item -> message(item.getRole(), item.getContent()))
                    .collect(Collectors.toCollection(ArrayList::new));
            return state;
        } catch (Exception e) {
            log.error("加载 AI预约会话失败 sessionId={}, cause={}", sessionId, summarizeException(e), e);
            return null;
        }
    }

    private AiAppointmentSessionRecord toSessionRecord(SessionState state) {
        AiAppointmentSessionRecord record = new AiAppointmentSessionRecord();
        record.setSessionId(state.sessionId);
        record.setUserId(state.userId);
        record.setStatus(state.status);
        record.setProcessingPhase(state.processingPhase);
        record.setThinkingProcess(state.thinkingProcess);
        record.setAssistantReply(state.assistantReply);
        record.setAssistantIntent(state.assistantIntent);
        record.setNeedMoreInfo(Boolean.TRUE.equals(state.needMoreInfo) ? 1 : 0);
        record.setMissingFieldsJson(writeJson(state.missingFields));
        record.setQuestionType(state.questionType);
        record.setQuestionKey(state.questionKey);
        record.setFollowUpType(state.followUpType);
        record.setTimeProposalJson(writeJson(state.timeProposal));
        record.setOptionsJson(writeJson(state.options));
        record.setCanMatch(Boolean.TRUE.equals(state.canMatch) ? 1 : 0);
        record.setReadyForConfirm(Boolean.TRUE.equals(state.readyForConfirm) ? 1 : 0);
        record.setFollowUpRound(state.followUpRound == null ? 0 : state.followUpRound);
        record.setRawDemandText(state.rawDemandText);
        record.setStructuredDemandJson(writeJson(state.structuredDemand));
        record.setFieldPatchJson(writeJson(state.fieldPatch));
        record.setConfirmSummaryJson(writeJson(state.confirmSummary));
        record.setMatchedListJson(writeJson(state.matchedList));
        record.setAppointmentNo(state.appointmentNo);
        record.setDegraded(Boolean.TRUE.equals(state.degraded) ? 1 : 0);
        return record;
    }

    private SessionState fromSessionRecord(AiAppointmentSessionRecord record) {
        SessionState state = new SessionState();
        state.sessionId = record.getSessionId();
        state.userId = record.getUserId();
        state.status = defaultString(record.getStatus());
        state.processingPhase = defaultString(record.getProcessingPhase());
        state.thinkingProcess = defaultString(record.getThinkingProcess());
        state.message = defaultString(record.getAssistantReply());
        state.assistantReply = defaultString(record.getAssistantReply());
        state.assistantIntent = defaultString(record.getAssistantIntent());
        state.needMoreInfo = record.getNeedMoreInfo() != null && record.getNeedMoreInfo() == 1;
        state.missingFields = readStringList(record.getMissingFieldsJson());
        state.questionType = defaultString(record.getQuestionType());
        state.questionKey = StringUtils.hasText(record.getQuestionKey()) ? record.getQuestionKey() : defaultString(record.getQuestionType());
        state.followUpType = defaultString(record.getFollowUpType());
        state.timeProposal = readObject(record.getTimeProposalJson(), AiAppointmentTimeProposal.class, null);
        state.options = readStringList(record.getOptionsJson());
        state.canMatch = record.getCanMatch() != null && record.getCanMatch() == 1;
        state.readyForConfirm = record.getReadyForConfirm() != null && record.getReadyForConfirm() == 1;
        state.followUpRound = record.getFollowUpRound() == null ? 0 : record.getFollowUpRound();
        state.rawDemandText = defaultString(record.getRawDemandText());
        state.structuredDemand = readObject(record.getStructuredDemandJson(), AiAppointmentStructuredDemand.class, new AiAppointmentStructuredDemand());
        state.fieldPatch = readObject(record.getFieldPatchJson(), AiAppointmentStructuredDemand.class, null);
        state.confirmSummary = readMap(record.getConfirmSummaryJson());
        state.matchedList = readMatchedList(record.getMatchedListJson());
        state.appointmentNo = defaultString(record.getAppointmentNo());
        state.degraded = record.getDegraded() != null && record.getDegraded() == 1;
        return state;
    }

    private String writeJson(Object value) {
        if (value == null) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("序列化 JSON 失败: {}", summarizeException(e));
            return "";
        }
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析字符串列表失败: {}", summarizeException(e));
            return new ArrayList<>();
        }
    }

    private Map<String, Object> readMap(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析映射失败: {}", summarizeException(e));
            return new LinkedHashMap<>();
        }
    }

    private List<MatchedAttendantVO> readMatchedList(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<MatchedAttendantVO>>() {});
        } catch (Exception e) {
            log.warn("解析匹配列表失败: {}", summarizeException(e));
            return new ArrayList<>();
        }
    }

    private <T> T readObject(String json, Class<T> type, T defaultValue) {
        if (!StringUtils.hasText(json)) {
            return defaultValue;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("解析对象失败 type={}, cause={}", type.getSimpleName(), summarizeException(e));
            return defaultValue;
        }
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("role", role);
        payload.put("content", content);
        return payload;
    }

    private String extractJson(String raw) {
        String content = normalize(raw);
        if (content.startsWith("```")) {
            content = content.replaceFirst("^```[a-zA-Z]*", "");
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }
            content = content.trim();
        }
        String balanced = extractBalancedJsonObject(content);
        if (StringUtils.hasText(balanced)) {
            return balanced;
        }
        return content;
    }

    private String extractBalancedJsonObject(String content) {
        int start = content.indexOf('{');
        if (start < 0) {
            return "";
        }
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return content.substring(start, i + 1);
                }
            }
        }
        return "";
    }

    private String mergeDemandText(String existing, String reply) {
        String cleanReply = normalize(reply);
        if (!StringUtils.hasText(existing)) {
            return cleanReply;
        }
        if (!StringUtils.hasText(cleanReply)) {
            return existing.trim();
        }
        return existing.trim() + "；" + cleanReply;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String normalizeReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return DEGRADE_REASON;
        }
        return reason.trim();
    }

    private String buildDemandSummary(AiAppointmentStructuredDemand demand) {
        if (demand == null) {
            return "{}";
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("rawDemandText", defaultString(demand.getRawDemandText()));
        summary.put("patientName", defaultString(demand.getPatientName()));
        summary.put("patientProfile", defaultString(demand.getPatientProfile()));
        summary.put("serviceDate", defaultString(demand.getServiceDate()));
        summary.put("timePeriod", defaultString(demand.getTimePeriod()));
        summary.put("serviceStartTime", defaultString(demand.getServiceStartTime()));
        summary.put("serviceEndTime", defaultString(demand.getServiceEndTime()));
        summary.put("hospital", defaultString(demand.getHospital()));
        summary.put("department", defaultString(demand.getDepartment()));
        summary.put("symptomDescription", defaultString(demand.getSymptomDescription()));
        summary.put("otherRequirement", defaultString(demand.getOtherRequirement()));
        summary.put("attendantGender", defaultString(demand.getAttendantGender()));
        summary.put("serviceTypeNumber", demand.getServiceTypeNumber());
        return summary.toString();
    }

    private Map<String, Object> buildStructuredPromptPayload(AiAppointmentStructuredDemand demand) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("patientName", defaultString(demand.getPatientName()));
        payload.put("patientProfile", defaultString(demand.getPatientProfile()));
        payload.put("timePeriod", defaultString(demand.getTimePeriod()));
        payload.put("serviceDate", defaultString(demand.getServiceDate()));
        payload.put("serviceStartTime", defaultString(demand.getServiceStartTime()));
        payload.put("serviceEndTime", defaultString(demand.getServiceEndTime()));
        payload.put("hospital", defaultString(demand.getHospital()));
        payload.put("department", defaultString(demand.getDepartment()));
        payload.put("symptomDescription", defaultString(demand.getSymptomDescription()));
        payload.put("otherRequirement", defaultString(demand.getOtherRequirement()));
        payload.put("attendantGender", defaultString(demand.getAttendantGender()));
        payload.put("preferenceTags", safeList(demand.getPreferenceTags()));
        payload.put("symptomTags", safeList(demand.getSymptomTags()));
        return payload;
    }

    private void normalizeStructuredDemandFields(AiAppointmentStructuredDemand demand) {
        if (demand == null) {
            return;
        }
        AiAppointmentStructuredDemand normalized = new AiAppointmentStructuredDemand();
        mergeStructuredDemand(normalized, demand);
        normalized.setRawDemandText(defaultString(demand.getRawDemandText()));
        normalized.setPatientName(StringUtils.hasText(normalized.getPatientName()) ? normalized.getPatientName() : demand.getPatientName());
        if (!StringUtils.hasText(normalized.getHospital()) && StringUtils.hasText(demand.getHospital())) {
            normalized.setHospital(cleanupHospitalCandidate(demand.getHospital()));
        }
        if (!StringUtils.hasText(normalized.getTimePeriod())
                && StringUtils.hasText(normalized.getServiceStartTime())
                && StringUtils.hasText(normalized.getServiceEndTime())) {
            normalized.setTimePeriod("具体时间");
        }
        demand.setPatientName(normalized.getPatientName());
        demand.setPatientProfile(normalized.getPatientProfile());
        demand.setServiceDate(normalized.getServiceDate());
        demand.setTimePeriod(normalized.getTimePeriod());
        demand.setServiceStartTime(normalized.getServiceStartTime());
        demand.setServiceEndTime(normalized.getServiceEndTime());
        demand.setHospital(normalized.getHospital());
        demand.setDepartment(normalized.getDepartment());
        demand.setSymptomDescription(normalized.getSymptomDescription());
        demand.setSymptomTags(normalized.getSymptomTags());
        demand.setOtherRequirement(normalized.getOtherRequirement());
        demand.setPreferenceTags(normalized.getPreferenceTags());
        demand.setAttendantGender(normalized.getAttendantGender());
        demand.setServiceTypeNumber(normalized.getServiceTypeNumber());
    }

    private String resolveQuestionKey(ConversationEnvelope envelope, List<String> missingFields) {
        String fromAi = envelope == null ? "" : normalize(envelope.questionKey);
        if (StringUtils.hasText(fromAi)) {
            if ("serviceStartTime".equals(fromAi) || "serviceEndTime".equals(fromAi) || "timeRange".equals(fromAi)) {
                return "timePeriod";
            }
            if (missingFields.contains(fromAi) || (!missingFields.isEmpty() && "symptomDescription".equals(fromAi))) {
                return fromAi;
            }
        }
        return missingFields.isEmpty() ? "" : missingFields.get(0);
    }

    private AiAppointmentTimeProposal sanitizeTimeProposal(AiAppointmentTimeProposal proposal) {
        if (proposal == null) {
            return null;
        }
        String start = parseFlexibleTimeToken(proposal.getProposedStartTime());
        String end = parseFlexibleTimeToken(proposal.getProposedEndTime());
        String text = normalize(proposal.getProposalText());
        if (!StringUtils.hasText(start) && !StringUtils.hasText(end) && !StringUtils.hasText(text)) {
            return null;
        }
        AiAppointmentTimeProposal sanitized = new AiAppointmentTimeProposal();
        sanitized.setProposedStartTime(start);
        sanitized.setProposedEndTime(end);
        sanitized.setProposalText(text);
        return sanitized;
    }

    private String abbreviate(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return defaultString(value);
        }
        return value.substring(0, Math.max(0, maxLength)) + "...";
    }

    private String summarizeException(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        Throwable current = throwable;
        Throwable last = throwable;
        while (current != null) {
            last = current;
            current = current.getCause();
        }
        String message = normalize(last.getMessage());
        if (!StringUtils.hasText(message)) {
            return last.getClass().getSimpleName();
        }
        return last.getClass().getSimpleName() + ": " + message;
    }

    private int timeToMinutes(String value) {
        try {
            String[] parts = normalize(value).split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    private String minutesToTime(int minutes) {
        int normalized = ((minutes % (24 * 60)) + (24 * 60)) % (24 * 60);
        return String.format(Locale.ROOT, "%02d:%02d", normalized / 60, normalized % 60);
    }

    private String[] splitTimeSlot(String slot) {
        if (!StringUtils.hasText(slot) || !slot.contains("-")) {
            return null;
        }
        String[] parts = slot.split("-");
        if (parts.length != 2) {
            return null;
        }
        return new String[]{parts[0].trim(), parts[1].trim()};
    }

    private static class TimeWindow {
        final String start;
        final String end;
        final String label;

        TimeWindow(String start, String end, String label) {
            this.start = start;
            this.end = end;
            this.label = label;
        }
    }

    private static class MatchBundle {
        private String appointmentNo;
        private List<MatchedAttendantVO> matchedList = new ArrayList<>();
        private Boolean degraded = Boolean.FALSE;
    }

    private static class ConversationEnvelope {
        private String assistantReply;
        private String assistantIntent;
        private String questionKey;
        private List<String> missingFields = new ArrayList<>();
        private AiAppointmentStructuredDemand fieldPatch;
        private String followUpType;
        private List<String> options = new ArrayList<>();
        private Map<String, Object> confirmSummary = new LinkedHashMap<>();
        private AiAppointmentTimeProposal timeProposal;
        private Boolean readyForConfirm;
    }

    private static class SessionState {
        private String sessionId;
        private Integer userId;
        private String status;
        private String processingPhase;
        private String thinkingProcess;
        private String message;
        private String assistantReply;
        private String assistantIntent;
        private Boolean needMoreInfo;
        private List<String> missingFields = new ArrayList<>();
        private String questionType;
        private String questionKey;
        private String followUpType;
        private AiAppointmentTimeProposal timeProposal;
        private List<String> options = new ArrayList<>();
        private Boolean canMatch;
        private Boolean readyForConfirm;
        private Integer followUpRound;
        private String rawDemandText;
        private AiAppointmentStructuredDemand structuredDemand;
        private AiAppointmentStructuredDemand fieldPatch;
        private Map<String, Object> confirmSummary = new LinkedHashMap<>();
        private List<Map<String, String>> history = new ArrayList<>();
        private List<MatchedAttendantVO> matchedList = new ArrayList<>();
        private String appointmentNo;
        private Boolean degraded = Boolean.FALSE;
    }
}
