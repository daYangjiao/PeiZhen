package org.example.interceptor;

import org.example.dao.UserMapper;
import org.example.model.User;
import org.example.unity.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * 用户认证拦截器
 * 验证请求头中的Authorization Token
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);
    
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS请求直接放行（跨域预检）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        
        // 获取请求路径
        String requestURI = request.getRequestURI();
        logger.debug("拦截请求: {}", requestURI);
        
        // 白名单路径（不需要认证的接口）
        if (isWhitelistPath(requestURI)) {
            return true;
        }
        
        // 获取Authorization头
        String authHeader = request.getHeader("Authorization");
        
        // 检查是否有Token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("请求缺少有效的Authorization头: {}", requestURI);
            sendUnauthorizedResponse(response, "未提供有效的认证令牌");
            return false;
        }
        
        // 提取Token
        String token = authHeader.substring(7); // 去掉 "Bearer " 前缀
        
        try {
            // 验证Token并获取用户ID
            Integer userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                logger.warn("Token解析失败，无效的用户ID");
                sendUnauthorizedResponse(response, "认证令牌无效");
                return false;
            }

            User currentUser = userMapper.findById(userId);
            if (currentUser == null) {
                logger.warn("Token验证失败，用户不存在: {}", userId);
                sendUnauthorizedResponse(response, "用户不存在");
                return false;
            }
            if (currentUser.getStatus() != null && currentUser.getStatus() == 0) {
                logger.warn("已被禁用的用户访问被拒绝: {}", userId);
                sendUnauthorizedResponse(response, "账号已被禁用");
                return false;
            }
            
            // 将用户ID存入请求属性，供后续处理器使用
            request.setAttribute("currentUserId", userId);
            request.setAttribute("currentUserToken", token);
            request.setAttribute("currentUser", currentUser);
            
            logger.debug("用户 {} 认证成功，访问路径: {}", userId, requestURI);
            return true;
            
        } catch (Exception e) {
            logger.warn("Token验证失败: {}", e.getMessage());
            sendUnauthorizedResponse(response, "认证令牌已过期或无效");
            return false;
        }
    }
    
    /**
     * 判断是否为白名单路径（不需要认证）
     */
    private boolean isWhitelistPath(String requestURI) {
        // 用户登录、注册等公开接口
        String[] whitelistPaths = {
            "/api/users/login",
            "/api/users/register",
            "/api/users/checkUsername",
            "/api/users/wechat/config-status",
            "/api/users/wechat/oauth-url",
            "/api/users/wechat/oauth-callback",
            "/api/users/wechat/login",
            "/api/users/wechat/bind-phone",
            "/api/common/upload",
            "/api/common/upload-image",
            "/attendant/recommended",  // 获取推荐陪诊师（允许未登录用户访问）
            "/swagger-ui",
            "/v2/api-docs",
            "/webjars",
            "/swagger-resources",
            "/favicon.ico"
        };
        
        // AI导诊相关白名单路径
        String[] aiGuideWhitelistPaths = {
            "/ai/guide/appointments",
            "/ai/guide/attendants/match",
            "/ai/guide/orders/",  // 注意：这里用 / 作为前缀匹配
            "/ai/guide/payments/status"
        };
        
        // 检查常规白名单
        for (String path : whitelistPaths) {
            if (requestURI.startsWith(path)) {
                return true;
            }
        }
        
        // 检查AI导诊白名单
        for (String path : aiGuideWhitelistPaths) {
            if (requestURI.startsWith(path)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 发送未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        String jsonResponse = String.format(
            "{\"code\":401,\"message\":\"%s\",\"data\":null}", 
            message
        );
        
        PrintWriter writer = response.getWriter();
        writer.write(jsonResponse);
        writer.flush();
        writer.close();
    }
}
