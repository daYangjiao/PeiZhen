package org.example.model;

import lombok.Data;

import java.util.Date;

@Data
public class AiAppointmentMessageRecord {
    private Long id;
    private String sessionId;
    private String role;
    private String content;
    private String fieldPatchJson;
    private Integer deleted;
    private Date createTime;
    private Date updateTime;
}
