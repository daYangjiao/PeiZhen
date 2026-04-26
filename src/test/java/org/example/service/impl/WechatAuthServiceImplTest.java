package org.example.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dao.ThirdPartyAccountMapper;
import org.example.dao.UserMapper;
import org.example.model.ThirdPartyAccount;
import org.example.model.User;
import org.example.model.request.WechatBindPhoneRequest;
import org.example.model.request.WechatLoginRequest;
import org.example.service.AttendantService;
import org.example.service.UserService;
import org.example.unity.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WechatAuthServiceImplTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ThirdPartyAccountMapper thirdPartyAccountMapper;
    @Mock
    private UserService userService;
    @Mock
    private AttendantService attendantService;

    private JwtUtil jwtUtil;
    private WechatAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("dev-jwt-secret-change-me-please-1234567890", 86400000L);
        service = new WechatAuthServiceImpl(
                restTemplate,
                new ObjectMapper(),
                jwtUtil,
                userMapper,
                thirdPartyAccountMapper,
                userService,
                attendantService
        );
        ReflectionTestUtils.setField(service, "miniAppId", "mini-app-id");
        ReflectionTestUtils.setField(service, "miniAppSecret", "mini-secret");
        ReflectionTestUtils.setField(service, "mobileAppId", "mobile-app-id");
        ReflectionTestUtils.setField(service, "mobileAppSecret", "mobile-secret");
        ReflectionTestUtils.setField(service, "h5AppId", "h5-app-id");
        ReflectionTestUtils.setField(service, "h5AppSecret", "h5-secret");
        ReflectionTestUtils.setField(service, "h5RedirectUri", "https://example.com/api/users/wechat/oauth-callback");
        ReflectionTestUtils.setField(service, "webAppId", "web-app-id");
        ReflectionTestUtils.setField(service, "webAppSecret", "web-secret");
        ReflectionTestUtils.setField(service, "webRedirectUri", "https://example.com/api/admin/auth/wechat/oauth-callback");
    }

    @Test
    void getConfigStatusShouldExposeEnabledWechatPlatforms() {
        Map<String, Object> status = service.getConfigStatus("user");

        assertThat(status).containsEntry("miniProgramEnabled", true);
        assertThat(status).containsEntry("appEnabled", true);
        assertThat(status).containsEntry("h5Enabled", true);
        assertThat(status).containsEntry("webScanEnabled", true);
    }

    @Test
    void buildOAuthUrlShouldUseWechatH5ScopeForUserLogin() {
        String url = service.buildUserOAuthUrl("user", "WECHAT_H5", "https://example.com/after-login");

        assertThat(url).contains("https://open.weixin.qq.com/connect/oauth2/authorize");
        assertThat(url).contains("appid=h5-app-id");
        assertThat(url).contains("scope=snsapi_userinfo");
        assertThat(url).contains("state=");
    }

    @Test
    void appLoginShouldExchangeCodeAndReturnBoundUserToken() {
        WechatLoginRequest request = new WechatLoginRequest();
        request.setPlatform("APP");
        request.setRole("user");
        request.setCode("app-code");

        ThirdPartyAccount account = new ThirdPartyAccount();
        account.setPrincipalId(12);
        when(restTemplate.getForObject(contains("sns/oauth2/access_token"), any()))
                .thenReturn("{\"openid\":\"app-openid\",\"unionid\":\"union-1\",\"access_token\":\"token\"}");
        when(thirdPartyAccountMapper.findUserAccountByWechat("APP", "app-openid", "union-1")).thenReturn(account);
        User user = new User();
        user.setId(12);
        user.setUserType(0);
        user.setPhone("13800000012");
        user.setName("微信用户");
        when(userMapper.findById(12)).thenReturn(user);

        Map<String, Object> result = service.login(request);

        assertThat(result).containsEntry("bindStatus", "BOUND");
        assertThat(result.get("token")).isInstanceOf(String.class);
    }

    @Test
    void bindPhoneShouldCreateThirdPartyAccountForWechatIdentity() {
        String bindToken = jwtUtil.generateToken(0, Map.of(
                "tokenType", "wechat_bind",
                "provider", "WECHAT",
                "platform", "APP",
                "openid", "app-openid",
                "unionid", "union-1",
                "role", "user"
        ), 600000L);

        WechatBindPhoneRequest request = new WechatBindPhoneRequest();
        request.setWechatBindToken(bindToken);
        request.setRole("user");
        request.setPhone("13800000012");
        request.setPassword("secret123");
        request.setName("微信用户");

        User existing = new User();
        existing.setId(12);
        existing.setUserType(0);
        existing.setPhone("13800000012");
        existing.setName("微信用户");
        when(userMapper.findByPhone("13800000012")).thenReturn(existing);
        when(userService.login("13800000012", "secret123")).thenReturn(existing);
        when(userMapper.findById(12)).thenReturn(existing);

        Map<String, Object> result = service.bindPhone(request);

        assertThat(result).containsEntry("bindStatus", "BOUND");
        verify(thirdPartyAccountMapper).upsert(any(ThirdPartyAccount.class));
    }
}
