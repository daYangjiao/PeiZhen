package org.example.model.response;

import lombok.Data;
import org.example.model.Attendant;
import org.example.model.AttendantQualification;
import org.example.model.User;

import java.util.List;

@Data
public class AdminUserDetailResponse {

    private User user;

    private Long orderCount;

    private Long completedOrderCount;

    private Attendant attendantProfile;

    private AttendantQualification qualification;

    private List<AdminOrderListItemResponse> recentOrders;
}
