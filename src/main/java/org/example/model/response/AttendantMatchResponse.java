package org.example.model.response;

import lombok.Data;
import java.util.List;

/**
 * 陪诊师匹配响应对象
 */
@Data
public class AttendantMatchResponse {
    private List<AttendantInfo> attendants;
    private String appointmentNo;
    
    @Data
    public static class AttendantInfo {
        private Integer id;
        private String name;
        private String introduction;
        private Double score;
        private String photo; // 对应avatar_url
        private Integer experienceYears;
        private String professionalField;
    }
}