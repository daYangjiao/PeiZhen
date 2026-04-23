package org.example.model.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class AdminAttendantListItemResponse {

    private Integer id;

    private String name;

    private String phone;

    private String hospitalName;

    private String professionalField;

    private Integer experienceYears;

    private BigDecimal score;

    private Integer status;

    private String statusLabel;

    private Integer userStatus;

    private String userStatusLabel;

    private Integer serviceCount;

    private Integer qualificationCompleteness;

    private Boolean idCardUploaded;

    private Boolean idCardFrontUploaded;

    private Boolean idCardBackUploaded;

    private Boolean practiceCertUploaded;

    private Boolean healthCertUploaded;

    private Boolean qualificationComplete;

    private Date createTime;

    private Date updateTime;
}
