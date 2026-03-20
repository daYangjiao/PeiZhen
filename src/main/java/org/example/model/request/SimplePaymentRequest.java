package org.example.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 简化版支付状态请求对象
 * 只接收订单号和支付状态（1=成功，0=失败）
 */
@Data
@ApiModel(description = "简化版支付状态更新请求，仅用于模拟或手动回写支付结果")
public class SimplePaymentRequest {
    @ApiModelProperty(value = "订单号", required = true, example = "ORD202603200001")
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @ApiModelProperty(value = "支付状态：1=支付成功，0=支付失败", required = true, example = "1")
    @NotNull(message = "支付状态不能为空")
    private Integer paymentStatus;
}
