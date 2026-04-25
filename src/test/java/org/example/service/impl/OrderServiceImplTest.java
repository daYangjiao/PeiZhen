package org.example.service.impl;

import org.example.dao.AttendantMapper;
import org.example.dao.AttendantQualificationMapper;
import org.example.dao.ChatMessageMapper;
import org.example.dao.OrderMapper;
import org.example.dao.UserMapper;
import org.example.model.Attendant;
import org.example.model.AttendantQualification;
import org.example.model.Order;
import org.example.model.User;
import org.example.model.request.OrderListQueryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private AttendantMapper attendantMapper;
    @Mock
    private AttendantQualificationMapper attendantQualificationMapper;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl();
        ReflectionTestUtils.setField(service, "orderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "chatMessageMapper", chatMessageMapper);
        ReflectionTestUtils.setField(service, "attendantMapper", attendantMapper);
        ReflectionTestUtils.setField(service, "attendantQualificationMapper", attendantQualificationMapper);
    }

    @Test
    void attendantAcceptOrderShouldRejectPendingReviewAttendant() {
        Order order = new Order();
        order.setOrderId(91);
        order.setOrderStatus(1);
        User attendantUser = new User();
        attendantUser.setId(22);
        attendantUser.setName("陪诊师");
        attendantUser.setUserType(1);
        Attendant attendant = new Attendant();
        attendant.setUserId(22);
        attendant.setStatus(0);

        when(orderMapper.selectByPrimaryKey(91)).thenReturn(order);
        when(userMapper.findById(22)).thenReturn(attendantUser);
        when(attendantMapper.findByUserId(22)).thenReturn(attendant);

        String result = service.attendantAcceptOrder(91, 22);

        assertThat(result).isEqualTo("资质审核通过后才能接单");
    }

    @Test
    void attendantAcceptOrderShouldRejectExpiredCertificate() {
        Order order = new Order();
        order.setOrderId(92);
        order.setOrderStatus(1);
        User attendantUser = new User();
        attendantUser.setId(23);
        attendantUser.setName("陪诊师");
        attendantUser.setUserType(1);
        Attendant attendant = new Attendant();
        attendant.setUserId(23);
        attendant.setStatus(1);
        AttendantQualification qualification = new AttendantQualification();
        qualification.setIdCardFrontFileUrl("front.png");
        qualification.setIdCardBackFileUrl("back.png");
        qualification.setPracticeCertFileUrl("practice.png");
        qualification.setHealthCertFileUrl("health.png");
        qualification.setPracticeCertExpireDate(LocalDate.now().plusYears(1).toString());
        qualification.setHealthCertExpireDate(LocalDate.now().minusDays(1).toString());

        when(orderMapper.selectByPrimaryKey(92)).thenReturn(order);
        when(userMapper.findById(23)).thenReturn(attendantUser);
        when(attendantMapper.findByUserId(23)).thenReturn(attendant);
        when(attendantQualificationMapper.findByUserId(23)).thenReturn(qualification);

        String result = service.attendantAcceptOrder(92, 23);

        assertThat(result).isEqualTo("健康证已过期，请更新资质后重新提交审核");
    }

    @Test
    void waitingOrdersShouldRejectUnapprovedAttendant() {
        Attendant attendant = new Attendant();
        attendant.setUserId(24);
        attendant.setStatus(3);
        when(attendantMapper.findByUserId(24)).thenReturn(attendant);

        OrderListQueryRequest query = new OrderListQueryRequest();
        query.setOrderStatus(1);

        assertThatThrownBy(() -> service.getWaitingOrdersForAttendant(24, query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("资质审核通过后才能查看接单大厅");
    }

    @Test
    void userConfirmTimeAndFeeShouldRejectOtherUserOrder() {
        Order order = new Order();
        order.setOrderId(101);
        order.setUserId(88);
        order.setOrderStatus(4);
        when(orderMapper.selectByPrimaryKey(101)).thenReturn(order);

        assertThatThrownBy(() -> service.userConfirmTimeAndFee(101, 99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("无权限操作该订单");
    }

    @Test
    void userConfirmTimeAndFeeShouldEnterBalancePaymentWhenBalancePositive() {
        Order order = new Order();
        order.setOrderId(102);
        order.setOrderNo("ORD-102");
        order.setUserId(10);
        order.setAttendantId(20);
        order.setOrderStatus(4);
        order.setOrderAmount(new BigDecimal("170.00"));
        order.setBalanceAmount(new BigDecimal("60.00"));
        when(orderMapper.selectByPrimaryKey(102)).thenReturn(order);

        String result = service.userConfirmTimeAndFee(102, 10);

        assertThat(result).isEqualTo("确认成功，请支付差额");
        verify(orderMapper).updateByPrimaryKeySelective(argThat(patch ->
                Integer.valueOf(102).equals(patch.getOrderId())
                        && Integer.valueOf(9).equals(patch.getOrderStatus())
                        && new BigDecimal("230.00").compareTo(patch.getOrderAmount()) == 0
                        && new BigDecimal("60.00").compareTo(patch.getBalanceAmount()) == 0
        ));
    }

    @Test
    void userConfirmTimeAndFeeShouldCompleteAndRefundWhenBalanceNegative() {
        Order order = new Order();
        order.setOrderId(103);
        order.setOrderNo("ORD-103");
        order.setUserId(10);
        order.setAttendantId(20);
        order.setOrderStatus(4);
        order.setOrderAmount(new BigDecimal("170.00"));
        order.setBalanceAmount(new BigDecimal("-30.00"));
        when(orderMapper.selectByPrimaryKey(103)).thenReturn(order);

        String result = service.userConfirmTimeAndFee(103, 10);

        assertThat(result).isEqualTo("确认成功，订单已完成");
        verify(orderMapper).updateByPrimaryKeySelective(argThat(patch ->
                Integer.valueOf(103).equals(patch.getOrderId())
                        && Integer.valueOf(6).equals(patch.getOrderStatus())
                        && new BigDecimal("140.00").compareTo(patch.getOrderAmount()) == 0
                        && new BigDecimal("30.00").compareTo(patch.getRefundAmount()) == 0
        ));
    }

    @Test
    void userDisputeTimeAndFeeShouldValidateReasonAndDuration() {
        Order order = new Order();
        order.setOrderId(104);
        order.setUserId(10);
        order.setOrderStatus(4);
        when(orderMapper.selectByPrimaryKey(104)).thenReturn(order);

        assertThatThrownBy(() -> service.userDisputeTimeAndFee(104, 10, BigDecimal.ZERO, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("用户认可时长必须大于0");

        assertThatThrownBy(() -> service.userDisputeTimeAndFee(104, 10, new BigDecimal("2.5"), " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("申诉原因不能为空");
    }

    @Test
    void userPayBalanceShouldCompleteBalancePaymentOrder() {
        Order order = new Order();
        order.setOrderId(105);
        order.setOrderNo("ORD-105");
        order.setUserId(10);
        order.setAttendantId(20);
        order.setOrderStatus(9);
        order.setBalanceAmount(new BigDecimal("60.00"));
        when(orderMapper.selectByPrimaryKey(105)).thenReturn(order);

        String result = service.userPayBalance(105, 10);

        assertThat(result).isEqualTo("差额支付成功，订单已完成");
        verify(orderMapper).updateByPrimaryKeySelective(argThat(patch ->
                Integer.valueOf(105).equals(patch.getOrderId())
                        && Integer.valueOf(6).equals(patch.getOrderStatus())
        ));
    }
}
