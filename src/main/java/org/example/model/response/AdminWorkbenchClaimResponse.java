package org.example.model.response;

import lombok.Data;

import java.util.Date;

@Data
public class AdminWorkbenchClaimResponse {

    private String taskType;

    private Integer targetId;

    private String lockToken;

    private Date expiresAt;

    private Boolean claimMine;

    private String operatorName;

    private String operatorRole;
}
