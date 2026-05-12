package org.example.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.model.ChatMessage;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    int insert(ChatMessage message);

    List<ChatMessage> findHistory(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);

    List<ChatMessage> findSystemMessages(@Param("userId") Integer userId);

    ChatMessage findByIdAndReceiver(@Param("id") Long id, @Param("receiverId") Integer receiverId);

    List<ChatMessage> findRecentContacts(@Param("userId") Integer userId);

    ChatMessage findConversationMessage(@Param("id") Long id,
                                        @Param("userId1") Integer userId1,
                                        @Param("userId2") Integer userId2);

    int markAsRead(@Param("senderId") Integer senderId, @Param("receiverId") Integer receiverId);

    int markMessageAsRead(@Param("id") Long id, @Param("receiverId") Integer receiverId);

    Long findLatestReadMessageId(@Param("senderId") Integer senderId, @Param("receiverId") Integer receiverId);

    int countUnread(@Param("userId") Integer userId);
}
