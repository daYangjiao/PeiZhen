package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "可用陪诊师响应对象")
public class AvailableAttendantResponse {
    
    @ApiModelProperty(value = "陪诊师ID", example = "1")
    private Integer id;
    
    @ApiModelProperty(value = "姓名", example = "张医生")
    private String name;
    
    @ApiModelProperty(value = "年龄", example = "30")
    private Integer age;
    
    @ApiModelProperty(value = "性别", example = "男")
    private String sex;
    
    @ApiModelProperty(value = "联系电话", example = "13800138000")
    private String phone;
    
    @ApiModelProperty(value = "简介", example = "5年陪诊经验")
    private String introduction;
    
    @ApiModelProperty(value = "专业领域", example = "内科")
    private String professionalField;
    
    @ApiModelProperty(value = "服务价格", example = "198.00")
    private Double price;
    
    @ApiModelProperty(value = "评分", example = "4.8")
    private Double score;

    @ApiModelProperty(value = "评价总数", example = "36")
    private Integer evaluationCount;

    @ApiModelProperty(value = "好评率", example = "97")
    private Integer praiseRate;
    
    @ApiModelProperty(value = "所属医院", example = "人民医院")
    private String hospital;
    
    @ApiModelProperty(value = "头像URL", example = "/images/avatar.jpg")
    private String avatarUrl;
    
    @ApiModelProperty(value = "从业年限", example = "5")
    private Integer experienceYears;
    
    @ApiModelProperty(value = "可用时间段", example = "09:00-12:00,14:00-17:00")
    private String availableTimeSlots;
}
