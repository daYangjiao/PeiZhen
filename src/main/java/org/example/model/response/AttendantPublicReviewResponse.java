package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel(description = "陪诊师公开评价摘要")
public class AttendantPublicReviewResponse {

    @ApiModelProperty("订单ID")
    private Integer orderId;

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("匿名化用户昵称")
    private String reviewerName;

    @ApiModelProperty("评分")
    private Integer rating;

    @ApiModelProperty("评价内容")
    private String content;

    @ApiModelProperty("评价标签，逗号分隔")
    private String tags;

    @ApiModelProperty("陪诊师回复")
    private String attendantReply;

    @ApiModelProperty("服务日期")
    private String serviceDate;

    @ApiModelProperty("服务类型")
    private String serviceTypeName;

    @ApiModelProperty("评价时间")
    private Date createTime;
}
