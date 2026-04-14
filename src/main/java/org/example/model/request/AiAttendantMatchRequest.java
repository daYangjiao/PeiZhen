package org.example.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.example.model.AiAppointmentStructuredDemand;

@Data
@ApiModel(description = "AI陪诊师匹配请求")
public class AiAttendantMatchRequest {

    @ApiModelProperty(value = "AI预约会话ID", example = "ai-app-123")
    private String sessionId;

    @ApiModelProperty(value = "原始需求文本")
    private String demandText;

    @ApiModelProperty(value = "结构化需求")
    private AiAppointmentStructuredDemand structuredDemand;
}
