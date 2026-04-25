package org.example.model.response;

import lombok.Data;

import java.util.Date;

@Data
public class AttendantQualificationLogResponse {

    private String action;

    private Integer fromStatus;

    private Integer toStatus;

    private String reason;

    private Date createTime;
}
