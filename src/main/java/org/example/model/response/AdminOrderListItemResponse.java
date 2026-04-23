package org.example.model.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class AdminOrderListItemResponse {

    private Integer orderId;

    private String orderNo;

    private Integer userId;

    private String userName;

    private String userPhone;

    private Integer attendantId;

    private String attendantName;

    private String attendantPhone;

    private String patientName;

    private Integer patientAge;

    private String patientSex;

    private String contactPerson;

    private String contactPhone;

    private String hospital;

    private String serviceContent;

    private String serviceDate;

    private String serviceTimeSlot;

    private String specialRequirements;

    private Integer orderStatus;

    private String orderStatusLabel;

    private Integer paymentStatus;

    private String paymentStatusLabel;

    private BigDecimal orderAmount;

    private Date paymentTime;

    private Date acceptTime;

    private Date serviceStartTime;

    private Date serviceEndTime;

    private BigDecimal actualDuration;

    private BigDecimal balanceAmount;

    private BigDecimal refundAmount;

    private String adminRemark;

    private Date createTime;
}
