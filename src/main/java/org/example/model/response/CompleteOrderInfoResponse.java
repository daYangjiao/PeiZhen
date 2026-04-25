package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@ApiModel(description = "完整订单信息响应对象")
public class CompleteOrderInfoResponse {
    
    @ApiModelProperty(value = "订单编号", example = "ORD1707484546987a1b2c")
    private String orderNo;
    
    @ApiModelProperty(value = "订单ID", example = "1001")
    private Integer orderId;
    
    @ApiModelProperty(value = "订单状态", example = "1")
    private Integer orderStatus;
    
    @ApiModelProperty(value = "订单状态描述", example = "待接单")
    private String orderStatusDesc;
    
    @ApiModelProperty(value = "支付状态", example = "1")
    private Integer paymentStatus;
    
    @ApiModelProperty(value = "支付状态描述", example = "已支付")
    private String paymentStatusDesc;
    
    @ApiModelProperty(value = "支付时间", example = "2024-01-15 10:32:00")
    private String paymentTime;

    @ApiModelProperty(value = "接单时间", example = "2024-01-15 10:50:00")
    private String acceptTime;
    
    @ApiModelProperty(value = "总价格", example = "80.00")
    private BigDecimal totalPrice;
    
    @ApiModelProperty(value = "价格计算说明", example = "普通陪诊 2.0小时＝起步价50元+延长时间费30元")
    private String priceCalculation;
    
    // 医院和时间信息
    @ApiModelProperty(value = "医院名称", example = "北京协和医院")
    private String hospital;
    
    @ApiModelProperty(value = "服务日期", example = "2024-01-15")
    private String serviceDate;
    
    @ApiModelProperty(value = "服务时间段", example = "08:00:00-10:00:00")
    private String serviceTime;
    
    @ApiModelProperty(value = "服务类型编号", example = "1")
    private Integer serviceTypeNumber;
    
    @ApiModelProperty(value = "服务类型名称", example = "普通陪诊")
    private String serviceTypeName;
    
    // 患者信息
    @ApiModelProperty(value = "患者姓名", example = "张三")
    private String patientName;
    
    @ApiModelProperty(value = "患者电话", example = "13800138000")
    private String patientPhone;
    
    @ApiModelProperty(value = "症状列表", example = "[\"发热\", \"咳嗽\"]")
    private List<String> symptoms;

    @ApiModelProperty(value = "症状描述", example = "心内科复诊，近期胸闷头晕")
    private String symptomDescription;

    @ApiModelProperty(value = "其他需求", example = "需要轮椅协助")
    private String otherRequirement;
    
    // 陪诊师信息
    @ApiModelProperty(value = "陪诊师ID", example = "1")
    private String attendantId;
    
    @ApiModelProperty(value = "陪诊师姓名", example = "李医生")
    private String attendantName;
    
    @ApiModelProperty(value = "陪诊师头像", example = "/uploads/attendant_avatar.jpg")
    private String attendantAvatar;
    
    @ApiModelProperty(value = "陪诊师简介", example = "7年儿童陪诊经验，擅长儿科常见病诊疗")
    private String attendantIntroduction;
    
    @ApiModelProperty(value = "陪诊师评分", example = "4.8")
    private BigDecimal attendantScore;

    @ApiModelProperty(value = "陪诊师评价总数", example = "36")
    private Integer attendantEvaluationCount;

    @ApiModelProperty(value = "陪诊师好评率", example = "97")
    private Integer attendantPraiseRate;
    
    @ApiModelProperty(value = "陪诊师专业领域", example = "儿科")
    private String professionalField;
    
    @ApiModelProperty(value = "陪诊师工作经验年限", example = "7")
    private Integer experienceYears;
    
    @ApiModelProperty(value = "陪诊师联系电话", example = "13900139000")
    private String attendantPhone;
    
    // 时间戳信息
    @ApiModelProperty(value = "创建时间", example = "2024-01-15 10:30:00")
    private String createTime;
    
    @ApiModelProperty(value = "更新时间", example = "2024-01-15 10:35:00")
    private String updateTime;
    
    // 新增：核销二维码
    @ApiModelProperty(value = "核销二维码URL")
    private String qrCodeUrl;
    
    // 新增：实际服务时长
    @ApiModelProperty(value = "实际服务时长")
    private BigDecimal actualDuration;
    
    // 新增：预估服务时长
    @ApiModelProperty(value = "预估服务时长")
    private BigDecimal estimatedDuration;
    
    // 新增：差价金额
    @ApiModelProperty(value = "差价金额")
    private BigDecimal balanceAmount;
    
    // 新增：服务开始时间
    @ApiModelProperty(value = "服务开始时间")
    private String serviceStartTime;
    
    // 新增：服务结束时间
    @ApiModelProperty(value = "服务结束时间")
    private String serviceEndTime;

    // 服务进度：1=已到院, 2=候诊中, 3=检查中, 4=就诊完成
    @ApiModelProperty(value = "服务进度：1=已到院, 2=候诊中, 3=检查中, 4=就诊完成")
    private Integer serviceProgressStep;

    // 取消信息
    @ApiModelProperty(value = "取消原因")
    private String cancelReason;

    @ApiModelProperty(value = "取消时间")
    private String cancelTime;

    @ApiModelProperty(value = "取消方：0用户，1陪诊师")
    private Integer cancelBy;

    @ApiModelProperty(value = "违约金比例", example = "0.2")
    private BigDecimal penaltyRate;

    @ApiModelProperty(value = "违约金金额", example = "16.00")
    private BigDecimal penaltyAmount;

    @ApiModelProperty(value = "退款金额", example = "80.00")
    private BigDecimal refundAmount;
}
