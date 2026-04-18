package org.example.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dao.AiAppointmentMessageMapper;
import org.example.dao.AiAppointmentSessionMapper;
import org.example.model.AiAppointmentMessageRecord;
import org.example.model.AiAppointmentSessionRecord;
import org.example.model.AiAppointmentStructuredDemand;
import org.example.model.User;
import org.example.model.request.AiAppointmentSessionRequest;
import org.example.model.response.AiAppointmentLatestOverviewResponse;
import org.example.model.response.AiAppointmentSessionResponse;
import org.example.service.AttendantService;
import org.example.service.OrderService;
import org.example.unity.DeepSeekClient;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAppointmentServiceImplTest {

    @Mock
    private DeepSeekClient deepSeekClient;
    @Mock
    private AiAppointmentSessionMapper aiAppointmentSessionMapper;
    @Mock
    private AiAppointmentMessageMapper aiAppointmentMessageMapper;
    @Mock
    private org.example.dao.GuideAppointmentMapper guideAppointmentMapper;
    @Mock
    private AttendantService attendantService;
    @Mock
    private OrderService orderService;
    @Mock
    private org.example.dao.UserMapper userMapper;

    @Test
    void getSessionShouldReturnPersistedChatMessages() {
        AiAppointmentServiceImpl service = new AiAppointmentServiceImpl(
                deepSeekClient,
                aiAppointmentSessionMapper,
                aiAppointmentMessageMapper,
                guideAppointmentMapper,
                attendantService,
                orderService,
                userMapper,
                new ObjectMapper(),
                "deepseek-chat"
        );

        AiAppointmentSessionRecord session = new AiAppointmentSessionRecord();
        session.setSessionId("ai-app-test");
        session.setStatus("COLLECTING");
        session.setProcessingPhase("completed");
        session.setAssistantReply("请补充时间段");

        AiAppointmentMessageRecord userMessage = new AiAppointmentMessageRecord();
        userMessage.setSessionId("ai-app-test");
        userMessage.setRole("user");
        userMessage.setContent("明天去华西医院复诊");
        userMessage.setCreateTime(new Date(1713331200000L));

        AiAppointmentMessageRecord assistantMessage = new AiAppointmentMessageRecord();
        assistantMessage.setSessionId("ai-app-test");
        assistantMessage.setRole("assistant");
        assistantMessage.setContent("好的，请再告诉我具体的时间段。");
        assistantMessage.setCreateTime(new Date(1713331260000L));

        when(aiAppointmentSessionMapper.selectBySessionId("ai-app-test")).thenReturn(session);
        when(aiAppointmentMessageMapper.selectBySessionId("ai-app-test")).thenReturn(List.of(userMessage, assistantMessage));

        AiAppointmentSessionResponse response = service.getSession("ai-app-test");

        assertThat(response).isNotNull();
        assertThat(response.getMessages()).hasSize(2);
        assertThat(response.getMessages().get(0).getRole()).isEqualTo("user");
        assertThat(response.getMessages().get(0).getContent()).isEqualTo("明天去华西医院复诊");
        assertThat(response.getMessages().get(0).getProcessingPhase()).isEqualTo("completed");
        assertThat(response.getMessages().get(1).getRole()).isEqualTo("assistant");
        assertThat(response.getMessages().get(1).getContent()).isEqualTo("好的，请再告诉我具体的时间段。");
    }

    @Test
    void createSessionShouldCopyPatientSexFromCurrentUser() {
        AiAppointmentServiceImpl service = new AiAppointmentServiceImpl(
                deepSeekClient,
                aiAppointmentSessionMapper,
                aiAppointmentMessageMapper,
                guideAppointmentMapper,
                attendantService,
                orderService,
                userMapper,
                new ObjectMapper(),
                "deepseek-chat"
        );

        User user = new User();
        user.setId(15);
        user.setName("范涵伶");
        user.setSex("女");
        when(userMapper.findById(15)).thenReturn(user);
        when(aiAppointmentSessionMapper.selectBySessionId(any())).thenReturn(null);

        AiAppointmentSessionRequest request = new AiAppointmentSessionRequest();
        request.setDemandText("明天 09:00-11:00 去华西医院复诊");

        AiAppointmentSessionResponse response = service.createSession(15, request);

        assertThat(response.getStructuredDemand()).isNotNull();
        assertThat(response.getStructuredDemand().getPatientName()).isEqualTo("范涵伶");
        assertThat(response.getStructuredDemand().getPatientSex()).isEqualTo("女");
    }

    @Test
    @SuppressWarnings("unchecked")
    void structuredPromptPayloadShouldIncludePatientSexAndTitleRule() throws Exception {
        AiAppointmentServiceImpl service = new AiAppointmentServiceImpl(
                deepSeekClient,
                aiAppointmentSessionMapper,
                aiAppointmentMessageMapper,
                guideAppointmentMapper,
                attendantService,
                orderService,
                userMapper,
                new ObjectMapper(),
                "deepseek-chat"
        );

        AiAppointmentStructuredDemand demand = new AiAppointmentStructuredDemand();
        demand.setPatientName("周铭");
        demand.setPatientSex("男");
        demand.setHospital("四川大学华西医院");

        Method payloadMethod = AiAppointmentServiceImpl.class.getDeclaredMethod("buildStructuredPromptPayload", AiAppointmentStructuredDemand.class);
        payloadMethod.setAccessible(true);
        Map<String, Object> payload = (Map<String, Object>) payloadMethod.invoke(service, demand);

        assertThat(payload).containsEntry("patientSex", "男");
        assertThat(payload).containsEntry("patientTitleRule", "男=先生，女=女士，未知或为空时不要使用先生/女士称谓");
    }

    @Test
    void getLatestRestorableSessionShouldReturnPersistedUserSession() {
        AiAppointmentServiceImpl service = new AiAppointmentServiceImpl(
                deepSeekClient,
                aiAppointmentSessionMapper,
                aiAppointmentMessageMapper,
                guideAppointmentMapper,
                attendantService,
                orderService,
                userMapper,
                new ObjectMapper(),
                "deepseek-chat"
        );

        AiAppointmentSessionRecord session = new AiAppointmentSessionRecord();
        session.setSessionId("ai-app-latest");
        session.setUserId(15);
        session.setStatus("COLLECTING");
        session.setProcessingPhase("completed");
        session.setAssistantReply("请补充医院");

        when(aiAppointmentSessionMapper.selectLatestRestorableByUserId(15)).thenReturn(session);
        when(aiAppointmentSessionMapper.selectBySessionId("ai-app-latest")).thenReturn(session);
        when(aiAppointmentMessageMapper.selectBySessionId("ai-app-latest")).thenReturn(List.of());

        AiAppointmentSessionResponse response = service.getLatestRestorableSession(15);

        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo("ai-app-latest");
    }

    @Test
    void getLatestRestorableSessionShouldBackfillPatientSexForOldSession() {
        AiAppointmentServiceImpl service = new AiAppointmentServiceImpl(
                deepSeekClient,
                aiAppointmentSessionMapper,
                aiAppointmentMessageMapper,
                guideAppointmentMapper,
                attendantService,
                orderService,
                userMapper,
                new ObjectMapper(),
                "deepseek-chat"
        );

        AiAppointmentSessionRecord session = new AiAppointmentSessionRecord();
        session.setSessionId("ai-app-latest-old");
        session.setUserId(15);
        session.setStatus("COLLECTING");
        session.setProcessingPhase("completed");
        session.setAssistantReply("请补充医院");
        session.setStructuredDemandJson("{\"rawDemandText\":\"明天去华西复诊\"}");

        User user = new User();
        user.setId(15);
        user.setName("范涵伶");
        user.setSex("女");

        when(aiAppointmentSessionMapper.selectLatestRestorableByUserId(15)).thenReturn(session);
        when(aiAppointmentSessionMapper.selectBySessionId("ai-app-latest-old")).thenReturn(session);
        when(aiAppointmentMessageMapper.selectBySessionId("ai-app-latest-old")).thenReturn(List.of());
        when(userMapper.findById(15)).thenReturn(user);

        AiAppointmentSessionResponse response = service.getLatestRestorableSession(15);

        assertThat(response).isNotNull();
        assertThat(response.getStructuredDemand().getPatientName()).isEqualTo("范涵伶");
        assertThat(response.getStructuredDemand().getPatientSex()).isEqualTo("女");

        ArgumentCaptor<AiAppointmentSessionRecord> recordCaptor = ArgumentCaptor.forClass(AiAppointmentSessionRecord.class);
        verify(aiAppointmentSessionMapper).updateBySessionId(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getStructuredDemandJson()).contains("\"patientSex\":\"女\"");
    }

    @Test
    void getSessionForUserShouldRejectAnotherUsersSession() {
        AiAppointmentServiceImpl service = new AiAppointmentServiceImpl(
                deepSeekClient,
                aiAppointmentSessionMapper,
                aiAppointmentMessageMapper,
                guideAppointmentMapper,
                attendantService,
                orderService,
                userMapper,
                new ObjectMapper(),
                "deepseek-chat"
        );

        AiAppointmentSessionRecord session = new AiAppointmentSessionRecord();
        session.setSessionId("ai-app-owned-by-other");
        session.setUserId(15);
        session.setStatus("COLLECTING");
        session.setProcessingPhase("completed");

        when(aiAppointmentSessionMapper.selectBySessionId("ai-app-owned-by-other")).thenReturn(session);

        AiAppointmentSessionResponse response = service.getSession(16, "ai-app-owned-by-other");

        assertThat(response).isNull();
    }

    @Test
    void getLatestOverviewShouldTreatMatchedSessionAsHistoryOnly() {
        AiAppointmentServiceImpl service = createService();
        AiAppointmentSessionRecord session = sessionRecord("ai-app-matched", 15, "MATCHED", new Date());
        session.setAppointmentNo("APP202604180001");

        when(aiAppointmentSessionMapper.selectLatestByUserId(15)).thenReturn(session);
        when(aiAppointmentSessionMapper.selectBySessionId("ai-app-matched")).thenReturn(session);
        when(aiAppointmentMessageMapper.selectBySessionId("ai-app-matched")).thenReturn(List.of());

        AiAppointmentLatestOverviewResponse response = service.getLatestSessionOverview(15);

        assertThat(response).isNotNull();
        assertThat(response.getSession().getSessionId()).isEqualTo("ai-app-matched");
        assertThat(response.getCompleted()).isTrue();
        assertThat(response.getHistoryOnly()).isTrue();
        assertThat(response.getContinuable()).isFalse();
    }

    @Test
    void getLatestOverviewShouldAllowRecentUnfinishedSessionToContinue() {
        AiAppointmentServiceImpl service = createService();
        AiAppointmentSessionRecord session = sessionRecord("ai-app-recent", 15, "COLLECTING", new Date());

        when(aiAppointmentSessionMapper.selectLatestByUserId(15)).thenReturn(session);
        when(aiAppointmentSessionMapper.selectBySessionId("ai-app-recent")).thenReturn(session);
        when(aiAppointmentMessageMapper.selectBySessionId("ai-app-recent")).thenReturn(List.of());

        AiAppointmentLatestOverviewResponse response = service.getLatestSessionOverview(15);

        assertThat(response).isNotNull();
        assertThat(response.getCompleted()).isFalse();
        assertThat(response.getContinuable()).isTrue();
        assertThat(response.getHistoryOnly()).isFalse();
        assertThat(response.getExpiresAt()).isAfter(LocalDateTime.now().minusMinutes(1));
    }

    @Test
    void getLatestOverviewShouldShowExpiredUnfinishedSessionAsHistoryOnly() {
        AiAppointmentServiceImpl service = createService();
        Date expiredUpdateTime = Date.from(LocalDateTime.now()
                .minus(Duration.ofMinutes(61))
                .atZone(ZoneId.systemDefault())
                .toInstant());
        AiAppointmentSessionRecord session = sessionRecord("ai-app-expired", 15, "READY", expiredUpdateTime);

        when(aiAppointmentSessionMapper.selectLatestByUserId(15)).thenReturn(session);
        when(aiAppointmentSessionMapper.selectBySessionId("ai-app-expired")).thenReturn(session);
        when(aiAppointmentMessageMapper.selectBySessionId("ai-app-expired")).thenReturn(List.of());

        AiAppointmentLatestOverviewResponse response = service.getLatestSessionOverview(15);

        assertThat(response).isNotNull();
        assertThat(response.getCompleted()).isFalse();
        assertThat(response.getContinuable()).isFalse();
        assertThat(response.getHistoryOnly()).isTrue();
        assertThat(response.getExpiresAt()).isBefore(LocalDateTime.now());
    }

    @Test
    @SuppressWarnings("unchecked")
    void conversationPromptShouldIncludeUserSessionIsolationPayload() {
        AiAppointmentServiceImpl service = createService();
        User user = new User();
        user.setId(15);
        user.setName("范涵伶");
        user.setSex("女");
        when(userMapper.findById(15)).thenReturn(user);
        when(aiAppointmentSessionMapper.selectBySessionId(any())).thenReturn(null);
        when(deepSeekClient.chatCompletion(any(), eq("deepseek-chat"))).thenReturn("""
                {"assistantReply":"已收到您的需求，请补充就诊医院。","assistantIntent":"collect","needMoreInfo":true,"missingFields":["hospital"],"questionType":"hospital","questionKey":"hospital","followUpType":"hospital_input","readyForConfirm":false}
                """);

        AiAppointmentSessionRequest request = new AiAppointmentSessionRequest();
        request.setDemandText("明天上午复诊，需要推轮椅");

        AiAppointmentSessionResponse response = service.createSession(15, request);

        ArgumentCaptor<List<Map<String, String>>> messagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(deepSeekClient, timeout(1000)).chatCompletion(messagesCaptor.capture(), eq("deepseek-chat"));
        List<Map<String, String>> modelMessages = messagesCaptor.getValue();
        String contextPayload = modelMessages.get(1).get("content");

        assertThat(response.getSessionId()).isNotBlank();
        assertThat(contextPayload).contains("\"conversationScope\":\"user_session\"");
        assertThat(contextPayload).contains("\"userId\":15");
        assertThat(contextPayload).contains("\"sessionId\":\"" + response.getSessionId() + "\"");
        assertThat(contextPayload).contains("\"requestId\":\"");
        assertThat(contextPayload).contains("\"contextPolicy\"");
        assertThat(contextPayload).contains("\"replyTemplate\"");
    }

    private AiAppointmentServiceImpl createService() {
        return new AiAppointmentServiceImpl(
                deepSeekClient,
                aiAppointmentSessionMapper,
                aiAppointmentMessageMapper,
                guideAppointmentMapper,
                attendantService,
                orderService,
                userMapper,
                new ObjectMapper(),
                "deepseek-chat"
        );
    }

    private AiAppointmentSessionRecord sessionRecord(String sessionId, Integer userId, String status, Date updateTime) {
        AiAppointmentSessionRecord session = new AiAppointmentSessionRecord();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setStatus(status);
        session.setProcessingPhase("completed");
        session.setAssistantReply("请继续补充");
        session.setCreateTime(Date.from(LocalDateTime.now().minusHours(2).atZone(ZoneId.systemDefault()).toInstant()));
        session.setUpdateTime(updateTime);
        session.setStructuredDemandJson("{\"rawDemandText\":\"明天复诊\"}");
        return session;
    }
}
