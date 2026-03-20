package org.example.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(OrderWebSocketHandler.class);

    // 存储用户ID和WebSocketSession的映射
    // Key: userId, Value: WebSocketSession
    private static final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从 URL 参数中获取 userId (例如 ws://localhost:8080/ws/orders?userId=1001)
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.put(userId, session);
            logger.info("用户 {} 建立 WebSocket 连接", userId);
        } else {
            logger.warn("建立连接失败：未找到 userId");
            session.close();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.remove(userId);
            logger.info("用户 {} 断开 WebSocket 连接", userId);
        }
    }

    /**
     * 发送消息给指定用户
     * @param userId 用户ID
     * @param message 消息内容
     */
    public void sendMessageToUser(String userId, String message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                logger.info("向用户 {} 发送消息成功: {}", userId, message);
            } catch (IOException e) {
                logger.error("向用户 {} 发送消息失败", userId, e);
            }
        } else {
            logger.warn("用户 {} 不在线，消息未发送", userId);
        }
    }

    private String getUserIdFromSession(WebSocketSession session) {
        Object currentUserId = session.getAttributes().get("currentUserId");
        return currentUserId == null ? null : String.valueOf(currentUserId);
    }
}
