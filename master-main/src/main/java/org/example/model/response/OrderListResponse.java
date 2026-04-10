package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单列表响应对象
 * 专为订单列表页面设计的精简响应模型
 */
@Data
@ApiModel(description = "订单列表响应对象")
public class OrderListResponse {

    @ApiModelProperty(value = "订单ID", example = "1001")
    private Integer orderId;

    @ApiModelProperty(value = "订单编号", example = "ORD20250905123456789")
    private String orderNo;

    @ApiModelProperty(value = "医院名称", example = "北京协和医院")
    private String hospital;

    @ApiModelProperty(value = "就诊人姓名", example = "张三")
    private String patientName;

    @ApiModelProperty(value = "就诊人年龄", example = "30")
    private Integer patientAge;

    @ApiModelProperty(value = "就诊人性别", example = "男")
    private String patientSex;

    @ApiModelProperty(value = "服务日期", example = "2025-12-01")
    private String serviceDate;

    @ApiModelProperty(value = "服务时间段", example = "上午9:00-11:00")
    private String serviceTimeSlot;

    @ApiModelProperty(value = "服务类型名称", example = "普通陪诊")
    private String serviceTypeName;

    @ApiModelProperty(value = "订单状态", example = "0")
    private Integer orderStatus;

    @ApiModelProperty(value = "订单状态描述", example = "待接单")
    private String orderStatusDesc;

    @ApiModelProperty(value = "支付状态", example = "0")
    private Integer paymentStatus;

    @ApiModelProperty(value = "支付状态描述", example = "待支付")
    private String paymentStatusDesc;

    @ApiModelProperty(value = "订单金额", example = "110.00")
    private BigDecimal orderAmount;

    @ApiModelProperty(value = "陪诊师姓名", example = "李四")
    private String attendantName;
    
    @ApiModelProperty(value = "陪诊师头像", example = "/uploads/avatar.jpg")
    private String attendantAvatar;

    @ApiModelProperty(value = "创建时间", example = "2025-02-10 15:30:00")
    private Date createTime;

    @ApiModelProperty(value = "预约时间", example = "2025-12-01 09:00:00")
    private Date appointmentTime;
}