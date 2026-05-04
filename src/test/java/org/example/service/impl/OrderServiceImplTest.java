package org.example.service.impl;

import org.example.dao.AttendantMapper;
import org.example.dao.AttendantQualificationMapper;
import org.example.dao.ChatMessageMapper;
import org.example.dao.OrderMapper;
import org.example.dao.UserMapper;
import org.example.model.Attendant;
import org.example.model.AttendantQualification;
import org.example.model.ChatMessage;
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
import java.util.Date;

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
    void attendantAcceptOrderShouldRejectNonAttendantUserType() {
        Order order = new Order();
        order.setOrderId(90);
        order.setOrderStatus(1);
        User user = new User();
        user.setId(25);
        user.setName("普通用户");
        user.setUserType(0);

        when(orderMapper.selectByPrimaryKey(90)).thenReturn(order);
        when(userMapper.findById(25)).thenReturn(user);

        String result = service.attendantAcceptOrder(90, 25);

        assertThat(result).isEqualTo("只有陪诊师账号可以接单");
    }


    @Test
    void attendantAcceptOrderShouldReleaseExpiredAssignedOrder() {
        Order order = new Order();
        order.setOrderId(93);
        order.setOrderStatus(8);
        order.setPaymentStatus(1);
        order.setAttendantId(22);
        order.setUserId(10);
        order.setPaymentTime(new Date(System.currentTimeMillis() - 16 * 60 * 1000L));

        when(orderMapper.selectByPrimaryKey(93)).thenReturn(order);
        when(orderMapper.releaseOrderBackToHall(org.mockito.ArgumentMatchers.eq(93), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(Date.class))).thenReturn(1);

        String result = service.attendantAcceptOrder(93, 22);

        assertThat(result).isEqualTo("专属派单已超时，订单已转入公共派单");
        verify(orderMapper).releaseOrderBackToHall(org.mockito.ArgumentMatchers.eq(93), org.mockito.ArgumentMatchers.contains("超时未确认"), org.mockito.ArgumentMatchers.any(Date.class));
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
    void userConfirmTimeAndFeeShouldEnterRefundPendingWhenBalanceNegative() {
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

        assertThat(result).isEqualTo("确认成功，等待平台退款");
        verify(orderMapper).updateByPrimaryKeySelective(argThat(patch ->
                Integer.valueOf(103).equals(patch.getOrderId())
                        && Integer.valueOf(10).equals(patch.getOrderStatus())
                        && new BigDecimal("140.00").compareTo(patch.getOrderAmount()) == 0
                        && new BigDecimal("30.00").compareTo(patch.getRefundAmount()) == 0
        ));
    }

    @Test
    void userDisputeTimeAndFeeShouldAllowDisputeAgainFromBalancePayment() {
        Order order = new Order();
        order.setOrderId(105);
        order.setOrderNo("ORD-105");
        order.setUserId(10);
        order.setAttendantId(20);
        order.setOrderStatus(9);
        order.setAdminRemark("平台核定补差额");
        when(orderMapper.selectByPrimaryKey(105)).thenReturn(order);

        String result = service.userDisputeTimeAndFee(105, 10, new BigDecimal("2.50"), "仍不认可平台核定时长");

        assertThat(result).isEqualTo("申诉已提交，等待平台处理");
        verify(orderMapper).updateByPrimaryKeySelective(argThat(patch ->
                Integer.valueOf(105).equals(patch.getOrderId())
                        && Integer.valueOf(5).equals(patch.getOrderStatus())
                        && new BigDecimal("2.50").compareTo(patch.getTimeDisputeUserDuration()) == 0
                        && "仍不认可平台核定时长".equals(patch.getTimeDisputeReason())
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
    void userDisputeTimeAndFeeShouldAllowBalancePaymentOrderToAppealAgain() {
        Order order = new Order();
        order.setOrderId(108);
        order.setUserId(10);
        order.setAttendantId(20);
        order.setOrderStatus(9);
        order.setBalanceAmount(new BigDecimal("60.00"));
        when(orderMapper.selectByPrimaryKey(108)).thenReturn(order);

        String result = service.userDisputeTimeAndFee(108, 10, new BigDecimal("2.5"), "仍不认可平台核定时长");

        assertThat(result).isEqualTo("申诉已提交，等待平台处理");
        verify(orderMapper).updateByPrimaryKeySelective(argThat(patch ->
                Integer.valueOf(108).equals(patch.getOrderId())
                        && Integer.valueOf(5).equals(patch.getOrderStatus())
                        && new BigDecimal("2.50").compareTo(patch.getTimeDisputeUserDuration()) == 0
                        && "仍不认可平台核定时长".equals(patch.getTimeDisputeReason())
        ));
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

    @Test
    void startServiceShouldRejectOtherAttendant() {
        Order order = new Order();
        order.setOrderId(106);
        order.setAttendantId(20);
        order.setOrderStatus(2);
        when(orderMapper.selectByPrimaryKey(106)).thenReturn(order);

        String result = service.startService(106, 21);

        assertThat(result).isEqualTo("无权操作该订单");
    }

    @Test
    void endServiceShouldRejectOtherAttendant() {
        Order order = new Order();
        order.setOrderId(107);
        order.setAttendantId(20);
        order.setOrderStatus(3);
        when(orderMapper.selectByPrimaryKey(107)).thenReturn(order);

        String result = service.endService(107, 21, new BigDecimal("2.5"), null);

        assertThat(result).isEqualTo("无权操作该订单");
    }

    @Test
    void endServiceShouldSaveAttendantTimeRemark() {
        Order order = new Order();
        order.setOrderId(110);
        order.setOrderNo("ORD-110");
        order.setUserId(10);
        order.setAttendantId(20);
        order.setOrderStatus(3);
        order.setOrderAmount(new BigDecimal("170.00"));
        when(orderMapper.selectByPrimaryKey(110)).thenReturn(order);

        String result = service.endService(110, 20, new BigDecimal("2.5"), "检查排队较久");

        assertThat(result).startsWith("服务已提交");
        verify(orderMapper).updateByPrimaryKeySelective(argThat(patch ->
                Integer.valueOf(110).equals(patch.getOrderId())
                        && Integer.valueOf(4).equals(patch.getOrderStatus())
                        && "检查排队较久".equals(patch.getAttendantTimeRemark())
        ));
    }

    @Test
    void endServiceShouldNotifyPendingConfirmationInsteadOfCompleted() {
        Order order = new Order();
        order.setOrderId(111);
        order.setOrderNo("ORD-111");
        order.setUserId(10);
        order.setAttendantId(20);
        order.setOrderStatus(3);
        order.setOrderAmount(new BigDecimal("170.00"));
        when(orderMapper.selectByPrimaryKey(111)).thenReturn(order);

        String result = service.endService(111, 20, new BigDecimal("2.5"), null);

        assertThat(result).startsWith("服务已提交");
        verify(chatMessageMapper).insert(argThat(message ->
                Integer.valueOf(10).equals(message.getReceiverId())
                        && Integer.valueOf(111).equals(message.getOrderId())
                        && message.getContent() != null
                        && message.getContent().contains("待您确认")
                        && !message.getContent().contains("订单已完成")
                        && !message.getContent().contains("服务已结束")
        ));
    }

    @Test
    void updateServiceProgressShouldRejectOtherAttendant() {
        Order order = new Order();
        order.setOrderId(108);
        order.setAttendantId(20);
        order.setOrderStatus(3);
        when(orderMapper.selectByPrimaryKey(108)).thenReturn(order);

        String result = service.updateServiceProgress(108, 21, 2);

        assertThat(result).isEqualTo("无权操作该订单");
    }

    @Test
    void attendantCancelOrderShouldRejectOtherAttendant() {
        Order order = new Order();
        order.setOrderId(109);
        order.setAttendantId(20);
        order.setOrderStatus(2);
        when(orderMapper.selectByPrimaryKey(109)).thenReturn(order);

        String result = service.attendantCancelOrder(109, 21, "临时无法服务", null, null, null);

        assertThat(result).isEqualTo("无权操作该订单");
    }

    @Test
    void getUserOrdersWithPaginationShouldExposeAttendantIdForAvatarNavigation() {
        Order order = new Order();
        order.setOrderId(112);
        order.setOrderNo("ORD-112");
        order.setUserId(10);
        order.setAttendantId(20);
        order.setOrderStatus(2);
        order.setPaymentStatus(1);
        order.setServiceContent("普通陪诊");
        User attendant = new User();
        attendant.setId(20);
        attendant.setName("李陪诊");
        attendant.setAvatar("/uploads/attendant-20.png");
        OrderListQueryRequest query = new OrderListQueryRequest();

        when(orderMapper.countUserOrders(10, query)).thenReturn(1);
        when(orderMapper.findUserOrdersWithPagination(10, query, 0, 10)).thenReturn(java.util.List.of(order));
        when(userMapper.findById(20)).thenReturn(attendant);

        var response = service.getUserOrdersWithPagination(10, query);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getAttendantId()).isEqualTo("20");
        assertThat(response.getContent().get(0).getAttendantName()).isEqualTo("李陪诊");
    }
}
