package org.example.model.request;

import lombok.Data;

/**
 * 微信支付回调请求对象
 */
@Data
public class PaymentCallbackRequest {
    private String appId;
    private String mchId;
    private String outTradeNo;  // 商户订单号
    private String transactionId; // 微信支付订单号
    private String tradeType;
    private String tradeState;
    private String tradeStateDesc;
    private String bankType;
    private Integer totalFee;
    private String cashFee;
    private String feeType;
    private String attach;
    private String timeEnd;
    private String openid;
}