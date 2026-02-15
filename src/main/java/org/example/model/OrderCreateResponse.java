package org.example.model;

import lombok.Data;
import java.util.List;

/**
 * 订单创建返回VO
 */
@Data
public class OrderCreateResponse {
    // 订单编号
    private String orderNo;
    // 服务类型列表
    private List<String> serviceTypes;
    // 陪诊师姓名
    private String attendantName;
    // 支付金额
    private Double payAmount;
}