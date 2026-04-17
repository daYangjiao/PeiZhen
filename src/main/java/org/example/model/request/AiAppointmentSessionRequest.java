package org.example.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.example.model.AiAppointmentStructuredDemand;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(description = "AI预约会话初始化请求")
public class AiAppointmentSessionRequest {

    @NotBlank(message = "需求描述不能为空")
    @ApiModelProperty(value = "用户自然语言需求", required = true, example = "下周三上午带80岁的爷爷去华西医院心内科复诊，需要一位有力气推轮椅的男陪诊师")
    private String demandText;

    @ApiModelProperty(value = "前端已整理的结构化需求")
    private AiAppointmentStructuredDemand structuredDemand;
}
