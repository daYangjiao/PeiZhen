package org.example.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dao.AiAppointmentMessageMapper;
import org.example.dao.AiAppointmentSessionMapper;
import org.example.model.AiAppointmentMessageRecord;
import org.example.model.AiAppointmentSessionRecord;
import org.example.model.response.AiAppointmentSessionResponse;
import org.example.service.AttendantService;
import org.example.service.OrderService;
import org.example.unity.DeepSeekClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
}
