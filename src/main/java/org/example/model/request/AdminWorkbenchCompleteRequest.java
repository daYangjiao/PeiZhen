package org.example.model.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminWorkbenchCompleteRequest {

    private String lockToken;

    private String action;

    private String reason;

    private BigDecimal finalDuration;

    private BigDecimal finalOrderAmount;

    private String adminRemark;
}
