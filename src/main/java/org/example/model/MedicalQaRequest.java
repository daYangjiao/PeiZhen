package org.example.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

import javax.validation.constraints.NotBlank;

@Getter
@ApiModel(description = "AI医疗问答请求")
public class MedicalQaRequest {
    @ApiModelProperty(value = "医疗问题内容", required = true, example = "孩子反复发烧三天，需要尽快去医院吗？")
    @NotBlank(message = "问题不能为空")
    private String question;

    public void setQuestion(String question) {
        this.question = question;
    }
}
