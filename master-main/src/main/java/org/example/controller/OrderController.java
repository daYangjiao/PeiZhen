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
}