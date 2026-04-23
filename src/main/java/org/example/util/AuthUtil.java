package org.example.util;

import org.example.unity.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class AuthUtil {
    private static final Logger logger = LoggerFactory.getLogger(AuthUtil.class);
    
    private static JwtUtil jwtUtil;
    
    @Autowired
    public void setJwtUtil(JwtUtil jwtUtil) {
        AuthUtil.jwtUtil = jwtUtil;
    }
    
    /**
     * 从请求中获取当前用户ID
     * @param request HTTP请求
     * @return 用户ID，如果未登录则返回null
     */
    public static Integer getCurrentUserId(HttpServletRequest request) {
        try {
            Object userIdObj = request.getAttribute("currentUserId");
            if (userIdObj != null) {
                return (Integer) userIdObj;
            }

            return resolveUserIdFromToken(request);
        } catch (Exception e) {
            logger.warn("获取用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    public static Integer getCurrentAdminId(HttpServletRequest request) {
        try {
            Object adminIdObj = request.getAttribute("currentAdminId");
            if (adminIdObj != null) {
                return (Integer) adminIdObj;
            }

            String token = extractBearerToken(request);
            if (token == null) {
                return null;
            }
            String principalType = jwtUtil.getPrincipalTypeFromToken(token);
            if (!"admin".equals(principalType)) {
                return null;
            }
            return jwtUtil.getAdminIdFromToken(token);
        } catch (Exception e) {
            logger.warn("获取管理员ID失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查用户是否有权限访问指定订单
     * @param request HTTP请求
     * @param orderUserId 订单所属用户ID
     * @return 是否有权限
     */
    public static boolean hasPermissionToAccessOrder(HttpServletRequest request, Integer orderUserId) {
        Integer currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return false;
        }
        return currentUserId.equals(orderUserId);
    }
    
    /**
     * 检查用户是否有权限访问指定用户的数据
     * @param request HTTP请求
     * @param targetUserId 目标用户ID
     * @return 是否有权限
     */
    public static boolean hasPermissionToAccessUser(HttpServletRequest request, Integer targetUserId) {
        Integer currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return false;
        }
        return currentUserId.equals(targetUserId);
    }

    private static Integer resolveUserIdFromToken(HttpServletRequest request) {
        String token = extractBearerToken(request);
        if (token == null) {
            return null;
        }
        String principalType = jwtUtil.getPrincipalTypeFromToken(token);
        if ("admin".equals(principalType)) {
            return null;
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    private static String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}
