package org.example.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 医疗问答响应
 */
@Data
public class MedicalQaResponse {
    private Long recordId;
    private String conversationId;
    private String question;
    private String answer;
    private Integer qaStatus;
    private String processingPhase;
    private String thinkingProcess;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
