package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@ApiModel(description = "陪诊师个人中心资料响应")
public class AttendantProfileResponse {

    @ApiModelProperty("用户ID")
    private Integer id;

    @ApiModelProperty("姓名")
    private String name;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("头像地址")
    private String avatarUrl;

    @ApiModelProperty("实名认证证书号")
    private String certificate;

    @ApiModelProperty("评分")
    private Double score;

    @ApiModelProperty("个人简介")
    private String introduction;

    @ApiModelProperty("擅长领域")
    private String professionalField;

    @ApiModelProperty("从业年限")
    private Integer experienceYears;

    @ApiModelProperty("常驻医院")
    private String hospitalName;

    @ApiModelProperty("资质状态码：0=待审核,1=已通过,2=未通过")
    private Integer qualificationStatusCode;

    @ApiModelProperty("资质状态文案")
    private String qualificationStatusText;

    @ApiModelProperty("资质审核失败原因")
    private String qualificationFailReason;

    @ApiModelProperty("身份证是否已上传")
    private Boolean idCardUploaded;

    @ApiModelProperty("执业证书是否已上传")
    private Boolean practiceCertUploaded;

    @ApiModelProperty("健康证是否已上传")
    private Boolean healthCertUploaded;

    @ApiModelProperty("身份证文件地址")
    private String idCardFileUrl;

    @ApiModelProperty("身份证正面文件地址")
    private String idCardFrontFileUrl;

    @ApiModelProperty("身份证正面扫描预览地址")
    private String idCardFrontScanFileUrl;

    @ApiModelProperty("身份证背面文件地址")
    private String idCardBackFileUrl;

    @ApiModelProperty("身份证背面扫描预览地址")
    private String idCardBackScanFileUrl;

    @ApiModelProperty("执业证书文件地址")
    private String practiceCertFileUrl;

    @ApiModelProperty("执业证书扫描预览地址")
    private String practiceCertScanFileUrl;

    @ApiModelProperty("健康证文件地址")
    private String healthCertFileUrl;

    @ApiModelProperty("健康证扫描预览地址")
    private String healthCertScanFileUrl;

    @ApiModelProperty("执业证书有效期，格式 yyyy-MM-dd")
    private String practiceCertExpireDate;

    @ApiModelProperty("健康证有效期，格式 yyyy-MM-dd")
    private String healthCertExpireDate;

    @ApiModelProperty("执业证书是否过期")
    private Boolean practiceCertExpired;

    @ApiModelProperty("健康证是否过期")
    private Boolean healthCertExpired;

    @ApiModelProperty("资质完整度百分比")
    private Integer qualificationCompleteness;

    @ApiModelProperty("是否可接单")
    private Boolean canAcceptOrders;

    @ApiModelProperty("接单阻断原因")
    private String qualificationBlockReason;

    @ApiModelProperty("是否需要弹窗提示")
    private Boolean qualificationPopupRequired;

    @ApiModelProperty("最近资质审核记录")
    private List<AttendantQualificationLogResponse> recentQualificationLogs;

    @ApiModelProperty("今日服务次数")
    private Integer todayService;

    @ApiModelProperty("本月服务次数")
    private Integer monthService;

    @ApiModelProperty("累计收入")
    private BigDecimal totalIncome;

    @ApiModelProperty("好评率(百分比整数，不含%)")
    private Integer praiseRate;

    @ApiModelProperty("评价总数")
    private Integer evaluationCount;

    @ApiModelProperty("钱包余额")
    private BigDecimal balance;

    @ApiModelProperty("总订单数（兼容旧版字段）")
    private Integer totalOrders;

    @ApiModelProperty("已完成订单数（兼容旧版字段）")
    private Integer completedOrders;

    @ApiModelProperty("总收入（兼容旧版字段）")
    private BigDecimal totalEarnings;
}
