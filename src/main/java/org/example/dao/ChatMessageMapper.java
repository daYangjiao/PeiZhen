package org.example.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.model.ChatMessage;

import java.util.List;

@Mapper
public interface ChatMessageMapper {
    
    // 插入消息
    int insert(ChatMessage message);
    
    // 查询两个用户之间的聊天记录
    List<ChatMessage> findHistory(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);

    // 查询当前用户收到的系统通知
    List<ChatMessage> findSystemMessages(@Param("userId") Integer userId);
    
    // 查询用户的最近联系人列表（包含最后一条消息）
    List<ChatMessage> findRecentContacts(@Param("userId") Integer userId);
    
    // 将消息标记为已读
    int markAsRead(@Param("senderId") Integer senderId, @Param("receiverId") Integer receiverId);

    // 查询某会话中最后一条已读消息ID（用于已读回执）
    Long findLatestReadMessageId(@Param("senderId") Integer senderId, @Param("receiverId") Integer receiverId);
    
    // 查询未读消息数
    int countUnread(@Param("userId") Integer userId);
}
