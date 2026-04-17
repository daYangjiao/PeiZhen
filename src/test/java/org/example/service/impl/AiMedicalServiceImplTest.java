package org.example.service.impl;

import org.example.dao.AiMedicalQaMapper;
import org.example.model.AiMedicalQa;
import org.example.unity.DeepSeekClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AiMedicalServiceImplTest {

    @Mock
    private AiMedicalQaMapper aiMedicalQaMapper;
    @Mock
    private DeepSeekClient deepSeekClient;

    @Test
    @SuppressWarnings("unchecked")
    void buildMessagesShouldMarkNewSessionWithoutHistoryContext() throws Exception {
        AiMedicalServiceImpl service = new AiMedicalServiceImpl(aiMedicalQaMapper, deepSeekClient);
        AiMedicalQa currentRecord = new AiMedicalQa();
        currentRecord.setId(100L);
        currentRecord.setQuestion("胃痛应该挂什么科");

        Method method = AiMedicalServiceImpl.class.getDeclaredMethod("buildMessages", List.class, AiMedicalQa.class);
        method.setAccessible(true);

        List<Map<String, String>> messages = (List<Map<String, String>>) method.invoke(service, new ArrayList<AiMedicalQa>(), currentRecord);

        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).get("role")).isEqualTo("system");
        assertThat(messages.get(1).get("role")).isEqualTo("system");
        assertThat(messages.get(1).get("content")).contains("new_session");
        assertThat(messages.get(1).get("content")).contains("不能假设");
        assertThat(messages.get(2).get("role")).isEqualTo("user");
        assertThat(messages.get(2).get("content")).isEqualTo("胃痛应该挂什么科");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildMessagesShouldMarkContinueSessionWhenHistoryExists() throws Exception {
        AiMedicalServiceImpl service = new AiMedicalServiceImpl(aiMedicalQaMapper, deepSeekClient);
        AiMedicalQa historyRecord = new AiMedicalQa();
        historyRecord.setId(1L);
        historyRecord.setQuestion("胸闷要看什么科");
        historyRecord.setAnswer("建议先看心内科。仅供参考，不能替代医生面诊。");
        historyRecord.setQaStatus(1);

        AiMedicalQa currentRecord = new AiMedicalQa();
        currentRecord.setId(2L);
        currentRecord.setQuestion("那需要急诊吗");

        Method method = AiMedicalServiceImpl.class.getDeclaredMethod("buildMessages", List.class, AiMedicalQa.class);
        method.setAccessible(true);

        List<Map<String, String>> messages = (List<Map<String, String>>) method.invoke(service, List.of(historyRecord, currentRecord), currentRecord);

        assertThat(messages).hasSize(5);
        assertThat(messages.get(1).get("role")).isEqualTo("system");
        assertThat(messages.get(1).get("content")).contains("continue_session");
        assertThat(messages.get(2).get("role")).isEqualTo("user");
        assertThat(messages.get(2).get("content")).isEqualTo("胸闷要看什么科");
        assertThat(messages.get(3).get("role")).isEqualTo("assistant");
        assertThat(messages.get(4).get("content")).isEqualTo("那需要急诊吗");
    }
}
