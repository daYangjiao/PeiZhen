package org.example.service;

import org.example.model.ChatMessage;

import java.util.List;

public interface ChatService {

    ChatMessage sendMessage(ChatMessage message);

    List<ChatMessage> getHistory(Integer userId1, Integer userId2);

    List<ChatMessage> getSystemMessages(Integer userId);

    ChatMessage getSystemMessageDetail(Long messageId, Integer receiverId);

    List<ChatMessage> getRecentContacts(Integer userId);

    void markAsRead(Integer senderId, Integer receiverId);

    int markMessageAsRead(Long messageId, Integer receiverId);

    int getUnreadCount(Integer userId);
}
