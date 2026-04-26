package org.example.service.impl;

import org.example.dao.SysAdminMapper;
import org.example.dao.ThirdPartyAccountMapper;
import org.example.entity.SysAdmin;
import org.example.model.ThirdPartyAccount;
import org.example.model.response.AdminLoginResponse;
import org.example.unity.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysAdminWechatLoginTest {

    @Mock
    private SysAdminMapper sysAdminMapper;
    @Mock
    private ThirdPartyAccountMapper thirdPartyAccountMapper;

    private SysAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SysAdminServiceImpl(
                sysAdminMapper,
                thirdPartyAccountMapper,
                new BCryptPasswordEncoder(),
                new JwtUtil("dev-jwt-secret-change-me-please-1234567890", 86400000L)
        );
    }

    @Test
    void loginByWechatIdentityShouldIssueAdminTokenWhenBound() {
        ThirdPartyAccount account = new ThirdPartyAccount();
        account.setPrincipalId(8);
        when(thirdPartyAccountMapper.findAdminAccountByWechat("WEB_SCAN", "openid-1", "union-1")).thenReturn(account);
        when(sysAdminMapper.findById(8)).thenReturn(superAdmin(8));

        AdminLoginResponse response = service.loginByWechatIdentity("WEB_SCAN", "openid-1", "union-1");

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUserInfo().getId()).isEqualTo(8);
        verify(sysAdminMapper).update(any(SysAdmin.class));
    }

    @Test
    void loginByWechatIdentityShouldRejectUnboundWechatAccount() {
        when(thirdPartyAccountMapper.findAdminAccountByWechat("WEB_SCAN", "openid-1", "union-1")).thenReturn(null);

        assertThatThrownBy(() -> service.loginByWechatIdentity("WEB_SCAN", "openid-1", "union-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("该微信尚未绑定管理员账号");
    }

    @Test
    void bindWechatIdentityShouldRequireSuperAdminOrSelf() {
        when(sysAdminMapper.findById(8)).thenReturn(superAdmin(8));

        service.bindWechatIdentity(8, "WEB_SCAN", "openid-1", "union-1");

        verify(thirdPartyAccountMapper).upsert(any(ThirdPartyAccount.class));
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
}
