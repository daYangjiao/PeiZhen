package org.example.interceptor;

import lombok.RequiredArgsConstructor;
import org.example.dao.SysAdminMapper;
import org.example.entity.SysAdmin;
import org.example.unity.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final SysAdminMapper sysAdminMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, "管理员未登录");
            return false;
        }

        String token = authHeader.substring(7);
        if (!"admin".equals(jwtUtil.getPrincipalTypeFromToken(token))) {
            writeUnauthorized(response, "无管理员权限");
            return false;
        }

        Integer adminId = jwtUtil.getAdminIdFromToken(token);
        if (adminId == null) {
            writeUnauthorized(response, "登录已失效");
            return false;
        }

        SysAdmin admin = sysAdminMapper.findById(adminId);
        if (admin == null) {
            writeUnauthorized(response, "管理员不存在");
            return false;
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            writeUnauthorized(response, "管理员账号已禁用");
            return false;
        }

        request.setAttribute("currentAdminId", adminId);
        request.setAttribute("currentAdmin", admin);
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(String.format("{\"code\":401,\"message\":\"%s\",\"data\":null}", message));
            writer.flush();
        }
    }
}
