package org.example.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 订单创建返回VO
 */
@Data
@ApiModel(description = "AI导诊创建订单后的响应")
public class OrderCreateResponse {
    @ApiModelProperty(value = "订单编号", example = "ORD202603200001")
    private String orderNo;

    @ApiModelProperty(value = "服务类型列表", example = "[\"普通陪诊\",\"术后护理\"]")
    private List<String> serviceTypes;

    @ApiModelProperty(value = "陪诊师姓名", example = "李医生")
    private String attendantName;

    @ApiModelProperty(value = "支付金额，单位元", example = "80.0")
    private Double payAmount;
}
