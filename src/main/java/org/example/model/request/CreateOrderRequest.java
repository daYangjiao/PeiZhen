package org.example.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 订单创建请求对象
 */
@Data
@ApiModel(description = "根据预约与陪诊师选择创建服务订单的请求")
public class CreateOrderRequest {
    @ApiModelProperty(value = "预约编号", required = true, example = "APT202603200001")
    private String appointmentNo;

    @ApiModelProperty(value = "旧版兼容陪诊师ID", example = "21")
    private String attendantId;

    @ApiModelProperty(value = "指定陪诊师ID", example = "21")
    private Long designatedAttendantId;

    @ApiModelProperty(value = "当前登录用户ID，由后端根据 token 注入，前端无需传递", example = "9")
    private Integer userId;
}
