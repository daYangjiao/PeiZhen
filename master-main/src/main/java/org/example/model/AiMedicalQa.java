package org.example.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiMedicalQa {
    private Long id;
    private String conversationId;
    private String question;
    private String answer;
    private Integer qaStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
    // 生成对应的getter/setter
    // AiMedicalQa 模型需新增字段（思考过程）
    private String thinkingProcess; // 思考过程文本
}