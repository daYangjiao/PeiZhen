package org.example.unity;

import org.example.model.Order;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class OrderSettlementCalculator {

    public static final BigDecimal PLATFORM_COMMISSION_RATE = new BigDecimal("0.10");
    public static final BigDecimal ATTENDANT_INCOME_RATE = BigDecimal.ONE.subtract(PLATFORM_COMMISSION_RATE);

    private OrderSettlementCalculator() {
    }

    public static BigDecimal settlementAmount(Order order) {
        if (order == null || !Integer.valueOf(6).equals(order.getOrderStatus())) {
            return money(BigDecimal.ZERO);
        }
        BigDecimal amount = value(order.getOrderAmount());
        BigDecimal refund = value(order.getRefundAmount());
        BigDecimal balance = order.getBalanceAmount();

        // New dispute/refund flow writes final amount back to order_amount and stores a negative balance.
        // Older rows may have refund_amount but no negative balance, so subtract once for compatibility.
        if (refund.compareTo(BigDecimal.ZERO) > 0
                && (balance == null || balance.compareTo(BigDecimal.ZERO) >= 0)) {
            amount = amount.subtract(refund);
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            amount = BigDecimal.ZERO;
        }
        return money(amount);
    }

    public static BigDecimal platformFee(Order order) {
        return money(settlementAmount(order).multiply(PLATFORM_COMMISSION_RATE));
    }

    public static BigDecimal attendantIncome(Order order) {
        return money(settlementAmount(order).multiply(ATTENDANT_INCOME_RATE));
    }

    private static BigDecimal value(BigDecimal input) {
        return input == null ? BigDecimal.ZERO : input;
    }

    private static BigDecimal money(BigDecimal input) {
        return value(input).setScale(2, RoundingMode.HALF_UP);
    }
}
