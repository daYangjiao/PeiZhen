package org.example.service.impl;

import org.example.dao.ChatMessageMapper;
import org.example.handler.ChatWebSocketHandler;
import org.example.model.ChatMessage;
import org.example.model.User;
import org.example.service.ChatService;
import org.example.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);
    
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    
    @Autowired
    private ChatWebSocketHandler webSocketHandler;
    
    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public ChatMessage sendMessage(ChatMessage message) {
        message.setCreateTime(new Date());
        message.setIsRead(false);
        
        // 获取发送者信息并设置头像
        try {
            User sender = userService.findById(message.getSenderId());
            if (sender != null && sender.getAvatar() != null) {
                message.setSenderAvatar(sender.getAvatar());
                message.setSenderName(sender.getName());
                logger.debug("设置发送者头像: {} = {}", message.getSenderId(), sender.getAvatar());
            }
        } catch (Exception e) {
            logger.warn("获取发送者信息失败: {}", e.getMessage());
        }

        hydrateReplyReference(message);
        
        chatMessageMapper.insert(message);
        
        // 通过 WebSocket 推送给接收者
        webSocketHandler.sendMessageToUser(message.getReceiverId(), message);
        
        return message;
    }

    private void hydrateReplyReference(ChatMessage message) {
        if (message == null || message.getReplyToMessageId() == null || message.getReplyToMessageId() <= 0) {
            clearReplyReference(message);
            return;
        }

        ChatMessage quoted = chatMessageMapper.findConversationMessage(
                message.getReplyToMessageId(),
                message.getSenderId(),
                message.getReceiverId()
        );
        if (quoted == null) {
            clearReplyReference(message);
            return;
        }

        message.setReplyToMessageId(quoted.getId());
        message.setReplyToContent(buildReplyPreview(quoted));
        message.setReplyToSenderName(resolveReplySenderName(quoted));
    }

    private void clearReplyReference(ChatMessage message) {
        if (message == null) {
            return;
        }
        message.setReplyToMessageId(null);
        message.setReplyToContent(null);
        message.setReplyToSenderName(null);
    }

    private String resolveReplySenderName(ChatMessage message) {
        if (message.getSenderName() != null && !message.getSenderName().trim().isEmpty()) {
            return message.getSenderName().trim();
        }
        return "用户" + message.getSenderId();
    }

    private String buildReplyPreview(ChatMessage message) {
        Integer type = message.getMsgType();
        if (type != null && type == 2) {
            return "[图片]";
        }
        if (type != null && type == 3) {
            return "[语音]";
        }
        if (type != null && type == 4) {
            return "[位置]";
        }
        String content = message.getContent() == null ? "" : message.getContent().trim();
        if (content.length() > 80) {
            return content.substring(0, 80) + "...";
        }
        return content;
    }

    @Override
    public List<ChatMessage> getHistory(Integer userId1, Integer userId2) {
        List<ChatMessage> messages = chatMessageMapper.findHistory(userId1, userId2);
        
        // 为每条消息补充发送者头像信息
        enrichMessagesWithSenderInfo(messages);
        
        return messages;
    }

    @Override
    public List<ChatMessage> getSystemMessages(Integer userId) {
        return chatMessageMapper.findSystemMessages(userId);
    }

    @Override
    public ChatMessage getSystemMessageDetail(Long messageId, Integer receiverId) {
        return chatMessageMapper.findByIdAndReceiver(messageId, receiverId);
    }

    @Override
    public List<ChatMessage> getRecentContacts(Integer userId) {
        List<ChatMessage> contacts = chatMessageMapper.findRecentContacts(userId);
        
        // 为每条消息补充发送者头像信息
        enrichMessagesWithSenderInfo(contacts);
        
        return contacts;
    }

    /**
     * 为消息列表补充发送者信息（头像和姓名）
     */
    private void enrichMessagesWithSenderInfo(List<ChatMessage> messages) {
        for (ChatMessage message : messages) {
            try {
                // 如果消息中还没有发送者头像，则获取并设置
                if (message.getSenderAvatar() == null || message.getSenderAvatar().isEmpty()) {
                    User sender = userService.findById(message.getSenderId());
                    if (sender != null) {
                        message.setSenderAvatar(sender.getAvatar());
                        message.setSenderName(sender.getName());
                    }
                }
            } catch (Exception e) {
                logger.warn("补充发送者信息失败，消息ID: {}, 错误: {}", message.getId(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void markAsRead(Integer senderId, Integer receiverId) {
        // 更新数据库中的已读状态
        chatMessageMapper.markAsRead(senderId, receiverId);
        if (Integer.valueOf(0).equals(senderId)) {
            return;
        }
        Long lastReadMessageId = chatMessageMapper.findLatestReadMessageId(senderId, receiverId);
        
        // 通过WebSocket通知发送方消息已被阅读
        try {
            // 构造已读回执消息
            ChatMessage readReceipt = new ChatMessage();
            readReceipt.setSenderId(receiverId); // 接收者ID作为发送者
            readReceipt.setReceiverId(senderId); // 原发送者作为接收者
            readReceipt.setMsgType(99); // 专用系统类型，避免与语音消息冲突
            readReceipt.setType("READ_RECEIPT");
            readReceipt.setContent("READ_RECEIPT");
            readReceipt.setLastReadMessageId(lastReadMessageId);
            readReceipt.setReadUpToTime(new Date());
            readReceipt.setCreateTime(new Date());
            
            // 通过WebSocket推送给发送方
            webSocketHandler.sendMessageToUser(senderId, readReceipt);
            
            logger.info("已发送已读回执: 接收者{} -> 发送者{}, lastReadMessageId={}", receiverId, senderId, lastReadMessageId);
        } catch (Exception e) {
            logger.error("发送已读回执失败", e);
        }
    }

    @Override
    @Transactional
    public int markMessageAsRead(Long messageId, Integer receiverId) {
        return chatMessageMapper.markMessageAsRead(messageId, receiverId);
    }

    @Override
    public int getUnreadCount(Integer userId) {
        return chatMessageMapper.countUnread(userId);
    }
}
