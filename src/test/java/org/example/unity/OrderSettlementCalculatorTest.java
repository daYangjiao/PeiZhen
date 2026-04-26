package org.example.unity;

import org.example.model.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderSettlementCalculatorTest {

    @Test
    void completedRefundWithNegativeBalanceUsesFinalOrderAmountWithoutDoubleSubtracting() {
        Order order = completedOrder("140.00");
        order.setBalanceAmount(new BigDecimal("-30.00"));
        order.setRefundAmount(new BigDecimal("30.00"));

        assertThat(OrderSettlementCalculator.settlementAmount(order)).isEqualByComparingTo("140.00");
        assertThat(OrderSettlementCalculator.platformFee(order)).isEqualByComparingTo("14.00");
        assertThat(OrderSettlementCalculator.attendantIncome(order)).isEqualByComparingTo("126.00");
    }

    @Test
    void legacyCompletedRefundSubtractsRefundOnceWhenBalanceIsMissing() {
        Order order = completedOrder("170.00");
        order.setRefundAmount(new BigDecimal("30.00"));

        assertThat(OrderSettlementCalculator.settlementAmount(order)).isEqualByComparingTo("140.00");
        assertThat(OrderSettlementCalculator.attendantIncome(order)).isEqualByComparingTo("126.00");
    }

    @Test
    void canceledRefundOrderDoesNotGenerateAttendantIncome() {
        Order order = new Order();
        order.setOrderStatus(7);
        order.setOrderAmount(new BigDecimal("170.00"));
        order.setRefundAmount(new BigDecimal("170.00"));
        order.setPenaltyAmount(BigDecimal.ZERO);

        assertThat(OrderSettlementCalculator.settlementAmount(order)).isEqualByComparingTo("0.00");
        assertThat(OrderSettlementCalculator.attendantIncome(order)).isEqualByComparingTo("0.00");
    }

    private Order completedOrder(String amount) {
        Order order = new Order();
        order.setOrderStatus(6);
        order.setOrderAmount(new BigDecimal(amount));
        return order;
    }
}
