package org.example.job;

import lombok.extern.slf4j.Slf4j;
import org.example.dao.OrderMapper;
import org.example.model.Order;
import org.example.service.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class OrderAssignTimeoutJob {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    public OrderAssignTimeoutJob(OrderMapper orderMapper, OrderService orderService) {
        this.orderMapper = orderMapper;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void releaseExpiredAssignedOrders() {
        Date now = new Date();
        Date deadline = new Date(now.getTime() - 15 * 60 * 1000L);
        List<Order> expired = orderMapper.findExpiredAssignedOrders(deadline);
        if (expired == null || expired.isEmpty()) {
            return;
        }

        for (Order order : expired) {
            try {
                if (order.getOrderStatus() == null || order.getOrderStatus() != 8 || order.getAttendantId() == null) {
                    continue;
                }
                String result = orderService.rejectAssignedOrder(order.getOrderId(), order.getAttendantId(), "指定陪诊师超时未确认，订单已转入公共派单");
                log.info("超时释放专属派单, orderId={}, orderNo={}, result={}", order.getOrderId(), order.getOrderNo(), result);
            } catch (Exception e) {
                log.error("超时释放专属派单失败, orderId={}", order.getOrderId(), e);
            }
        }
    }
}
