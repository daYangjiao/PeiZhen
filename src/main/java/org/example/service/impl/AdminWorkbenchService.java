package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dao.AdminTaskClaimMapper;
import org.example.dao.AttendantMapper;
import org.example.dao.OrderMapper;
import org.example.dao.SysAdminMapper;
import org.example.entity.SysAdmin;
import org.example.model.AdminTaskClaim;
import org.example.model.Attendant;
import org.example.model.Order;
import org.example.model.request.AdminAttendantReviewRequest;
import org.example.model.request.AdminOrderDisputeResolutionRequest;
import org.example.model.request.AdminWorkbenchCompleteRequest;
import org.example.model.response.AdminAttendantListItemResponse;
import org.example.model.response.AdminOrderListItemResponse;
import org.example.model.response.AdminWorkbenchClaimResponse;
import org.example.model.response.AdminWorkbenchSummaryResponse;
import org.example.model.response.AdminWorkbenchTaskResponse;
import org.example.model.response.PagedResponse;
import org.example.service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminWorkbenchService {

    public static final String TYPE_ORDER_DISPUTE = "ORDER_DISPUTE";
    public static final String TYPE_ATTENDANT_REVIEW = "ATTENDANT_REVIEW";
    private static final long LOCK_TTL_MILLIS = 10 * 60 * 1000L;

    private final AdminTaskClaimMapper claimMapper;
    private final SysAdminMapper sysAdminMapper;
    private final OrderMapper orderMapper;
    private final AttendantMapper attendantMapper;
    private final AdminService adminService;

    public AdminWorkbenchSummaryResponse getSummary(Integer operatorId) {
        Date now = new Date();
        AdminWorkbenchSummaryResponse response = new AdminWorkbenchSummaryResponse();
        response.setDisputeOrderCount(adminService.getOrders(null, 5, null, null, null, 0, 1).getTotalElements());
        response.setAttendantReviewCount(adminService.getAttendants(null, 0, 0, 1).getTotalElements());
        response.setMyClaimCount(claimMapper.countActiveByOperator(operatorId, now));
        return response;
    }

    public PagedResponse<AdminWorkbenchTaskResponse> getTasks(Integer operatorId, String type, String keyword, Integer page, Integer pageSize) {
        String taskType = normalizeType(type);
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = pageSize == null || pageSize <= 0 ? 10 : Math.min(pageSize, 50);
        boolean showOperator = isSuperAdmin(operatorId);
        Date now = new Date();
        List<AdminWorkbenchTaskResponse> tasks = new ArrayList<>();
        long total;

        if (TYPE_ORDER_DISPUTE.equals(taskType)) {
            PagedResponse<AdminOrderListItemResponse> orders = adminService.getOrders(keyword, 5, null, null, null, safePage, safeSize);
            total = orders.getTotalElements();
            for (AdminOrderListItemResponse order : orders.getContent()) {
                AdminWorkbenchTaskResponse item = new AdminWorkbenchTaskResponse();
                item.setTaskType(TYPE_ORDER_DISPUTE);
                item.setTargetId(order.getOrderId());
                item.setTitle(order.getOrderNo());
                item.setSubtitle((order.getPatientName() == null ? "-" : order.getPatientName()) + " · " + (order.getHospital() == null ? "-" : order.getHospital()));
                item.setStatusLabel(order.getOrderStatusLabel());
                item.setOrder(order);
                applyClaim(item, claimMapper.findByTask(TYPE_ORDER_DISPUTE, order.getOrderId()), operatorId, now, showOperator);
                tasks.add(item);
            }
        } else {
            PagedResponse<AdminAttendantListItemResponse> attendants = adminService.getAttendants(keyword, 0, safePage, safeSize);
            total = attendants.getTotalElements();
            for (AdminAttendantListItemResponse attendant : attendants.getContent()) {
                AdminWorkbenchTaskResponse item = new AdminWorkbenchTaskResponse();
                item.setTaskType(TYPE_ATTENDANT_REVIEW);
                item.setTargetId(attendant.getId());
                item.setTitle(attendant.getName() == null ? "陪诊师 " + attendant.getId() : attendant.getName());
                item.setSubtitle((attendant.getPhone() == null ? "-" : attendant.getPhone()) + " · " + (attendant.getHospitalName() == null ? "未填写医院" : attendant.getHospitalName()));
                item.setStatusLabel(attendant.getStatusLabel());
                item.setAttendant(attendant);
                applyClaim(item, claimMapper.findByTask(TYPE_ATTENDANT_REVIEW, attendant.getId()), operatorId, now, showOperator);
                tasks.add(item);
            }
        }
        return new PagedResponse<>(tasks, total, safePage, safeSize);
    }

    public AdminWorkbenchClaimResponse claimTask(Integer operatorId, String type, Integer targetId) {
        String taskType = normalizeType(type);
        SysAdmin admin = requireAdmin(operatorId);
        Date now = new Date();
        AdminTaskClaim desired = new AdminTaskClaim();
        desired.setTaskType(taskType);
        desired.setTargetId(targetId);
        desired.setOperatorId(operatorId);
        desired.setOperatorName(admin.getName());
        desired.setOperatorRole(admin.getRole());
        desired.setLockToken(UUID.randomUUID().toString());
        desired.setClaimedAt(now);
        desired.setExpiresAt(new Date(now.getTime() + LOCK_TTL_MILLIS));
        desired.setUpdatedAt(now);

        claimMapper.tryClaim(desired, now);
        AdminTaskClaim current = claimMapper.findByTask(taskType, targetId);
        if (current == null || !operatorId.equals(current.getOperatorId()) || !desired.getLockToken().equals(current.getLockToken())) {
            throw new IllegalStateException("任务正在处理中");
        }
        return toClaimResponse(current, true, true);
    }

    public void releaseTask(Integer operatorId, String type, Integer targetId) {
        claimMapper.releaseByOwner(normalizeType(type), targetId, operatorId);
    }

    @Transactional
    public void completeTask(Integer operatorId, String type, Integer targetId, AdminWorkbenchCompleteRequest request) {
        String taskType = normalizeType(type);
        requireActiveLock(operatorId, taskType, targetId, request == null ? null : request.getLockToken());
        if (TYPE_ORDER_DISPUTE.equals(taskType)) {
            completeOrderDispute(operatorId, targetId, request);
        } else {
            completeAttendantReview(operatorId, targetId, request);
        }
        claimMapper.deleteByTask(taskType, targetId);
    }

    private void completeOrderDispute(Integer operatorId, Integer orderId, AdminWorkbenchCompleteRequest request) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null || order.getOrderStatus() == null || order.getOrderStatus() != 5) {
            throw new IllegalStateException("任务状态已变化，请刷新队列");
        }
        AdminOrderDisputeResolutionRequest disputeRequest = new AdminOrderDisputeResolutionRequest();
        disputeRequest.setFinalDuration(request == null ? null : request.getFinalDuration());
        disputeRequest.setFinalOrderAmount(request == null ? null : request.getFinalOrderAmount());
        disputeRequest.setAdminRemark(request == null ? null : request.getAdminRemark());
        adminService.resolveDispute(operatorId, orderId, disputeRequest);
    }

    private void completeAttendantReview(Integer operatorId, Integer userId, AdminWorkbenchCompleteRequest request) {
        Attendant attendant = attendantMapper.findByUserId(userId);
        if (attendant == null || attendant.getStatus() == null || attendant.getStatus() != 0) {
            throw new IllegalStateException("任务状态已变化，请刷新队列");
        }
        AdminAttendantReviewRequest reviewRequest = new AdminAttendantReviewRequest();
        String action = request == null || request.getAction() == null || request.getAction().isBlank()
                ? "approve"
                : request.getAction().trim();
        if (!"approve".equals(action) && !"reject".equals(action)) {
            throw new IllegalArgumentException("工作台仅支持通过或驳回待审陪诊师");
        }
        reviewRequest.setAction(action);
        reviewRequest.setReason(request == null ? null : request.getReason());
        adminService.reviewAttendantQualification(operatorId, userId, reviewRequest.getAction(), reviewRequest.getReason());
    }

    private AdminTaskClaim requireActiveLock(Integer operatorId, String taskType, Integer targetId, String lockToken) {
        AdminTaskClaim claim = claimMapper.findByTask(taskType, targetId);
        if (claim == null) {
            throw new IllegalStateException("任务未领取");
        }
        if (!operatorId.equals(claim.getOperatorId())) {
            throw new SecurityException("任务已被其他管理员领取");
        }
        if (lockToken == null || lockToken.isBlank() || !lockToken.equals(claim.getLockToken()) || isExpired(claim, new Date())) {
            throw new IllegalStateException("任务锁已失效，请重新领取");
        }
        return claim;
    }

    private void applyClaim(AdminWorkbenchTaskResponse item, AdminTaskClaim claim, Integer operatorId, Date now, boolean showOperator) {
        boolean active = claim != null && !isExpired(claim, now);
        item.setClaimed(active);
        item.setClaimMine(active && operatorId != null && operatorId.equals(claim.getOperatorId()));
        if (active) {
            item.setClaimExpiresAt(claim.getExpiresAt());
            if (showOperator) {
                item.setOperatorName(claim.getOperatorName());
                item.setOperatorRole(claim.getOperatorRole());
            }
        }
    }

    private AdminWorkbenchClaimResponse toClaimResponse(AdminTaskClaim claim, boolean showOperator, boolean mine) {
        AdminWorkbenchClaimResponse response = new AdminWorkbenchClaimResponse();
        response.setTaskType(claim.getTaskType());
        response.setTargetId(claim.getTargetId());
        response.setLockToken(claim.getLockToken());
        response.setExpiresAt(claim.getExpiresAt());
        response.setClaimMine(mine);
        if (showOperator) {
            response.setOperatorName(claim.getOperatorName());
            response.setOperatorRole(claim.getOperatorRole());
        }
        return response;
    }

    private boolean isExpired(AdminTaskClaim claim, Date now) {
        return claim.getExpiresAt() == null || !claim.getExpiresAt().after(now);
    }

    private String normalizeType(String type) {
        if (TYPE_ORDER_DISPUTE.equals(type) || TYPE_ATTENDANT_REVIEW.equals(type)) {
            return type;
        }
        throw new IllegalArgumentException("不支持的工作台任务类型");
    }

    private SysAdmin requireAdmin(Integer operatorId) {
        SysAdmin admin = operatorId == null ? null : sysAdminMapper.findById(operatorId);
        if (admin == null || (admin.getStatus() != null && admin.getStatus() == 0)) {
            throw new SecurityException("管理员不存在或已禁用");
        }
        return admin;
    }

    private boolean isSuperAdmin(Integer operatorId) {
        SysAdmin admin = operatorId == null ? null : sysAdminMapper.findById(operatorId);
        return admin != null && "SUPER_ADMIN".equals(admin.getRole());
    }
}
