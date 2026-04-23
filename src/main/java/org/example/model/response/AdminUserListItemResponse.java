package org.example.model.response;

import lombok.Data;

import java.util.Date;

@Data
public class AdminUserListItemResponse {

    private Integer id;

    private String name;

    private String phone;

    private Integer userType;

    private String userTypeLabel;

    private Integer status;

    private String statusLabel;

    private String avatar;

    private Date createTime;

    private long orderCount;

    private long completedOrderCount;

    private Integer attendantAuditStatus;

    private String attendantAuditStatusLabel;

    private Boolean attendantProfileCompleted;

    private String attendantHospitalName;

    private String attendantProfessionalField;

    private Integer attendantExperienceYears;

    private Integer qualificationCompleteness;
}
