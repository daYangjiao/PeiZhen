package org.example.service;

import org.example.model.request.AdminAttendantReviewRequest;
import org.example.model.request.AdminAttendantStatusUpdateRequest;
import org.example.model.request.AdminOrderCancelRequest;
import org.example.model.request.AdminOrderDisputeResolutionRequest;
import org.example.model.response.*;

public interface AdminService {

    AdminDashboardOverviewResponse getDashboardOverview(Integer operatorId);

    PagedResponse<AdminUserListItemResponse> getUsers(String keyword, Integer userType, Integer status, Integer page, Integer pageSize);

    AdminUserDetailResponse getUserDetail(Integer userId);

    void updateUserStatus(Integer operatorId, Integer userId, Integer status);

    PagedResponse<AdminAttendantListItemResponse> getAttendants(String keyword, Integer auditStatus, Integer page, Integer pageSize);

    AdminAttendantDetailResponse getNextPendingAttendant(Integer operatorId, Integer excludeId);

    AdminAttendantDetailResponse getAttendantDetail(Integer operatorId, Integer userId);

    java.util.List<AdminAttendantQualificationLogResponse> getAttendantQualificationLogs(Integer operatorId, Integer userId, Integer limit);

    void updateAttendantStatus(Integer operatorId, Integer userId, Integer status, String reason);

    void reviewAttendantQualification(Integer operatorId, Integer userId, String action, String reason);

    PagedResponse<AdminOrderListItemResponse> getOrders(String keyword, Integer orderStatus, Integer paymentStatus,
                                                        String startDate, String endDate, Integer page, Integer pageSize);

    AdminOrderDetailResponse getOrderDetail(Integer operatorId, Integer orderId);

    void cancelOrder(Integer operatorId, Integer orderId, AdminOrderCancelRequest request);

    void resolveDispute(Integer operatorId, Integer orderId, AdminOrderDisputeResolutionRequest request);

    void completeDisputeRefund(Integer operatorId, Integer orderId, String adminRemark);
}
