package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.ResponseResult;
import org.example.model.Order;
import org.example.model.request.OrderListQueryRequest;
import org.example.model.response.OrderListResponse;
import org.example.model.response.PagedResponse;
import org.example.service.OrderService;
import org.example.util.AuthUtil;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@Api(tags = "订单管理接口")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ApiOperation(value = "创建订单", notes = "用户提交订单请求创建新订单。userId 从当前登录态自动注入，请求体无需传 userId。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "创建成功，data 为订单 ID"),
            @ApiResponse(code = 400, message = "请求参数不完整"),
            @ApiResponse(code = 401, message = "用户未登录或 token 无效"),
            @ApiResponse(code = 500, message = "创建订单失败")
    })
    public ResponseResult<Integer> createOrder(
            @ApiParam(value = "订单创建请求体", required = true)
            @Valid @RequestBody Order order,
            @ApiIgnore HttpServletRequest request) {
        try {
            Integer currentUserId = AuthUtil.getCurrentUserId(request);
            order.setUserId(currentUserId);
            int orderId = orderService.createOrder(order);
            return ResponseResult.success(orderId);
        } catch (Exception e) {
            log.error("创建订单失败", e);
            return ResponseResult.error("创建订单失败");
        }
    }

    @GetMapping("/{orderId}")
    @ApiOperation(value = "根据ID查询订单", notes = "根据订单主键查询完整订单详情。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功"),
            @ApiResponse(code = 404, message = "订单不存在")
    })
    public ResponseResult<Order> getOrderById(
            @ApiParam(value = "订单ID", required = true, example = "62")
            @PathVariable Integer orderId) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseResult.error("订单不存在");
        }
        return ResponseResult.success(order);
    }

    @PutMapping("/{orderId}")
    @ApiOperation(value = "更新订单信息", notes = "更新订单基础信息。路径中的 orderId 会覆盖请求体中的 orderId。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "更新成功，data 为影响行数"),
            @ApiResponse(code = 400, message = "请求体不合法"),
            @ApiResponse(code = 500, message = "更新失败")
    })
    public ResponseResult<Integer> updateOrder(
            @ApiParam(value = "订单ID", required = true, example = "62")
            @PathVariable Integer orderId,
            @ApiParam(value = "订单更新请求体", required = true)
            @RequestBody Order order) {
        order.setOrderId(orderId);
        int rows = orderService.updateOrder(order);
        return ResponseResult.success(rows);
    }

    @PostMapping("/{orderId}/confirm-time-fee")
    @ApiOperation(value = "用户确认服务时长与费用", notes = "服务结束后，用户确认实际时长与差价结算结果。若返回成功，表示用户接受多退少补结果。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "确认成功"),
            @ApiResponse(code = 400, message = "订单状态不允许确认或确认失败"),
            @ApiResponse(code = 500, message = "确认时长与费用失败")
    })
    public ResponseResult<String> confirmTimeAndFee(
            @ApiParam(value = "订单ID", required = true, example = "62")
            @PathVariable Integer orderId) {
        try {
            String result = orderService.userConfirmTimeAndFee(orderId);
            if (result.startsWith("确认成功")) {
                return ResponseResult.success(result);
            }
            return ResponseResult.error(result);
        } catch (Exception e) {
            log.error("确认时长与费用失败，orderId={}", orderId, e);
            return ResponseResult.error("确认时长与费用失败");
        }
    }

    @PostMapping("/{orderId}/dispute-time-fee")
    @ApiOperation(value = "用户不认可时长与费用，提交申诉", notes = "用户可提交自己认可的服务时长和争议原因，进入人工处理或后续协商流程。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "申诉提交成功"),
            @ApiResponse(code = 400, message = "参数不合法或订单状态不允许申诉"),
            @ApiResponse(code = 500, message = "提交申诉失败")
    })
    public ResponseResult<String> disputeTimeAndFee(
            @ApiParam(value = "订单ID", required = true, example = "62")
            @PathVariable Integer orderId,
            @ApiParam(value = "用户认可的实际服务时长，单位小时", example = "2.5")
            @RequestParam(required = false) java.math.BigDecimal userDuration,
            @ApiParam(value = "争议原因说明", example = "陪诊师登记时长偏长，实际服务只有 2.5 小时")
            @RequestParam(required = false) String reason) {
        try {
            String result = orderService.userDisputeTimeAndFee(orderId, userDuration, reason);
            if (result.startsWith("申诉已提交")) {
                return ResponseResult.success(result);
            }
            return ResponseResult.error(result);
        } catch (Exception e) {
            log.error("提交时长费用申诉失败，orderId={}", orderId, e);
            return ResponseResult.error("提交申诉失败");
        }
    }

    @GetMapping("/user-orders")
    @ApiOperation(value = "查询当前用户订单列表", notes = "返回当前登录用户的分页订单列表，可按订单状态筛选。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功"),
            @ApiResponse(code = 401, message = "用户未登录或 token 无效"),
            @ApiResponse(code = 500, message = "查询失败")
    })
    public ResponseResult<PagedResponse<OrderListResponse>> getCurrentUserOrders(
            @ApiParam(value = "订单状态筛选：1=待接单，2=待服务，3=服务中，4=待确认时长费用，5=时长费用有争议，6=已完成，7=已取消", example = "6")
            @RequestParam(required = false) Integer status,
            @ApiParam(value = "页码，从 0 开始", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @ApiParam(value = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiIgnore HttpServletRequest request) {
        try {
            Integer currentUserId = AuthUtil.getCurrentUserId(request);
            OrderListQueryRequest queryRequest = new OrderListQueryRequest();
            queryRequest.setPage(page);
            queryRequest.setSize(pageSize);
            queryRequest.setOrderStatus(status);
            
            PagedResponse<OrderListResponse> result = orderService.getUserOrdersWithPagination(currentUserId, queryRequest);
            return ResponseResult.success(result);
        } catch (Exception e) {
            log.error("查询用户订单列表失败", e);
            return ResponseResult.error("查询失败");
        }
    }

    @PutMapping("/{orderId}/cancel")
    @ApiOperation(value = "用户取消订单", notes = "用户主动取消订单。可携带取消原因、违约金、退款金额等结算字段，后端会校验当前用户是否为下单人。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "取消成功"),
            @ApiResponse(code = 400, message = "订单状态不允许取消或参数不合法"),
            @ApiResponse(code = 401, message = "无权限操作该订单"),
            @ApiResponse(code = 404, message = "订单不存在"),
            @ApiResponse(code = 500, message = "取消订单失败")
    })
    public ResponseResult<String> cancelOrder(
            @ApiParam(value = "订单ID", required = true, example = "62")
            @PathVariable Integer orderId,
            @ApiParam(value = "取消原因", example = "患者临时改期")
            @RequestParam(required = false) String reason,
            @ApiParam(value = "违约金金额，单位元", example = "16.00")
            @RequestParam(required = false) java.math.BigDecimal penaltyAmount,
            @ApiParam(value = "退款金额，单位元", example = "64.00")
            @RequestParam(required = false) java.math.BigDecimal refundAmount,
            @ApiParam(value = "违约金比例，0-1 之间", example = "0.2")
            @RequestParam(required = false) java.math.BigDecimal penaltyRate,
            @ApiIgnore HttpServletRequest request) {
        try {
            log.info("用户取消订单请求，orderId={}, reason={}, penaltyAmount={}, refundAmount={}, penaltyRate={}",
                    orderId, reason, penaltyAmount, refundAmount, penaltyRate);
            Integer currentUserId = AuthUtil.getCurrentUserId(request);
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                return ResponseResult.error("订单不存在");
            }
            if (order.getUserId() == null || !order.getUserId().equals(currentUserId)) {
                return ResponseResult.unauthorized("无权限操作该订单");
            }
            if (order.getOrderStatus() != null && (order.getOrderStatus() == 6 || order.getOrderStatus() == 7)) {
                return ResponseResult.error("订单已完成或已取消，无法重复取消");
            }
            if (order.getOrderStatus() != null && order.getOrderStatus() == 3) {
                return ResponseResult.error("服务中订单不允许取消");
            }

            java.math.BigDecimal orderAmount = order.getOrderAmount() == null ? java.math.BigDecimal.ZERO : order.getOrderAmount();
            java.math.BigDecimal finalRate = penaltyRate != null ? penaltyRate : java.math.BigDecimal.ZERO;
            java.math.BigDecimal finalPenalty = penaltyAmount != null ? penaltyAmount : orderAmount.multiply(finalRate);
            java.math.BigDecimal finalRefund = refundAmount != null ? refundAmount : orderAmount;
            Integer previousStatus = order.getOrderStatus();
            order.setOrderStatus(7); // 已取消
            order.setCancelReason(reason != null ? reason.trim() : null);
            order.setCancelTime(new java.util.Date());
            order.setCancelBy(0); // 0=用户
            order.setPenaltyRate(finalRate);
            order.setPenaltyAmount(finalPenalty);
            order.setRefundAmount(finalRefund);
            orderService.updateOrder(order);
            orderService.notifyOrderParties(
                    order,
                    "您的订单" + (order.getOrderNo() != null ? order.getOrderNo() : "") + "已取消。取消原因："
                            + (order.getCancelReason() != null ? order.getCancelReason() : "订单已取消"),
                    order.getAttendantId() != null
                            ? "用户已取消订单 " + (order.getOrderNo() != null ? order.getOrderNo() : "") + "。取消原因："
                            + (order.getCancelReason() != null ? order.getCancelReason() : "订单已取消")
                            : null
            );
            orderService.publishOrderEvent(order, "ORDER_STATUS_CHANGED", null, null, true, true);
            if (previousStatus != null && previousStatus == 1) {
                orderService.broadcastWaitingOrderUpdate(order);
            }
            return ResponseResult.success("订单已取消");
        } catch (Exception e) {
            log.error("取消订单失败，orderId={}", orderId, e);
            return ResponseResult.error("取消订单失败");
        }
    }
}
