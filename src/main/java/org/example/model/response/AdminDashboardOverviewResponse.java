package org.example.model.response;

import lombok.Data;

import java.util.List;

@Data
public class AdminDashboardOverviewResponse {

    private long totalUsers;

    private long totalPatientUsers;

    private long totalAttendants;

    private long pendingAttendantReviews;

    private long totalOrders;

    private long todayOrders;

    private long disputeOrders;

    private long pendingDisputeOrders;

    private long todayOperationCount;

    private List<AdminOrderListItemResponse> recentOrders;

    private List<AdminOperationLogResponse> recentOperationLogs;
}
