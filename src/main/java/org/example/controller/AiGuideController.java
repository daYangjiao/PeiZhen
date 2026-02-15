package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.example.model.*;
import org.example.model.request.CreateOrderRequest;
import org.example.model.request.SimplePaymentRequest;
import org.example.model.response.AppointmentResponse;
import org.example.model.response.AttendantMatchResponse;
import org.example.model.response.SimpleOrderDetailResponse;
import org.example.model.response.CompleteOrderInfoResponse;
import org.example.service.AiGuideService;
import org.example.util.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/guide")
@Api(tags = "AI导诊模块", description = "陪诊预约需求匹配选陪诊师订单全流程")
public class AiGuideController {
    
    private static final Logger logger = LoggerFactory.getLogger(AiGuideController.class);

    @Autowired
    private AiGuideService aiGuideService;

    
    // ===== 陪诊预约核心流程接口 =====
    
    // 1. 预约创建接口
    @PostMapping("/appointments")
    @ApiOperation("创建陪诊预约")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody GuideAppointmentRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldError().getDefaultMessage();
            throw new RuntimeException("参数错误：" + errorMsg);
        }
        
        AppointmentResponse response = aiGuideService.submitDemand(request);
        return ResponseEntity.ok(response);
    }
    
    // 2. 陪诊师匹配接口
    @GetMapping("/attendants/match")
    @ApiOperation("匹配推荐陪诊师")
    public ResponseEntity<AttendantMatchResponse> matchAttendants(
            @RequestParam String appointmentNo) {
        // 手动验证参数
        if (appointmentNo == null || appointmentNo.trim().isEmpty()) {
            throw new RuntimeException("预约编号不能为空");
        }
        
        AttendantMatchResponse response = aiGuideService.matchAttendantsByAppointmentNo(appointmentNo);
        return ResponseEntity.ok(response);
    }
    
    // 3. 订单创建接口
    @PostMapping("/orders")
    @ApiOperation("创建服务订单")
    public ResponseEntity<OrderCreateResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest) { // 注入 HttpServletRequest
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldError().getDefaultMessage();
            throw new RuntimeException("参数错误：" + errorMsg);
        }
        
        // 获取当前用户ID
        Integer currentUserId = AuthUtil.getCurrentUserId(httpRequest);
        if (currentUserId == null) {
            throw new RuntimeException("用户未登录");
        }
        request.setUserId(currentUserId);
        
        OrderCreateResponse response = aiGuideService.createOrderBySelection(request);
        return ResponseEntity.ok(response);
    }
    
    // 4. 支付状态更新接口
    @PostMapping("/payments/status")
    @ApiOperation("更新支付状态（1=成功，0=失败）")
    public ResponseEntity<String> updatePaymentStatus(
            @Valid @RequestBody SimplePaymentRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body("0");
        }
        
        try {
            // 验证支付状态值
            if (request.getPaymentStatus() != 0 && request.getPaymentStatus() != 1) {
                return ResponseEntity.badRequest().body("0");
            }
            
            aiGuideService.updatePaymentStatus(request.getOrderNo(), request.getPaymentStatus());
            return ResponseEntity.ok("1");
        } catch (Exception e) {
            logger.error("支付状态更新失败", e);
            return ResponseEntity.badRequest().body("0");
        }
    }
    
    // ===== 订单查询接口 =====
    
    // 订单详情查询接口
    @GetMapping("/orders/{orderNo}")
    @ApiOperation("查询订单详情")
    public ResponseEntity<SimpleOrderDetailResponse> getOrderDetail(
            @PathVariable String orderNo) {
        SimpleOrderDetailResponse response = aiGuideService.getOrderDetail(orderNo);
        return ResponseEntity.ok(response);
    }
    
    // 订单支付状态查询接口
    @GetMapping("/orders/{orderNo}/payment-status")
    @ApiOperation("查询支付状态")
    public ResponseEntity<Map<String, Object>> getOrderPaymentStatus(
            @PathVariable String orderNo) {
        Map<String, Object> result = aiGuideService.getOrderPaymentStatus(orderNo);
        return ResponseEntity.ok(result);
    }
    
    // 支付成功后获取完整订单信息接口
    @GetMapping("/orders/{orderNo}/complete-info")
    @ApiOperation("获取完整订单信息")
    public ResponseEntity<CompleteOrderInfoResponse> getCompleteOrderInfo(
            @PathVariable String orderNo) {
        CompleteOrderInfoResponse response = aiGuideService.getCompleteOrderInfo(orderNo);
        return ResponseEntity.ok(response);
    }
    
    // ===== 新增：测试接口 =====
    
    @PostMapping("/test/create-order-with-appointment")
    @ApiOperation("创建带预约关联的测试订单")
    public ResponseEntity<Map<String, Object>> createTestOrderWithAppointment() {
        try {
            Map<String, Object> result = aiGuideService.createTestOrderWithAppointment();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("创建测试订单失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "创建测试订单失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResult);
        }
    }
    
    @GetMapping("/test/latest-appointment")
    @ApiOperation("获取最新的预约信息用于测试")
    public ResponseEntity<Map<String, Object>> getLatestAppointment() {
        try {
            Map<String, Object> result = aiGuideService.getLatestAppointment();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取最新预约信息失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "获取预约信息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResult);
        }
    }
}