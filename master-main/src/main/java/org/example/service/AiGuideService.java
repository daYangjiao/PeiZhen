package org.example.service;

import org.example.model.*;
import org.example.model.request.CreateOrderRequest;
import org.example.model.request.SimplePaymentRequest;
import org.example.model.response.AppointmentResponse;
import org.example.model.response.AttendantMatchResponse;
import org.example.model.response.CompleteOrderInfoResponse;
import org.example.model.response.SimpleOrderDetailResponse;
import java.util.List;
import java.util.Map;

public interface AiGuideService {
    // 步骤1：提交需求并返回预约编号
    AppointmentResponse submitDemand(GuideAppointmentRequest request);

    // 步骤2：根据预约编号匹配陪诊师
    AttendantMatchResponse matchAttendantsByAppointmentNo(String appointmentNo);

    // 步骤3：选择陪诊师创建订单
    OrderCreateResponse createOrderBySelection(CreateOrderRequest request);

//    // 步骤4：处理支付成功回调
//    void handlePaymentCallback(PaymentCallbackRequest callbackRequest);
    
    // 简化版支付状态更新
    void updatePaymentStatus(String orderNo, Integer paymentStatus);

    // 步骤1：提交预约（移除预约ID相关逻辑）
    void submitAppointment(GuideAppointmentRequest request);

    // ===== 保留原有接口用于兼容 =====
    // 步骤1.5：从预约信息匹配陪诊师
    List<MatchedAttendantVO> matchAttendantsFromAppointment(GuideAppointmentRequest request);

    // 步骤2：匹配陪诊师（保留原有接口）
    List<MatchedAttendantVO> matchAttendants(GuideDemandRequest request);

    // 步骤3：创建订单（保留原有接口）
    OrderCreateResponse createOrder(GuideOrderRequest request);

    // 步骤4：查询订单详情
    SimpleOrderDetailResponse getOrderDetail(String orderNo);
    
    // 未下单前：根据预约编号获取预约详情（用于订单确认页）
    SimpleOrderDetailResponse getAppointmentDetail(String appointmentNo);
    
    // 测试方法：获取最新的预约信息
    Map<String, Object> getLatestAppointment();

    // 支付状态更新
    void updatePayStatus(String orderNo);
    
    // 查询订单支付状态
    Map<String, Object> getOrderPaymentStatus(String orderNo);
    
    // 支付成功后获取完整订单信息
    CompleteOrderInfoResponse getCompleteOrderInfo(String orderNo);
    
    // 创建测试订单（用于验证修复效果）
    Map<String, Object> createTestOrderWithAppointment();
}