package org.example.model;

import lombok.Getter;

import javax.validation.constraints.NotBlank;

@Getter
public class MedicalQaRequest {
    // 只保留question的getter和setter
    // 移除userId和conversationId字段，只保留问题字段
    @NotBlank(message = "问题不能为空")
    private String question;

    public void setQuestion(String question) {
        this.question = question;
    }
}