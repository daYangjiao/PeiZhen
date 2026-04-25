package org.example.model.response;

import lombok.Data;

import java.util.Date;

@Data
public class AdminAttendantQualificationLogResponse {

    private Long id;

    private Integer userId;

    private String action;

    private Integer fromStatus;

    private Integer toStatus;

    private String reason;

    private Date createTime;

    private String operatorName;

    private String operatorPhoneMasked;

    private String operatorRole;
}
