package org.example.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.ThirdPartyAccountMapper;
import org.example.dao.UserMapper;
import org.example.model.Attendant;
import org.example.model.ThirdPartyAccount;
import org.example.model.User;
import org.example.model.request.WechatBindPhoneRequest;
import org.example.model.request.WechatLoginRequest;
import org.example.service.AttendantService;
import org.example.service.SysAdminService;
import org.example.service.UserService;
import org.example.service.WechatAuthService;
import org.example.unity.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
    private final ThirdPartyAccountMapper thirdPartyAccountMapper;
    private final UserService userService;
    private final AttendantService attendantService;

    @Autowired(required = false)
    private SysAdminService sysAdminService;

    @Value("${wechat.mini-app.app-id:}")
    private String miniAppId;

    @Value("${wechat.mini-app.secret:}")
    private String miniAppSecret;

    @Value("${wechat.mobile-app.app-id:}")
    private String mobileAppId;

    @Value("${wechat.mobile-app.secret:}")
    private String mobileAppSecret;

    @Value("${wechat.h5.app-id:}")
    private String h5AppId;

    @Value("${wechat.h5.secret:}")
    private String h5AppSecret;

    @Value("${wechat.h5.redirect-uri:}")
    private String h5RedirectUri;

    @Value("${wechat.web-scan.app-id:}")
    private String webAppId;

    @Value("${wechat.web-scan.secret:}")
    private String webAppSecret;

    @Value("${wechat.web-scan.redirect-uri:}")
    private String webRedirectUri;

    @Value("${wechat.bind-token-expiration-ms:600000}")
    private long bindTokenExpirationMs;

    @Override
    public Map<String, Object> getConfigStatus(String role) {
        Map<String, Object> data = new HashMap<>();
        String normalizedRole = normalizeRole(role);
        boolean miniEnabled = isConfigured("MINI_PROGRAM");
        boolean appEnabled = isConfigured("APP");
        boolean h5Enabled = isConfigured("WECHAT_H5");
        boolean webScanEnabled = isConfigured("WEB_SCAN");
        boolean enabled = (miniEnabled || appEnabled || h5Enabled) && isSupportedRole(normalizedRole);
        data.put("enabled", enabled);
        data.put("role", normalizedRole);
        data.put("miniProgramOnly", false);
        data.put("miniProgramEnabled", miniEnabled);
        data.put("appEnabled", appEnabled);
        data.put("h5Enabled", h5Enabled);
        data.put("webScanEnabled", webScanEnabled);
        data.put("supportedRoles", new String[]{USER_ROLE, ESCORT_ROLE});
        if (!enabled) {
            data.put("reason", "微信登录暂未开通");
        } else if (!isSupportedRole(normalizedRole)) {
            data.put("reason", "当前角色暂不支持微信登录");
        }
        return data;
    }

    @Override
    public String buildUserOAuthUrl(String role, String platform, String redirectUrl) {
        String normalizedPlatform = normalizePlatform(platform);
        if (!"WECHAT_H5".equals(normalizedPlatform)) {
            throw new IllegalArgumentException("当前平台不支持网页授权登录");
        }
        ensureConfigured(normalizedPlatform);
        ensureSupportedRole(role);
        String state = encodeState(Map.of(
                "role", normalizeRole(role),
                "platform", normalizedPlatform,
                "redirectUrl", safeRedirectUrl(redirectUrl, "/pages/auth/login")
        ));
        return UriComponentsBuilder
                .fromHttpUrl("https://open.weixin.qq.com/connect/oauth2/authorize")
                .queryParam("appid", h5AppId)
                .queryParam("redirect_uri", h5RedirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "snsapi_userinfo")
                .queryParam("state", state)
                .build()
                .toUriString() + "#wechat_redirect";
    }

    @Override
    public String handleUserOAuthCallback(String platform, String code, String state) {
        String normalizedPlatform = normalizePlatform(platform);
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("微信授权 code 不能为空");
        }
        Map<String, String> stateMap = decodeState(state);
        String role = normalizeRole(stateMap.get("role"));
        String redirectUrl = safeRedirectUrl(stateMap.get("redirectUrl"), "/pages/auth/login");
        JsonNode identity = exchangeOauthCode(normalizedPlatform, code.trim());
        Map<String, Object> result = loginWithWechatIdentity(
                normalizedPlatform,
                textValue(identity, "openid"),
                textValue(identity, "unionid"),
                role
        );
        return appendOAuthResult(redirectUrl, result);
    }

    @Override
    public String buildAdminOAuthUrl(String redirectUrl) {
        ensureConfigured("WEB_SCAN");
        String state = encodeState(Map.of(
                "platform", "WEB_SCAN",
                "redirectUrl", safeRedirectUrl(redirectUrl, "/admin/login")
        ));
        return UriComponentsBuilder
                .fromHttpUrl("https://open.weixin.qq.com/connect/qrconnect")
                .queryParam("appid", webAppId)
                .queryParam("redirect_uri", webRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "snsapi_login")
                .queryParam("state", state)
                .build()
                .toUriString() + "#wechat_redirect";
    }

    @Override
    public String handleAdminOAuthCallback(String code, String state) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("微信授权 code 不能为空");
        }
        if (sysAdminService == null) {
            throw new IllegalStateException("管理员微信登录暂不可用");
        }
        Map<String, String> stateMap = decodeState(state);
        String redirectUrl = safeRedirectUrl(stateMap.get("redirectUrl"), "/admin/login");
        JsonNode identity = exchangeOauthCode("WEB_SCAN", code.trim());
        String openid = textValue(identity, "openid");
        String unionid = textValue(identity, "unionid");
        try {
            org.example.model.response.AdminLoginResponse response =
                    sysAdminService.loginByWechatIdentity("WEB_SCAN", openid, unionid);
            return appendHash(redirectUrl, "wechatToken=" + encode(response.getToken()));
        } catch (IllegalArgumentException unbound) {
            Map<String, Object> claims = new HashMap<>();
            claims.put("tokenType", "admin_wechat_bind");
            claims.put("provider", "WECHAT");
            claims.put("platform", "WEB_SCAN");
            claims.put("openid", openid);
            claims.put("unionid", emptyToNull(unionid));
            String bindToken = jwtUtil.generateToken(0, claims, bindTokenExpirationMs);
            return appendHash(redirectUrl, "wechatBindToken=" + encode(bindToken));
        }
    }

    @Override
    public Map<String, Object> login(WechatLoginRequest request) {
        String platform = normalizePlatform(request.getPlatform());
        ensureConfigured(platform);
        ensureSupportedRole(request.getRole());
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("微信登录 code 不能为空");
        }

        JsonNode sessionNode = "MINI_PROGRAM".equals(platform)
                ? exchangeCodeForSession(request.getCode().trim())
                : exchangeOauthCode(platform, request.getCode().trim());
        return loginWithWechatIdentity(
                platform,
                textValue(sessionNode, "openid"),
                textValue(sessionNode, "unionid"),
                normalizeRole(request.getRole())
        );
    }

    @Transactional
    @Override
    public Map<String, Object> bindPhone(WechatBindPhoneRequest request) {
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
        String platform = normalizePlatform(String.valueOf(claims.get("platform")));
        String unionid = emptyToNull(String.valueOf(claims.get("unionid")));
        User openidUser = findWechatUser(platform, openid, unionid);
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
            if ("MINI_PROGRAM".equals(platform)
                    && existing.getOpenid() != null
                    && !existing.getOpenid().isBlank()
                    && !openid.equals(existing.getOpenid())) {
                throw new IllegalArgumentException("该手机号已绑定其他微信账号");
            }
            User authenticatedUser = userService.login(request.getPhone().trim(), request.getPassword().trim());
            if (authenticatedUser == null) {
                throw new IllegalArgumentException("手机号或密码错误，无法完成绑定");
            }
            User patch = new User();
            patch.setId(authenticatedUser.getId());
            if ("MINI_PROGRAM".equals(platform)) {
                patch.setOpenid(openid);
            }
            if (authenticatedUser.getName() == null || authenticatedUser.getName().isBlank()) {
                patch.setName(request.getName().trim());
            }
            userService.update(patch);
            targetUser = userMapper.findById(authenticatedUser.getId());
        }

        upsertWechatUserAccount(targetUser.getId(), platform, openid, unionid);
        return buildBoundResult(targetUser);
    }

    private Map<String, Object> loginWithWechatIdentity(String platform, String openid, String unionid, String role) {
        if (openid == null || openid.isBlank()) {
            throw new IllegalArgumentException("微信登录失败，未获取到 openid");
        }

        User user = findWechatUser(platform, openid, unionid);
        Map<String, Object> data = new HashMap<>();
        if (user != null) {
            if (!isSupportedWechatUser(user, role)) {
                throw new IllegalArgumentException(buildRoleMismatchMessage(user, role));
            }
            requireEnabledUser(user);
            upsertWechatUserAccount(user.getId(), platform, openid, unionid);
            data.put("bindStatus", "BOUND");
            data.put("token", jwtUtil.generateToken(user.getId()));
            data.put("userInfo", buildUserInfo(user));
            return data;
        }

        Map<String, Object> bindClaims = new HashMap<>();
        bindClaims.put("tokenType", WECHAT_BIND_TOKEN_TYPE);
        bindClaims.put("provider", "WECHAT");
        bindClaims.put("platform", platform);
        bindClaims.put("openid", openid);
        bindClaims.put("unionid", emptyToNull(unionid));
        bindClaims.put("role", normalizeRole(role));
        data.put("bindStatus", "UNBOUND");
        data.put("wechatBindToken", jwtUtil.generateToken(0, bindClaims, bindTokenExpirationMs));
        return data;
    }

    private Map<String, Object> buildBoundResult(User user) {
        requireEnabledUser(user);
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

    private JsonNode exchangeOauthCode(String platform, String code) {
        try {
            String appId = appIdFor(platform);
            String secret = secretFor(platform);
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api.weixin.qq.com/sns/oauth2/access_token")
                    .queryParam("appid", appId)
                    .queryParam("secret", secret)
                    .queryParam("code", code)
                    .queryParam("grant_type", "authorization_code")
                    .toUriString();
            String body = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            int errCode = root.path("errcode").asInt(0);
            if (errCode != 0) {
                log.warn("微信 OAuth code 换取失败: code={}, msg={}", errCode, textValue(root, "errmsg"));
                throw new IllegalArgumentException("微信登录失败，请稍后重试");
            }
            return root;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用微信 OAuth 失败", e);
            throw new IllegalArgumentException("微信登录失败，请稍后重试");
        }
    }

    private boolean isConfigured(String platform) {
        return !isBlank(appIdFor(platform)) && !isBlank(secretFor(platform));
    }

    private void ensureConfigured(String platform) {
        if (!isConfigured(platform)) {
            throw new IllegalStateException("微信登录暂未开通");
        }
    }

    private String appIdFor(String platform) {
        String normalized = normalizePlatform(platform);
        if ("APP".equals(normalized)) return mobileAppId;
        if ("WECHAT_H5".equals(normalized)) return h5AppId;
        if ("WEB_SCAN".equals(normalized)) return webAppId;
        return miniAppId;
    }

    private String secretFor(String platform) {
        String normalized = normalizePlatform(platform);
        if ("APP".equals(normalized)) return mobileAppSecret;
        if ("WECHAT_H5".equals(normalized)) return h5AppSecret;
        if ("WEB_SCAN".equals(normalized)) return webAppSecret;
        return miniAppSecret;
    }

    private void ensureSupportedRole(String role) {
        if (!isSupportedRole(normalizeRole(role))) {
            throw new IllegalArgumentException("当前角色暂不支持微信登录");
        }
    }

    private String normalizeRole(String role) {
        return role == null || role.isBlank() ? USER_ROLE : role.trim().toLowerCase();
    }

    private String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank() || "null".equalsIgnoreCase(platform)) {
            return "MINI_PROGRAM";
        }
        String normalized = platform.trim().toUpperCase();
        if ("MP_WEIXIN".equals(normalized) || "MINI".equals(normalized)) return "MINI_PROGRAM";
        if ("H5".equals(normalized)) return "WECHAT_H5";
        if ("WEB".equals(normalized)) return "WEB_SCAN";
        return normalized;
    }

    private boolean isSupportedRole(String role) {
        return USER_ROLE.equals(role) || ESCORT_ROLE.equals(role);
    }

    private boolean isSupportedWechatUser(User user, String role) {
        if (user == null) return false;
        Integer expectedUserType = expectedUserType(role);
        return expectedUserType != null && expectedUserType.equals(user.getUserType());
    }

    private void requireEnabledUser(User user) {
        if (user != null && Integer.valueOf(0).equals(user.getStatus())) {
            throw new IllegalStateException("账号已被禁用，请联系平台客服");
        }
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
            attendant.setQualificationStatus(0);
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

    private User findWechatUser(String platform, String openid, String unionid) {
        ThirdPartyAccount account = thirdPartyAccountMapper.findUserAccountByWechat(platform, openid, emptyToNull(unionid));
        if (account != null && account.getPrincipalId() != null) {
            return userMapper.findById(account.getPrincipalId());
        }
        return userMapper.findByOpenid(openid);
    }

    private void upsertWechatUserAccount(Integer userId, String platform, String openid, String unionid) {
        if (userId == null || isBlank(openid)) return;
        ThirdPartyAccount account = new ThirdPartyAccount();
        account.setPrincipalType("USER");
        account.setPrincipalId(userId);
        account.setProvider("WECHAT");
        account.setPlatform(platform);
        account.setOpenid(openid);
        account.setUnionid(emptyToNull(unionid));
        thirdPartyAccountMapper.upsert(account);
    }

    private String encodeState(Map<String, String> values) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(values));
        } catch (Exception e) {
            throw new IllegalArgumentException("微信登录状态生成失败");
        }
    }

    private Map<String, String> decodeState(String state) {
        if (state == null || state.isBlank()) return Map.of();
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(state);
            return objectMapper.readValue(bytes, objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String appendOAuthResult(String redirectUrl, Map<String, Object> result) {
        if ("BOUND".equals(result.get("bindStatus"))) {
            return appendHash(redirectUrl, "wechatToken=" + encode(String.valueOf(result.get("token"))));
        }
        return appendHash(redirectUrl, "wechatBindToken=" + encode(String.valueOf(result.get("wechatBindToken"))));
    }

    private String appendHash(String redirectUrl, String hash) {
        String base = redirectUrl == null || redirectUrl.isBlank() ? "/" : redirectUrl;
        return base + (base.contains("#") ? "&" : "#") + hash;
    }

    private String safeRedirectUrl(String redirectUrl, String fallback) {
        if (redirectUrl == null || redirectUrl.isBlank()) return fallback;
        String decoded = URLDecoder.decode(redirectUrl, StandardCharsets.UTF_8);
        if (decoded.startsWith("http://") || decoded.startsWith("https://") || decoded.startsWith("/")) {
            return decoded;
        }
        return fallback;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() || "null".equalsIgnoreCase(value) ? null : value;
    }

    private String textValue(JsonNode root, String fieldName) {
        JsonNode value = root.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}
