package org.example.model.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminOrderCancelRequest {

    private String reason;

    private BigDecimal refundAmount;

    private String adminRemark;
}
