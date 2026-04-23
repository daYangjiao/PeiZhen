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
        when(sysAdminMapper.findByPhone("13800000008")).thenReturn(admin);

        AdminLoginResponse response = service.login("13800000008", "secret123");

        assertThat(response.getToken()).isNotBlank();
        assertThat(jwtUtil.getPrincipalTypeFromToken(response.getToken())).isEqualTo("admin");
        assertThat(jwtUtil.getAdminIdFromToken(response.getToken())).isEqualTo(8);

        ArgumentCaptor<SysAdmin> patchCaptor = ArgumentCaptor.forClass(SysAdmin.class);
        verify(sysAdminMapper).update(patchCaptor.capture());
        assertThat(patchCaptor.getValue().getId()).isEqualTo(8);
        assertThat(patchCaptor.getValue().getLastLoginTime()).isNotNull();
    }

    @Test
    void createAdminShouldRejectDuplicatePhone() {
        SysAdmin existing = new SysAdmin();
        existing.setId(1);
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
        assertThat(response.getId()).isEqualTo(9);
    }

    @Test
    void updateStatusShouldRejectDisablingCurrentAdmin() {
        SysAdmin current = new SysAdmin();
        current.setId(5);
        current.setStatus(1);
        when(sysAdminMapper.findById(5)).thenReturn(current);

        assertThatThrownBy(() -> service.updateStatus(5, 5, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("不能禁用当前登录管理员");
    }

    @Test
    void getAdminsShouldReturnPagedAdmins() {
        SysAdmin admin = new SysAdmin();
        admin.setId(6);
        admin.setName("审核管理员");
        admin.setPhone("13800000006");
        admin.setStatus(1);

        when(sysAdminMapper.countAdmins("审核", 1)).thenReturn(1);
        when(sysAdminMapper.findAdmins("审核", 1, 0, 10)).thenReturn(List.of(admin));

        PagedResponse<SysAdmin> response = service.getAdmins("审核", 1, 0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getPhone()).isEqualTo("13800000006");
    }
}
