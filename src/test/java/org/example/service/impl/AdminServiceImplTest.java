package org.example.service.impl;

import org.example.dao.AttendantMapper;
import org.example.dao.AttendantQualificationMapper;
import org.example.dao.OrderMapper;
import org.example.dao.UserMapper;
import org.example.model.Attendant;
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

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
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
    private OrderMapper orderMapper;
    @Mock
    private OrderService orderService;

    @Test
    void getAttendantsShouldIncludeQualificationCompleteness() {
        AdminServiceImpl service = new AdminServiceImpl(userMapper, attendantMapper, attendantQualificationMapper, orderMapper, orderService);

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

        PagedResponse<AdminAttendantListItemResponse> response = service.getAttendants(null, null, 0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getQualificationCompleteness()).isEqualTo(50);
    }

    @Test
    void getNextPendingAttendantShouldReturnNullWhenNoPendingRecord() {
        AdminServiceImpl service = new AdminServiceImpl(userMapper, attendantMapper, attendantQualificationMapper, orderMapper, orderService);
        when(attendantMapper.findNextPendingUserId(null)).thenReturn(null);

        AdminAttendantDetailResponse response = service.getNextPendingAttendant(null);

        assertThat(response).isNull();
    }

    @Test
    void reviewAttendantQualificationShouldRejectUnsupportedAction() {
        AdminServiceImpl service = new AdminServiceImpl(userMapper, attendantMapper, attendantQualificationMapper, orderMapper, orderService);

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
        AdminServiceImpl service = new AdminServiceImpl(userMapper, attendantMapper, attendantQualificationMapper, orderMapper, orderService);

        User user = new User();
        user.setId(301);
        user.setName("陪诊用户");
        user.setPhone("13800003333");
        user.setUserType(1);
        user.setStatus(1);
        user.setCreateTime(new Date());

        Attendant attendant = new Attendant();
        attendant.setUserId(301);
        attendant.setStatus(3);
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
        assertThat(item.getAttendantAuditStatus()).isEqualTo(3);
        assertThat(item.getAttendantAuditStatusLabel()).isEqualTo("审核驳回");
        assertThat(item.getAttendantProfileCompleted()).isTrue();
        assertThat(item.getQualificationCompleteness()).isEqualTo(75);
    }

    @Test
    void getOrdersShouldIncludeExtendedOrderFields() {
        AdminServiceImpl service = new AdminServiceImpl(userMapper, attendantMapper, attendantQualificationMapper, orderMapper, orderService);

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
}
