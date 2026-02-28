package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
    @ApiOperation("创建订单")
    public ResponseResult<Integer> createOrder(@Valid @RequestBody Order order, HttpServletRequest request) {
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
    @ApiOperation("根据ID查询订单")
    public ResponseResult<Order> getOrderById(@PathVariable Integer orderId) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseResult.error("订单不存在");
        }
        return ResponseResult.success(order);
    }

    @PutMapping("/{orderId}")
    @ApiOperation("更新订单信息")
    public ResponseResult<Integer> updateOrder(@PathVariable Integer orderId, @RequestBody Order order) {
        order.setOrderId(orderId);
        int rows = orderService.updateOrder(order);
        return ResponseResult.success(rows);
    }

    @PostMapping("/{orderId}/confirm-time-fee")
    @ApiOperation("用户确认服务时长与费用（多退少补）")
    public ResponseResult<String> confirmTimeAndFee(@PathVariable Integer orderId) {
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
    @ApiOperation("用户不认可时长与费用，提交申诉")
    public ResponseResult<String> disputeTimeAndFee(
            @PathVariable Integer orderId,
            @RequestParam(required = false) java.math.BigDecimal userDuration,
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
    @ApiOperation("查询当前用户订单列表")
    public ResponseResult<PagedResponse<OrderListResponse>> getCurrentUserOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        
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
    @ApiOperation("用户取消订单")
    public ResponseResult<String> cancelOrder(
            @PathVariable Integer orderId,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) java.math.BigDecimal penaltyAmount,
            @RequestParam(required = false) java.math.BigDecimal refundAmount,
            @RequestParam(required = false) java.math.BigDecimal penaltyRate,
            HttpServletRequest request) {
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
            order.setOrderStatus(7); // 已取消
            order.setCancelReason(reason != null ? reason.trim() : null);
            order.setCancelTime(new java.util.Date());
            order.setCancelBy(0); // 0=用户
            order.setPenaltyRate(finalRate);
            order.setPenaltyAmount(finalPenalty);
            order.setRefundAmount(finalRefund);
            orderService.updateOrder(order);
            // 发送系统消息通知用户订单已取消
            orderService.notifyUserOrderCancelled(order);
            return ResponseResult.success("订单已取消");
        } catch (Exception e) {
            log.error("取消订单失败，orderId={}", orderId, e);
            return ResponseResult.error("取消订单失败");
        }
    }
}