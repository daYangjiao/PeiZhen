package org.example.model;

import lombok.Data;
import java.util.List;
import java.util.Date;

/**
 * 导诊预约实体
 */
@Data
public class GuideAppointment {
    private Integer id;
    private String appointmentNo;
    private Integer userId;
    private String patientName;
    private String patientPhone;
    private List<String> symptoms;
    private String hospitalName;
    private Integer serviceTypeNumber;
    private String serviceDate;
    private String serviceStartTime;
    private String serviceEndTime;
    private String otherRequirement;
    private Date createTime;
}