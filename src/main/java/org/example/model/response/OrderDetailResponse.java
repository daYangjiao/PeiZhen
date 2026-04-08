package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.example.model.Attendant;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单详情响应对象
 * 包含完整的订单信息和费用明细供用户确认
 */
@Data
@ApiModel(description = "订单详情响应对象")
public class OrderDetailResponse {

    @ApiModelProperty(value = "订单编号", example = "ORD20250905123456789")
    private String orderNo;

    @ApiModelProperty(value = "医院名称", example = "北京协和医院")
    private String hospitalName;

    @ApiModelProperty(value = "合并后的时间（日期+时间段）", example = "2025-12-01 上午9:00-11:00")
    private String serviceDateTime;

    @ApiModelProperty(value = "就诊人姓名", example = "张三")
    private String patientName;

    @ApiModelProperty(value = "症状描述", example = "发热、咳嗽")
    private String symptomDescription;

    @ApiModelProperty(value = "陪诊师信息")
    private AttendantInfo attendantInfo;

    @ApiModelProperty(value = "服务简介", example = "提供全程陪诊服务，包括挂号、候诊、取药等")
    private String serviceIntroduction;

    @ApiModelProperty(value = "用户备注", example = "患者行动不便，需要轮椅")
    private String remark;

    @ApiModelProperty(value = "总价格（元）", example = "110.00")
    private BigDecimal totalPrice;

    @ApiModelProperty(value = "费用明细列表")
    private List<FeeItem> feeDetails;

    @ApiModelProperty(value = "计算依据说明", example = "根据服务类型编号1(普通陪诊)和时长2.5小时计算")
    private String calculationBasis;

    @ApiModelProperty(value = "支付状态（0=待支付，1=已支付）", example = "0")
    private Integer paymentStatus;

    @ApiModelProperty(value = "订单状态（0=待支付，1=待接单，2=待服务，3=服务中，4=待确认时长，5=待补款，6=已完成，7=已取消）", example = "2")
    private Integer orderStatus;

    /**
     * 陪诊师信息内部类
     */
    @Data
    @ApiModel(description = "陪诊师信息")
    public static class AttendantInfo {
        @ApiModelProperty(value = "陪诊师ID", example = "1")
        private Integer id;

        @ApiModelProperty(value = "陪诊师姓名", example = "李四")
        private String name;

        @ApiModelProperty(value = "陪诊师头像URL", example = "/images/avatar1.jpg")
        private String avatarUrl;

        @ApiModelProperty(value = "陪诊师简介", example = "从事陪诊工作5年，经验丰富")
        private String introduction;

        @ApiModelProperty(value = "陪诊师评分", example = "4.8")
        private Double score;

        @ApiModelProperty(value = "所属医院", example = "XX人民医院")
        private String hospital;
    }

    /**
     * 费用明细项内部类
     */
    @Data
    @ApiModel(description = "费用明细项")
    public static class FeeItem {
        @ApiModelProperty(value = "费用项目名称", example = "起步价")
        private String itemName;

        @ApiModelProperty(value = "费用金额（元）", example = "50.00")
        private BigDecimal amount;

        @ApiModelProperty(value = "费用说明", example = "2小时最低起约时长")
        private String description;
    }
}
