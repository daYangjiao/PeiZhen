package org.example.model;

import lombok.Data;

import java.util.Date;

@Data
public class AttendantQualificationAuditLog {

    private Long id;

    private Integer userId;

    private String actorType;

    private Integer actorId;

    private String actorName;

    private String actorPhone;

    private String actorRole;

    private String action;

    private Integer fromStatus;

    private Integer toStatus;

    private String reason;

    private String snapshotJson;

    private Date createTime;
}
