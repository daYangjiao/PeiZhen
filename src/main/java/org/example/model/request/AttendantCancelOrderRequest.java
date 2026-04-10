package org.example.model.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AttendantCancelOrderRequest {
    private String reason;
    private BigDecimal penaltyAmount;
    private BigDecimal refundAmount;
    private BigDecimal penaltyRate;
}
