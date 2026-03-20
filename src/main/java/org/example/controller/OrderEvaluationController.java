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
import org.example.model.OrderEvaluation;
import org.example.service.OrderEvaluationService;
import org.example.service.OrderService;
import org.example.util.AuthUtil;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

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
    @ApiOperation(value = "查询订单评价详情", notes = "用户查询自己订单的评价详情。仅订单归属用户可以访问。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功，返回评价详情；若尚未评价，data 可能为 null"),
            @ApiResponse(code = 401, message = "无权限查看该订单评价"),
            @ApiResponse(code = 404, message = "订单不存在"),
            @ApiResponse(code = 500, message = "查询评价失败")
    })
    public ResponseResult<OrderEvaluation> getEvaluation(
            @ApiParam(value = "订单ID", required = true, example = "62")
            @PathVariable Integer orderId,
            @ApiIgnore HttpServletRequest request) {
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
    @ApiOperation(value = "提交或更新订单评价", notes = "仅已完成订单允许评价。前端只需提交 rating、tags、content，其他字段由后端补齐。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "评价提交成功"),
            @ApiResponse(code = 400, message = "评分不合法或订单状态不允许评价"),
            @ApiResponse(code = 401, message = "无权限评价该订单"),
            @ApiResponse(code = 404, message = "订单不存在"),
            @ApiResponse(code = 500, message = "提交评价失败")
    })
    public ResponseResult<String> submitEvaluation(
            @ApiParam(value = "订单ID", required = true, example = "62")
            @PathVariable Integer orderId,
            @ApiParam(value = "评价请求体，仅需填写 rating、tags、content", required = true)
            @RequestBody OrderEvaluation payload,
            @ApiIgnore HttpServletRequest request) {
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
