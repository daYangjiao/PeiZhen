package org.example.service.impl;

import org.example.dao.AiMedicalQaMapper;
import org.example.dao.UserMapper;
import org.example.model.AiMedicalQa;
import org.example.model.User;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiMedicalServiceImplTest {

    @Mock
    private AiMedicalQaMapper aiMedicalQaMapper;
    @Mock
    private DeepSeekClient deepSeekClient;
    @Mock
    private UserMapper userMapper;

    @Test
    @SuppressWarnings("unchecked")
    void buildMessagesShouldMarkNewSessionWithoutHistoryContext() throws Exception {
        AiMedicalServiceImpl service = new AiMedicalServiceImpl(aiMedicalQaMapper, deepSeekClient, userMapper);
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
        AiMedicalServiceImpl service = new AiMedicalServiceImpl(aiMedicalQaMapper, deepSeekClient, userMapper);
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

    @Test
    @SuppressWarnings("unchecked")
    void buildMessagesShouldIncludeMaleUserProfileContext() throws Exception {
        AiMedicalServiceImpl service = new AiMedicalServiceImpl(aiMedicalQaMapper, deepSeekClient, userMapper);
        User user = new User();
        user.setName("周铭");
        user.setSex("男");
        user.setAge(35);
        when(userMapper.findById(17)).thenReturn(user);

        AiMedicalQa currentRecord = new AiMedicalQa();
        currentRecord.setId(200L);
        currentRecord.setUserId(17);
        currentRecord.setQuestion("胸闷气短要不要急诊");

        Method method = AiMedicalServiceImpl.class.getDeclaredMethod("buildMessages", List.class, AiMedicalQa.class);
        method.setAccessible(true);

        List<Map<String, String>> messages = (List<Map<String, String>>) method.invoke(service, List.of(currentRecord), currentRecord);

        assertThat(messages.get(1).get("role")).isEqualTo("system");
        assertThat(messages.get(1).get("content"))
                .contains("用户资料")
                .contains("姓名=周铭")
                .contains("性别=男")
                .contains("年龄=35")
                .contains("先生");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildMessagesShouldIncludeFemaleUserProfileContext() throws Exception {
        AiMedicalServiceImpl service = new AiMedicalServiceImpl(aiMedicalQaMapper, deepSeekClient, userMapper);
        User user = new User();
        user.setName("范涵伶");
        user.setSex("女");
        user.setAge(26);
        when(userMapper.findById(15)).thenReturn(user);

        AiMedicalQa currentRecord = new AiMedicalQa();
        currentRecord.setId(201L);
        currentRecord.setUserId(15);
        currentRecord.setQuestion("反复胃痛挂什么科");

        Method method = AiMedicalServiceImpl.class.getDeclaredMethod("buildMessages", List.class, AiMedicalQa.class);
        method.setAccessible(true);

        List<Map<String, String>> messages = (List<Map<String, String>>) method.invoke(service, List.of(currentRecord), currentRecord);

        assertThat(messages.get(1).get("content"))
                .contains("姓名=范涵伶")
                .contains("性别=女")
                .contains("年龄=26")
                .contains("女士");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildMessagesShouldAvoidGenderedTitleWhenSexUnknown() throws Exception {
        AiMedicalServiceImpl service = new AiMedicalServiceImpl(aiMedicalQaMapper, deepSeekClient, userMapper);
        User user = new User();
        user.setName("用户");
        user.setSex("unknown");
        when(userMapper.findById(88)).thenReturn(user);

        AiMedicalQa currentRecord = new AiMedicalQa();
        currentRecord.setId(202L);
        currentRecord.setUserId(88);
        currentRecord.setQuestion("头晕恶心应该看哪个科");

        Method method = AiMedicalServiceImpl.class.getDeclaredMethod("buildMessages", List.class, AiMedicalQa.class);
        method.setAccessible(true);

        List<Map<String, String>> messages = (List<Map<String, String>>) method.invoke(service, List.of(currentRecord), currentRecord);

        assertThat(messages.get(1).get("content"))
                .contains("性别=未知")
                .contains("不要猜测用户性别")
                .contains("不要使用先生或女士");
    }
}
