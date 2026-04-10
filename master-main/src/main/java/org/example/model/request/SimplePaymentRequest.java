package org.example.model.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 简化版支付状态请求对象
 * 只接收订单号和支付状态（1=成功，0=失败）
 */
@Data
public class SimplePaymentRequest {
    @NotBlank(message = "订单号不能为空")
    private String orderNo;
    
    @NotNull(message = "支付状态不能为空")
    private Integer paymentStatus; // 1=支付成功，0=支付失败
}