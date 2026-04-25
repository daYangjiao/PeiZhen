package org.example.service.impl;

import org.example.dao.ChatMessageMapper;
import org.example.handler.ChatWebSocketHandler;
import org.example.model.ChatMessage;
import org.example.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private ChatWebSocketHandler webSocketHandler;

    @Mock
    private UserService userService;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Test
    void markAsReadDoesNotSendReceiptForSystemMessages() {
        chatService.markAsRead(0, 12);

        verify(chatMessageMapper).markAsRead(0, 12);
        verify(chatMessageMapper, never()).findLatestReadMessageId(0, 12);
        verify(webSocketHandler, never()).sendMessageToUser(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void markAsReadSendsReceiptForNormalConversation() {
        when(chatMessageMapper.findLatestReadMessageId(21, 12)).thenReturn(88L);

        chatService.markAsRead(21, 12);

        verify(chatMessageMapper).markAsRead(21, 12);
        verify(webSocketHandler).sendMessageToUser(
                org.mockito.ArgumentMatchers.eq(21),
                argThat((ChatMessage message) ->
                        Integer.valueOf(12).equals(message.getSenderId())
                                && Integer.valueOf(21).equals(message.getReceiverId())
                                && Integer.valueOf(99).equals(message.getMsgType())
                                && "READ_RECEIPT".equals(message.getType())
                                && Long.valueOf(88L).equals(message.getLastReadMessageId()))
        );
    }
}
