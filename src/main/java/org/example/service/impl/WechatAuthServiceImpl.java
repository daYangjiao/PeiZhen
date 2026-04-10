package org.example.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Attendant;
import org.example.dao.UserMapper;
import org.example.model.User;
import org.example.model.request.WechatBindPhoneRequest;
import org.example.model.request.WechatLoginRequest;
import org.example.service.AttendantService;
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
    private static final String ESCORT_ROLE = "escort";
    private static final String WECHAT_BIND_TOKEN_TYPE = "wechat_bind";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final UserService userService;
    private final AttendantService attendantService;
    @Value("${wechat.mini-app.app-id:}")
    private String miniAppId;

    @Value("${wechat.mini-app.secret:}")
    private String miniAppSecret;

    @Value("${wechat.bind-token-expiration-ms:600000}")
    private long bindTokenExpirationMs;

    @Override
    public Map<String, Object> getConfigStatus(String role) {
        Map<String, Object> data = new HashMap<>();
        String normalizedRole = normalizeRole(role);
        boolean enabled = isConfigured();
        data.put("enabled", enabled && isSupportedRole(normalizedRole));
        data.put("role", normalizedRole);
        data.put("miniProgramOnly", true);
        data.put("supportedRoles", new String[]{USER_ROLE, ESCORT_ROLE});
        if (!enabled) {
            data.put("reason", "微信登录暂未开通");
        } else if (!isSupportedRole(normalizedRole)) {
            data.put("reason", "当前角色暂不支持微信登录");
        }
        return data;
    }

    @Override
    public Map<String, Object> login(WechatLoginRequest request) {
        ensureWechatLoginEnabled();
        ensureSupportedRole(request.getRole());
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
            if (!isSupportedWechatUser(user, request.getRole())) {
                throw new IllegalArgumentException(buildRoleMismatchMessage(user, request.getRole()));
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
        ensureSupportedRole(request.getRole());
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
        String tokenRole = normalizeRole(String.valueOf(claims.get("role")));
        if (request.getRole() != null && !normalizeRole(request.getRole()).equals(tokenRole)) {
            throw new IllegalArgumentException("微信绑定角色不一致，请重新发起微信登录");
        }
        String openid = String.valueOf(claims.get("openid"));
        if (openid == null || openid.isBlank() || "null".equalsIgnoreCase(openid)) {
            throw new IllegalArgumentException("微信身份无效");
        }
        User openidUser = userMapper.findByOpenid(openid);
        if (openidUser != null) {
            if (!isSupportedWechatUser(openidUser, tokenRole)) {
                throw new IllegalArgumentException(buildRoleMismatchMessage(openidUser, tokenRole));
            }
            return buildBoundResult(openidUser);
        }

        User existing = userMapper.findByPhone(request.getPhone().trim());
        User targetUser;
        if (existing == null) {
            targetUser = createWechatUser(request, tokenRole, openid);
        } else {
            if (!isSupportedWechatUser(existing, tokenRole)) {
                throw new IllegalArgumentException(buildRoleMismatchMessage(existing, tokenRole));
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

    private void ensureSupportedRole(String role) {
        if (!isSupportedRole(normalizeRole(role))) {
            throw new IllegalArgumentException("当前角色暂不支持微信登录");
        }
    }

    private String normalizeRole(String role) {
        return role == null || role.isBlank() ? USER_ROLE : role.trim().toLowerCase();
    }

    private boolean isSupportedRole(String role) {
        return USER_ROLE.equals(role) || ESCORT_ROLE.equals(role);
    }

    private boolean isSupportedWechatUser(User user, String role) {
        if (user == null) return false;
        Integer expectedUserType = expectedUserType(role);
        return expectedUserType != null && expectedUserType.equals(user.getUserType());
    }

    private Integer expectedUserType(String role) {
        String normalizedRole = normalizeRole(role);
        if (ESCORT_ROLE.equals(normalizedRole)) return 1;
        return 0;
    }

    private String buildRoleMismatchMessage(User user, String role) {
        String requestedRoleName = ESCORT_ROLE.equals(normalizeRole(role)) ? "陪诊师" : "用户";
        String actualRoleName = Integer.valueOf(1).equals(user.getUserType()) ? "陪诊师" : "用户";
        if (actualRoleName.equals(requestedRoleName)) {
            return "当前角色暂不支持微信登录";
        }
        return String.format("当前微信已绑定%s账号，请切换到%s端登录", actualRoleName, actualRoleName);
    }

    private User createWechatUser(WechatBindPhoneRequest request, String role, String openid) {
        User user = new User();
        user.setPhone(request.getPhone().trim());
        user.setPassword(request.getPassword().trim());
        user.setName(request.getName().trim());
        user.setOpenid(openid);
        if (ESCORT_ROLE.equals(role)) {
            user.setUserType(1);
            Attendant attendant = new Attendant();
            attendant.setStatus(0);
            attendant.setQualificationFailReason("");
            attendant.setIntroduction("");
            attendant.setProfessionalField("");
            attendant.setExperienceYears(0);
            attendant.setHospitalName("");
            attendantService.registerAttendant(user, attendant);
        } else {
            user.setUserType(0);
            userService.register(user);
        }
        return userMapper.findById(user.getId());
    }

    private String textValue(JsonNode root, String fieldName) {
        JsonNode value = root.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}
