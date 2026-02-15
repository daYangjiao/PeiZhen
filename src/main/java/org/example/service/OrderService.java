package org.example.service;

import org.example.model.Order;
import org.example.model.request.OrderListQueryRequest;
import org.example.model.response.OrderListResponse;
import org.example.model.response.PagedResponse;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {

    int createOrder(Order order);

    Order getOrderById(Integer orderId);

    int updateOrder(Order order);

    String attendantAcceptOrder(Integer orderId, Integer attendantId);

    String startService(Integer orderId);

    String endService(Integer orderId, BigDecimal actualDuration);

    PagedResponse<OrderListResponse> getUserOrdersWithPagination(Integer userId, OrderListQueryRequest queryRequest);

    PagedResponse<OrderListResponse> getAllOrdersWithPagination(OrderListQueryRequest queryRequest);

    List<Order> findAllOrders();
}