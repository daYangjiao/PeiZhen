package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 预约响应对象 - 返回预约编号
 */
@Data
@ApiModel(description = "创建预约后的响应")
public class AppointmentResponse {
    @ApiModelProperty(value = "预约编号", example = "APT202603200001")
    private String appointmentNo;

    @ApiModelProperty(value = "结果说明", example = "预约创建成功")
    private String message;
}
