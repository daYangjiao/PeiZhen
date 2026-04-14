package org.example.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "AI预约补充回复请求")
public class AiAppointmentReplyRequest {

    @ApiModelProperty(value = "用户补充文本", example = "明天下午")
    private String replyText;

    @ApiModelProperty(value = "结构化字段Key", example = "serviceDate")
    private String fieldKey;

    @ApiModelProperty(value = "结构化选择值", example = "2026-04-16")
    private String selectedValue;
}
