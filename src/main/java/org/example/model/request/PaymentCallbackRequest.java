package org.example.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 微信支付回调请求对象
 */
@Data
@ApiModel(description = "微信支付回调请求对象")
public class PaymentCallbackRequest {
    @ApiModelProperty(value = "小程序或应用 appId", example = "wx1234567890abcdef")
    private String appId;
    @ApiModelProperty(value = "商户号", example = "1900000109")
    private String mchId;
    @ApiModelProperty(value = "商户订单号", example = "ORD202603200001")
    private String outTradeNo;
    @ApiModelProperty(value = "微信支付订单号", example = "4200001234202603201234567890")
    private String transactionId;
    @ApiModelProperty(value = "交易类型", example = "JSAPI")
    private String tradeType;
    @ApiModelProperty(value = "交易状态", example = "SUCCESS")
    private String tradeState;
    @ApiModelProperty(value = "交易状态说明", example = "支付成功")
    private String tradeStateDesc;
    @ApiModelProperty(value = "付款银行", example = "CMC")
    private String bankType;
    @ApiModelProperty(value = "订单总金额，单位分", example = "8000")
    private Integer totalFee;
    @ApiModelProperty(value = "现金支付金额，单位分字符串", example = "8000")
    private String cashFee;
    @ApiModelProperty(value = "货币类型", example = "CNY")
    private String feeType;
    @ApiModelProperty(value = "透传字段", example = "userId=9")
    private String attach;
    @ApiModelProperty(value = "支付完成时间", example = "20260320113059")
    private String timeEnd;
    @ApiModelProperty(value = "用户 openid", example = "oUpF8uMuAJO_M2pxb1Q9zNjWeS6o")
    private String openid;
}
