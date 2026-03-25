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

    private String hospital;

    private String serviceDate;

    private String serviceTimeSlot;

    private Integer orderStatus;

    private String orderStatusLabel;

    private Integer paymentStatus;

    private String paymentStatusLabel;

    private BigDecimal orderAmount;

    private Date createTime;
}
