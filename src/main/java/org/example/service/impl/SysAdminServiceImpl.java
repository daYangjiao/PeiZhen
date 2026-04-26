package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dao.SysAdminMapper;
import org.example.dao.ThirdPartyAccountMapper;
import org.example.entity.SysAdmin;
import org.example.model.ThirdPartyAccount;
import org.example.model.request.AdminCreateSysAdminRequest;
import org.example.model.response.AdminLoginResponse;
import org.example.model.response.PagedResponse;
import org.example.service.SysAdminService;
import org.example.unity.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SysAdminServiceImpl implements SysAdminService {

    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]?\\$.{56}$");
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String MANAGE_ADMIN_FORBIDDEN_MESSAGE = "仅超级管理员可管理管理员账号";
    private static final String ADMIN_WECHAT_BIND_TOKEN_TYPE = "admin_wechat_bind";

    private final SysAdminMapper sysAdminMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private ThirdPartyAccountMapper thirdPartyAccountMapper;

    @Autowired(required = false)
    private AdminOperationLogService operationLogService;

    @Autowired
    public SysAdminServiceImpl(SysAdminMapper sysAdminMapper,
                               ThirdPartyAccountMapper thirdPartyAccountMapper,
                               PasswordEncoder passwordEncoder,
                               JwtUtil jwtUtil) {
        this.sysAdminMapper = sysAdminMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.thirdPartyAccountMapper = thirdPartyAccountMapper;
    }

    @Override
    @Transactional
    public AdminLoginResponse login(String account, String password) {
        String phone = trim(account);
        if (phone == null || phone.isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        SysAdmin admin = sysAdminMapper.findByPhone(phone);
        if (admin == null) {
            throw new IllegalArgumentException("手机号或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new IllegalArgumentException("管理员账号已禁用");
        }

        String storedPassword = admin.getPassword();
        boolean matched = false;
        if (storedPassword != null && isEncodedPassword(storedPassword)) {
            matched = passwordEncoder.matches(password, storedPassword);
        } else if (storedPassword != null) {
            matched = storedPassword.equals(password);
        }
        if (!matched) {
            throw new IllegalArgumentException("手机号或密码错误");
        }

        if (!isEncodedPassword(storedPassword)) {
            SysAdmin patch = new SysAdmin();
            patch.setId(admin.getId());
            patch.setPassword(passwordEncoder.encode(password));
            sysAdminMapper.update(patch);
            admin.setPassword(patch.getPassword());
        }

        SysAdmin loginPatch = new SysAdmin();
        loginPatch.setId(admin.getId());
        loginPatch.setLastLoginTime(new java.util.Date());
        sysAdminMapper.update(loginPatch);
        admin.setLastLoginTime(loginPatch.getLastLoginTime());

        Map<String, Object> claims = new HashMap<>();
        claims.put("principalType", "admin");
        claims.put("adminId", admin.getId());
        claims.put("role", normalizeRole(admin.getRole()));
        admin.setRole(normalizeRole(admin.getRole()));
        return new AdminLoginResponse(jwtUtil.generateAdminToken(admin.getId(), claims), admin);
    }

    @Override
    @Transactional
    public AdminLoginResponse loginByWechatIdentity(String platform, String openid, String unionid) {
        ensureThirdPartyMapper();
        ThirdPartyAccount account = thirdPartyAccountMapper.findAdminAccountByWechat(
                normalizeWechatPlatform(platform),
                trim(openid),
                trim(unionid)
        );
        if (account == null || account.getPrincipalId() == null) {
            throw new IllegalArgumentException("该微信尚未绑定管理员账号");
        }
        SysAdmin admin = requireAdmin(account.getPrincipalId());
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new IllegalArgumentException("管理员账号已禁用");
        }
        SysAdmin loginPatch = new SysAdmin();
        loginPatch.setId(admin.getId());
        loginPatch.setLastLoginTime(new java.util.Date());
        sysAdminMapper.update(loginPatch);
        admin.setLastLoginTime(loginPatch.getLastLoginTime());
        admin.setRole(normalizeRole(admin.getRole()));

        Map<String, Object> claims = new HashMap<>();
        claims.put("principalType", "admin");
        claims.put("adminId", admin.getId());
        claims.put("role", admin.getRole());
        return new AdminLoginResponse(jwtUtil.generateAdminToken(admin.getId(), claims), admin);
    }

    @Override
    @Transactional
    public void bindWechatIdentity(Integer adminId, String platform, String openid, String unionid) {
        ensureThirdPartyMapper();
        SysAdmin admin = requireAdmin(adminId);
        ThirdPartyAccount account = new ThirdPartyAccount();
        account.setPrincipalType("ADMIN");
        account.setPrincipalId(admin.getId());
        account.setProvider("WECHAT");
        account.setPlatform(normalizeWechatPlatform(platform));
        account.setOpenid(trim(openid));
        account.setUnionid(emptyToNull(trim(unionid)));
        if (account.getOpenid() == null || account.getOpenid().isEmpty()) {
            throw new IllegalArgumentException("微信身份无效");
        }
        thirdPartyAccountMapper.upsert(account);
        recordOperation(adminId, "BIND_ADMIN_WECHAT", admin, null, admin.getStatus(), "绑定管理员微信登录");
    }

    @Override
    @Transactional
    public void bindWechatIdentityToken(Integer adminId, String wechatBindToken) {
        if (wechatBindToken == null || wechatBindToken.isBlank()) {
            throw new IllegalArgumentException("微信绑定凭证不能为空");
        }
        io.jsonwebtoken.Claims claims = jwtUtil.getAllClaimsFromToken(wechatBindToken);
        if (!ADMIN_WECHAT_BIND_TOKEN_TYPE.equals(String.valueOf(claims.get("tokenType")))) {
            throw new IllegalArgumentException("微信绑定凭证无效");
        }
        bindWechatIdentity(
                adminId,
                String.valueOf(claims.get("platform")),
                String.valueOf(claims.get("openid")),
                String.valueOf(claims.get("unionid"))
        );
    }

    @Override
    public PagedResponse<SysAdmin> getAdmins(Integer operatorId, String keyword, Integer status, Integer page, Integer pageSize) {
        requireSuperAdmin(operatorId);
        int safePage = normalizePage(page);
        int safeSize = normalizePageSize(pageSize);
        int total = sysAdminMapper.countAdmins(trim(keyword), status);
        return new PagedResponse<>(
                sysAdminMapper.findAdmins(trim(keyword), status, safePage * safeSize, safeSize),
                total,
                safePage,
                safeSize
        );
    }

    @Override
    @Transactional
    public SysAdmin createAdmin(Integer operatorId, AdminCreateSysAdminRequest request) {
        requireSuperAdmin(operatorId);
        if (sysAdminMapper.findByPhone(request.getPhone().trim()) != null) {
            throw new IllegalArgumentException("管理员手机号已存在");
        }
        SysAdmin admin = new SysAdmin();
        admin.setName(request.getName().trim());
        admin.setPhone(request.getPhone().trim());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setStatus(1);
        admin.setRole(normalizeCreateRole(request.getRole()));
        sysAdminMapper.insert(admin);
        SysAdmin persisted = sysAdminMapper.findById(admin.getId());
        recordOperation(operatorId, "CREATE_ADMIN", persisted, null, persisted == null ? null : persisted.getStatus(), "创建管理员账号");
        return persisted;
    }

    @Override
    @Transactional
    public void updateStatus(Integer operatorId, Integer adminId, Integer status) {
        requireSuperAdmin(operatorId);
        SysAdmin current = requireAdmin(adminId);
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("状态值不合法");
        }
        if (operatorId != null && operatorId.equals(adminId) && status == 0) {
            throw new IllegalArgumentException("不能禁用当前登录管理员");
        }
        if (status == 0 && Integer.valueOf(1).equals(current.getStatus())
                && ROLE_SUPER_ADMIN.equals(normalizeRole(current.getRole()))
                && sysAdminMapper.countEnabledSuperAdmins() <= 1) {
            throw new IllegalArgumentException("至少保留一个启用状态的超级管理员");
        }
        if (current.getStatus() != null && current.getStatus().equals(status)) {
            return;
        }
        SysAdmin patch = new SysAdmin();
        patch.setId(adminId);
        patch.setStatus(status);
        sysAdminMapper.update(patch);
        recordOperation(operatorId, status == 1 ? "ENABLE_ADMIN" : "DISABLE_ADMIN", current, current.getStatus(), status, null);
    }

    @Override
    @Transactional
    public void deleteAdmin(Integer operatorId, Integer adminId) {
        requireSuperAdmin(operatorId);
        if (operatorId != null && operatorId.equals(adminId)) {
            throw new IllegalArgumentException("不能删除当前登录管理员");
        }
        SysAdmin current = requireAdmin(adminId);
        if (Integer.valueOf(1).equals(current.getStatus())
                && ROLE_SUPER_ADMIN.equals(normalizeRole(current.getRole()))
                && sysAdminMapper.countEnabledSuperAdmins() <= 1) {
            throw new IllegalArgumentException("至少保留一个启用状态的超级管理员");
        }
        sysAdminMapper.deleteById(adminId);
        recordOperation(operatorId, "DELETE_ADMIN", current, current.getStatus(), null, "删除管理员账号");
    }

    @Override
    public SysAdmin findById(Integer adminId) {
        return sysAdminMapper.findById(adminId);
    }

    private SysAdmin requireAdmin(Integer adminId) {
        if (adminId == null) {
            throw new SecurityException(MANAGE_ADMIN_FORBIDDEN_MESSAGE);
        }
        SysAdmin admin = sysAdminMapper.findById(adminId);
        if (admin == null) {
            throw new IllegalArgumentException("管理员不存在");
        }
        return admin;
    }

    private void requireSuperAdmin(Integer operatorId) {
        SysAdmin operator = requireAdmin(operatorId);
        if (!ROLE_SUPER_ADMIN.equals(normalizeRole(operator.getRole()))) {
            throw new SecurityException(MANAGE_ADMIN_FORBIDDEN_MESSAGE);
        }
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 10;
        }
        return Math.min(pageSize, 50);
    }

    private boolean isEncodedPassword(String password) {
        return password != null && BCRYPT_PATTERN.matcher(password).matches();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeRole(String role) {
        String normalized = trim(role);
        if (ROLE_SUPER_ADMIN.equals(normalized)) {
            return ROLE_SUPER_ADMIN;
        }
        return ROLE_ADMIN;
    }

    private String normalizeWechatPlatform(String platform) {
        String normalized = trim(platform);
        if (normalized == null || normalized.isEmpty()) {
            return "WEB_SCAN";
        }
        return normalized.toUpperCase();
    }

    private String emptyToNull(String value) {
        return value == null || value.isEmpty() || "null".equalsIgnoreCase(value) ? null : value;
    }

    private void ensureThirdPartyMapper() {
        if (thirdPartyAccountMapper == null) {
            throw new IllegalStateException("第三方账号组件未初始化");
        }
    }

    private String normalizeCreateRole(String role) {
        String normalized = trim(role);
        if (normalized == null || normalized.isEmpty()) {
            return ROLE_ADMIN;
        }
        if (ROLE_SUPER_ADMIN.equals(normalized) || ROLE_ADMIN.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("账号类型不合法");
    }

    private void recordOperation(Integer operatorId, String action, SysAdmin target, Integer fromStatus, Integer toStatus, String remark) {
        if (operationLogService == null || target == null) {
            return;
        }
        operationLogService.record(
                operatorId,
                "ADMIN_ACCOUNT",
                action,
                "SYS_ADMIN",
                target.getId(),
                target.getName(),
                fromStatus,
                toStatus,
                remark,
                "{\"phone\":\"" + escapeJson(target.getPhone()) + "\",\"role\":\"" + escapeJson(target.getRole()) + "\"}"
        );
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
