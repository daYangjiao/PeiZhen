package org.example.model.response;

import lombok.Data;
import org.example.model.Attendant;
import org.example.model.AttendantQualification;
import org.example.model.User;

import java.util.List;

@Data
public class AdminAttendantDetailResponse {

    private User user;

    private Attendant attendant;

    private AttendantQualification qualification;

    private Long totalOrderCount;

    private Long completedOrderCount;

    private List<AdminOrderListItemResponse> recentOrders;

    private List<AdminAttendantQualificationLogResponse> qualificationLogs;
}
