package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dao.AttendantQualificationAuditLogMapper;
import org.example.dao.AttendantMapper;
import org.example.dao.AttendantQualificationMapper;
import org.example.dao.OrderEvaluationMapper;
import org.example.dao.OrderMapper;
import org.example.dao.SysAdminMapper;
import org.example.dao.UserMapper;
import org.example.entity.SysAdmin;
import org.example.model.Attendant;
import org.example.model.AttendantQualification;
import org.example.model.AttendantQualificationAuditLog;
import org.example.model.Order;
import org.example.model.User;
import org.example.model.request.AdminOrderCancelRequest;
import org.example.model.request.AdminOrderDisputeResolutionRequest;
import org.example.model.request.OrderListQueryRequest;
import org.example.model.response.*;
import org.example.service.AdminService;
import org.example.service.OrderService;
import org.example.util.AttendantQualificationPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final AttendantMapper attendantMapper;
    private final AttendantQualificationMapper attendantQualificationMapper;
    private final AttendantQualificationAuditLogMapper auditLogMapper;
    private final SysAdminMapper sysAdminMapper;
    private final OrderMapper orderMapper;
    private final OrderEvaluationMapper orderEvaluationMapper;
    private final OrderService orderService;

    @Autowired(required = false)
    private AdminOperationLogService operationLogService;

    @Override
    public AdminDashboardOverviewResponse getDashboardOverview(Integer operatorId) {
        AdminDashboardOverviewResponse response = new AdminDashboardOverviewResponse();
        response.setTotalUsers(userMapper.countAdminUsers(null, null, null));
        response.setTotalAttendants(userMapper.countByUserType(1));
        response.setPendingAttendantReviews(attendantMapper.countAdminAttendants(null, 0));
        response.setTotalOrders(orderMapper.countAllOrders(new OrderListQueryRequest()));

        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        OrderListQueryRequest todayQuery = new OrderListQueryRequest();
        todayQuery.setStartDate(today);
        todayQuery.setEndDate(today);
        response.setTodayOrders(orderMapper.countAllOrders(todayQuery));

        OrderListQueryRequest disputeQuery = new OrderListQueryRequest();
        disputeQuery.setOrderStatus(5);
        response.setDisputeOrders(orderMapper.countAllOrders(disputeQuery));
        response.setPendingDisputeOrders(response.getDisputeOrders());
        response.setRecentOrders(getOrders(null, null, null, null, null, 0, 5).getContent());
        if (operationLogService != null) {
            response.setTodayOperationCount(operationLogService.countTodayLogs(operatorId, today));
            response.setRecentOperationLogs(operationLogService.getDashboardRecentLogs(operatorId, 6));
        }
        return response;
    }

    @Override
    public PagedResponse<AdminUserListItemResponse> getUsers(String keyword, Integer userType, Integer status, Integer page, Integer pageSize) {
        int safePage = normalizePage(page);
        int safeSize = normalizePageSize(pageSize);
        int total = userMapper.countAdminUsers(trim(keyword), userType, status);
        List<User> users = userMapper.findAdminUsers(trim(keyword), userType, status, safePage * safeSize, safeSize);

        List<AdminUserListItemResponse> items = new ArrayList<>();
        for (User user : users) {
            AdminUserListItemResponse item = new AdminUserListItemResponse();
            item.setId(user.getId());
            item.setName(user.getName());
            item.setPhone(user.getPhone());
            item.setUserType(user.getUserType());
            item.setUserTypeLabel(mapUserType(user.getUserType()));
            item.setStatus(normalizeUserStatus(user.getStatus()));
            item.setStatusLabel(mapUserStatus(user.getStatus()));
            item.setAvatar(user.getAvatar());
            item.setCreateTime(user.getCreateTime());
            item.setOrderCount(orderMapper.countUserOrders(user.getId(), new OrderListQueryRequest()));
            item.setCompletedOrderCount(orderMapper.countUserOrders(user.getId(), buildCompletedOrderQuery()));
            if (user.getUserType() != null && user.getUserType() == 1) {
                Attendant attendant = attendantMapper.findByUserId(user.getId());
                AttendantQualification qualification = attendantQualificationMapper.findByUserId(user.getId());
                item.setQualificationCompleteness(calculateQualificationCompleteness(qualification));
                if (attendant != null) {
                    Integer qualificationStatus = qualificationStatus(attendant);
                    item.setAttendantAuditStatus(qualificationStatus);
                    item.setAttendantAuditStatusLabel(mapAttendantStatus(qualificationStatus));
                    item.setAttendantProfileCompleted(isAttendantProfileCompleted(attendant));
                    item.setAttendantHospitalName(attendant.getHospitalName());
                    item.setAttendantProfessionalField(attendant.getProfessionalField());
                    item.setAttendantExperienceYears(attendant.getExperienceYears());
                }
            }
            items.add(item);
        }
        return new PagedResponse<>(items, total, safePage, safeSize);
    }

    @Override
    public AdminUserDetailResponse getUserDetail(Integer userId) {
        User user = requireUser(userId);
        AdminUserDetailResponse response = new AdminUserDetailResponse();
        response.setUser(user);
        response.setOrderCount((long) orderMapper.countUserOrders(userId, new OrderListQueryRequest()));

        OrderListQueryRequest completedQuery = new OrderListQueryRequest();
        completedQuery.setOrderStatus(6);
        response.setCompletedOrderCount((long) orderMapper.countUserOrders(userId, completedQuery));
        response.setRecentOrders(loadRecentUserOrders(userId));

        if (user.getUserType() != null && user.getUserType() == 1) {
            response.setAttendantProfile(applyRatingSummary(attendantMapper.findByUserId(userId)));
            response.setQualification(attendantQualificationMapper.findByUserId(userId));
        }
        return response;
    }

    @Override
    @Transactional
    public void updateUserStatus(Integer operatorId, Integer userId, Integer status) {
        User user = requireUser(userId);
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("状态值不合法");
        }
        User patch = new User();
        patch.setId(userId);
        patch.setStatus(status);
        userMapper.update(patch);
        recordOperation(operatorId, "USER", status == 1 ? "ENABLE_USER" : "DISABLE_USER", "USER",
                userId, buildUserTargetLabel(userId), normalizeUserStatus(user.getStatus()), status, null, "{}");
    }

    @Override
    public PagedResponse<AdminAttendantListItemResponse> getAttendants(String keyword, Integer auditStatus, Integer page, Integer pageSize) {
        int safePage = normalizePage(page);
        int safeSize = normalizePageSize(pageSize);
        int total = attendantMapper.countAdminAttendants(trim(keyword), auditStatus);
        List<Attendant> attendants = attendantMapper.findAdminAttendants(trim(keyword), auditStatus, safePage * safeSize, safeSize);

        List<AdminAttendantListItemResponse> items = new ArrayList<>();
        for (Attendant attendant : attendants) {
            applyRatingSummary(attendant);
            AdminAttendantListItemResponse item = new AdminAttendantListItemResponse();
            item.setId(attendant.getUserId());
            item.setName(attendant.getName());
            item.setPhone(attendant.getPhone());
            item.setHospitalName(attendant.getHospitalName());
            item.setProfessionalField(attendant.getProfessionalField());
            item.setExperienceYears(attendant.getExperienceYears());
            item.setScore(attendant.getScore() == null ? null : BigDecimal.valueOf(attendant.getScore()));
            item.setEvaluationCount(attendant.getEvaluationCount() == null ? 0 : attendant.getEvaluationCount());
            item.setPraiseRate(attendant.getPraiseRate() == null ? 0 : attendant.getPraiseRate());
            Integer qualificationStatus = qualificationStatus(attendant);
            item.setStatus(qualificationStatus);
            item.setStatusLabel(mapAttendantStatus(qualificationStatus));
            item.setUserStatus(attendant.getUserStatus());
            item.setUserStatusLabel(mapUserStatus(attendant.getUserStatus()));
            item.setServiceCount(attendant.getServiceCount());
            applyQualificationSummary(item, attendantQualificationMapper.findByUserId(attendant.getUserId()));
            item.setCreateTime(attendant.getCreateTime());
            item.setUpdateTime(attendant.getUpdateTime());
            items.add(item);
        }
        return new PagedResponse<>(items, total, safePage, safeSize);
    }

    @Override
    public AdminAttendantDetailResponse getNextPendingAttendant(Integer operatorId, Integer excludeId) {
        Integer userId = attendantMapper.findNextPendingUserId(excludeId);
        if (userId == null) {
            return null;
        }
        return getAttendantDetail(operatorId, userId);
    }

    @Override
    public AdminAttendantDetailResponse getAttendantDetail(Integer operatorId, Integer userId) {
        User user = requireUser(userId);
        Attendant attendant = applyRatingSummary(attendantMapper.findByUserId(userId));
        if (attendant == null) {
            throw new IllegalArgumentException("陪诊师不存在");
        }
        attendant.setStatus(qualificationStatus(attendant));

        AdminAttendantDetailResponse response = new AdminAttendantDetailResponse();
        response.setUser(user);
        response.setAttendant(attendant);
        response.setQualification(attendantQualificationMapper.findByUserId(userId));

        List<Order> orders = orderMapper.findAllOrdersWithPagination(new OrderListQueryRequest(), 0, 1000);
        long totalOrderCount = orders.stream().filter(order -> userId.equals(order.getAttendantId())).count();
        long completedOrderCount = orders.stream()
                .filter(order -> userId.equals(order.getAttendantId()) && order.getOrderStatus() != null && order.getOrderStatus() == 6)
                .count();
        response.setTotalOrderCount(totalOrderCount);
        response.setCompletedOrderCount(completedOrderCount);

        List<Order> recentOrders = new ArrayList<>();
        for (Order order : orders) {
            if (userId.equals(order.getAttendantId())) {
                recentOrders.add(order);
            }
            if (recentOrders.size() >= 5) {
                break;
            }
        }
        response.setRecentOrders(toAdminOrderItems(recentOrders));
        response.setQualificationLogs(getAttendantQualificationLogs(operatorId, userId, 10));
        return response;
    }

    @Override
    public List<AdminAttendantQualificationLogResponse> getAttendantQualificationLogs(Integer operatorId, Integer userId, Integer limit) {
        SysAdmin operator = operatorId == null ? null : sysAdminMapper.findById(operatorId);
        boolean showOperator = operator != null && "SUPER_ADMIN".equals(operator.getRole());
        int safeLimit = limit == null || limit <= 0 ? 10 : Math.min(limit, 50);
        List<AttendantQualificationAuditLog> logs = auditLogMapper.findLatestByUserId(userId, safeLimit);
        List<AdminAttendantQualificationLogResponse> responses = new ArrayList<>();
        for (AttendantQualificationAuditLog log : logs) {
            AdminAttendantQualificationLogResponse item = new AdminAttendantQualificationLogResponse();
            item.setId(log.getId());
            item.setUserId(log.getUserId());
            item.setAction(log.getAction());
            item.setFromStatus(log.getFromStatus());
            item.setToStatus(log.getToStatus());
            item.setReason(log.getReason());
            item.setCreateTime(log.getCreateTime());
            if (showOperator) {
                item.setOperatorName(log.getActorName());
                item.setOperatorPhoneMasked(maskPhone(log.getActorPhone()));
                item.setOperatorRole(log.getActorRole());
            }
            responses.add(item);
        }
        return responses;
    }

    @Override
    @Transactional
    public void updateAttendantStatus(Integer operatorId, Integer userId, Integer status, String reason) {
        User user = requireUser(userId);
        Attendant attendant = attendantMapper.findByUserId(userId);
        if (attendant == null) {
            throw new IllegalArgumentException("陪诊师不存在");
        }
        Integer targetStatus = normalizeAccountStatus(status);
        if (targetStatus == null) {
            throw new IllegalArgumentException("只支持设置为账号正常或禁用");
        }
        Integer previousStatus = normalizeUserStatus(user.getStatus());
        User patch = new User();
        patch.setId(userId);
        patch.setStatus(targetStatus);
        userMapper.update(patch);
        writeAdminQualificationLog(operatorId, userId, targetStatus == 1 ? "RESTORE" : "BAN", previousStatus, targetStatus, reason);
    }

    @Override
    @Transactional
    public void reviewAttendantQualification(Integer operatorId, Integer userId, String action, String reason) {
        requireUser(userId);
        Attendant attendant = attendantMapper.findByUserId(userId);
        if (attendant == null) {
            throw new IllegalArgumentException("陪诊师不存在");
        }

        Attendant patch = new Attendant();
        patch.setUserId(userId);
        String normalizedAction = validateReviewAction(action, reason);
        if ("approve".equals(normalizedAction) || "restore".equals(normalizedAction)) {
            AttendantQualification qualification = attendantQualificationMapper.findByUserId(userId);
            if (!AttendantQualificationPolicy.isComplete(qualification)) {
                throw new IllegalArgumentException("资质材料不完整，不能通过审核");
            }
            AttendantQualificationPolicy.requireSubmittable(qualification);
        }
        Integer previousStatus = qualificationStatus(attendant);
        if ("approve".equals(normalizedAction) || "restore".equals(normalizedAction)) {
            patch.setQualificationStatus(1);
            patch.setQualificationFailReason("");
        } else if ("reject".equals(normalizedAction)) {
            patch.setQualificationStatus(2);
            patch.setQualificationFailReason(reason.trim());
        }
        attendantMapper.update(patch);
        writeAdminQualificationLog(operatorId, userId, normalizedAction.toUpperCase(), previousStatus, patch.getQualificationStatus(), reason);
    }

    @Override
    public PagedResponse<AdminOrderListItemResponse> getOrders(String keyword, Integer orderStatus, Integer paymentStatus,
                                                               String startDate, String endDate, Integer page, Integer pageSize) {
        int safePage = normalizePage(page);
        int safeSize = normalizePageSize(pageSize);
        OrderListQueryRequest query = new OrderListQueryRequest();
        query.setKeyword(trim(keyword));
        query.setOrderStatus(orderStatus);
        query.setPaymentStatus(paymentStatus);
        query.setStartDate(trim(startDate));
        query.setEndDate(trim(endDate));
        query.setPage(safePage);
        query.setSize(safeSize);

        int total = orderMapper.countAllOrders(query);
        List<Order> orders = orderMapper.findAllOrdersWithPagination(query, safePage * safeSize, safeSize);
        return new PagedResponse<>(toAdminOrderItems(orders), total, safePage, safeSize);
    }

    @Override
    public AdminOrderDetailResponse getOrderDetail(Integer operatorId, Integer orderId) {
        Order order = requireOrder(orderId);
        AdminOrderDetailResponse response = new AdminOrderDetailResponse();
        response.setOrder(order);
        response.setUser(order.getUserId() == null ? null : userMapper.findById(order.getUserId()));
        response.setAttendant(order.getAttendantId() == null ? null : userMapper.findById(order.getAttendantId()));
        if (isSuperAdmin(operatorId) && order.getDisputeResolvedBy() != null) {
            SysAdmin resolver = sysAdminMapper.findById(order.getDisputeResolvedBy());
            if (resolver != null) {
                response.setDisputeResolverName(resolver.getName());
                response.setDisputeResolverPhoneMasked(maskPhone(resolver.getPhone()));
                response.setDisputeResolverRole(resolver.getRole());
            }
        }
        return response;
    }

    @Override
    @Transactional
    public void cancelOrder(Integer operatorId, Integer orderId, AdminOrderCancelRequest request) {
        Order order = requireOrder(orderId);
        if (order.getOrderStatus() != null && (order.getOrderStatus() == 6 || order.getOrderStatus() == 7)) {
            throw new IllegalArgumentException("当前订单状态不允许取消");
        }
        String reason = request != null && hasText(request.getReason()) ? request.getReason().trim() : "管理员取消订单";
        BigDecimal refundAmount = request != null && request.getRefundAmount() != null
                ? request.getRefundAmount()
                : (order.getPaymentStatus() != null && order.getPaymentStatus() == 1
                ? (order.getOrderAmount() == null ? BigDecimal.ZERO : order.getOrderAmount())
                : BigDecimal.ZERO);
        Integer previousStatus = order.getOrderStatus();

        Order patch = new Order();
        patch.setOrderId(orderId);
        patch.setOrderStatus(7);
        patch.setCancelBy(2);
        patch.setCancelReason(reason);
        patch.setCancelTime(new Date());
        patch.setPenaltyAmount(BigDecimal.ZERO);
        patch.setPenaltyRate(BigDecimal.ZERO);
        patch.setRefundAmount(refundAmount);
        if (request != null) {
            patch.setAdminRemark(trim(request.getAdminRemark()));
        }
        orderMapper.updateByPrimaryKeySelective(patch);
        order.setOrderStatus(7);
        order.setCancelBy(2);
        order.setCancelReason(reason);
        order.setCancelTime(patch.getCancelTime());
        order.setPenaltyAmount(BigDecimal.ZERO);
        order.setPenaltyRate(BigDecimal.ZERO);
        order.setRefundAmount(refundAmount);
        orderService.notifyOrderParties(
                order,
                "您的订单" + (order.getOrderNo() != null ? order.getOrderNo() : "") + "已取消。取消原因：" + reason,
                order.getAttendantId() != null
                        ? "订单 " + (order.getOrderNo() != null ? order.getOrderNo() : "") + " 已被管理员取消。取消原因：" + reason
                        : null
        );
        orderService.publishOrderEvent(order, "ORDER_STATUS_CHANGED", null, null, true, true);
        if (previousStatus != null && previousStatus == 1) {
            orderService.broadcastWaitingOrderUpdate(order);
        }
        recordOperation(operatorId, "ORDER", "CANCEL_ORDER", "ORDER", orderId, order.getOrderNo(),
                previousStatus, 7, reason, buildOrderSnapshot(order));
    }

    @Override
    @Transactional
    public void resolveDispute(Integer operatorId, Integer orderId, AdminOrderDisputeResolutionRequest request) {
        Order order = requireOrder(orderId);
        if (order.getOrderStatus() == null || order.getOrderStatus() != 5) {
            throw new IllegalArgumentException("只有争议订单才能处理");
        }

        BigDecimal currentAmount = order.getOrderAmount() == null ? BigDecimal.ZERO : order.getOrderAmount();
        BigDecimal finalAmount = request != null && request.getFinalOrderAmount() != null
                ? request.getFinalOrderAmount()
                : currentAmount.add(order.getBalanceAmount() == null ? BigDecimal.ZERO : order.getBalanceAmount());
        BigDecimal finalDuration = request != null && request.getFinalDuration() != null
                ? request.getFinalDuration()
                : (order.getTimeDisputeUserDuration() != null ? order.getTimeDisputeUserDuration() : order.getActualDuration());
        if (finalDuration == null || finalDuration.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("最终服务时长必须大于0");
        }
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("最终订单金额不能小于0");
        }
        if (request == null || !hasText(request.getAdminRemark())) {
            throw new IllegalArgumentException("处理备注不能为空");
        }

        BigDecimal normalizedFinalAmount = finalAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal balance = normalizedFinalAmount.subtract(currentAmount).setScale(2, RoundingMode.HALF_UP);
        Integer nextStatus = balance.compareTo(BigDecimal.ZERO) > 0 ? 9 : 6;
        Order patch = new Order();
        patch.setOrderId(orderId);
        patch.setActualDuration(finalDuration);
        patch.setOrderAmount(normalizedFinalAmount);
        patch.setBalanceAmount(balance);
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            patch.setRefundAmount(balance.abs().setScale(2, RoundingMode.HALF_UP));
        }
        patch.setOrderStatus(nextStatus);
        patch.setDisputeResolvedBy(operatorId);
        patch.setDisputeResolvedTime(new Date());
        patch.setAdminRemark(trim(request.getAdminRemark()));
        orderMapper.updateByPrimaryKeySelective(patch);
        order.setActualDuration(finalDuration);
        order.setOrderAmount(normalizedFinalAmount);
        order.setBalanceAmount(balance);
        order.setRefundAmount(patch.getRefundAmount());
        order.setOrderStatus(nextStatus);
        String userMessage = nextStatus == 9
                ? "您的订单" + (order.getOrderNo() != null ? order.getOrderNo() : "") + " 的争议已由平台处理，请完成差额支付。"
                : "您的订单" + (order.getOrderNo() != null ? order.getOrderNo() : "") + " 的争议已由平台处理，订单已完成。";
        String attendantMessage = nextStatus == 9
                ? "订单 " + (order.getOrderNo() != null ? order.getOrderNo() : "") + " 的争议已由平台处理，等待用户支付差额。"
                : "订单 " + (order.getOrderNo() != null ? order.getOrderNo() : "") + " 的争议已由平台处理，订单已完成。";
        orderService.notifyOrderParties(
                order,
                userMessage,
                order.getAttendantId() != null ? attendantMessage : null
        );
        orderService.publishOrderEvent(order, "ORDER_STATUS_CHANGED", null, null, true, true);
        recordOperation(operatorId, "ORDER", "RESOLVE_DISPUTE", "ORDER", orderId, order.getOrderNo(),
                5, nextStatus, trim(request.getAdminRemark()), buildOrderSnapshot(order));
    }

    private List<AdminOrderListItemResponse> loadRecentUserOrders(Integer userId) {
        OrderListQueryRequest query = new OrderListQueryRequest();
        query.setPage(0);
        query.setSize(5);
        List<Order> orders = orderMapper.findUserOrdersWithPagination(userId, query, 0, 5);
        return toAdminOrderItems(orders);
    }

    private List<AdminOrderListItemResponse> toAdminOrderItems(List<Order> orders) {
        List<AdminOrderListItemResponse> items = new ArrayList<>();
        for (Order order : orders) {
            AdminOrderListItemResponse item = new AdminOrderListItemResponse();
            item.setOrderId(order.getOrderId());
            item.setOrderNo(order.getOrderNo());
            item.setUserId(order.getUserId());
            User user = order.getUserId() == null ? null : userMapper.findById(order.getUserId());
            if (user != null) {
                item.setUserName(user.getName());
                item.setUserPhone(user.getPhone());
            }
            item.setAttendantId(order.getAttendantId());
            User attendant = order.getAttendantId() == null ? null : userMapper.findById(order.getAttendantId());
            if (attendant != null) {
                item.setAttendantName(attendant.getName());
                item.setAttendantPhone(attendant.getPhone());
            } else {
                item.setAttendantName(order.getAttendantName());
            }
            item.setPatientName(order.getPatientName());
            item.setPatientAge(order.getPatientAge());
            item.setPatientSex(order.getPatientSex());
            item.setContactPerson(order.getContactPerson());
            item.setContactPhone(order.getContactPhone());
            item.setHospital(order.getHospital());
            item.setServiceContent(order.getServiceContent());
            item.setServiceDate(order.getServiceDate());
            item.setServiceTimeSlot(order.getServiceTimeSlot());
            item.setSpecialRequirements(order.getSpecialRequirements());
            item.setOrderStatus(order.getOrderStatus());
            item.setOrderStatusLabel(mapOrderStatus(order.getOrderStatus()));
            item.setPaymentStatus(order.getPaymentStatus());
            item.setPaymentStatusLabel(mapPaymentStatus(order.getPaymentStatus()));
            item.setOrderAmount(order.getOrderAmount());
            item.setPaymentTime(order.getPaymentTime());
            item.setAcceptTime(order.getAcceptTime());
            item.setServiceStartTime(order.getServiceStartTime());
            item.setServiceEndTime(order.getServiceEndTime());
            item.setActualDuration(order.getActualDuration());
            item.setAttendantTimeRemark(order.getAttendantTimeRemark());
            item.setBalanceAmount(order.getBalanceAmount());
            item.setRefundAmount(order.getRefundAmount());
            item.setAdminRemark(order.getAdminRemark());
            item.setCreateTime(order.getCreateTime());
            items.add(item);
        }
        return items;
    }

    private OrderListQueryRequest buildCompletedOrderQuery() {
        OrderListQueryRequest completedQuery = new OrderListQueryRequest();
        completedQuery.setOrderStatus(6);
        return completedQuery;
    }

    private User requireUser(Integer userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        return user;
    }

    private Order requireOrder(Integer orderId) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        return order;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 10;
        }
        return Math.min(pageSize, 50);
    }

    private int normalizeUserStatus(Integer status) {
        return status == null ? 1 : status;
    }

    private Integer normalizeAccountStatus(Integer status) {
        if (status == null) {
            return null;
        }
        if (status == 1) {
            return 1;
        }
        if (status == 0 || status == 2) {
            return 0;
        }
        return null;
    }

    private Integer qualificationStatus(Attendant attendant) {
        return AttendantQualificationPolicy.qualificationStatus(attendant);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String validateReviewAction(String action, String reason) {
        String normalizedAction = trim(action);
        if (!"approve".equals(normalizedAction)
                && !"restore".equals(normalizedAction)
                && !"reject".equals(normalizedAction)) {
            throw new IllegalArgumentException("不支持的审核动作");
        }
        if ("reject".equals(normalizedAction) && !hasText(reason)) {
            throw new IllegalArgumentException("驳回原因不能为空");
        }
        return normalizedAction;
    }

    private boolean isAttendantProfileCompleted(Attendant attendant) {
        return attendant != null
                && hasText(attendant.getHospitalName())
                && hasText(attendant.getProfessionalField())
                && attendant.getExperienceYears() != null
                && hasText(attendant.getIntroduction())
                && hasText(attendant.getCertificate());
    }

    private int calculateQualificationCompleteness(AttendantQualification qualification) {
        return AttendantQualificationPolicy.completeness(qualification);
    }

    private void applyQualificationSummary(AdminAttendantListItemResponse item, AttendantQualification qualification) {
        boolean idCardFrontUploaded = qualification != null && hasText(AttendantQualificationPolicy.firstNonBlank(qualification.getIdCardFrontFileUrl(), qualification.getIdCardFileUrl()));
        boolean idCardBackUploaded = qualification != null && hasText(qualification.getIdCardBackFileUrl());
        boolean practiceCertUploaded = qualification != null && hasText(qualification.getPracticeCertFileUrl());
        boolean healthCertUploaded = qualification != null && hasText(qualification.getHealthCertFileUrl());
        boolean practiceExpired = qualification != null && AttendantQualificationPolicy.isExpired(qualification.getPracticeCertExpireDate());
        boolean healthExpired = qualification != null && AttendantQualificationPolicy.isExpired(qualification.getHealthCertExpireDate());

        item.setIdCardFrontUploaded(idCardFrontUploaded);
        item.setIdCardBackUploaded(idCardBackUploaded);
        item.setIdCardUploaded(idCardFrontUploaded && idCardBackUploaded);
        item.setPracticeCertUploaded(practiceCertUploaded);
        item.setHealthCertUploaded(healthCertUploaded);
        item.setQualificationComplete(AttendantQualificationPolicy.canAcceptOrders(activeUserStub(), activeAttendantStub(), qualification));
        item.setQualificationCompleteness(calculateQualificationCompleteness(qualification));
        item.setPracticeCertExpireDate(qualification == null ? null : qualification.getPracticeCertExpireDate());
        item.setHealthCertExpireDate(qualification == null ? null : qualification.getHealthCertExpireDate());
        item.setPracticeCertExpired(practiceExpired);
        item.setHealthCertExpired(healthExpired);
    }

    private void writeAdminQualificationLog(Integer operatorId, Integer userId, String action, Integer fromStatus, Integer toStatus, String reason) {
        SysAdmin admin = operatorId == null ? null : sysAdminMapper.findById(operatorId);
        AttendantQualification qualification = attendantQualificationMapper.findByUserId(userId);
        AttendantQualificationAuditLog log = new AttendantQualificationAuditLog();
        log.setUserId(userId);
        log.setActorType("ADMIN");
        log.setActorId(operatorId);
        if (admin != null) {
            log.setActorName(admin.getName());
            log.setActorPhone(admin.getPhone());
            log.setActorRole(admin.getRole());
        }
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setReason(trim(reason));
        log.setSnapshotJson(buildQualificationSnapshot(qualification));
        auditLogMapper.insert(log);
        recordOperation(operatorId, "ATTENDANT", action, "ATTENDANT", userId, buildUserTargetLabel(userId),
                fromStatus, toStatus, reason, log.getSnapshotJson());
    }

    private String buildUserTargetLabel(Integer userId) {
        User user = userId == null ? null : userMapper.findById(userId);
        if (user == null) {
            return userId == null ? null : String.valueOf(userId);
        }
        String name = trim(user.getName());
        String phone = trim(user.getPhone());
        if (hasText(name) && hasText(phone)) {
            return name + "（" + maskPhone(phone) + "）";
        }
        if (hasText(name)) {
            return name;
        }
        if (hasText(phone)) {
            return maskPhone(phone);
        }
        return userId == null ? null : String.valueOf(userId);
    }

    private String buildQualificationSnapshot(AttendantQualification qualification) {
        if (qualification == null) {
            return "{}";
        }
        return "{"
                + "\"idCardFrontFileUrl\":\"" + escapeJson(AttendantQualificationPolicy.firstNonBlank(qualification.getIdCardFrontFileUrl(), qualification.getIdCardFileUrl())) + "\","
                + "\"idCardFrontScanFileUrl\":\"" + escapeJson(qualification.getIdCardFrontScanFileUrl()) + "\","
                + "\"idCardBackFileUrl\":\"" + escapeJson(qualification.getIdCardBackFileUrl()) + "\","
                + "\"idCardBackScanFileUrl\":\"" + escapeJson(qualification.getIdCardBackScanFileUrl()) + "\","
                + "\"practiceCertFileUrl\":\"" + escapeJson(qualification.getPracticeCertFileUrl()) + "\","
                + "\"practiceCertScanFileUrl\":\"" + escapeJson(qualification.getPracticeCertScanFileUrl()) + "\","
                + "\"healthCertFileUrl\":\"" + escapeJson(qualification.getHealthCertFileUrl()) + "\","
                + "\"healthCertScanFileUrl\":\"" + escapeJson(qualification.getHealthCertScanFileUrl()) + "\","
                + "\"practiceCertExpireDate\":\"" + escapeJson(qualification.getPracticeCertExpireDate()) + "\","
                + "\"healthCertExpireDate\":\"" + escapeJson(qualification.getHealthCertExpireDate()) + "\""
                + "}";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String maskPhone(String phone) {
        if (!hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private boolean isSuperAdmin(Integer operatorId) {
        SysAdmin operator = operatorId == null ? null : sysAdminMapper.findById(operatorId);
        return operator != null && "SUPER_ADMIN".equals(operator.getRole());
    }

    private void recordOperation(Integer operatorId, String module, String action, String targetType, Integer targetId,
                                 String targetLabel, Integer fromStatus, Integer toStatus, String remark, String snapshotJson) {
        if (operationLogService == null) {
            return;
        }
        operationLogService.record(operatorId, module, action, targetType, targetId, targetLabel,
                fromStatus, toStatus, remark, snapshotJson);
    }

    private String buildOrderSnapshot(Order order) {
        if (order == null) {
            return "{}";
        }
        return "{"
                + "\"orderNo\":\"" + escapeJson(order.getOrderNo()) + "\","
                + "\"orderAmount\":\"" + escapeJson(order.getOrderAmount() == null ? null : order.getOrderAmount().toPlainString()) + "\","
                + "\"balanceAmount\":\"" + escapeJson(order.getBalanceAmount() == null ? null : order.getBalanceAmount().toPlainString()) + "\","
                + "\"refundAmount\":\"" + escapeJson(order.getRefundAmount() == null ? null : order.getRefundAmount().toPlainString()) + "\""
                + "}";
    }

    private User activeUserStub() {
        User user = new User();
        user.setStatus(1);
        return user;
    }

    private Attendant activeAttendantStub() {
        Attendant attendant = new Attendant();
        attendant.setQualificationStatus(1);
        return attendant;
    }

    private Attendant applyRatingSummary(Attendant attendant) {
        if (attendant == null || attendant.getUserId() == null) {
            return attendant;
        }
        Integer totalEvalCount = orderEvaluationMapper.countByAttendantId(attendant.getUserId());
        int evaluationCount = totalEvalCount == null ? 0 : totalEvalCount;
        attendant.setEvaluationCount(evaluationCount);
        if (evaluationCount <= 0) {
            attendant.setScore(null);
            attendant.setPraiseRate(0);
            return attendant;
        }
        BigDecimal score = orderEvaluationMapper.averageRatingByAttendantId(attendant.getUserId());
        Integer goodEvalCount = orderEvaluationMapper.countGoodByAttendantId(attendant.getUserId(), 4);
        attendant.setScore(score == null ? null : score.setScale(1, RoundingMode.HALF_UP));
        attendant.setPraiseRate((int) Math.round((goodEvalCount == null ? 0 : goodEvalCount) * 100.0 / evaluationCount));
        return attendant;
    }

    private String mapUserType(Integer userType) {
        if (userType == null) return "未知";
        switch (userType) {
            case 0:
                return "普通用户";
            case 1:
                return "陪诊师";
            case 2:
                return "管理员";
            default:
                return "未知";
        }
    }

    private String mapUserStatus(Integer status) {
        return normalizeUserStatus(status) == 1 ? "正常" : "禁用";
    }

    private String mapAttendantStatus(Integer status) {
        if (status == null) return "待审核";
        switch (status) {
            case 0:
                return "待审核";
            case 1:
                return "已通过";
            case 2:
            case 3:
                return "未通过";
            default:
                return "未知";
        }
    }

    private String mapOrderStatus(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0:
                return "待支付";
            case 1:
                return "待接单";
            case 2:
                return "待服务";
            case 3:
                return "服务中";
            case 4:
                return "待确认时长费用";
            case 5:
                return "平台争议处理中";
            case 6:
                return "已完成";
            case 7:
                return "已取消";
            case 8:
                return "专属派单待确认";
            case 9:
                return "待用户补差额";
            default:
                return "未知";
        }
    }

    private String mapPaymentStatus(Integer status) {
        return status != null && status == 1 ? "已支付" : "待支付";
    }
}
