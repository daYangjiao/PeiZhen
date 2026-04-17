package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel(description = "AI预约历史聊天消息")
public class AiAppointmentChatMessageVO {

    @ApiModelProperty(value = "消息角色", example = "assistant")
    private String role;

    @ApiModelProperty(value = "消息内容")
    private String content;

    @ApiModelProperty(value = "处理阶段", example = "completed")
    private String processingPhase;

    @ApiModelProperty(value = "阶段提示")
    private String thinkingProcess;

    @ApiModelProperty(value = "创建时间")
    private Date createdAt;
}
