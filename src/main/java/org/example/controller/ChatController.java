package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.example.common.ResponseResult;
import org.example.model.ChatMessage;
import org.example.model.Order;
import org.example.model.response.EscortSystemMessageDetailResponse;
import org.example.service.ChatService;
import org.example.service.OrderService;
import org.example.util.AuthUtil;
import org.example.util.OrderTimeoutCloseUtils;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/chat")
@Api(tags = "聊天消息接口")
@RequiredArgsConstructor
public class ChatController {

    private static final Pattern ORDER_NO_PATTERN =
            Pattern.compile("\\b(ORD[A-Za-z0-9_-]{6,})\\b", Pattern.CASE_INSENSITIVE);

    private final ChatService chatService;
    private final OrderService orderService;

    @PostMapping("/send")
    @ApiOperation(value = "发送消息")
    @ApiResponses({
            @ApiResponse(code = 200, message = "发送成功"),
            @ApiResponse(code = 401, message = "未登录"),
            @ApiResponse(code = 500, message = "发送失败")
    })
    public ResponseResult<ChatMessage> sendMessage(
            @ApiParam(value = "消息体", required = true)
            @RequestBody ChatMessage message,
            @ApiIgnore HttpServletRequest request) {
        Integer currentUserId = AuthUtil.getCurrentUserId(request);
        message.setSenderId(currentUserId);
        return ResponseResult.success(chatService.sendMessage(message));
    }

    @GetMapping("/history")
    @ApiOperation(value = "获取聊天记录")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功"),
            @ApiResponse(code = 401, message = "未登录"),
            @ApiResponse(code = 500, message = "查询失败")
    })
    public ResponseResult<List<ChatMessage>> getHistory(
            @ApiParam(value = "对方用户ID", required = true, example = "21")
            @RequestParam Integer targetUserId,
            @ApiIgnore HttpServletRequest request) {
        Integer currentUserId = AuthUtil.getCurrentUserId(request);
        return ResponseResult.success(chatService.getHistory(currentUserId, targetUserId));
    }

    @GetMapping("/system")
    @ApiOperation(value = "获取系统消息列表")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功"),
            @ApiResponse(code = 401, message = "未登录"),
            @ApiResponse(code = 500, message = "查询失败")
    })
    public ResponseResult<List<ChatMessage>> getSystemMessages(@ApiIgnore HttpServletRequest request) {
        Integer currentUserId = AuthUtil.getCurrentUserId(request);
        return ResponseResult.success(chatService.getSystemMessages(currentUserId));
    }

    @GetMapping("/system/{messageId}")
    @ApiOperation(value = "获取陪诊师系统消息详情")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功"),
            @ApiResponse(code = 404, message = "消息不存在"),
            @ApiResponse(code = 500, message = "查询失败")
    })
    public ResponseResult<EscortSystemMessageDetailResponse> getSystemMessageDetail(
            @ApiParam(value = "消息ID", required = true, example = "1001")
            @PathVariable Long messageId,
            @ApiIgnore HttpServletRequest request) {
        Integer currentUserId = AuthUtil.getCurrentUserId(request);
        ChatMessage message = chatService.getSystemMessageDetail(messageId, currentUserId);
        if (message == null || !Integer.valueOf(0).equals(message.getSenderId())) {
            return ResponseResult.error("系统消息不存在或已删除");
        }

        boolean markedRead = false;
        if (!Boolean.TRUE.equals(message.getIsRead())) {
            markedRead = chatService.markMessageAsRead(messageId, currentUserId) > 0;
            if (markedRead) {
                message.setIsRead(true);
            }
        }

        EscortSystemMessageDetailResponse response = new EscortSystemMessageDetailResponse();
        response.setMessageId(message.getId());
        response.setOrderId(message.getOrderId());
        response.setContent(message.getContent());
        response.setCreateTime(message.getCreateTime());
        response.setIsRead(Boolean.TRUE.equals(message.getIsRead()));
        response.setMarkedRead(markedRead);
        fillOrderSummary(response, message, currentUserId);
        return ResponseResult.success(response);
    }

    @GetMapping("/contacts")
    @ApiOperation(value = "获取最近联系人")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功"),
            @ApiResponse(code = 500, message = "查询失败")
    })
    public ResponseResult<List<ChatMessage>> getRecentContacts(@ApiIgnore HttpServletRequest request) {
        Integer currentUserId = AuthUtil.getCurrentUserId(request);
        return ResponseResult.success(chatService.getRecentContacts(currentUserId));
    }

    @PostMapping("/read")
    @ApiOperation(value = "批量标记会话已读")
    @ApiResponses({
            @ApiResponse(code = 200, message = "标记成功"),
            @ApiResponse(code = 401, message = "未登录"),
            @ApiResponse(code = 500, message = "标记失败")
    })
    public ResponseResult<Void> markAsRead(
            @ApiParam(value = "发送方用户ID", required = true, example = "21")
            @RequestParam Integer senderId,
            @ApiIgnore HttpServletRequest request) {
        Integer currentUserId = AuthUtil.getCurrentUserId(request);
        chatService.markAsRead(senderId, currentUserId);
        return ResponseResult.success(null);
    }

    private void fillOrderSummary(EscortSystemMessageDetailResponse response, ChatMessage message, Integer currentUserId) {
        Order order = resolveOrderFromMessage(message);
        if (order == null) {
            response.setOrderAvailable(false);
            response.setOrderUnavailableReason("该消息未关联订单");
            return;
        }

        if (order.getAttendantId() != null && !order.getAttendantId().equals(currentUserId)) {
            response.setOrderAvailable(false);
            response.setOrderUnavailableReason("无权限查看该订单");
            return;
        }

        EscortSystemMessageDetailResponse.OrderSummary summary = new EscortSystemMessageDetailResponse.OrderSummary();
        summary.setOrderId(order.getOrderId());
        summary.setOrderNo(order.getOrderNo());
        summary.setOrderStatus(order.getOrderStatus());
        summary.setOrderStatusText(OrderTimeoutCloseUtils.resolveOrderStatusText(order));
        summary.setHospital(order.getHospital());
        summary.setPatientName(order.getPatientName());
        summary.setServiceDate(order.getServiceDate());
        summary.setServiceTimeSlot(order.getServiceTimeSlot());

        response.setOrderId(order.getOrderId());
        response.setOrderAvailable(true);
        response.setOrder(summary);
    }

    private Order resolveOrderFromMessage(ChatMessage message) {
        if (message == null) {
            return null;
        }

        Integer directOrderId = message.getOrderId();
        if (directOrderId != null && directOrderId > 0) {
            Order directOrder = orderService.getOrderById(directOrderId);
            if (directOrder != null) {
                return directOrder;
            }
        }

        String orderNo = extractOrderNo(message.getContent());
        if (orderNo == null || orderNo.isBlank()) {
            return null;
        }
        return orderService.getOrderByOrderNo(orderNo);
    }

    private String extractOrderNo(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        Matcher matcher = ORDER_NO_PATTERN.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private String resolveOrderStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "待接单";
            case 2 -> "待服务";
            case 3 -> "服务中";
            case 4 -> "待确认时长";
            case 5 -> "待补款";
            case 6 -> "已完成";
            case 7 -> "已取消";
            default -> "未知";
        };
    }
}
