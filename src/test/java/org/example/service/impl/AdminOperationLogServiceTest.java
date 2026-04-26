package org.example.service.impl;

import org.example.dao.AdminOperationLogMapper;
import org.example.dao.SysAdminMapper;
import org.example.dao.UserMapper;
import org.example.entity.SysAdmin;
import org.example.model.AdminOperationLog;
import org.example.model.User;
import org.example.model.response.PagedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOperationLogServiceTest {

    @Mock
    private AdminOperationLogMapper operationLogMapper;
    @Mock
    private SysAdminMapper sysAdminMapper;
    @Mock
    private UserMapper userMapper;

    @Test
    void queryLogsShouldRejectNormalAdmin() {
        AdminOperationLogService service = new AdminOperationLogService(operationLogMapper, sysAdminMapper, userMapper);
        SysAdmin admin = new SysAdmin();
        admin.setId(2);
        admin.setRole("ADMIN");
        when(sysAdminMapper.findById(2)).thenReturn(admin);

        assertThatThrownBy(() -> service.getLogs(2, null, null, null, null, null, null, 0, 10))
                .isInstanceOf(SecurityException.class)
                .hasMessage("仅超级管理员可查看操作日志");
    }

    @Test
    void recordShouldSnapshotOperatorIdentity() {
        AdminOperationLogService service = new AdminOperationLogService(operationLogMapper, sysAdminMapper, userMapper);
        SysAdmin admin = new SysAdmin();
        admin.setId(1);
        admin.setName("总管理员");
        admin.setPhone("18650680037");
        admin.setRole("SUPER_ADMIN");
        when(sysAdminMapper.findById(1)).thenReturn(admin);

        service.record(1, "ORDER", "RESOLVE_DISPUTE", "ORDER", 88, "ORD-88", 5, 9, "平台处理", "{\"orderId\":88}");

        ArgumentCaptor<AdminOperationLog> captor = ArgumentCaptor.forClass(AdminOperationLog.class);
        verify(operationLogMapper).insert(captor.capture());
        AdminOperationLog log = captor.getValue();
        assertThat(log.getOperatorId()).isEqualTo(1);
        assertThat(log.getOperatorName()).isEqualTo("总管理员");
        assertThat(log.getOperatorPhone()).isEqualTo("18650680037");
        assertThat(log.getOperatorRole()).isEqualTo("SUPER_ADMIN");
        assertThat(log.getModule()).isEqualTo("ORDER");
        assertThat(log.getAction()).isEqualTo("RESOLVE_DISPUTE");
        assertThat(log.getTargetId()).isEqualTo(88);
        assertThat(log.getFromStatus()).isEqualTo(5);
        assertThat(log.getToStatus()).isEqualTo(9);
        assertThat(log.getCreateTime()).isNotNull();
    }

    @Test
    void queryLogsShouldResolveNumericAttendantTargetLabel() {
        AdminOperationLogService service = new AdminOperationLogService(operationLogMapper, sysAdminMapper, userMapper);
        SysAdmin admin = new SysAdmin();
        admin.setId(1);
        admin.setRole("SUPER_ADMIN");
        when(sysAdminMapper.findById(1)).thenReturn(admin);

        AdminOperationLog log = new AdminOperationLog();
        log.setId(7L);
        log.setModule("ATTENDANT");
        log.setAction("APPROVE");
        log.setTargetType("ATTENDANT");
        log.setTargetId(21);
        log.setTargetLabel("21");
        when(operationLogMapper.countLogs(null, null, null, null, null, null)).thenReturn(1);
        when(operationLogMapper.findLogs(null, null, null, null, null, null, 0, 10)).thenReturn(java.util.List.of(log));

        User attendant = new User();
        attendant.setId(21);
        attendant.setName("王小花");
        attendant.setPhone("17311209183");
        when(userMapper.findById(21)).thenReturn(attendant);

        PagedResponse<?> response = service.getLogs(1, null, null, null, null, null, null, 0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(((org.example.model.response.AdminOperationLogResponse) response.getContent().get(0)).getTargetLabel())
                .isEqualTo("王小花（173****9183）");
    }
}
