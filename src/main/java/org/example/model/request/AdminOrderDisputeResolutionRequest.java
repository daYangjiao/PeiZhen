package org.example.model.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminOrderDisputeResolutionRequest {

    private BigDecimal finalDuration;

    private BigDecimal finalOrderAmount;

    private String adminRemark;
}
