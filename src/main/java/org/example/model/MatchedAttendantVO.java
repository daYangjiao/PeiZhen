package org.example.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 陪诊师匹配结果VO
 */
@Data
@ApiModel(description = "AI匹配陪诊师卡片")
public class MatchedAttendantVO {

    @ApiModelProperty(value = "陪诊师ID", example = "21")
    private Integer attendantId;

    @ApiModelProperty(value = "陪诊师姓名", example = "张敏")
    private String attendantName;

    @ApiModelProperty(value = "头像")
    private String avatar;

    @ApiModelProperty(value = "性别", example = "男")
    private String gender;

    @ApiModelProperty(value = "陪诊师电话")
    private String attendantPhone;

    @ApiModelProperty(value = "专业领域")
    private String specialty;

    @ApiModelProperty(value = "评分", example = "4.9")
    private Double score;

    @ApiModelProperty(value = "评价总数", example = "36")
    private Integer evaluationCount;

    @ApiModelProperty(value = "好评率", example = "97")
    private Integer praiseRate;

    @ApiModelProperty(value = "从业经验说明", example = "7年经验")
    private String experience;

    @ApiModelProperty(value = "从业年限", example = "7")
    private Integer experienceYears;

    @ApiModelProperty(value = "专业领域原始文本")
    private String professionalField;

    @ApiModelProperty(value = "简介")
    private String introduction;

    @ApiModelProperty(value = "已完成订单数", example = "120")
    private Integer completedOrders;

    @ApiModelProperty(value = "匹配分", example = "98")
    private Integer matchScore;

    @ApiModelProperty(value = "AI推荐理由")
    private String reason;
}
