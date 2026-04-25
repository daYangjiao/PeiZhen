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

    Order getOrderByOrderNo(String orderNo);

    int updateOrder(Order order);

    String attendantAcceptOrder(Integer orderId, Integer attendantId);

    String rejectAssignedOrder(Integer orderId, Integer attendantId, String reason);

    String closeTimedOutUnmatchedOrder(Integer orderId);

    String startService(Integer orderId);

    String endService(Integer orderId, BigDecimal actualDuration);

    /**
     * 用户确认时长与费用（多退少补）
     */
    String userConfirmTimeAndFee(Integer orderId, Integer currentUserId);

    /**
     * 用户不认可时长，提交申诉
     */
    String userDisputeTimeAndFee(Integer orderId, Integer currentUserId, BigDecimal userDuration, String reason);

    /**
     * 用户支付时长费用差额
     */
    String userPayBalance(Integer orderId, Integer currentUserId);

    /**
     * 更新订单服务进度（已到院/候诊中/检查中/就诊完成）
     */
    String updateServiceProgress(Integer orderId, Integer step);

    /**
     * 陪诊师取消订单：若在预约开始前取消则订单释放回接单大厅（待接单），并通知用户；否则按已取消处理。
     * @return 成功文案，如 "订单已释放回接单大厅，将重新为您匹配合诊师" 或 "订单已取消"
     */
    String attendantCancelOrder(Integer orderId, String reason, java.math.BigDecimal penaltyAmount,
                                java.math.BigDecimal refundAmount, java.math.BigDecimal penaltyRate);

    /**
     * 发送订单取消系统消息（用户或系统取消）
     */
    void notifyUserOrderCancelled(Order order);

    void notifyOrderParties(Order order, String userMessage, String attendantMessage);

    void publishOrderEvent(Order order, String eventType, String reason, Integer step,
                           boolean notifyUser, boolean notifyAttendant);

    void broadcastWaitingOrderUpdate(Order order);

    PagedResponse<OrderListResponse> getUserOrdersWithPagination(Integer userId, OrderListQueryRequest queryRequest);

    PagedResponse<OrderListResponse> getWaitingOrdersForAttendant(Integer attendantId, OrderListQueryRequest queryRequest);

    PagedResponse<OrderListResponse> getAllOrdersWithPagination(OrderListQueryRequest queryRequest);

    List<Order> findAllOrders();
}
