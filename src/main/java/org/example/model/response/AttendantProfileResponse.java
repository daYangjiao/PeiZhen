package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(description = "陪诊师个人中心资料响应")
public class AttendantProfileResponse {

    @ApiModelProperty("用户ID")
    private Integer id;

    @ApiModelProperty("登录用户名")
    private String username;

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

    @ApiModelProperty("资质状态码：0=待审核,1=已审核,2=封禁,3=审核失败")
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

    @ApiModelProperty("身份证背面文件地址")
    private String idCardBackFileUrl;

    @ApiModelProperty("执业证书文件地址")
    private String practiceCertFileUrl;

    @ApiModelProperty("健康证文件地址")
    private String healthCertFileUrl;

    @ApiModelProperty("今日服务次数")
    private Integer todayService;

    @ApiModelProperty("本月服务次数")
    private Integer monthService;

    @ApiModelProperty("累计收入")
    private BigDecimal totalIncome;

    @ApiModelProperty("好评率(百分比整数，不含%)")
    private Integer praiseRate;

    @ApiModelProperty("钱包余额")
    private BigDecimal balance;

    @ApiModelProperty("总订单数（兼容旧版字段）")
    private Integer totalOrders;

    @ApiModelProperty("已完成订单数（兼容旧版字段）")
    private Integer completedOrders;

    @ApiModelProperty("总收入（兼容旧版字段）")
    private BigDecimal totalEarnings;
}
