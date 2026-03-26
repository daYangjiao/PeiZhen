package org.example.service;

import org.example.model.ChatMessage;
import java.util.List;

public interface ChatService {
    
    // 发送消息
    ChatMessage sendMessage(ChatMessage message);
    
    // 获取聊天记录
    List<ChatMessage> getHistory(Integer userId1, Integer userId2);

    // 获取系统通知列表
    List<ChatMessage> getSystemMessages(Integer userId);
    
    // 获取最近联系人列表
    List<ChatMessage> getRecentContacts(Integer userId);
    
    // 标记消息为已读
    void markAsRead(Integer senderId, Integer receiverId);
    
    // 获取未读消息数
    int getUnreadCount(Integer userId);
}
