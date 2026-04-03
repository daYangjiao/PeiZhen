package org.example.service.impl;

import org.example.dao.ChatMessageMapper;
import org.example.dao.GuideAppointmentMapper;
import org.example.dao.OrderMapper;
import org.example.dao.UserMapper;
import org.example.exception.AppointmentValidationException;
import org.example.model.*;
import org.example.model.request.CreateOrderRequest;
import org.example.model.response.AppointmentResponse;
import org.example.model.response.AttendantMatchResponse;
import org.example.model.response.CompleteOrderInfoResponse;
import org.example.model.response.SimpleOrderDetailResponse;
import org.example.service.AiGuideService;
import org.example.service.AttendantService;
import org.example.unity.ServiceFeeCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class AiGuideServiceImpl implements AiGuideService {

    private static final Logger logger = LoggerFactory.getLogger(AiGuideServiceImpl.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GuideAppointmentMapper guideAppointmentMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private AttendantService attendantService;

    @Override
    @Transactional
    public OrderCreateResponse createOrderBySelection(CreateOrderRequest request) {
        logger.info("创建陪诊订单：{}", request);
        GuideAppointment appointment = guideAppointmentMapper.selectByAppointmentNo(request.getAppointmentNo());
        if (appointment == null) {
            throw new RuntimeException("预约信息不存在，预约编号：" + request.getAppointmentNo());
        }

        Integer targetUserId = appointment.getUserId() != null ? appointment.getUserId() : request.getUserId();
        if (targetUserId == null) {
            throw new IllegalStateException("预约缺少用户信息，请重新提交预约");
        }

        User user = userMapper.findById(targetUserId);
        if (user == null) {
            throw new IllegalStateException("当前登录状态已失效，请重新登录后再预约");
        }

        double durationHours = calculateServiceDurationFromAppointment(appointment);
        ServiceFeeCalculator.FeeCalculationResult feeResult =
            ServiceFeeCalculator.calculateFee(appointment.getServiceTypeNumber(), durationHours);

        Order order = new Order();
        String orderNo = "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);
        order.setOrderNo(orderNo);
        order.setUserId(user.getId());
        order.setOrderDate(new Date());
        order.setCreateTime(new Date());

        order.setPatientName(appointment.getPatientName());
        order.setPatientAge(user.getAge());
        order.setPatientSex(user.getSex());
        order.setContactPerson(appointment.getPatientName());
        order.setContactPhone(appointment.getPatientPhone());

        order.setHospital(appointment.getHospitalName());
        order.setServiceContent(getServiceTypeName(appointment.getServiceTypeNumber()));
        order.setClinicType(appointment.getServiceTypeNumber());
        order.setServiceDate(appointment.getServiceDate());
        order.setServiceTimeSlot(appointment.getServiceStartTime() + "-" + appointment.getServiceEndTime());

        if (appointment.getSymptoms() != null && !appointment.getSymptoms().isEmpty()) {
            String symptomsStr = String.join(",", appointment.getSymptoms());
            order.setSpecialRequirements(symptomsStr);
        }

        String otherReq = appointment.getOtherRequirement();
        if (otherReq != null && !otherReq.trim().isEmpty()) {
            order.setCustomRequirement(otherReq);
        } else {
            order.setCustomRequirement("无");
        }

        order.setOrderAmount(feeResult.getTotalFee());
        order.setCreateTime(new Date());
        order.setPaymentStatus(0);
        order.setOrderStatus(0);

        orderMapper.insert(order);

        // 下单成功系统消息：提醒用户在15分钟内完成预付款
        try {
            if (order.getUserId() != null) {
                String msg = "您已成功创建订单 " + orderNo + "，请在15分钟内完成预付款，逾期系统将自动取消订单。";
                sendSystemMessage(order.getUserId(), msg);
            }
        } catch (Exception e) {
            logger.error("发送下单成功系统消息失败, orderNo={}", orderNo, e);
        }

        OrderCreateResponse response = new OrderCreateResponse();
        response.setOrderNo(orderNo);
        response.setPayAmount(feeResult.getTotalFee().doubleValue());

        return response;
    }

    @Override
    @Transactional
    public void updatePaymentStatus(String orderNo, Integer paymentStatus) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) return;

        order.setPaymentStatus(paymentStatus);
        if (paymentStatus == 1) {
            order.setPaymentTime(new Date());
            if (order.getOrderStatus() == 0) {
                order.setOrderStatus(1);
                // 支付成功时发送系统消息（仅当订单状态从0变为1时）
                sendSystemMessage(order.getUserId(), "恭喜您!订单No." + orderNo + "支付完成，我们已通知陪诊师为您服务。陪诊师将在30分钟内与您联系，请保持电话畅通。");
            }
        }
        orderMapper.updateByPrimaryKeySelective(order);
    }

    @Override
    public CompleteOrderInfoResponse getCompleteOrderInfo(String orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) throw new RuntimeException("订单不存在");

        CompleteOrderInfoResponse response = new CompleteOrderInfoResponse();
        response.setOrderNo(order.getOrderNo());
        response.setOrderId(order.getOrderId()); // 补充 OrderId
        response.setOrderStatus(order.getOrderStatus());
        response.setOrderStatusDesc(getOrderStatusDesc(order.getOrderStatus()));
        response.setPaymentStatus(order.getPaymentStatus());
        response.setPaymentStatusDesc(getOrderPaymentStatusDesc(order.getPaymentStatus()));
        response.setTotalPrice(order.getOrderAmount());

        // 创建/支付/更新时间（用于前端倒计时与服务记录）
        SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (order.getCreateTime() != null) {
            response.setCreateTime(dtf.format(order.getCreateTime()));
        }
        if (order.getPaymentTime() != null) {
            response.setPaymentTime(dtf.format(order.getPaymentTime()));
        }
        if (order.getAcceptTime() != null) {
            response.setAcceptTime(dtf.format(order.getAcceptTime()));
        }
        Date updateTime = order.getServiceEndTime();
        if (updateTime == null) {
            updateTime = order.getCancelTime();
        }
        if (updateTime == null) {
            updateTime = order.getServiceStartTime();
        }
        if (updateTime == null) {
            updateTime = order.getAcceptTime();
        }
        if (updateTime == null) {
            updateTime = order.getPaymentTime();
        }
        if (updateTime == null) {
            updateTime = order.getCreateTime();
        }
        if (updateTime != null) {
            response.setUpdateTime(dtf.format(updateTime));
        }

        // 补充核销二维码和状态信息
        response.setQrCodeUrl(order.getQrCodeUrl());
        response.setActualDuration(order.getActualDuration());
        response.setEstimatedDuration(order.getEstimatedDuration());
        response.setBalanceAmount(order.getBalanceAmount());
        response.setCancelReason(order.getCancelReason());
        response.setCancelBy(order.getCancelBy());
        response.setPenaltyRate(order.getPenaltyRate());
        response.setPenaltyAmount(order.getPenaltyAmount());
        response.setRefundAmount(order.getRefundAmount());
        if (order.getServiceStartTime() != null) {
            response.setServiceStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(order.getServiceStartTime()));
        }
        if (order.getServiceEndTime() != null) {
            response.setServiceEndTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(order.getServiceEndTime()));
        }
        if (order.getCancelTime() != null) {
            response.setCancelTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(order.getCancelTime()));
        }

        // 服务进度
        response.setServiceProgressStep(order.getServiceProgressStep());

        response.setHospital(order.getHospital());
        response.setServiceDate(order.getServiceDate());
        response.setServiceTime(order.getServiceTimeSlot());
        response.setPatientName(order.getPatientName());
        response.setPatientPhone(order.getContactPhone());

        if (order.getSpecialRequirements() != null && !order.getSpecialRequirements().isEmpty()) {
            response.setSymptoms(Arrays.asList(order.getSpecialRequirements().split(",")));
        }

        String customReq = order.getCustomRequirement();
        response.setOtherRequirement((customReq != null && !customReq.isEmpty()) ? customReq : "无");

        response.setServiceTypeName(order.getServiceContent());

        if (order.getAttendantId() != null) {
            // 回传陪诊师ID，便于用户端跳转在线聊天
            response.setAttendantId(String.valueOf(order.getAttendantId()));
            User attendantUser = userMapper.findById(order.getAttendantId());
            if (attendantUser != null) {
                response.setAttendantName(attendantUser.getName());
                response.setAttendantPhone(attendantUser.getPhone());
                response.setAttendantAvatar(attendantUser.getAvatar());
            }
        }

        return response;
    }

    @Override
    @Transactional
    public AppointmentResponse submitDemand(GuideAppointmentRequest request) {
        validateAppointmentSchedule(request);

        String appointmentNo = "APP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);
        GuideAppointment appointment = new GuideAppointment();
        appointment.setAppointmentNo(appointmentNo);
        appointment.setUserId(request.getUserId());
        appointment.setHospitalName(request.getHospital());
        appointment.setServiceDate(request.getServiceDate());
        appointment.setServiceStartTime(request.getServiceStartTime());
        appointment.setServiceEndTime(request.getServiceEndTime());
        appointment.setServiceTypeNumber(request.getServiceTypeNumber());
        appointment.setPatientName(request.getPatientName());
        appointment.setPatientPhone(request.getPatientPhone());
        appointment.setSymptoms(request.getSymptoms());
        appointment.setOtherRequirement(request.getOtherRequirement());
        appointment.setCreateTime(new Date());

        guideAppointmentMapper.insertGuideAppointment(appointment);

        AppointmentResponse response = new AppointmentResponse();
        response.setAppointmentNo(appointmentNo);
        response.setMessage("需求提交成功");
        return response;
    }

    private void validateAppointmentSchedule(GuideAppointmentRequest request) {
        final LocalDate appointmentDate;
        try {
            appointmentDate = LocalDate.parse(String.valueOf(request.getServiceDate()).trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new AppointmentValidationException("服务日期格式错误，请重新选择");
        }

        LocalDate today = LocalDate.now();
        if (appointmentDate.isBefore(today)) {
            throw new AppointmentValidationException("不能预约过去的日期");
        }

        String startTimeRaw = String.valueOf(request.getServiceStartTime()).trim();
        if (startTimeRaw.isEmpty()) {
            throw new AppointmentValidationException("服务开始时间不能为空");
        }

        final LocalTime appointmentStartTime;
        try {
            appointmentStartTime = parseStartTime(startTimeRaw);
        } catch (DateTimeParseException e) {
            throw new AppointmentValidationException("服务开始时间格式错误，请重新选择");
        }

        String endTimeRaw = String.valueOf(request.getServiceEndTime()).trim();
        if (endTimeRaw.isEmpty()) {
            throw new AppointmentValidationException("服务结束时间不能为空");
        }

        final LocalTime appointmentEndTime;
        try {
            appointmentEndTime = parseStartTime(endTimeRaw);
        } catch (DateTimeParseException e) {
            throw new AppointmentValidationException("服务结束时间格式错误，请重新选择");
        }

        long durationMinutes = calculateSlotDurationMinutes(appointmentStartTime, appointmentEndTime);
        if (durationMinutes <= 0) {
            throw new AppointmentValidationException("结束时间需晚于开始时间");
        }

        if (appointmentDate.isEqual(today)) {
            LocalTime minAllowedStartTime = LocalTime.now().plusMinutes(30);
            if (appointmentStartTime.isBefore(minAllowedStartTime)) {
                throw new AppointmentValidationException("今日预约需至少提前30分钟");
            }
        }
    }

    private LocalTime parseStartTime(String timeValue) {
        String normalized = timeValue.trim();
        if (normalized.contains("-")) {
            normalized = normalized.split("-")[0].trim();
        }
        return LocalTime.parse(normalized, DateTimeFormatter.ofPattern("HH:mm"));
    }

    private long calculateSlotDurationMinutes(LocalTime startTime, LocalTime endTime) {
        int startMinutes = startTime.getHour() * 60 + startTime.getMinute();
        int endMinutes = endTime.getHour() * 60 + endTime.getMinute();
        if (endMinutes == startMinutes) {
            return 0;
        }
        return endMinutes > startMinutes
                ? endMinutes - startMinutes
                : 24 * 60L - startMinutes + endMinutes;
    }

    @Override
    public AttendantMatchResponse matchAttendantsByAppointmentNo(String appointmentNo) {
        List<Attendant> recommended = attendantService.findRecommended();
        List<AttendantMatchResponse.AttendantInfo> attendants = new ArrayList<>();

        for (Attendant att : recommended) {
            User user = userMapper.findById(att.getUserId());
            if (user != null) {
                AttendantMatchResponse.AttendantInfo info = new AttendantMatchResponse.AttendantInfo();
                info.setId(user.getId());
                info.setName(user.getName());
                info.setPhoto(user.getAvatar());
                info.setScore(att.getScore() != null ? att.getScore().doubleValue() : 5.0);
                info.setExperienceYears(att.getExperienceYears());
                info.setProfessionalField(att.getProfessionalField());
                attendants.add(info);
            }
        }

        AttendantMatchResponse response = new AttendantMatchResponse();
        response.setAttendants(attendants);
        response.setAppointmentNo(appointmentNo);
        return response;
    }

    @Override
    public SimpleOrderDetailResponse getAppointmentDetail(String appointmentNo) {
        GuideAppointment appointment = guideAppointmentMapper.selectByAppointmentNo(appointmentNo);
        if (appointment == null) throw new RuntimeException("预约不存在");

        SimpleOrderDetailResponse response = new SimpleOrderDetailResponse();
        response.setHospital(appointment.getHospitalName());
        response.setPatientName(appointment.getPatientName());
        response.setPatientPhone(appointment.getPatientPhone());
        response.setServiceDate(appointment.getServiceDate());
        response.setServiceTime(appointment.getServiceStartTime() + "-" + appointment.getServiceEndTime());
        response.setTotalPrice(BigDecimal.ZERO);

        response.setSymptoms(appointment.getSymptoms());
        response.setOtherRequirement(appointment.getOtherRequirement() != null ? appointment.getOtherRequirement() : "无");

        return response;
    }

    @Override
    public SimpleOrderDetailResponse getOrderDetail(String orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) throw new RuntimeException("订单不存在");

        SimpleOrderDetailResponse response = new SimpleOrderDetailResponse();
        response.setHospital(order.getHospital());
        response.setPatientName(order.getPatientName());
        response.setPatientPhone(order.getContactPhone());
        response.setServiceDate(order.getServiceDate());
        response.setServiceTime(order.getServiceTimeSlot());
        response.setTotalPrice(order.getOrderAmount());

        if (order.getSpecialRequirements() != null) {
            response.setSymptoms(Arrays.asList(order.getSpecialRequirements().split(",")));
        }
        response.setOtherRequirement(order.getCustomRequirement() != null ? order.getCustomRequirement() : "无");

        if (order.getAttendantId() != null) {
            User attUser = userMapper.findById(order.getAttendantId());
            if (attUser != null) {
                response.setAttendantName(attUser.getName());
                response.setAttendantAvatar(attUser.getAvatar());
            }
        }
        return response;
    }

    private double calculateServiceDurationFromAppointment(GuideAppointment appointment) {
        try {
            String[] start = appointment.getServiceStartTime().split(":");
            String[] end = appointment.getServiceEndTime().split(":");
            int h1 = Integer.parseInt(start[0]), m1 = Integer.parseInt(start[1]);
            int h2 = Integer.parseInt(end[0]), m2 = Integer.parseInt(end[1]);
            int startMinutes = h1 * 60 + m1;
            int endMinutes = h2 * 60 + m2;
            int durationMinutes = endMinutes > startMinutes
                    ? endMinutes - startMinutes
                    : (endMinutes < startMinutes ? 24 * 60 - startMinutes + endMinutes : 0);
            if (durationMinutes <= 0) {
                return 2.0;
            }
            return durationMinutes / 60.0;
        } catch (Exception e) {
            return 2.0;
        }
    }

    private String getServiceTypeName(int type) {
        return switch (type) {
            case 1 -> "普通陪诊";
            case 2 -> "术后护理";
            case 3 -> "急诊陪同";
            case 4 -> "上门陪诊";
            default -> "陪诊服务";
        };
    }

    private String getOrderStatusDesc(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "待接单";
            case 2 -> "待服务";
            case 3 -> "服务中";
            case 4 -> "待确认时长费用";
            case 5 -> "时长费用有争议";
            case 6 -> "已完成";
            case 7 -> "已取消";
            default -> "未知";
        };
    }

    private String getOrderPaymentStatusDesc(Integer status) {
        return status != null && status == 1 ? "已支付" : "待支付";
    }

    // 辅助方法：发送系统消息
    private void sendSystemMessage(Integer receiverId, String content) {
        try {
            ChatMessage sysMsg = new ChatMessage();
            sysMsg.setSenderId(0); // 0 代表系统
            sysMsg.setReceiverId(receiverId);
            sysMsg.setContent(content);
            sysMsg.setMsgType(1); // 文本
            sysMsg.setIsRead(false);
            sysMsg.setCreateTime(new Date());
            chatMessageMapper.insert(sysMsg);
        } catch (Exception e) {
            logger.error("发送系统消息失败", e);
        }
    }

    @Override public void submitAppointment(GuideAppointmentRequest request) {}
    @Override public List<MatchedAttendantVO> matchAttendantsFromAppointment(GuideAppointmentRequest request) { return null; }
    @Override public List<MatchedAttendantVO> matchAttendants(GuideDemandRequest request) { return null; }
    @Override public OrderCreateResponse createOrder(GuideOrderRequest request) { return null; }
    @Override public void updatePayStatus(String orderNo) {}
    @Override public Map<String, Object> getLatestAppointment() { return null; }
    @Override public Map<String, Object> getOrderPaymentStatus(String orderNo) { return null; }
    @Override public Map<String, Object> createTestOrderWithAppointment() { return null; }
}
