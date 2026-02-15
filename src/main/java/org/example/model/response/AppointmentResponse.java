package org.example.model.response;

import lombok.Data;

/**
 * 预约响应对象 - 返回预约编号
 */
@Data
public class AppointmentResponse {
    private String appointmentNo;
    private String message;
}