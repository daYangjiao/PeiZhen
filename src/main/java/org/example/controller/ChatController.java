package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.example.common.ResponseResult;
import org.example.model.ChatMessage;
import org.example.service.ChatService;
import org.example.util.AuthUtil;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Api(tags = "聊天消息接口")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    @ApiOperation(value = "发送消息", notes = "当前登录用户向目标用户发送一条聊天消息。发送者 ID 从登录态自动识别，无需前端传入。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "发送成功，返回持久化后的消息"),
            @ApiResponse(code = 401, message = "用户未登录或 token 无效"),
            @ApiResponse(code = 500, message = "消息发送失败")
    })
    public ResponseResult<ChatMessage> sendMessage(
            @ApiParam(value = "消息体，需包含 receiverId、content、messageType 等字段", required = true)
            @RequestBody ChatMessage message,
            @ApiIgnore HttpServletRequest request) {
        Integer currentUserId = AuthUtil.getCurrentUserId(request);
        message.setSenderId(currentUserId);
        return ResponseResult.success(chatService.sendMessage(message));
    }

    @GetMapping("/history")
    @ApiOperation(value = "获取聊天记录", notes = "返回当前登录用户与指定用户之间的历史消息列表，按时间顺序组织。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功，返回聊天记录列表"),
            @ApiResponse(code = 401, message = "用户未登录或 token 无效"),
            @ApiResponse(code = 500, message = "查询聊天记录失败")
    })
    public ResponseResult<List<ChatMessage>> getHistory(
            @ApiParam(value = "对方用户ID", required = true, example = "21")
            @RequestParam Integer targetUserId,
            @ApiIgnore HttpServletRequest request) {
        Integer currentUserId = AuthUtil.getCurrentUserId(request);
        return ResponseResult.success(chatService.getHistory(currentUserId, targetUserId));
    }

    @GetMapping("/system")
    @ApiOperation(value = "获取系统通知", notes = "返回当前登录用户收到的系统通知列表，按最新时间倒序排列。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功，返回系统通知列表"),
            @ApiResponse(code = 401, message = "用户未登录或 token 无效"),
            @ApiResponse(code = 500, message = "查询系统通知失败")
    })
    public ResponseResult<List<ChatMessage>> getSystemMessages(@ApiIgnore HttpServletRequest request) {
        Integer currentUserId = AuthUtil.getCurrentUserId(request);
        return ResponseResult.success(chatService.getSystemMessages(currentUserId));
    }

    @GetMapping("/contacts")
    @ApiOperation(value = "获取最近联系人", notes = "返回当前用户最近联系过的用户列表及未读消息数。未登录时可能返回空列表。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功，返回联系人列表"),
            @ApiResponse(code = 500, message = "查询联系人失败")
    })
    public ResponseResult<List<ChatMessage>> getRecentContacts(@ApiIgnore HttpServletRequest request) {
        Integer currentUserId = AuthUtil.getCurrentUserId(request);
        return ResponseResult.success(chatService.getRecentContacts(currentUserId));
    }

    @PostMapping("/read")
    @ApiOperation(value = "标记消息已读", notes = "将指定发送者发给当前登录用户的未读消息统一标记为已读。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "标记成功"),
            @ApiResponse(code = 401, message = "用户未登录或 token 无效"),
            @ApiResponse(code = 500, message = "标记已读失败")
    })
    public ResponseResult<Void> markAsRead(
            @ApiParam(value = "消息发送者ID", required = true, example = "21")
            @RequestParam Integer senderId,
            @ApiIgnore HttpServletRequest request) {
        Integer currentUserId = AuthUtil.getCurrentUserId(request);
        chatService.markAsRead(senderId, currentUserId);
        return ResponseResult.success(null);
    }
}
