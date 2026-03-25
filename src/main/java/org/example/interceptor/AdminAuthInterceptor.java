package org.example.interceptor;

import lombok.RequiredArgsConstructor;
import org.example.dao.UserMapper;
import org.example.model.User;
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
    private final UserMapper userMapper;

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

        Integer userId = jwtUtil.getUserIdFromToken(authHeader.substring(7));
        if (userId == null) {
            writeUnauthorized(response, "登录已失效");
            return false;
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            writeUnauthorized(response, "管理员不存在");
            return false;
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            writeUnauthorized(response, "管理员账号已禁用");
            return false;
        }
        if (user.getUserType() == null || user.getUserType() != 2) {
            writeUnauthorized(response, "无管理员权限");
            return false;
        }

        request.setAttribute("currentUserId", userId);
        request.setAttribute("currentAdmin", user);
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
