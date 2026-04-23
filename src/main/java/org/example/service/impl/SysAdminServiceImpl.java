package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dao.SysAdminMapper;
import org.example.entity.SysAdmin;
import org.example.model.request.AdminCreateSysAdminRequest;
import org.example.model.response.AdminLoginResponse;
import org.example.model.response.PagedResponse;
import org.example.service.SysAdminService;
import org.example.unity.JwtUtil;
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

    private final SysAdminMapper sysAdminMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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
        return sysAdminMapper.findById(admin.getId());
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
}
