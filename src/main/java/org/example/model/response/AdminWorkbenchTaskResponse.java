package org.example.model.response;

import lombok.Data;

import java.util.Date;

@Data
public class AdminWorkbenchTaskResponse {

    private String taskType;

    private Integer targetId;

    private String title;

    private String subtitle;

    private String statusLabel;

    private Boolean claimed;

    private Boolean claimMine;

    private Date claimExpiresAt;

    private String operatorName;

    private String operatorRole;

    private AdminOrderListItemResponse order;

    private AdminAttendantListItemResponse attendant;
}
