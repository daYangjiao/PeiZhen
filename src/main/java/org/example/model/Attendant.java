package org.example.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@ApiModel(description = "陪诊师扩展信息实体")
public class Attendant {

    @ApiModelProperty(value = "关联的用户ID", required = true, example = "2")
    private Integer userId;

    @ApiModelProperty(value = "资格证书编号", example = "CERT123456")
    private String certificate;

    @ApiModelProperty(value = "状态：0=审核中, 1=正常, 2=封禁, 3=审核失败", example = "1")
    private Integer status;

    @ApiModelProperty(value = "资质审核失败原因", example = "健康证过期")
    private String qualificationFailReason;

    @ApiModelProperty(value = "个人简介", example = "从事陪诊行业5年，熟悉成都各大医院就诊流程，耐心细致。")
    private String introduction;

    @ApiModelProperty(value = "擅长领域", example = "全科陪诊,老人陪护,挂号协助")
    private String professionalField;

    @ApiModelProperty(value = "评分", example = "5.0")
    private BigDecimal score;

    @ApiModelProperty(value = "从业年限", example = "5")
    private Integer experienceYears;

    @ApiModelProperty(value = "常驻医院", example = "四川大学华西医院")
    private String hospitalName;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    private Date updateTime;
    
    @ApiModelProperty(value = "陪诊师ID")
    private Integer id;
    
    @ApiModelProperty(value = "陪诊师姓名")
    private String name;
    
    @ApiModelProperty(value = "电话号码")
    private String phone;
    
    @ApiModelProperty(value = "年龄")
    private Integer age;
    
    @ApiModelProperty(value = "性别")
    private String sex;
    
    @ApiModelProperty(value = "头像URL")
    private String avatarUrl;
    
    @ApiModelProperty(value = "价格")
    private BigDecimal price;
    
    @ApiModelProperty(value = "医院")
    private String hospital;

    @ApiModelProperty(value = "已完成服务人次")
    private Integer serviceCount;

    @ApiModelProperty(value = "评价总数")
    private Integer evaluationCount;

    @ApiModelProperty(value = "好评率")
    private Integer praiseRate;

    @ApiModelProperty(value = "账号状态：1=正常, 0=禁用")
    private Integer userStatus;
    
    // 手动添加缺失的getter方法
    public String getName() {
        return this.name;
    }
    
    public String getPhone() {
        return this.phone;
    }
    
    public String getProfessionalField() {
        return this.professionalField;
    }
    
    public Double getScore() {
        return this.score != null ? this.score.doubleValue() : null;
    }
    
    public Integer getExperienceYears() {
        return this.experienceYears;
    }
    
    public String getAvatarUrl() {
        return this.avatarUrl;
    }
    
    public Integer getId() {
        return this.id;
    }
    
    public BigDecimal getPrice() {
        return this.price;
    }
    
    public String getHospital() {
        return this.hospital;
    }
    
    public String getIntroduction() {
        return this.introduction;
    }
    
    public Integer getServiceCount() {
        return this.serviceCount;
    }

    public Integer getUserStatus() {
        return this.userStatus;
    }
}
