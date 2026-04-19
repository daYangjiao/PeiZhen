package org.example.util;

import org.example.model.Order;
import org.springframework.util.StringUtils;

public final class OrderTimeoutCloseUtils {

    public static final String TIMEOUT_CLOSE_STATUS_TEXT = "超时关闭";
    private static final String TIMEOUT_REASON_KEYWORD = "超时未匹配到陪诊师";
    private static final String DEFAULT_TIMEOUT_CLOSE_MESSAGE = "订单已超时未匹配到陪诊师，系统已自动关闭并发起退款。";

    private OrderTimeoutCloseUtils() {
    }

    public static String buildTimeoutCloseMessage(Order order) {
        if (order == null) {
            return DEFAULT_TIMEOUT_CLOSE_MESSAGE;
        }

        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(order.getServiceDate()) || StringUtils.hasText(order.getServiceTimeSlot())) {
            builder.append("原定");
            if (StringUtils.hasText(order.getServiceDate())) {
                builder.append(order.getServiceDate().trim()).append(' ');
            }
            if (StringUtils.hasText(order.getServiceTimeSlot())) {
                builder.append(order.getServiceTimeSlot().trim());
            }
            builder.append("的服务已开始，");
        }

        builder.append(TIMEOUT_REASON_KEYWORD).append("，系统已自动关闭并发起退款。");
        return builder.toString();
    }

    public static String resolveOrderStatusText(Order order) {
        if (order == null) {
            return "未知";
        }
        if (isTimeoutClosedOrder(order)) {
            return TIMEOUT_CLOSE_STATUS_TEXT;
        }

        Integer status = order.getOrderStatus();
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "待支付";
            case 1:
                return "待接单";
            case 2:
                return "待服务";
            case 3:
                return "服务中";
            case 4:
                return "待确认时长";
            case 5:
                return "待补款";
            case 6:
                return "已完成";
            case 7:
                return "已取消";
            case 8:
                return "专属派单待确认";
            default:
                return "未知";
        }
    }

    public static boolean isTimeoutClosedOrder(Order order) {
        return order != null
                && Integer.valueOf(7).equals(order.getOrderStatus())
                && StringUtils.hasText(order.getCancelReason())
                && order.getCancelReason().contains(TIMEOUT_REASON_KEYWORD);
    }
}
