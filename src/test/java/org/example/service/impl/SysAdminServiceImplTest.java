package org.example.service.impl;

import org.example.dao.SysAdminMapper;
import org.example.entity.SysAdmin;
import org.example.model.request.AdminCreateSysAdminRequest;
import org.example.model.response.AdminLoginResponse;
import org.example.model.response.PagedResponse;
import org.example.unity.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysAdminServiceImplTest {

    @Mock
    private SysAdminMapper sysAdminMapper;

    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private SysAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtUtil = new JwtUtil("dev-jwt-secret-change-me-please-1234567890", 86400000L);
        service = new SysAdminServiceImpl(sysAdminMapper, passwordEncoder, jwtUtil);
    }

    @Test
    void loginShouldIssueAdminTokenAndUpdateLastLoginTime() {
        SysAdmin admin = new SysAdmin();
        admin.setId(8);
        admin.setPhone("13800000008");
        admin.setPassword(passwordEncoder.encode("secret123"));
        admin.setStatus(1);
        admin.setRole("SUPER_ADMIN");
        when(sysAdminMapper.findByPhone("13800000008")).thenReturn(admin);

        AdminLoginResponse response = service.login("13800000008", "secret123");

        assertThat(response.getToken()).isNotBlank();
        assertThat(jwtUtil.getPrincipalTypeFromToken(response.getToken())).isEqualTo("admin");
        assertThat(jwtUtil.getAdminIdFromToken(response.getToken())).isEqualTo(8);
        assertThat(jwtUtil.getAllClaimsFromToken(response.getToken()).get("role", String.class)).isEqualTo("SUPER_ADMIN");
        assertThat(response.getUserInfo().getRole()).isEqualTo("SUPER_ADMIN");

        ArgumentCaptor<SysAdmin> patchCaptor = ArgumentCaptor.forClass(SysAdmin.class);
        verify(sysAdminMapper).update(patchCaptor.capture());
        assertThat(patchCaptor.getValue().getId()).isEqualTo(8);
        assertThat(patchCaptor.getValue().getLastLoginTime()).isNotNull();
    }

    @Test
    void createAdminShouldRejectDuplicatePhone() {
        SysAdmin existing = new SysAdmin();
        existing.setId(1);
        when(sysAdminMapper.findById(1)).thenReturn(superAdmin(1));
        when(sysAdminMapper.findByPhone("13800000000")).thenReturn(existing);

        AdminCreateSysAdminRequest request = new AdminCreateSysAdminRequest();
        request.setName("运营管理员");
        request.setPhone("13800000000");
        request.setPassword("secret123");

        assertThatThrownBy(() -> service.createAdmin(1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("管理员手机号已存在");
    }

    @Test
    void createAdminShouldPersistEncodedPassword() {
        when(sysAdminMapper.findById(1)).thenReturn(superAdmin(1));
        when(sysAdminMapper.findByPhone("13800000009")).thenReturn(null);
        doAnswer(invocation -> {
            SysAdmin admin = invocation.getArgument(0);
            admin.setId(9);
            return 1;
        }).when(sysAdminMapper).insert(any(SysAdmin.class));

        SysAdmin persisted = new SysAdmin();
        persisted.setId(9);
        persisted.setName("新管理员");
        persisted.setPhone("13800000009");
        persisted.setStatus(1);
        persisted.setRole("ADMIN");
        when(sysAdminMapper.findById(9)).thenReturn(persisted);

        AdminCreateSysAdminRequest request = new AdminCreateSysAdminRequest();
        request.setName("新管理员");
        request.setPhone("13800000009");
        request.setPassword("secret123");

        SysAdmin response = service.createAdmin(1, request);

        ArgumentCaptor<SysAdmin> insertCaptor = ArgumentCaptor.forClass(SysAdmin.class);
        verify(sysAdminMapper).insert(insertCaptor.capture());
        assertThat(insertCaptor.getValue().getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches("secret123", insertCaptor.getValue().getPassword())).isTrue();
        assertThat(insertCaptor.getValue().getRole()).isEqualTo("ADMIN");
        assertThat(response.getId()).isEqualTo(9);
    }

    @Test
    void createAdminShouldAllowSuperAdminRoleWhenRequestedBySuperAdmin() {
        when(sysAdminMapper.findById(1)).thenReturn(superAdmin(1));
        when(sysAdminMapper.findByPhone("13800000010")).thenReturn(null);
        doAnswer(invocation -> {
            SysAdmin admin = invocation.getArgument(0);
            admin.setId(10);
            return 1;
        }).when(sysAdminMapper).insert(any(SysAdmin.class));

        SysAdmin persisted = superAdmin(10);
        persisted.setName("超级管理员");
        persisted.setPhone("13800000010");
        when(sysAdminMapper.findById(10)).thenReturn(persisted);

        AdminCreateSysAdminRequest request = new AdminCreateSysAdminRequest();
        request.setName("超级管理员");
        request.setPhone("13800000010");
        request.setPassword("secret123");
        request.setRole("SUPER_ADMIN");

        SysAdmin response = service.createAdmin(1, request);

        ArgumentCaptor<SysAdmin> insertCaptor = ArgumentCaptor.forClass(SysAdmin.class);
        verify(sysAdminMapper).insert(insertCaptor.capture());
        assertThat(insertCaptor.getValue().getRole()).isEqualTo("SUPER_ADMIN");
        assertThat(response.getRole()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void createAdminShouldRejectNormalAdminOperator() {
        when(sysAdminMapper.findById(2)).thenReturn(normalAdmin(2));

        AdminCreateSysAdminRequest request = new AdminCreateSysAdminRequest();
        request.setName("新管理员");
        request.setPhone("13800000011");
        request.setPassword("secret123");

        assertThatThrownBy(() -> service.createAdmin(2, request))
                .isInstanceOf(SecurityException.class)
                .hasMessage("仅超级管理员可管理管理员账号");
    }

    @Test
    void updateStatusShouldRejectDisablingCurrentAdmin() {
        SysAdmin current = new SysAdmin();
        current.setId(5);
        current.setStatus(1);
        current.setRole("SUPER_ADMIN");
        when(sysAdminMapper.findById(5)).thenReturn(current);

        assertThatThrownBy(() -> service.updateStatus(5, 5, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("不能禁用当前登录管理员");
    }

    @Test
    void updateStatusShouldRejectNormalAdminOperator() {
        when(sysAdminMapper.findById(2)).thenReturn(normalAdmin(2));

        assertThatThrownBy(() -> service.updateStatus(2, 1, 0))
                .isInstanceOf(SecurityException.class)
                .hasMessage("仅超级管理员可管理管理员账号");
    }

    @Test
    void updateStatusShouldRejectDisablingLastEnabledSuperAdmin() {
        when(sysAdminMapper.findById(1)).thenReturn(superAdmin(1));
        when(sysAdminMapper.findById(5)).thenReturn(superAdmin(5));
        when(sysAdminMapper.countEnabledSuperAdmins()).thenReturn(1);

        assertThatThrownBy(() -> service.updateStatus(1, 5, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("至少保留一个启用状态的超级管理员");
    }

    @Test
    void deleteAdminShouldRejectNormalAdminOperator() {
        when(sysAdminMapper.findById(2)).thenReturn(normalAdmin(2));

        assertThatThrownBy(() -> service.deleteAdmin(2, 8))
                .isInstanceOf(SecurityException.class)
                .hasMessage("仅超级管理员可管理管理员账号");
    }

    @Test
    void deleteAdminShouldRejectDeletingCurrentAdmin() {
        when(sysAdminMapper.findById(1)).thenReturn(superAdmin(1));

        assertThatThrownBy(() -> service.deleteAdmin(1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("不能删除当前登录管理员");
        verify(sysAdminMapper, never()).deleteById(any());
    }

    @Test
    void deleteAdminShouldRejectWhenTargetNotFound() {
        when(sysAdminMapper.findById(1)).thenReturn(superAdmin(1));
        when(sysAdminMapper.findById(99)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteAdmin(1, 99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("管理员不存在");
        verify(sysAdminMapper, never()).deleteById(any());
    }

    @Test
    void deleteAdminShouldRejectDeletingLastEnabledSuperAdmin() {
        when(sysAdminMapper.findById(1)).thenReturn(superAdmin(1));
        when(sysAdminMapper.findById(8)).thenReturn(superAdmin(8));
        when(sysAdminMapper.countEnabledSuperAdmins()).thenReturn(1);

        assertThatThrownBy(() -> service.deleteAdmin(1, 8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("至少保留一个启用状态的超级管理员");
        verify(sysAdminMapper, never()).deleteById(any());
    }

    @Test
    void deleteAdminShouldDeleteById() {
        when(sysAdminMapper.findById(1)).thenReturn(superAdmin(1));
        when(sysAdminMapper.findById(8)).thenReturn(normalAdmin(8));

        service.deleteAdmin(1, 8);

        verify(sysAdminMapper).deleteById(8);
    }

    @Test
    void getAdminsShouldReturnPagedAdmins() {
        SysAdmin admin = new SysAdmin();
        admin.setId(6);
        admin.setName("审核管理员");
        admin.setPhone("13800000006");
        admin.setStatus(1);
        admin.setRole("ADMIN");

        when(sysAdminMapper.findById(1)).thenReturn(superAdmin(1));
        when(sysAdminMapper.countAdmins("审核", 1)).thenReturn(1);
        when(sysAdminMapper.findAdmins("审核", 1, 0, 10)).thenReturn(List.of(admin));

        PagedResponse<SysAdmin> response = service.getAdmins(1, "审核", 1, 0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getPhone()).isEqualTo("13800000006");
        assertThat(response.getContent().get(0).getRole()).isEqualTo("ADMIN");
    }

    @Test
    void getAdminsShouldRejectNormalAdminOperator() {
        when(sysAdminMapper.findById(2)).thenReturn(normalAdmin(2));

        assertThatThrownBy(() -> service.getAdmins(2, null, null, 0, 10))
                .isInstanceOf(SecurityException.class)
                .hasMessage("仅超级管理员可管理管理员账号");
    }

    private SysAdmin superAdmin(Integer id) {
        SysAdmin admin = new SysAdmin();
        admin.setId(id);
        admin.setName("超级管理员");
        admin.setPhone("13800000000");
        admin.setStatus(1);
        admin.setRole("SUPER_ADMIN");
        return admin;
    }

    private SysAdmin normalAdmin(Integer id) {
        SysAdmin admin = new SysAdmin();
        admin.setId(id);
        admin.setName("普通管理员");
        admin.setPhone("13800000002");
        admin.setStatus(1);
        admin.setRole("ADMIN");
        return admin;
    }
}
