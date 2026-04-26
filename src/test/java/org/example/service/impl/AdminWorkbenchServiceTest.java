package org.example.service.impl;

import org.example.dao.AdminTaskClaimMapper;
import org.example.dao.AttendantMapper;
import org.example.dao.OrderMapper;
import org.example.dao.SysAdminMapper;
import org.example.entity.SysAdmin;
import org.example.model.AdminTaskClaim;
import org.example.model.Attendant;
import org.example.model.Order;
import org.example.model.request.AdminWorkbenchCompleteRequest;
import org.example.service.AdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminWorkbenchServiceTest {

    @Mock
    private AdminTaskClaimMapper claimMapper;
    @Mock
    private SysAdminMapper sysAdminMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private AttendantMapper attendantMapper;
    @Mock
    private AdminService adminService;

    @Test
    void claimTaskShouldReturnTokenForCurrentAdminWhenAvailable() {
        AdminWorkbenchService service = service();
        SysAdmin admin = admin(1, "SUPER_ADMIN");
        AtomicReference<AdminTaskClaim> capturedClaim = new AtomicReference<>();
        when(sysAdminMapper.findById(1)).thenReturn(admin);
        Order order = new Order();
        order.setOrderId(88);
        order.setOrderStatus(5);
        when(orderMapper.selectByPrimaryKey(88)).thenReturn(order);
        doAnswer(invocation -> {
            capturedClaim.set(invocation.getArgument(0));
            return null;
        }).when(claimMapper).tryClaim(any(AdminTaskClaim.class), any(Date.class));
        when(claimMapper.findByTask("ORDER_DISPUTE", 88)).thenAnswer(invocation -> {
            AdminTaskClaim desired = capturedClaim.get();
            AdminTaskClaim claim = new AdminTaskClaim();
            claim.setTaskType("ORDER_DISPUTE");
            claim.setTargetId(88);
            claim.setOperatorId(1);
            claim.setOperatorName("管理员1");
            claim.setOperatorRole("SUPER_ADMIN");
            claim.setLockToken(desired.getLockToken());
            claim.setClaimedAt(desired.getClaimedAt());
            claim.setExpiresAt(desired.getExpiresAt());
            return claim;
        });

        var response = service.claimTask(1, "ORDER_DISPUTE", 88);

        assertThat(response.getLockToken()).isNotBlank();
        assertThat(response.getClaimMine()).isTrue();
        ArgumentCaptor<AdminTaskClaim> claimCaptor = ArgumentCaptor.forClass(AdminTaskClaim.class);
        verify(claimMapper).tryClaim(claimCaptor.capture(), any(Date.class));
        assertThat(claimCaptor.getValue().getOperatorId()).isEqualTo(1);
        assertThat(claimCaptor.getValue().getExpiresAt()).isAfter(claimCaptor.getValue().getClaimedAt());
    }

    @Test
    void claimTaskShouldRejectWhenAnotherActiveAdminAlreadyClaimed() {
        AdminWorkbenchService service = service();
        when(sysAdminMapper.findById(2)).thenReturn(admin(2, "ADMIN"));
        Attendant attendant = new Attendant();
        attendant.setUserId(66);
        attendant.setQualificationStatus(0);
        when(attendantMapper.findByUserId(66)).thenReturn(attendant);
        AdminTaskClaim claimed = new AdminTaskClaim();
        claimed.setTaskType("ATTENDANT_REVIEW");
        claimed.setTargetId(66);
        claimed.setOperatorId(1);
        claimed.setLockToken("other-token");
        claimed.setExpiresAt(new Date(System.currentTimeMillis() + 600_000L));
        when(claimMapper.findByTask("ATTENDANT_REVIEW", 66)).thenReturn(claimed);

        assertThatThrownBy(() -> service.claimTask(2, "ATTENDANT_REVIEW", 66))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("任务正在处理中");
    }

    @Test
    void claimTaskShouldRejectOrderDisputeWhenOrderIsNotDisputing() {
        AdminWorkbenchService service = service();
        when(sysAdminMapper.findById(2)).thenReturn(admin(2, "ADMIN"));
        Order order = new Order();
        order.setOrderId(91);
        order.setOrderStatus(6);
        when(orderMapper.selectByPrimaryKey(91)).thenReturn(order);

        assertThatThrownBy(() -> service.claimTask(2, "ORDER_DISPUTE", 91))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("任务状态已变化，请刷新队列");
    }

    @Test
    void completeOrderDisputeShouldRequireCurrentLock() {
        AdminWorkbenchService service = service();
        AdminWorkbenchCompleteRequest request = new AdminWorkbenchCompleteRequest();
        request.setLockToken("bad-token");
        request.setFinalDuration(new BigDecimal("2.0"));
        request.setFinalOrderAmount(new BigDecimal("200.00"));
        request.setAdminRemark("处理备注");

        AdminTaskClaim claim = activeClaim("ORDER_DISPUTE", 90, 1, "good-token");
        when(claimMapper.findByTask("ORDER_DISPUTE", 90)).thenReturn(claim);

        assertThatThrownBy(() -> service.completeTask(1, "ORDER_DISPUTE", 90, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("任务锁已失效，请重新领取");
        verify(adminService, never()).resolveDispute(any(), any(), any());
    }

    @Test
    void completeOrderDisputeShouldRejectWhenOrderAlreadyHandled() {
        AdminWorkbenchService service = service();
        AdminWorkbenchCompleteRequest request = new AdminWorkbenchCompleteRequest();
        request.setLockToken("token-90");
        request.setFinalDuration(new BigDecimal("2.0"));
        request.setFinalOrderAmount(new BigDecimal("200.00"));
        request.setAdminRemark("处理备注");

        when(claimMapper.findByTask("ORDER_DISPUTE", 90)).thenReturn(activeClaim("ORDER_DISPUTE", 90, 1, "token-90"));
        Order order = new Order();
        order.setOrderId(90);
        order.setOrderStatus(6);
        when(orderMapper.selectByPrimaryKey(90)).thenReturn(order);

        assertThatThrownBy(() -> service.completeTask(1, "ORDER_DISPUTE", 90, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("任务状态已变化，请刷新队列");
        verify(adminService, never()).resolveDispute(any(), any(), any());
    }

    @Test
    void completePendingAttendantReviewShouldDelegateAndReleaseClaim() {
        AdminWorkbenchService service = service();
        AdminWorkbenchCompleteRequest request = new AdminWorkbenchCompleteRequest();
        request.setLockToken("token-77");
        request.setAction("reject");
        request.setReason("证件不清晰");

        when(claimMapper.findByTask("ATTENDANT_REVIEW", 77)).thenReturn(activeClaim("ATTENDANT_REVIEW", 77, 3, "token-77"));
        Attendant attendant = new Attendant();
        attendant.setUserId(77);
        attendant.setQualificationStatus(0);
        when(attendantMapper.findByUserId(77)).thenReturn(attendant);

        service.completeTask(3, "ATTENDANT_REVIEW", 77, request);

        verify(adminService).reviewAttendantQualification(3, 77, "reject", "证件不清晰");
        verify(claimMapper).deleteByTask("ATTENDANT_REVIEW", 77);
    }

    private AdminWorkbenchService service() {
        return new AdminWorkbenchService(claimMapper, sysAdminMapper, orderMapper, attendantMapper, adminService);
    }

    private SysAdmin admin(Integer id, String role) {
        SysAdmin admin = new SysAdmin();
        admin.setId(id);
        admin.setName("管理员" + id);
        admin.setPhone("1380000000" + id);
        admin.setRole(role);
        admin.setStatus(1);
        return admin;
    }

    private AdminTaskClaim activeClaim(String type, Integer targetId, Integer operatorId, String token) {
        AdminTaskClaim claim = new AdminTaskClaim();
        claim.setTaskType(type);
        claim.setTargetId(targetId);
        claim.setOperatorId(operatorId);
        claim.setLockToken(token);
        claim.setExpiresAt(new Date(System.currentTimeMillis() + 600_000L));
        return claim;
    }
}
