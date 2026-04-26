package org.example.service.impl;

import org.example.dao.ChatMessageMapper;
import org.example.dao.GuideAppointmentMapper;
import org.example.dao.OrderMapper;
import org.example.dao.UserMapper;
import org.example.model.GuideAppointment;
import org.example.model.Order;
import org.example.model.User;
import org.example.model.request.CreateOrderRequest;
import org.example.model.OrderCreateResponse;
import org.example.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiGuideServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private GuideAppointmentMapper guideAppointmentMapper;
    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private OrderService orderService;

    private AiGuideServiceImpl newService() {
        AiGuideServiceImpl service = new AiGuideServiceImpl();
        ReflectionTestUtils.setField(service, "orderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "guideAppointmentMapper", guideAppointmentMapper);
        ReflectionTestUtils.setField(service, "chatMessageMapper", chatMessageMapper);
        ReflectionTestUtils.setField(service, "orderService", orderService);
        return service;
    }

    @Test
    void createOrderWithoutDesignatedAttendantShouldCreatePublicUnpaidOrder() {
        AiGuideServiceImpl service = newService();
        GuideAppointment appointment = new GuideAppointment();
        appointment.setAppointmentNo("APP-1");
        appointment.setUserId(9);
        appointment.setPatientName("肖阳");
        appointment.setPatientPhone("18600010001");
        appointment.setHospitalName("都江堰市人民医院");
        appointment.setServiceTypeNumber(1);
        appointment.setServiceDate(LocalDate.now().plusDays(1).toString());
        appointment.setServiceStartTime("09:00");
        appointment.setServiceEndTime("11:00");
        appointment.setSymptoms(List.of("复诊"));
        appointment.setOtherRequirement("需要轮椅");

        User user = new User();
        user.setId(9);
        user.setName("肖阳");
        user.setAge(32);
        user.setSex("男");

        when(guideAppointmentMapper.selectByAppointmentNo("APP-1")).thenReturn(appointment);
        when(userMapper.findById(9)).thenReturn(user);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(1001);
            return 1;
        });

        CreateOrderRequest request = new CreateOrderRequest();
        request.setAppointmentNo("APP-1");
        request.setUserId(9);

        OrderCreateResponse response = service.createOrderBySelection(request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(captor.capture());
        Order created = captor.getValue();
        assertThat(created.getAttendantId()).isNull();
        assertThat(created.getAttendantName()).isNull();
        assertThat(created.getOrderStatus()).isZero();
        assertThat(created.getPaymentStatus()).isZero();
        assertThat(created.getContactPerson()).isEqualTo("肖阳");
        assertThat(created.getContactPhone()).isEqualTo("18600010001");
        assertThat(created.getSpecialRequirements()).isEqualTo("复诊");
        assertThat(created.getCustomRequirement()).isEqualTo("需要轮椅");
        assertThat(response.getOrderNo()).isNotBlank();
        assertThat(response.getPayAmount()).isGreaterThan(0);
    }

    @Test
    void paidPublicOrderShouldEnterWaitingHallInsteadOfAssignedWaitingConfirm() {
        AiGuideServiceImpl service = newService();
        Order order = new Order();
        order.setOrderId(1002);
        order.setOrderNo("ORD-1002");
        order.setUserId(9);
        order.setOrderStatus(0);
        order.setPaymentStatus(0);
        order.setOrderAmount(new BigDecimal("50.00"));

        when(orderMapper.selectByOrderNo("ORD-1002")).thenReturn(order);

        service.updatePaymentStatus("ORD-1002", 1);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).updateByPrimaryKeySelective(captor.capture());
        Order patch = captor.getValue();
        assertThat(patch.getPaymentStatus()).isEqualTo(1);
        assertThat(patch.getOrderStatus()).isEqualTo(1);
        assertThat(patch.getAttendantId()).isNull();
        verify(orderService).publishOrderEvent(eq(order), eq("ORDER_STATUS_CHANGED"), eq(null), eq(null), eq(true), eq(false));
        verify(orderService).broadcastWaitingOrderUpdate(order);
    }

    @Test
    void createOrderWithDesignatedAttendantShouldKeepAiGuideExclusiveDispatch() {
        AiGuideServiceImpl service = newService();
        GuideAppointment appointment = new GuideAppointment();
        appointment.setAppointmentNo("APP-AI-1");
        appointment.setUserId(9);
        appointment.setPatientName("肖阳");
        appointment.setPatientPhone("18600010001");
        appointment.setHospitalName("都江堰市人民医院");
        appointment.setServiceTypeNumber(1);
        appointment.setServiceDate(LocalDate.now().plusDays(1).toString());
        appointment.setServiceStartTime("09:00");
        appointment.setServiceEndTime("11:00");

        User user = new User();
        user.setId(9);
        user.setName("肖阳");

        User attendant = new User();
        attendant.setId(21);
        attendant.setName("王小花");

        when(guideAppointmentMapper.selectByAppointmentNo("APP-AI-1")).thenReturn(appointment);
        when(userMapper.findById(9)).thenReturn(user);
        when(userMapper.findById(21)).thenReturn(attendant);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(1003);
            return 1;
        });

        CreateOrderRequest request = new CreateOrderRequest();
        request.setAppointmentNo("APP-AI-1");
        request.setUserId(9);
        request.setDesignatedAttendantId(21L);

        service.createOrderBySelection(request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(captor.capture());
        Order created = captor.getValue();
        assertThat(created.getAttendantId()).isEqualTo(21);
        assertThat(created.getAttendantName()).isEqualTo("王小花");
        assertThat(created.getOrderStatus()).isZero();
        assertThat(created.getPaymentStatus()).isZero();
    }

    @Test
    void paidDesignatedOrderShouldKeepExclusiveDispatchStatusForAiGuideSelection() {
        AiGuideServiceImpl service = newService();
        Order order = new Order();
        order.setOrderId(1004);
        order.setOrderNo("ORD-1004");
        order.setUserId(9);
        order.setAttendantId(21);
        order.setOrderStatus(0);
        order.setPaymentStatus(0);
        order.setOrderAmount(new BigDecimal("50.00"));

        when(orderMapper.selectByOrderNo("ORD-1004")).thenReturn(order);

        service.updatePaymentStatus("ORD-1004", 1);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).updateByPrimaryKeySelective(captor.capture());
        Order patch = captor.getValue();
        assertThat(patch.getPaymentStatus()).isEqualTo(1);
        assertThat(patch.getOrderStatus()).isEqualTo(8);
        assertThat(patch.getAttendantId()).isEqualTo(21);
        verify(orderService).publishOrderEvent(eq(order), eq("ORDER_STATUS_CHANGED"), eq(null), eq(null), eq(true), eq(true));
    }
}
