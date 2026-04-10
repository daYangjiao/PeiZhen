package org.example.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 订单创建请求对象
 */
@Data
@ApiModel(description = "根据预约与陪诊师选择创建服务订单的请求")
public class CreateOrderRequest {
    @ApiModelProperty(value = "预约编号", required = true, example = "APT202603200001")
    @NotBlank(message = "预约编号不能为空")
    private String appointmentNo;

    @ApiModelProperty(value = "陪诊师ID", required = true, example = "21")
    @NotBlank(message = "陪诊师ID不能为空")
    private String attendantId;

    @ApiModelProperty(value = "当前登录用户ID，由后端根据 token 注入，前端无需传递", example = "9")
    private Integer userId;
}
