package org.example.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel(description = "陪诊师系统消息详情响应")
public class EscortSystemMessageDetailResponse {

    @ApiModelProperty(value = "消息ID")
    private Long messageId;

    @ApiModelProperty(value = "关联订单ID")
    private Integer orderId;

    @ApiModelProperty(value = "消息内容")
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "是否已读")
    private Boolean isRead;

    @ApiModelProperty(value = "本次打开是否触发已读")
    private Boolean markedRead;

    @ApiModelProperty(value = "是否可查看关联订单")
    private Boolean orderAvailable;

    @ApiModelProperty(value = "关联订单不可用原因")
    private String orderUnavailableReason;

    @ApiModelProperty(value = "关联订单摘要")
    private OrderSummary order;

    @Data
    @ApiModel(description = "系统消息关联订单摘要")
    public static class OrderSummary {

        @ApiModelProperty(value = "订单ID")
        private Integer orderId;

        @ApiModelProperty(value = "订单号")
        private String orderNo;

        @ApiModelProperty(value = "订单状态")
        private Integer orderStatus;

        @ApiModelProperty(value = "订单状态描述")
        private String orderStatusText;

        @ApiModelProperty(value = "医院")
        private String hospital;

        @ApiModelProperty(value = "就诊人")
        private String patientName;

        @ApiModelProperty(value = "服务日期")
        private String serviceDate;

        @ApiModelProperty(value = "时间段")
        private String serviceTimeSlot;
    }
}
