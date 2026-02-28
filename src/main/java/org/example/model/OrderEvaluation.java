package org.example.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel(description = "订单评价实体")
public class OrderEvaluation {

    @ApiModelProperty("主键ID")
    private Integer id;

    @ApiModelProperty("订单ID")
    private Integer orderId;

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("评价用户ID")
    private Integer userId;

    @ApiModelProperty("陪诊师ID")
    private Integer attendantId;

    @ApiModelProperty("总体评分（1-5星）")
    private Integer rating;

    @ApiModelProperty("服务亮点标签，逗号分隔")
    private String tags;

    @ApiModelProperty("评价内容")
    private String content;

    @ApiModelProperty("陪诊师回复内容")
    private String attendantReply;

    @ApiModelProperty("陪诊师回复时间")
    private Date replyTime;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("更新时间")
    private Date updateTime;
}

