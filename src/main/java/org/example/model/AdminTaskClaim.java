package org.example.model;

import lombok.Data;

import java.util.Date;

@Data
public class AdminTaskClaim {

    private String taskType;

    private Integer targetId;

    private Integer operatorId;

    private String operatorName;

    private String operatorRole;

    private String lockToken;

    private Date claimedAt;

    private Date expiresAt;

    private Date updatedAt;
}
