package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 陪诊师匹配响应对象
 */
@Data
@ApiModel(description = "预约匹配陪诊师结果")
public class AttendantMatchResponse {
    @ApiModelProperty(value = "匹配到的陪诊师列表")
    private List<AttendantInfo> attendants;

    @ApiModelProperty(value = "预约编号", example = "APT202603200001")
    private String appointmentNo;

    @Data
    @ApiModel(description = "匹配到的陪诊师摘要信息")
    public static class AttendantInfo {
        @ApiModelProperty(value = "陪诊师用户ID", example = "21")
        private Integer id;

        @ApiModelProperty(value = "陪诊师姓名", example = "李医生")
        private String name;

        @ApiModelProperty(value = "陪诊师简介", example = "三甲医院门诊陪诊经验 7 年")
        private String introduction;

        @ApiModelProperty(value = "评分", example = "4.9")
        private Double score;

        @ApiModelProperty(value = "头像地址", example = "/uploads/attendant_21.png")
        private String photo;

        @ApiModelProperty(value = "从业年限", example = "7")
        private Integer experienceYears;

        @ApiModelProperty(value = "专业领域", example = "儿科, 呼吸科")
        private String professionalField;
    }
}
