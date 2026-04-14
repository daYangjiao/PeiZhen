package org.example.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.GuideAppointmentMapper;
import org.example.model.AiAppointmentStructuredDemand;
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
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
    private static final int MAX_CANDIDATES = 10;
    private static final int MAX_MATCHED = 3;
    private static final int MATCH_TIMEOUT_SECONDS = 20;
    private static final Set<Integer> OCCUPIED_ORDER_STATUS = Set.of(2, 3, 4, 5, 8);

    private static final Pattern HOSPITAL_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z0-9（）()·]+医院)");
    private static final Pattern DEPARTMENT_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{1,12}(?:科|门诊))");
    private static final Pattern EXACT_DATE_PATTERN = Pattern.compile("(20\\d{2})[-/.年](\\d{1,2})[-/.月](\\d{1,2})");
    private static final Pattern MONTH_DAY_PATTERN = Pattern.compile("(\\d{1,2})[-/.月](\\d{1,2})(?:日)?");
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile("(\\d{1,2}:\\d{2})\\s*[-到至]\\s*(\\d{1,2}:\\d{2})");
    private static final Pattern SINGLE_TIME_PATTERN = Pattern.compile("(\\d{1,2}:\\d{2})");

    private static final String MATCH_SYSTEM_PROMPT = """
            你是一个专业的医疗陪诊派单专家。
            任务：根据用户原始需求与结构化需求，从候选陪诊师 JSON 列表中选出最合适的 1 到 3 位。
            要求：
            1. 只能从候选列表中选择，不得虚构人选。
            2. 输出必须是严格 JSON，不要输出解释性文字、不要加 markdown 代码块。
            3. JSON 顶层格式固定为：
               {"matchedList":[{"attendantId":101,"matchScore":98,"reason":"..."}]}
            4. matchScore 范围 0-100，reason 必须简洁明确，突出为什么适合当前用户。
            5. reason 必须尽量结合用户的就诊时间、患者情况、医院/科室、性别偏好或技能偏好，不要写空泛结论。
            6. 优先推荐真正契合老人陪护、轮椅协助、急救经验、护士背景、医院熟悉度等具体需求的人选。
            """;

    private final DeepSeekClient deepSeekClient;
    private final GuideAppointmentMapper guideAppointmentMapper;
    private final AttendantService attendantService;
    private final OrderService orderService;
    private final org.example.dao.UserMapper userMapper;
    private final ObjectMapper objectMapper;

    private final ConcurrentMap<String, SessionState> sessions = new ConcurrentHashMap<>();

    private static final ExecutorService SESSION_EXECUTOR = Executors.newFixedThreadPool(2);
    private static final ExecutorService MATCH_EXECUTOR = Executors.newFixedThreadPool(2);

    public AiAppointmentServiceImpl(DeepSeekClient deepSeekClient,
                                    GuideAppointmentMapper guideAppointmentMapper,
                                    AttendantService attendantService,
                                    OrderService orderService,
                                    org.example.dao.UserMapper userMapper,
                                    ObjectMapper objectMapper) {
        this.deepSeekClient = deepSeekClient;
        this.guideAppointmentMapper = guideAppointmentMapper;
        this.attendantService = attendantService;
        this.orderService = orderService;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
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
        state.followUpRound = 0;
        state.structuredDemand = new AiAppointmentStructuredDemand();
        state.structuredDemand.setRawDemandText(state.rawDemandText);
        sessions.put(state.sessionId, state);

        CompletableFuture.runAsync(() -> analyzeSession(state.sessionId), SESSION_EXECUTOR);
        return toResponse(state);
    }

    @Override
    public AiAppointmentSessionResponse getSession(String sessionId) {
        SessionState state = sessions.get(sessionId);
        return state == null ? null : toResponse(state);
    }

    @Override
    public AiAppointmentSessionResponse replySession(String sessionId, AiAppointmentReplyRequest request) {
        SessionState state = requireSession(sessionId);
        synchronized (state) {
            mergeStructuredReply(state, request);
            if (StringUtils.hasText(request.getReplyText())) {
                state.rawDemandText = mergeDemandText(state.rawDemandText, request.getReplyText());
                state.structuredDemand.setRawDemandText(state.rawDemandText);
            }
            state.processingPhase = PHASE_THINKING;
            state.thinkingProcess = COLLECTING_MESSAGE;
            state.message = "";
        }
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
            state.matchedList = new ArrayList<>();
            state.degraded = Boolean.FALSE;
        }
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
        parseDemandTextInto(demand, demand.getRawDemandText());
        MatchBundle bundle;
        try {
            bundle = performMatch(null, demand);
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
            parseDemandTextInto(state.structuredDemand, state.rawDemandText);
            List<String> missingFields = collectMissingFields(state.structuredDemand);

            synchronized (state) {
                state.missingFields = missingFields;
                if (!missingFields.isEmpty()) {
                    state.status = STATUS_COLLECTING;
                    state.processingPhase = PHASE_COMPLETED;
                    state.needMoreInfo = Boolean.TRUE;
                    state.canMatch = Boolean.FALSE;
                    state.followUpRound = (state.followUpRound == null ? 0 : state.followUpRound) + 1;
                    String questionType = missingFields.get(0);
                    state.questionType = questionType;
                    state.options = buildOptions(questionType);
                    state.thinkingProcess = "已完成";
                    state.message = buildFollowUpQuestion(questionType, state.followUpRound);
                    return;
                }

                state.status = STATUS_READY;
                state.processingPhase = PHASE_COMPLETED;
                state.thinkingProcess = "已完成";
                state.needMoreInfo = Boolean.FALSE;
                state.canMatch = Boolean.TRUE;
                state.questionType = "";
                state.options = List.of();
                state.message = "信息已确认，正在为您匹配合适的陪诊师。";
            }
        } catch (Exception e) {
            log.error("分析 AI 预约需求失败, sessionId={}", sessionId, e);
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
            MatchBundle bundle = performMatch(state.userId, state.structuredDemand);
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
                        ? "已为您生成快速推荐结果，可先选择合适的陪诊师。"
                        : "已为您匹配到更合适的陪诊师，请查看推荐结果。";
            }
        } catch (Exception e) {
            log.error("AI 预约匹配失败, sessionId={}", sessionId, e);
            markFailed(state, "匹配失败，请稍后重试");
        }
    }

    private MatchBundle performMatch(Integer userId, AiAppointmentStructuredDemand demand) throws Exception {
        List<Attendant> roughCandidates = attendantService.findAiCandidates(20);
        List<Attendant> filtered = filterAvailableCandidates(roughCandidates, demand);
        List<Attendant> shortlist = filtered.stream().limit(MAX_CANDIDATES).collect(Collectors.toList());
        if (shortlist.isEmpty()) {
            throw new IllegalStateException("当前暂无可用陪诊师");
        }

        String appointmentNo = createAppointmentIfNeeded(userId, demand);
        List<MatchedAttendantVO> degraded = buildMatchedVoList(shortlist.stream().limit(MAX_MATCHED).collect(Collectors.toList()), null, true);

        try {
            List<MatchedAttendantVO> aiMatched = runAiRanking(demand, shortlist);
            MatchBundle bundle = new MatchBundle();
            bundle.appointmentNo = appointmentNo;
            bundle.matchedList = aiMatched.isEmpty() ? degraded : aiMatched;
            bundle.degraded = aiMatched.isEmpty();
            return bundle;
        } catch (Exception e) {
            log.warn("AI 陪诊匹配降级, reason={}", e.getMessage());
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
        promptPayload.put("structuredDemand", demand);
        promptPayload.put("candidates", shortlist.stream().map(this::buildCandidatePayload).collect(Collectors.toList()));

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", MATCH_SYSTEM_PROMPT));
        messages.add(message("user", objectMapper.writeValueAsString(promptPayload)));

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> deepSeekClient.chatCompletion(messages), MATCH_EXECUTOR);
        String raw = future.get(MATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        JsonNode root = objectMapper.readTree(extractJson(raw));
        JsonNode listNode = root.path("matchedList");
        if (!listNode.isArray()) {
            return List.of();
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
        appointment.setPatientName(resolvePatientName(user));
        appointment.setPatientPhone(resolvePatientPhone(user));
        appointment.setSymptoms(toSymptoms(demand));
        appointment.setHospitalName(defaultString(demand.getHospital()));
        appointment.setServiceTypeNumber(demand.getServiceTypeNumber() == null ? 1 : demand.getServiceTypeNumber());
        appointment.setServiceDate(defaultString(demand.getServiceDate()));
        appointment.setServiceStartTime(defaultString(demand.getServiceStartTime()));
        appointment.setServiceEndTime(defaultString(demand.getServiceEndTime()));
        appointment.setOtherRequirement(defaultString(demand.getRawDemandText()));
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

        if (!StringUtils.hasText(demand.getHospital())) {
            Matcher hospitalMatcher = HOSPITAL_PATTERN.matcher(source);
            if (hospitalMatcher.find()) {
                demand.setHospital(hospitalMatcher.group(1));
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
            TimeWindow window = extractTimeWindow(source);
            if (window != null) {
                demand.setTimePeriod(window.label);
                demand.setServiceStartTime(window.start);
                demand.setServiceEndTime(window.end);
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
            if (source.contains("爷爷") || source.contains("奶奶") || source.contains("老人") || source.contains("老爷子")) {
                demand.setPatientProfile("老人就诊");
            } else if (source.contains("孩子") || source.contains("宝宝") || source.contains("小朋友")) {
                demand.setPatientProfile("儿童就诊");
            }
        }

        Set<String> preferenceTags = new LinkedHashSet<>(demand.getPreferenceTags());
        for (String keyword : List.of("轮椅", "急救", "护士", "老人陪护", "术后护理", "跑腿", "熟悉医院", "耐心", "力气大")) {
            if (source.contains(keyword.replace("陪护", "")) || source.contains(keyword)) {
                preferenceTags.add(keyword);
            }
        }
        demand.setPreferenceTags(new ArrayList<>(preferenceTags));

        Set<String> symptoms = new LinkedHashSet<>(demand.getSymptomTags());
        for (String keyword : List.of("复诊", "发烧", "胸闷", "头晕", "胃痛", "咳嗽", "术后", "检查")) {
            if (source.contains(keyword)) {
                symptoms.add(keyword);
            }
        }
        demand.setSymptomTags(new ArrayList<>(symptoms));
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

    private TimeWindow extractTimeWindow(String text) {
        Matcher rangeMatcher = TIME_RANGE_PATTERN.matcher(text);
        if (rangeMatcher.find()) {
            return new TimeWindow(rangeMatcher.group(1), rangeMatcher.group(2), "具体时间");
        }

        Matcher singleTimeMatcher = SINGLE_TIME_PATTERN.matcher(text);
        if (singleTimeMatcher.find()) {
            String start = singleTimeMatcher.group(1);
            int startMinutes = timeToMinutes(start);
            int endMinutes = (startMinutes + 120) % (24 * 60);
            return new TimeWindow(start, minutesToTime(endMinutes), "具体时间");
        }

        if (text.contains("上午")) return new TimeWindow("09:00", "12:00", "上午");
        if (text.contains("下午")) return new TimeWindow("14:00", "17:00", "下午");
        if (text.contains("晚上")) return new TimeWindow("18:00", "21:00", "晚上");
        if (text.contains("中午")) return new TimeWindow("11:30", "13:30", "中午");
        return null;
    }

    private List<String> collectMissingFields(AiAppointmentStructuredDemand demand) {
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(demand.getServiceDate())) {
            missing.add("serviceDate");
        }
        if (!StringUtils.hasText(demand.getServiceStartTime()) || !StringUtils.hasText(demand.getServiceEndTime())) {
            missing.add("timePeriod");
        }
        if (!StringUtils.hasText(demand.getHospital())) {
            missing.add("hospital");
        }
        return missing;
    }

    private String buildFollowUpQuestion(String questionType, Integer round) {
        if ("serviceDate".equals(questionType)) {
            return round != null && round >= 2
                    ? "还缺少就诊日期，您可以直接选一个日期，我再继续为您匹配。"
                    : "我还需要确认就诊日期，您是今天、明天，还是下周哪一天去医院？";
        }
        if ("timePeriod".equals(questionType)) {
            return round != null && round >= 2
                    ? "还缺少就诊时段，您可以直接选择上午、下午、晚上或具体时间。"
                    : "我还需要确认大致就诊时段，方便安排合适的陪诊师。您是上午、下午、晚上，还是有具体时间？";
        }
        if ("hospital".equals(questionType)) {
            return round != null && round >= 2
                    ? "还缺少就诊医院，您可以直接输入医院名称或在弹出的输入框里补充。"
                    : "我还需要确认就诊医院，是哪家医院呢？";
        }
        return "为了更准确匹配，请再补充一点信息。";
    }

    private List<String> buildOptions(String questionType) {
        if ("serviceDate".equals(questionType)) {
            return List.of(
                    LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
                    LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE)
            );
        }
        if ("timePeriod".equals(questionType)) {
            return List.of("上午", "下午", "晚上", "具体时间");
        }
        return Collections.emptyList();
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
            case "timePeriod" -> {
                state.structuredDemand.setTimePeriod(selectedValue);
                TimeWindow preset = extractTimeWindow(selectedValue);
                if (preset != null) {
                    state.structuredDemand.setServiceStartTime(preset.start);
                    state.structuredDemand.setServiceEndTime(preset.end);
                }
            }
            case "attendantGender" -> state.structuredDemand.setAttendantGender(selectedValue);
            default -> {
            }
        }
    }

    private void ensureReadyForMatch(SessionState state) {
        if (state == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        List<String> missingFields = collectMissingFields(state.structuredDemand);
        if (!missingFields.isEmpty()) {
            throw new IllegalStateException("预约信息还不完整");
        }
    }

    private SessionState requireSession(String sessionId) {
        SessionState state = sessions.get(sessionId);
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
            state.needMoreInfo = Boolean.FALSE;
            state.canMatch = Boolean.FALSE;
        }
    }

    private AiAppointmentSessionResponse toResponse(SessionState state) {
        AiAppointmentSessionResponse response = new AiAppointmentSessionResponse();
        response.setSessionId(state.sessionId);
        response.setStatus(state.status);
        response.setProcessingPhase(state.processingPhase);
        response.setThinkingProcess(state.thinkingProcess);
        response.setMessage(state.message);
        response.setNeedMoreInfo(Boolean.TRUE.equals(state.needMoreInfo));
        response.setMissingFields(state.missingFields == null ? List.of() : new ArrayList<>(state.missingFields));
        response.setQuestionType(defaultString(state.questionType));
        response.setOptions(state.options == null ? List.of() : new ArrayList<>(state.options));
        response.setCanMatch(Boolean.TRUE.equals(state.canMatch));
        response.setFollowUpRound(state.followUpRound == null ? 0 : state.followUpRound);
        response.setStructuredDemand(state.structuredDemand);
        response.setMatchedList(state.matchedList == null ? List.of() : new ArrayList<>(state.matchedList));
        response.setAppointmentNo(state.appointmentNo);
        response.setDegraded(Boolean.TRUE.equals(state.degraded));
        return response;
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
            content = content.replaceFirst("^```json", "").replaceFirst("^```", "");
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }
            content = content.trim();
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
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

    private static class SessionState {
        private String sessionId;
        private Integer userId;
        private String status;
        private String processingPhase;
        private String thinkingProcess;
        private String message;
        private Boolean needMoreInfo;
        private List<String> missingFields = new ArrayList<>();
        private String questionType;
        private List<String> options = new ArrayList<>();
        private Boolean canMatch;
        private Integer followUpRound;
        private String rawDemandText;
        private AiAppointmentStructuredDemand structuredDemand;
        private List<MatchedAttendantVO> matchedList = new ArrayList<>();
        private String appointmentNo;
        private Boolean degraded = Boolean.FALSE;
    }
}
