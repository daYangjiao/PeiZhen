package org.example.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "AI预约时间建议")
public class AiAppointmentTimeProposal {

    @ApiModelProperty(value = "建议开始时间", example = "08:00")
    private String proposedStartTime;

    @ApiModelProperty(value = "建议结束时间", example = "10:00")
    private String proposedEndTime;

    @ApiModelProperty(value = "建议说明", example = "如果您方便，我可以先按明天 08:00-10:00 为您记录。")
    private String proposalText;
}
