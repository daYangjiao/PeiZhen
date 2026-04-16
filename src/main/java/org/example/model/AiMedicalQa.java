package org.example.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "AI医疗问答记录")
public class AiMedicalQa {
    @ApiModelProperty(value = "问答记录ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "用户ID", example = "15")
    private Integer userId;

    @ApiModelProperty(value = "会话ID", example = "conv-20260320-0001")
    private String conversationId;

    @ApiModelProperty(value = "用户问题", example = "孩子反复发烧三天，需要尽快去医院吗？")
    private String question;

    @ApiModelProperty(value = "AI回答", example = "建议尽快前往医院儿科就诊，并监测体温变化。")
    private String answer;

    @ApiModelProperty(value = "问答状态：0=处理中，1=已完成，2=失败", example = "1")
    private Integer qaStatus;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "逻辑删除标记：0=未删除，1=已删除", example = "0")
    private Integer deleted;

    @ApiModelProperty(value = "AI思考过程文本", example = "先分析症状持续时间，再给出就医建议。")
    private String thinkingProcess;
}
