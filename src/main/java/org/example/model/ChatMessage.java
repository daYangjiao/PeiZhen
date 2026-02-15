package org.example.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;
@Data
@ApiModel(description = "聊天消息实体")
public class ChatMessage {
    @ApiModelProperty(value = "消息ID")
    private Long id;

    @ApiModelProperty(value = "发送者ID")
    private Integer senderId;

    @ApiModelProperty(value = "接收者ID")
    private Integer receiverId;

    @ApiModelProperty(value = "消息内容")
    private String content;

    @ApiModelProperty(value = "消息类型：1=文本, 2=图片")
    private Integer msgType;

    @ApiModelProperty(value = "是否已读")
    private Boolean isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "发送时间")
    private Date createTime;

    // 辅助字段：发送者头像和姓名（用于前端展示）
    @ApiModelProperty(value = "发送者姓名")
    private String senderName;

    @ApiModelProperty(value = "发送者头像")
    private String senderAvatar;
}