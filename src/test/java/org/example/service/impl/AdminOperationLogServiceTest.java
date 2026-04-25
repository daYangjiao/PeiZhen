package org.example.service.impl;

import org.example.dao.AdminOperationLogMapper;
import org.example.dao.SysAdminMapper;
import org.example.entity.SysAdmin;
import org.example.model.AdminOperationLog;
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

    @Test
    void queryLogsShouldRejectNormalAdmin() {
        AdminOperationLogService service = new AdminOperationLogService(operationLogMapper, sysAdminMapper);
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
        AdminOperationLogService service = new AdminOperationLogService(operationLogMapper, sysAdminMapper);
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
}
