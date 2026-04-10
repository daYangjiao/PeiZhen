package org.example.model;

import lombok.Data;

/**
 * 陪诊师匹配结果VO
 */
@Data
public class MatchedAttendantVO {
    // 陪诊师ID
    private Integer attendantId;
    // 陪诊师姓名
    private String attendantName;
    // 陪诊师电话
    private String attendantPhone;
    // 专业领域
    private String specialty;
    // 匹配评分
    private Double score;
    // 从业经验
    private String experience;
}