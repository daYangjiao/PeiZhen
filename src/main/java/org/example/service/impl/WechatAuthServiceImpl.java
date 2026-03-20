package org.example.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.UserMapper;
import org.example.model.User;
import org.example.model.request.WechatBindPhoneRequest;
import org.example.model.request.WechatLoginRequest;
import org.example.service.UserService;
import org.example.service.WechatAuthService;
import org.example.unity.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WechatAuthServiceImpl implements WechatAuthService {

    private static final String USER_ROLE = "user";
    private static final String WECHAT_BIND_TOKEN_TYPE = "wechat_bind";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final UserService userService;
    @Value("${wechat.mini-app.app-id:}")
    private String miniAppId;

    @Value("${wechat.mini-app.secret:}")
    private String miniAppSecret;

    @Value("${wechat.bind-token-expiration-ms:600000}")
    private long bindTokenExpirationMs;

    @Override
    public Map<String, Object> getConfigStatus() {
        Map<String, Object> data = new HashMap<>();
        boolean enabled = isConfigured();
        data.put("enabled", enabled);
        if (!enabled) {
            data.put("reason", "微信登录暂未开通");
        }
        return data;
    }

    @Override
    public Map<String, Object> login(WechatLoginRequest request) {
        ensureWechatLoginEnabled();
        ensureUserRole(request.getRole());
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("微信登录 code 不能为空");
        }

        JsonNode sessionNode = exchangeCodeForSession(request.getCode().trim());
        String openid = textValue(sessionNode, "openid");
        if (openid == null || openid.isBlank()) {
            throw new IllegalArgumentException("微信登录失败，未获取到 openid");
        }

        User user = userMapper.findByOpenid(openid);
        Map<String, Object> data = new HashMap<>();
        if (user != null) {
            if (!isSupportedWechatUser(user)) {
                throw new IllegalArgumentException("当前仅支持普通用户微信登录");
            }
            data.put("bindStatus", "BOUND");
            data.put("token", jwtUtil.generateToken(user.getId()));
            data.put("userInfo", buildUserInfo(user));
            return data;
        }

        Map<String, Object> bindClaims = new HashMap<>();
        bindClaims.put("tokenType", WECHAT_BIND_TOKEN_TYPE);
        bindClaims.put("openid", openid);
        bindClaims.put("role", normalizeRole(request.getRole()));
        data.put("bindStatus", "UNBOUND");
        data.put("wechatBindToken", jwtUtil.generateToken(0, bindClaims, bindTokenExpirationMs));
        return data;
    }

    @Transactional
    @Override
    public Map<String, Object> bindPhone(WechatBindPhoneRequest request) {
        ensureWechatLoginEnabled();
        ensureUserRole(request.getRole());
        if (request.getWechatBindToken() == null || request.getWechatBindToken().isBlank()) {
            throw new IllegalArgumentException("微信绑定凭证不能为空");
        }
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("姓名不能为空");
        }

        Claims claims = jwtUtil.getAllClaimsFromToken(request.getWechatBindToken());
        if (!WECHAT_BIND_TOKEN_TYPE.equals(String.valueOf(claims.get("tokenType")))) {
            throw new IllegalArgumentException("微信绑定凭证无效");
        }
        String openid = String.valueOf(claims.get("openid"));
        if (openid == null || openid.isBlank() || "null".equalsIgnoreCase(openid)) {
            throw new IllegalArgumentException("微信身份无效");
        }
        User openidUser = userMapper.findByOpenid(openid);
        if (openidUser != null) {
            if (!isSupportedWechatUser(openidUser)) {
                throw new IllegalArgumentException("当前仅支持普通用户微信登录");
            }
            return buildBoundResult(openidUser);
        }

        User existing = userMapper.findByPhone(request.getPhone().trim());
        User targetUser;
        if (existing == null) {
            User user = new User();
            user.setPhone(request.getPhone().trim());
            user.setPassword(request.getPassword().trim());
            user.setName(request.getName().trim());
            user.setUserType(0);
            user.setOpenid(openid);
            userService.register(user);
            targetUser = userMapper.findById(user.getId());
        } else {
            if (!isSupportedWechatUser(existing)) {
                throw new IllegalArgumentException("当前仅支持普通用户微信登录");
            }
            if (existing.getOpenid() != null && !existing.getOpenid().isBlank() && !openid.equals(existing.getOpenid())) {
                throw new IllegalArgumentException("该手机号已绑定其他微信账号");
            }
            User authenticatedUser = userService.login(request.getPhone().trim(), request.getPassword().trim());
            if (authenticatedUser == null) {
                throw new IllegalArgumentException("手机号或密码错误，无法完成绑定");
            }
            User patch = new User();
            patch.setId(authenticatedUser.getId());
            patch.setOpenid(openid);
            if (authenticatedUser.getName() == null || authenticatedUser.getName().isBlank()) {
                patch.setName(request.getName().trim());
            }
            userService.update(patch);
            targetUser = userMapper.findById(authenticatedUser.getId());
        }

        return buildBoundResult(targetUser);
    }

    private Map<String, Object> buildBoundResult(User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("bindStatus", "BOUND");
        data.put("token", jwtUtil.generateToken(user.getId()));
        data.put("userInfo", buildUserInfo(user));
        return data;
    }

    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("name", user.getName());
        userInfo.put("userType", user.getUserType());
        userInfo.put("phone", user.getPhone());
        userInfo.put("avatar", user.getAvatar());
        return userInfo;
    }

    private JsonNode exchangeCodeForSession(String code) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api.weixin.qq.com/sns/jscode2session")
                    .queryParam("appid", miniAppId)
                    .queryParam("secret", miniAppSecret)
                    .queryParam("js_code", code)
                    .queryParam("grant_type", "authorization_code")
                    .toUriString();
            String body = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            int errCode = root.path("errcode").asInt(0);
            if (errCode != 0) {
                String errMsg = textValue(root, "errmsg");
                log.warn("微信 code2Session 失败: code={}, msg={}", errCode, errMsg);
                throw new IllegalArgumentException("微信登录失败，请稍后重试");
            }
            return root;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用微信 code2Session 失败", e);
            throw new IllegalArgumentException("微信登录失败，请稍后重试");
        }
    }

    private boolean isConfigured() {
        return miniAppId != null && !miniAppId.isBlank()
                && miniAppSecret != null && !miniAppSecret.isBlank();
    }

    private void ensureWechatLoginEnabled() {
        if (!isConfigured()) {
            throw new IllegalStateException("微信登录暂未开通");
        }
    }

    private void ensureUserRole(String role) {
        if (!USER_ROLE.equals(normalizeRole(role))) {
            throw new IllegalArgumentException("当前仅支持普通用户微信登录");
        }
    }

    private String normalizeRole(String role) {
        return role == null || role.isBlank() ? USER_ROLE : role.trim().toLowerCase();
    }

    private boolean isSupportedWechatUser(User user) {
        return user != null && Integer.valueOf(0).equals(user.getUserType());
    }

    private String textValue(JsonNode root, String fieldName) {
        JsonNode value = root.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}
