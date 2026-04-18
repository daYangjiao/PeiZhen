package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "最近 AI导诊会话概览")
public class AiAppointmentLatestOverviewResponse {

    @ApiModelProperty(value = "最近一条会话")
    private AiAppointmentSessionResponse session;

    @ApiModelProperty(value = "是否已完成匹配或生成预约")
    private Boolean completed;

    @ApiModelProperty(value = "是否仍可继续上次请求")
    private Boolean continuable;

    @ApiModelProperty(value = "是否仅展示历史，不复用为当前请求")
    private Boolean historyOnly;

    @ApiModelProperty(value = "未完成会话可继续截止时间")
    private LocalDateTime expiresAt;
}
