package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.ResponseResult;
import org.example.model.Order;
import org.example.model.OrderEvaluation;
import org.example.service.OrderEvaluationService;
import org.example.service.OrderService;
import org.example.util.AuthUtil;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/orders")
@Api(tags = "订单评价接口")
@RequiredArgsConstructor
@Slf4j
public class OrderEvaluationController {

    private final OrderService orderService;
    private final OrderEvaluationService evaluationService;

    @GetMapping("/{orderId}/evaluation")
    @ApiOperation("查询订单评价详情")
    public ResponseResult<OrderEvaluation> getEvaluation(@PathVariable Integer orderId,
                                                         HttpServletRequest request) {
        try {
            Integer currentUserId = AuthUtil.getCurrentUserId(request);
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                return ResponseResult.error("订单不存在");
            }
            if (order.getUserId() == null || !order.getUserId().equals(currentUserId)) {
                return ResponseResult.unauthorized("无权限查看该订单评价");
            }
            OrderEvaluation evaluation = evaluationService.getByOrderId(orderId);
            return ResponseResult.success(evaluation);
        } catch (Exception e) {
            log.error("查询订单评价失败, orderId={}", orderId, e);
            return ResponseResult.error("查询评价失败");
        }
    }

    @PostMapping("/{orderId}/evaluation")
    @ApiOperation("提交或更新订单评价")
    public ResponseResult<String> submitEvaluation(@PathVariable Integer orderId,
                                                   @RequestBody OrderEvaluation payload,
                                                   HttpServletRequest request) {
        try {
            Integer currentUserId = AuthUtil.getCurrentUserId(request);
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                return ResponseResult.error("订单不存在");
            }
            if (order.getUserId() == null || !order.getUserId().equals(currentUserId)) {
                return ResponseResult.unauthorized("无权限评价该订单");
            }
            if (order.getOrderStatus() == null || order.getOrderStatus() != 6) {
                return ResponseResult.error("仅已完成的订单可以评价");
            }
            if (payload.getRating() == null || payload.getRating() < 1 || payload.getRating() > 5) {
                return ResponseResult.error("评分必须在1-5之间");
            }

            OrderEvaluation evaluation = new OrderEvaluation();
            evaluation.setOrderId(orderId);
            evaluation.setOrderNo(order.getOrderNo());
            evaluation.setUserId(currentUserId);
            evaluation.setAttendantId(order.getAttendantId());
            evaluation.setRating(payload.getRating());
            evaluation.setTags(payload.getTags());
            evaluation.setContent(payload.getContent());

            evaluationService.saveOrUpdateEvaluation(evaluation);
            return ResponseResult.success("评价提交成功");
        } catch (Exception e) {
            log.error("提交订单评价失败, orderId={}", orderId, e);
            return ResponseResult.error("提交评价失败");
        }
    }
}

