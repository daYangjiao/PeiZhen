package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.example.common.ResponseResult;
import org.example.model.ChatMessage;
import org.example.service.ChatService;
import org.example.util.AuthUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Api(tags = "聊天消息接口")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    @ApiOperation("发送消息")
    public ResponseResult<ChatMessage> sendMessage(@RequestBody ChatMessage message, HttpServletRequest request) {
        Integer currentUserId = AuthUtil.getCurrentUserId(request);
        message.setSenderId(currentUserId);
        return ResponseResult.success(chatService.sendMessage(message));
    }

    @GetMapping("/history")
    @ApiOperation("获取聊天记录")
    public ResponseResult<List<ChatMessage>> getHistory(@RequestParam Integer targetUserId, HttpServletRequest request) {
        Integer currentUserId = AuthUtil.getCurrentUserId(request);
        return ResponseResult.success(chatService.getHistory(currentUserId, targetUserId));
    }

    @GetMapping("/contacts")
    @ApiOperation("获取最近联系人")
    public ResponseResult<List<ChatMessage>> getRecentContacts(HttpServletRequest request) {
        Integer currentUserId = AuthUtil.getCurrentUserId(request);
        return ResponseResult.success(chatService.getRecentContacts(currentUserId));
    }
    
    @PostMapping("/read")
    @ApiOperation("标记消息已读")
    public ResponseResult<Void> markAsRead(@RequestParam Integer senderId, HttpServletRequest request) {
        Integer currentUserId = AuthUtil.getCurrentUserId(request);
        chatService.markAsRead(senderId, currentUserId);
        return ResponseResult.success(null);
    }
}