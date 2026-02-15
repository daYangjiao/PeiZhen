package org.example.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 医疗问答响应（包含分段信息）
 */
@Data
public class MedicalQaResponse {
    // 主键ID
    private Long id;
    // 会话ID
    private String conversationId;
    // 提问内容
    private String question;
    // 原始完整答案
    private String answer;
    // 分段后的答案
    private List<String> segments;
    // 问答状态
    private Integer qaStatus;
    // 创建时间
    private LocalDateTime createTime;
    // 更新时间
    private LocalDateTime updateTime;
    // 思考过程
    private String thinkingProcess;

    // 添加便捷方法：判断是否已分段
    public boolean isSegmented() {
        return segments != null && segments.size() > 1;
    }

    // 添加便捷方法：获取分段数量
    public int getSegmentCount() {
        return segments != null ? segments.size() : 1;
    }
}