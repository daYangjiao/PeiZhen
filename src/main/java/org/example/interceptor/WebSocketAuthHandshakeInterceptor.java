package org.example.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unity.JwtUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            String token = extractToken(request);
            if (token == null || token.isBlank()) {
                reject(response, "WebSocket连接缺少认证令牌");
                return false;
            }

            Integer userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                reject(response, "WebSocket认证失败");
                return false;
            }
            attributes.put("currentUserId", userId);
            attributes.put("currentUserToken", token);
            return true;
        } catch (Exception e) {
            log.warn("WebSocket握手认证失败: {}", e.getMessage());
            reject(response, "WebSocket认证失败");
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String extractToken(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        List<String> authorizationHeaders = headers.get(HttpHeaders.AUTHORIZATION);
        if (authorizationHeaders != null) {
            for (String header : authorizationHeaders) {
                String bearerToken = extractBearerToken(header);
                if (bearerToken != null && !bearerToken.isBlank()) {
                    return bearerToken;
                }
            }
        }

        return UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7).trim();
    }

    private void reject(ServerHttpResponse response, String message) {
        if (response instanceof ServletServerHttpResponse) {
            ServletServerHttpResponse servletResponse = (ServletServerHttpResponse) response;
            servletResponse.setStatusCode(HttpStatus.UNAUTHORIZED);
            servletResponse.getServletResponse().setHeader("X-WebSocket-Auth-Error", message);
        }
    }
}
