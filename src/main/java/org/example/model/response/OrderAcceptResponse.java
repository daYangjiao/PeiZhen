package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "陪诊师接单结果")
public class OrderAcceptResponse {

    @ApiModelProperty(value = "订单ID", example = "62")
    private Integer orderId;

    @ApiModelProperty(value = "结果说明", example = "接单成功")
    private String message;
}
