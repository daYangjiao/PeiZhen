package org.example.job;

import lombok.extern.slf4j.Slf4j;
import org.example.dao.OrderMapper;
import org.example.handler.OrderWebSocketHandler;
import org.example.model.Order;
import org.example.service.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 系统自动取消：下单后超过15分钟仍未完成预付款的订单
 */
@Component
@Slf4j
public class OrderAutoCancelJob {

    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final OrderWebSocketHandler webSocketHandler;

    public OrderAutoCancelJob(OrderMapper orderMapper, OrderService orderService, OrderWebSocketHandler webSocketHandler) {
        this.orderMapper = orderMapper;
        this.orderService = orderService;
        this.webSocketHandler = webSocketHandler;
    }

    @Scheduled(fixedDelay = 60_000) // 每分钟扫描一次
    @Transactional
    public void autoCancelExpiredUnpaidOrders() {
        Date now = new Date();
        Date deadline = new Date(now.getTime() - 15 * 60 * 1000L);

        List<Order> expired = orderMapper.findExpiredUnpaidOrders(deadline);
        if (expired == null || expired.isEmpty()) {
            return;
        }

        for (Order order : expired) {
            try {
                // 二次校验，避免并发下误取消
                if (order.getPaymentStatus() != null && order.getPaymentStatus() != 0) {
                    continue;
                }
                if (order.getOrderStatus() == null || order.getOrderStatus() != 0) {
                    continue;
                }

                Order update = new Order();
                update.setOrderId(order.getOrderId());
                update.setOrderStatus(7); // 已取消
                update.setCancelReason("超时未支付，系统自动取消");
                update.setCancelTime(now);
                update.setCancelBy(2); // 2=系统
                update.setPenaltyRate(BigDecimal.ZERO);
                update.setPenaltyAmount(BigDecimal.ZERO);
                update.setRefundAmount(BigDecimal.ZERO);
                orderMapper.updateByPrimaryKeySelective(update);

                // 系统消息通知用户
                order.setOrderStatus(7);
                order.setCancelReason(update.getCancelReason());
                order.setCancelTime(now);
                order.setCancelBy(2);
                orderService.notifyUserOrderCancelled(order);

                // WebSocket 推送订单状态变更，前端实时刷新列表/详情
                try {
                    String wsMsg = String.format("{\"type\":\"ORDER_STATUS_CHANGED\",\"orderId\":%d,\"orderStatus\":7}",
                            order.getOrderId());
                    if (order.getUserId() != null) {
                        webSocketHandler.sendMessageToUser(String.valueOf(order.getUserId()), wsMsg);
                    }
                    if (order.getAttendantId() != null) {
                        webSocketHandler.sendMessageToUser(String.valueOf(order.getAttendantId()), wsMsg);
                    }
                } catch (Exception e) {
                    log.error("自动取消订单后发送 WebSocket 失败, orderId={}", order.getOrderId(), e);
                }

                log.info("自动取消超时未支付订单成功, orderId={}, orderNo={}", order.getOrderId(), order.getOrderNo());
            } catch (Exception e) {
                log.error("自动取消超时未支付订单失败, orderId={}", order != null ? order.getOrderId() : null, e);
            }
        }
    }
}

