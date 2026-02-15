package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 简化版订单详情响应对象
 * 按照用户要求的字段格式返回
 */
@Data
@ApiModel(description = "简化版订单详情响应对象")
public class SimpleOrderDetailResponse {

    @ApiModelProperty(value = "医院名称", example = "北京协和医院")
    private String hospital;

    @ApiModelProperty(value = "其他需求", example = "需要轮椅协助")
    private String otherRequirement;

    @ApiModelProperty(value = "就诊人姓名", example = "张三")
    private String patientName;

    @ApiModelProperty(value = "就诊人电话", example = "13800138000")
    private String patientPhone;

    @ApiModelProperty(value = "服务日期", example = "2025-12-01")
    private String serviceDate;

    @ApiModelProperty(value = "就诊时间", example = "上午9:00-11:00")
    private String serviceTime;

    @ApiModelProperty(value = "服务类型编号", example = "1")
    private Integer serviceTypeNumber;

    @ApiModelProperty(value = "症状列表", example = "[\"发热\", \"咳嗽\"]")
    private List<String> symptoms;

    @ApiModelProperty(value = "陪诊师姓名", example = "李四")
    private String attendantName;

    @ApiModelProperty(value = "陪诊师头像URL", example = "/images/avatar1.jpg")
    private String attendantAvatar;

    @ApiModelProperty(value = "陪诊师简介", example = "从事陪诊工作5年，经验丰富")
    private String attendantIntroduction;

    @ApiModelProperty(value = "总价格（元）", example = "110.00")
    private BigDecimal totalPrice;

    @ApiModelProperty(value = "价格计算说明", example = "普通陪诊2小时，起步价50元")
    private String priceCalculation;
}