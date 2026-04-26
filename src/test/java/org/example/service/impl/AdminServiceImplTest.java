package org.example.service.impl;

import org.example.dao.AttendantMapper;
import org.example.dao.AttendantQualificationAuditLogMapper;
import org.example.dao.AttendantQualificationMapper;
import org.example.dao.OrderEvaluationMapper;
import org.example.dao.OrderMapper;
import org.example.dao.SysAdminMapper;
import org.example.dao.UserMapper;
import org.example.entity.SysAdmin;
import org.example.model.Attendant;
import org.example.model.AttendantQualificationAuditLog;
import org.example.model.AttendantQualification;
import org.example.model.Order;
import org.example.model.User;
import org.example.model.request.OrderListQueryRequest;
import org.example.model.response.AdminAttendantDetailResponse;
import org.example.model.response.AdminAttendantListItemResponse;
import org.example.model.response.AdminOrderListItemResponse;
import org.example.model.response.AdminUserListItemResponse;
import org.example.model.response.PagedResponse;
import org.example.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private AttendantMapper attendantMapper;
    @Mock
    private AttendantQualificationMapper attendantQualificationMapper;
    @Mock
    private AttendantQualificationAuditLogMapper auditLogMapper;
    @Mock
    private SysAdminMapper sysAdminMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderEvaluationMapper orderEvaluationMapper;
    @Mock
    private OrderService orderService;
    @Mock
    private AdminOperationLogService operationLogService;

    private AdminServiceImpl newService() {
        return new AdminServiceImpl(userMapper, attendantMapper, attendantQualificationMapper, auditLogMapper,
                sysAdminMapper, orderMapper, orderEvaluationMapper, orderService);
    }

    @Test
    void getAttendantsShouldIncludeQualificationCompleteness() {
        AdminServiceImpl service = newService();

        Attendant attendant = new Attendant();
        attendant.setUserId(101);
        attendant.setName("陪诊师A");
        attendant.setPhone("13800001111");
        attendant.setStatus(0);
        attendant.setUserStatus(1);
        attendant.setScore(new BigDecimal("4.80"));
        attendant.setServiceCount(12);
        attendant.setCreateTime(new Date());
        attendant.setUpdateTime(new Date());

        AttendantQualification qualification = new AttendantQualification();
        qualification.setIdCardFrontFileUrl("front.png");
        qualification.setPracticeCertFileUrl("practice.png");

        when(attendantMapper.countAdminAttendants(null, null)).thenReturn(1);
        when(attendantMapper.findAdminAttendants(null, null, 0, 10)).thenReturn(List.of(attendant));
        when(attendantQualificationMapper.findByUserId(101)).thenReturn(qualification);
        when(orderEvaluationMapper.countByAttendantId(101)).thenReturn(3);
        when(orderEvaluationMapper.countGoodByAttendantId(101, 4)).thenReturn(2);
        when(orderEvaluationMapper.averageRatingByAttendantId(101)).thenReturn(new BigDecimal("4.0"));

        PagedResponse<AdminAttendantListItemResponse> response = service.getAttendants(null, null, 0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getQualificationCompleteness()).isEqualTo(33);
        assertThat(response.getContent().get(0).getScore()).isEqualByComparingTo("4.0");
        assertThat(response.getContent().get(0).getEvaluationCount()).isEqualTo(3);
        assertThat(response.getContent().get(0).getPraiseRate()).isEqualTo(67);
    }

    @Test
    void getAttendantsShouldHideScoreWhenNoEvaluations() {
        AdminServiceImpl service = newService();

        Attendant attendant = new Attendant();
        attendant.setUserId(102);
        attendant.setName("陪诊师B");
        attendant.setStatus(1);
        attendant.setScore(new BigDecimal("5.0"));

        when(attendantMapper.countAdminAttendants(null, null)).thenReturn(1);
        when(attendantMapper.findAdminAttendants(null, null, 0, 10)).thenReturn(List.of(attendant));
        when(orderEvaluationMapper.countByAttendantId(102)).thenReturn(0);

        PagedResponse<AdminAttendantListItemResponse> response = service.getAttendants(null, null, 0, 10);

        AdminAttendantListItemResponse item = response.getContent().get(0);
        assertThat(item.getScore()).isNull();
        assertThat(item.getEvaluationCount()).isZero();
        assertThat(item.getPraiseRate()).isZero();
    }

    @Test
    void getNextPendingAttendantShouldReturnNullWhenNoPendingRecord() {
        AdminServiceImpl service = newService();
        when(attendantMapper.findNextPendingUserId(null)).thenReturn(null);

        AdminAttendantDetailResponse response = service.getNextPendingAttendant(1, null);

        assertThat(response).isNull();
    }

    @Test
    void reviewAttendantQualificationShouldRejectUnsupportedAction() {
        AdminServiceImpl service = newService();

        User user = new User();
        user.setId(201);
        user.setStatus(1);
        Attendant attendant = new Attendant();
        attendant.setUserId(201);
        attendant.setStatus(0);

        when(userMapper.findById(201)).thenReturn(user);
        when(attendantMapper.findByUserId(201)).thenReturn(attendant);

        assertThatThrownBy(() -> service.reviewAttendantQualification(1, 201, "unknown", "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("不支持的审核动作");
    }

    @Test
    void getUsersShouldIncludeCompletedOrderCountAndAttendantAuditFields() {
        AdminServiceImpl service = newService();

        User user = new User();
        user.setId(301);
        user.setName("陪诊用户");
        user.setPhone("13800003333");
        user.setUserType(1);
        user.setStatus(1);
        user.setCreateTime(new Date());

        Attendant attendant = new Attendant();
        attendant.setUserId(301);
        attendant.setQualificationStatus(2);
        attendant.setHospitalName("华西医院");
        attendant.setProfessionalField("肿瘤陪诊");
        attendant.setExperienceYears(6);
        attendant.setIntroduction("有经验");
        attendant.setCertificate("CERT-001");

        AttendantQualification qualification = new AttendantQualification();
        qualification.setIdCardFrontFileUrl("front.png");
        qualification.setIdCardBackFileUrl("back.png");
        qualification.setPracticeCertFileUrl("practice.png");

        when(userMapper.countAdminUsers(null, null, null)).thenReturn(1);
        when(userMapper.findAdminUsers(null, null, null, 0, 10)).thenReturn(List.of(user));
        when(orderMapper.countUserOrders(eq(301), ArgumentMatchers.argThat(query -> query != null && query.getOrderStatus() == null))).thenReturn(7);
        when(orderMapper.countUserOrders(eq(301), ArgumentMatchers.argThat(query -> query != null && Integer.valueOf(6).equals(query.getOrderStatus())))).thenReturn(4);
        when(attendantMapper.findByUserId(301)).thenReturn(attendant);
        when(attendantQualificationMapper.findByUserId(301)).thenReturn(qualification);

        PagedResponse<AdminUserListItemResponse> response = service.getUsers(null, null, null, 0, 10);

        assertThat(response.getContent()).hasSize(1);
        AdminUserListItemResponse item = response.getContent().get(0);
        assertThat(item.getCompletedOrderCount()).isEqualTo(4);
        assertThat(item.getAttendantAuditStatus()).isEqualTo(2);
        assertThat(item.getAttendantAuditStatusLabel()).isEqualTo("未通过");
        assertThat(item.getAttendantProfileCompleted()).isTrue();
        assertThat(item.getQualificationCompleteness()).isEqualTo(50);
    }

    @Test
    void updateUserStatusShouldRecordConsistentUserTargetLabel() {
        AdminServiceImpl service = newService();
        ReflectionTestUtils.setField(service, "operationLogService", operationLogService);

        User user = new User();
        user.setId(302);
        user.setName("肖阳");
        user.setPhone("18600010001");
        user.setStatus(0);

        when(userMapper.findById(302)).thenReturn(user);

        service.updateUserStatus(2, 302, 1);

        verify(userMapper).update(ArgumentMatchers.argThat(patch ->
                Integer.valueOf(302).equals(patch.getId())
                        && Integer.valueOf(1).equals(patch.getStatus())
        ));
        verify(operationLogService).record(
                eq(2),
                eq("USER"),
                eq("ENABLE_USER"),
                eq("USER"),
                eq(302),
                eq("肖阳（186****0001）"),
                eq(0),
                eq(1),
                isNull(),
                eq("{}")
        );
    }

    @Test
    void getOrdersShouldIncludeExtendedOrderFields() {
        AdminServiceImpl service = newService();

        Order order = new Order();
        order.setOrderId(401);
        order.setOrderNo("ORD-401");
        order.setUserId(501);
        order.setAttendantId(601);
        order.setPatientName("张三");
        order.setPatientAge(68);
        order.setPatientSex("男");
        order.setContactPerson("李四");
        order.setContactPhone("13900000000");
        order.setHospital("省医院");
        order.setServiceContent("陪同检查");
        order.setSpecialRequirements("轮椅协助");
        order.setPaymentTime(new Date(1000));
        order.setAcceptTime(new Date(2000));
        order.setServiceStartTime(new Date(3000));
        order.setServiceEndTime(new Date(4000));
        order.setActualDuration(new BigDecimal("3.5"));
        order.setBalanceAmount(new BigDecimal("88.00"));
        order.setRefundAmount(new BigDecimal("20.00"));
        order.setAdminRemark("管理员备注");
        order.setOrderStatus(6);
        order.setPaymentStatus(1);
        order.setOrderAmount(new BigDecimal("188.00"));
        order.setCreateTime(new Date(5000));

        User user = new User();
        user.setId(501);
        user.setName("用户A");
        user.setPhone("13811112222");
        User attendant = new User();
        attendant.setId(601);
        attendant.setName("陪诊师B");
        attendant.setPhone("13833334444");

        when(orderMapper.countAllOrders(ArgumentMatchers.any(OrderListQueryRequest.class))).thenReturn(1);
        when(orderMapper.findAllOrdersWithPagination(ArgumentMatchers.any(OrderListQueryRequest.class), eq(0), eq(10))).thenReturn(List.of(order));
        when(userMapper.findById(501)).thenReturn(user);
        when(userMapper.findById(601)).thenReturn(attendant);

        PagedResponse<AdminOrderListItemResponse> response = service.getOrders(null, null, null, null, null, 0, 10);

        assertThat(response.getContent()).hasSize(1);
        AdminOrderListItemResponse item = response.getContent().get(0);
        assertThat(item.getPatientAge()).isEqualTo(68);
        assertThat(item.getPatientSex()).isEqualTo("男");
        assertThat(item.getContactPerson()).isEqualTo("李四");
        assertThat(item.getContactPhone()).isEqualTo("13900000000");
        assertThat(item.getServiceContent()).isEqualTo("陪同检查");
        assertThat(item.getSpecialRequirements()).isEqualTo("轮椅协助");
        assertThat(item.getPaymentTime()).isEqualTo(new Date(1000));
        assertThat(item.getAcceptTime()).isEqualTo(new Date(2000));
        assertThat(item.getServiceStartTime()).isEqualTo(new Date(3000));
        assertThat(item.getServiceEndTime()).isEqualTo(new Date(4000));
        assertThat(item.getActualDuration()).isEqualByComparingTo("3.5");
        assertThat(item.getBalanceAmount()).isEqualByComparingTo("88.00");
        assertThat(item.getRefundAmount()).isEqualByComparingTo("20.00");
        assertThat(item.getAdminRemark()).isEqualTo("管理员备注");
    }

    @Test
    void reviewAttendantQualificationShouldRejectIncompleteQualificationBeforeApprove() {
        AdminServiceImpl service = newService();

        User user = new User();
        user.setId(701);
        user.setStatus(1);
        Attendant attendant = new Attendant();
        attendant.setUserId(701);
        attendant.setStatus(0);
        AttendantQualification qualification = new AttendantQualification();
        qualification.setIdCardFrontFileUrl("front.png");

        when(userMapper.findById(701)).thenReturn(user);
        when(attendantMapper.findByUserId(701)).thenReturn(attendant);
        when(attendantQualificationMapper.findByUserId(701)).thenReturn(qualification);

        assertThatThrownBy(() -> service.reviewAttendantQualification(1, 701, "approve", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("资质材料不完整，不能通过审核");
    }

    @Test
    void reviewAttendantQualificationShouldWriteAdminAuditLog() {
        AdminServiceImpl service = newService();
        ReflectionTestUtils.setField(service, "operationLogService", operationLogService);

        User user = new User();
        user.setId(702);
        user.setName("王小花");
        user.setPhone("17311209183");
        user.setStatus(1);
        Attendant attendant = new Attendant();
        attendant.setUserId(702);
        attendant.setStatus(0);
        AttendantQualification qualification = completeQualification();
        SysAdmin admin = new SysAdmin();
        admin.setId(2);
        admin.setName("审核员");
        admin.setPhone("18650680037");
        admin.setRole("SUPER_ADMIN");

        when(userMapper.findById(702)).thenReturn(user);
        when(attendantMapper.findByUserId(702)).thenReturn(attendant);
        when(attendantQualificationMapper.findByUserId(702)).thenReturn(qualification);
        when(sysAdminMapper.findById(2)).thenReturn(admin);

        service.reviewAttendantQualification(2, 702, "approve", null);

        verify(auditLogMapper).insert(ArgumentMatchers.argThat(log ->
                Integer.valueOf(702).equals(log.getUserId())
                        && "ADMIN".equals(log.getActorType())
                        && Integer.valueOf(2).equals(log.getActorId())
                        && "审核员".equals(log.getActorName())
                        && "SUPER_ADMIN".equals(log.getActorRole())
                        && "APPROVE".equals(log.getAction())
                        && Integer.valueOf(0).equals(log.getFromStatus())
                        && Integer.valueOf(1).equals(log.getToStatus())
        ));
        verify(attendantMapper).update(ArgumentMatchers.argThat(patch ->
                Integer.valueOf(702).equals(patch.getUserId())
                        && Integer.valueOf(1).equals(patch.getQualificationStatus())
                        && patch.getStatus() == null
        ));
        verify(operationLogService).record(
                eq(2),
                eq("ATTENDANT"),
                eq("APPROVE"),
                eq("ATTENDANT"),
                eq(702),
                eq("王小花（173****9183）"),
                eq(0),
                eq(1),
                isNull(),
                ArgumentMatchers.anyString()
        );
    }

    @Test
    void updateAttendantStatusShouldOnlyUpdateUserAccountStatus() {
        AdminServiceImpl service = newService();

        User user = new User();
        user.setId(704);
        user.setStatus(1);
        user.setName("被封禁陪诊师");
        Attendant attendant = new Attendant();
        attendant.setUserId(704);
        attendant.setQualificationStatus(1);
        attendant.setStatus(1);

        when(userMapper.findById(704)).thenReturn(user);
        when(attendantMapper.findByUserId(704)).thenReturn(attendant);

        service.updateAttendantStatus(2, 704, 0, "违规服务");

        verify(userMapper).update(ArgumentMatchers.argThat(patch ->
                Integer.valueOf(704).equals(patch.getId())
                        && Integer.valueOf(0).equals(patch.getStatus())
        ));
        verify(attendantMapper, never()).update(ArgumentMatchers.any());
    }

    @Test
    void getAttendantQualificationLogsShouldHideOperatorForNormalAdmin() {
        AdminServiceImpl service = newService();
        SysAdmin admin = new SysAdmin();
        admin.setId(3);
        admin.setRole("ADMIN");
        AttendantQualificationAuditLog log = new AttendantQualificationAuditLog();
        log.setUserId(703);
        log.setActorName("普通管理员");
        log.setActorPhone("13800000000");
        log.setActorRole("ADMIN");
        log.setAction("REJECT");

        when(sysAdminMapper.findById(3)).thenReturn(admin);
        when(auditLogMapper.findLatestByUserId(703, 20)).thenReturn(List.of(log));

        var logs = service.getAttendantQualificationLogs(3, 703, 20);

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getOperatorName()).isNull();
        assertThat(logs.get(0).getOperatorRole()).isNull();
    }

    @Test
    void resolveDisputeShouldEnterBalancePaymentWhenFinalAmountGreaterThanPaid() {
        AdminServiceImpl service = newService();
        Order order = new Order();
        order.setOrderId(801);
        order.setOrderNo("ORD-801");
        order.setUserId(11);
        order.setAttendantId(12);
        order.setOrderStatus(5);
        order.setOrderAmount(new BigDecimal("170.00"));
        order.setBalanceAmount(new BigDecimal("20.00"));

        org.example.model.request.AdminOrderDisputeResolutionRequest request = new org.example.model.request.AdminOrderDisputeResolutionRequest();
        request.setFinalDuration(new BigDecimal("3.5"));
        request.setFinalOrderAmount(new BigDecimal("230.00"));
        request.setAdminRemark("平台核定补差额");

        when(orderMapper.selectByPrimaryKey(801)).thenReturn(order);

        service.resolveDispute(2, 801, request);

        verify(orderMapper).updateByPrimaryKeySelective(ArgumentMatchers.argThat(patch ->
                Integer.valueOf(801).equals(patch.getOrderId())
                        && Integer.valueOf(9).equals(patch.getOrderStatus())
                        && new BigDecimal("230.00").compareTo(patch.getOrderAmount()) == 0
                        && new BigDecimal("60.00").compareTo(patch.getBalanceAmount()) == 0
                        && Integer.valueOf(2).equals(patch.getDisputeResolvedBy())
                        && patch.getDisputeResolvedTime() != null
                        && "平台核定补差额".equals(patch.getAdminRemark())
        ));
    }

    @Test
    void resolveDisputeShouldEnterRefundPendingWhenFinalAmountLowerThanPaid() {
        AdminServiceImpl service = newService();
        Order order = new Order();
        order.setOrderId(802);
        order.setOrderNo("ORD-802");
        order.setUserId(11);
        order.setAttendantId(12);
        order.setOrderStatus(5);
        order.setOrderAmount(new BigDecimal("230.00"));

        org.example.model.request.AdminOrderDisputeResolutionRequest request = new org.example.model.request.AdminOrderDisputeResolutionRequest();
        request.setFinalDuration(new BigDecimal("2.0"));
        request.setFinalOrderAmount(new BigDecimal("170.00"));
        request.setAdminRemark("平台核定退款");

        when(orderMapper.selectByPrimaryKey(802)).thenReturn(order);

        service.resolveDispute(2, 802, request);

        verify(orderMapper).updateByPrimaryKeySelective(ArgumentMatchers.argThat(patch ->
                Integer.valueOf(802).equals(patch.getOrderId())
                        && Integer.valueOf(10).equals(patch.getOrderStatus())
                        && new BigDecimal("170.00").compareTo(patch.getOrderAmount()) == 0
                        && new BigDecimal("-60.00").compareTo(patch.getBalanceAmount()) == 0
                        && new BigDecimal("60.00").compareTo(patch.getRefundAmount()) == 0
        ));
    }

    @Test
    void resolveDisputeShouldRejectZeroFinalAmount() {
        AdminServiceImpl service = newService();
        Order order = new Order();
        order.setOrderId(804);
        order.setOrderNo("ORD-804");
        order.setUserId(11);
        order.setAttendantId(12);
        order.setOrderStatus(5);
        order.setOrderAmount(new BigDecimal("170.00"));

        org.example.model.request.AdminOrderDisputeResolutionRequest request = new org.example.model.request.AdminOrderDisputeResolutionRequest();
        request.setFinalDuration(new BigDecimal("1.0"));
        request.setFinalOrderAmount(BigDecimal.ZERO);
        request.setAdminRemark("零元争议金额");

        when(orderMapper.selectByPrimaryKey(804)).thenReturn(order);

        assertThatThrownBy(() -> service.resolveDispute(2, 804, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("最终订单金额必须大于0");
        verify(orderMapper, never()).updateByPrimaryKeySelective(ArgumentMatchers.any());
    }

    @Test
    void resolveDisputeShouldCalculateFinalAmountWhenOnlyDurationProvided() {
        AdminServiceImpl service = newService();
        Order order = new Order();
        order.setOrderId(803);
        order.setOrderNo("ORD-803");
        order.setUserId(11);
        order.setAttendantId(12);
        order.setOrderStatus(5);
        order.setClinicType(1);
        order.setOrderAmount(new BigDecimal("170.00"));

        org.example.model.request.AdminOrderDisputeResolutionRequest request = new org.example.model.request.AdminOrderDisputeResolutionRequest();
        request.setFinalDuration(new BigDecimal("4.5"));
        request.setAdminRemark("按最终时长自动核算金额");

        when(orderMapper.selectByPrimaryKey(803)).thenReturn(order);

        service.resolveDispute(2, 803, request);

        verify(orderMapper).updateByPrimaryKeySelective(ArgumentMatchers.argThat(patch ->
                Integer.valueOf(803).equals(patch.getOrderId())
                        && Integer.valueOf(10).equals(patch.getOrderStatus())
                        && new BigDecimal("140.00").compareTo(patch.getOrderAmount()) == 0
                        && new BigDecimal("-30.00").compareTo(patch.getBalanceAmount()) == 0
                        && new BigDecimal("30.00").compareTo(patch.getRefundAmount()) == 0
        ));
    }

    @Test
    void completeDisputeRefundShouldCompleteRefundPendingOrder() {
        AdminServiceImpl service = newService();
        Order order = new Order();
        order.setOrderId(805);
        order.setOrderNo("ORD-805");
        order.setUserId(11);
        order.setAttendantId(12);
        order.setOrderStatus(10);
        order.setOrderAmount(new BigDecimal("170.00"));
        order.setBalanceAmount(new BigDecimal("-60.00"));
        order.setRefundAmount(new BigDecimal("60.00"));

        when(orderMapper.selectByPrimaryKey(805)).thenReturn(order);

        service.completeDisputeRefund(2, 805, "已完成原路退款");

        verify(orderMapper).updateByPrimaryKeySelective(ArgumentMatchers.argThat(patch ->
                Integer.valueOf(805).equals(patch.getOrderId())
                        && Integer.valueOf(6).equals(patch.getOrderStatus())
                        && "已完成原路退款".equals(patch.getAdminRemark())
        ));
    }

    private AttendantQualification completeQualification() {
        AttendantQualification qualification = new AttendantQualification();
        qualification.setIdCardFrontFileUrl("front.png");
        qualification.setIdCardBackFileUrl("back.png");
        qualification.setPracticeCertFileUrl("practice.png");
        qualification.setHealthCertFileUrl("health.png");
        qualification.setPracticeCertExpireDate(java.time.LocalDate.now().plusYears(1).toString());
        qualification.setHealthCertExpireDate(java.time.LocalDate.now().plusYears(1).toString());
        return qualification;
    }
}
