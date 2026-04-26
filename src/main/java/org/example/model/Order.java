package org.example.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@ApiModel(description = "订单实体类")
public class Order {

    @ApiModelProperty(value = "订单ID")
    private Integer orderId;

    @ApiModelProperty(value = "订单号")
    private String orderNo;

    @ApiModelProperty(value = "下单用户ID")
    private Integer userId;

    @ApiModelProperty(value = "接单陪诊师ID")
    private Integer attendantId;

    @ApiModelProperty(value = "就诊人姓名（快照）")
    private String patientName;

    @ApiModelProperty(value = "就诊人年龄（快照）")
    private Integer patientAge;

    @ApiModelProperty(value = "就诊人性别（快照）")
    private String patientSex;
    
    @ApiModelProperty(value = "联系人姓名")
    private String contactPerson;

    @ApiModelProperty(value = "联系电话")
    private String contactPhone;

    @ApiModelProperty(value = "就诊医院")
    private String hospital;

    @ApiModelProperty(value = "服务内容/类型名称")
    private String serviceContent;

    @ApiModelProperty(value = "服务类型ID")
    private Integer clinicType;

    @ApiModelProperty(value = "服务日期")
    private String serviceDate;

    @ApiModelProperty(value = "服务时间段")
    private String serviceTimeSlot;

    @ApiModelProperty(value = "特殊需求")
    private String specialRequirements;

    @ApiModelProperty(value = "订单总额")
    private BigDecimal orderAmount;

    @ApiModelProperty(value = "支付状态：0=待支付, 1=已支付")
    private Integer paymentStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "支付时间")
    private Date paymentTime;

    @ApiModelProperty(value = "订单状态：0=待支付, 1=待接单, 2=待服务, 3=服务中, 4=待确认时长费用, 5=平台争议处理中, 6=已完成, 7=已取消, 8=专属派单待确认, 9=待用户补差额")
    private Integer orderStatus;

    @ApiModelProperty(value = "取消原因")
    private String cancelReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "取消时间")
    private Date cancelTime;

    @ApiModelProperty(value = "取消方：0=用户, 1=陪诊师, 2=系统")
    private Integer cancelBy;

    @ApiModelProperty(value = "违约金比例(0-1)")
    private BigDecimal penaltyRate;

    @ApiModelProperty(value = "违约金金额")
    private BigDecimal penaltyAmount;

    @ApiModelProperty(value = "退款金额")
    private BigDecimal refundAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    // 业务逻辑支持字段
    @ApiModelProperty(value = "二维码URL")
    private String qrCodeUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "接单时间")
    private Date acceptTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "服务开始时间")
    private Date serviceStartTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "服务结束时间")
    private Date serviceEndTime;

    @ApiModelProperty(value = "服务进度：1=已到院, 2=候诊中, 3=检查中, 4=就诊完成")
    private Integer serviceProgressStep;

    @ApiModelProperty(value = "预估服务时长(小时)")
    private BigDecimal estimatedDuration;
    
    @ApiModelProperty(value = "实际服务时长(小时)")
    private BigDecimal actualDuration;

    @ApiModelProperty(value = "陪诊师提交时长说明")
    private String attendantTimeRemark;
    
    @ApiModelProperty(value = "差价金额（正数需补付，负数自动退款）")
    private BigDecimal balanceAmount;
    
    @ApiModelProperty(value = "用户申诉的实际时长(小时)")
    private BigDecimal timeDisputeUserDuration;
    
    @ApiModelProperty(value = "用户申诉说明")
    private String timeDisputeReason;
    
    @ApiModelProperty(value = "选中选项列表")
    private java.util.List<Integer> selectedOptions;
    
    @ApiModelProperty(value = "自定义需求")
    private String customRequirement;
    
    @ApiModelProperty(value = "咨询时长")
    private BigDecimal consultationDuration;
    
    @ApiModelProperty(value = "单价")
    private BigDecimal unitPrice;
    
    @ApiModelProperty(value = "押金金额")
    private BigDecimal depositAmount;
    
    @ApiModelProperty(value = "预约下单时间")
    private Date appointmentTime;
    
    @ApiModelProperty(value = "电子病历路径")
    private String electronicMedicalRecord;
    
    @ApiModelProperty(value = "陪诊师姓名快照")
    private String attendantName;
    
    @ApiModelProperty(value = "订单日期快照")
    private Date orderDate;
    
    @ApiModelProperty(value = "核销验证码")
    private String verificationCode;

    @ApiModelProperty(value = "关联导诊单号")
    private String guideAppointmentId;

    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    @ApiModelProperty(value = "下单用户头像（仅接单后返回，待接单不返回）")
    private String userAvatar;

    @ApiModelProperty(value = "后台处理备注")
    private String adminRemark;

    @ApiModelProperty(value = "争议处理管理员ID")
    private Integer disputeResolvedBy;

    @ApiModelProperty(value = "争议处理时间")
    private Date disputeResolvedTime;

    @ApiModelProperty(value = "实际结算金额：已完成订单按最终金额，取消/退款关闭订单为0")
    private BigDecimal settlementAmount;

    @ApiModelProperty(value = "平台服务费金额")
    private BigDecimal platformFeeAmount;

    @ApiModelProperty(value = "陪诊师实际收入金额")
    private BigDecimal attendantIncomeAmount;

    // 业务方法
    public BigDecimal calculateBalance() {
        if (this.actualDuration == null || this.unitPrice == null || this.depositAmount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal actualCost = this.actualDuration.multiply(this.unitPrice);
        return actualCost.subtract(this.depositAmount);
    }

    public boolean needPayBalance() {
        BigDecimal balance = calculateBalance();
        return balance.compareTo(BigDecimal.ZERO) > 0;
    }
    
    // 兼容性方法 (用于旧代码)
    public String getUserPhone() { return this.contactPhone; }
    public void setUserPhone(String phone) { this.contactPhone = phone; }
}
