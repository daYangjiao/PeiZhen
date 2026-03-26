package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
import springfox.documentation.annotations.ApiIgnore;

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

    @PostMapping("/appointments")
    @ApiOperation(value = "创建陪诊预约", notes = "AI导诊第一步：提交就诊需求，生成预约编号，供后续匹配陪诊师和创建订单使用。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "创建成功，返回预约编号和结果说明"),
            @ApiResponse(code = 400, message = "请求参数不完整或格式错误"),
            @ApiResponse(code = 500, message = "创建预约失败")
    })
    public ResponseEntity<AppointmentResponse> createAppointment(
            @ApiParam(value = "预约请求体", required = true)
            @Valid @RequestBody GuideAppointmentRequest request,
            @ApiIgnore BindingResult bindingResult,
            @ApiIgnore HttpServletRequest httpRequest) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldError().getDefaultMessage();
            throw new RuntimeException("参数错误：" + errorMsg);
        }

        Integer currentUserId = AuthUtil.getCurrentUserId(httpRequest);
        request.setUserId(currentUserId);
        
        AppointmentResponse response = aiGuideService.submitDemand(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/attendants/match")
    @ApiOperation(value = "匹配推荐陪诊师", notes = "根据预约编号匹配推荐陪诊师列表，用于用户选择服务人员。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "匹配成功，返回推荐陪诊师列表"),
            @ApiResponse(code = 400, message = "预约编号为空"),
            @ApiResponse(code = 404, message = "预约不存在"),
            @ApiResponse(code = 500, message = "匹配失败")
    })
    public ResponseEntity<AttendantMatchResponse> matchAttendants(
            @ApiParam(value = "预约编号", required = true, example = "APT202603200001")
            @RequestParam String appointmentNo) {
        if (appointmentNo == null || appointmentNo.trim().isEmpty()) {
            throw new RuntimeException("预约编号不能为空");
        }
        
        AttendantMatchResponse response = aiGuideService.matchAttendantsByAppointmentNo(appointmentNo);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/orders")
    @ApiOperation(value = "创建服务订单", notes = "AI导诊第三步：用户选定陪诊师后创建正式订单。当前登录用户 ID 从 token 自动注入。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "创建成功，返回订单编号、服务类型和支付金额"),
            @ApiResponse(code = 400, message = "请求参数不完整或格式错误"),
            @ApiResponse(code = 401, message = "用户未登录"),
            @ApiResponse(code = 500, message = "创建订单失败")
    })
    public ResponseEntity<OrderCreateResponse> createOrder(
            @ApiParam(value = "创建订单请求体", required = true)
            @Valid @RequestBody CreateOrderRequest request,
            @ApiIgnore BindingResult bindingResult,
            @ApiIgnore HttpServletRequest httpRequest) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldError().getDefaultMessage();
            throw new RuntimeException("参数错误：" + errorMsg);
        }

        Integer currentUserId = AuthUtil.getCurrentUserId(httpRequest);
        if (currentUserId == null) {
            throw new RuntimeException("用户未登录");
        }
        request.setUserId(currentUserId);
        
        OrderCreateResponse response = aiGuideService.createOrderBySelection(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payments/status")
    @ApiOperation(value = "更新支付状态", notes = "手动回写订单支付状态。paymentStatus 仅支持 1=成功、0=失败。返回字符串 1 表示成功，0 表示失败。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "更新成功，响应体为字符串 1"),
            @ApiResponse(code = 400, message = "参数不合法或更新失败，响应体为字符串 0")
    })
    public ResponseEntity<String> updatePaymentStatus(
            @ApiParam(value = "支付状态回写请求体", required = true)
            @Valid @RequestBody SimplePaymentRequest request,
            @ApiIgnore BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body("0");
        }

        try {
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

    @GetMapping("/orders/{orderNo}")
    @ApiOperation(value = "查询订单详情", notes = "根据订单编号查询简化版订单详情，适合支付完成后的确认页展示。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功"),
            @ApiResponse(code = 404, message = "订单不存在"),
            @ApiResponse(code = 500, message = "查询失败")
    })
    public ResponseEntity<SimpleOrderDetailResponse> getOrderDetail(
            @ApiParam(value = "订单编号", required = true, example = "ORD202603200001")
            @PathVariable String orderNo) {
        SimpleOrderDetailResponse response = aiGuideService.getOrderDetail(orderNo);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders/{orderNo}/payment-status")
    @ApiOperation(value = "查询支付状态", notes = "根据订单编号查询当前支付状态，通常用于前端轮询支付结果。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功"),
            @ApiResponse(code = 404, message = "订单不存在"),
            @ApiResponse(code = 500, message = "查询失败")
    })
    public ResponseEntity<Map<String, Object>> getOrderPaymentStatus(
            @ApiParam(value = "订单编号", required = true, example = "ORD202603200001")
            @PathVariable String orderNo) {
        Map<String, Object> result = aiGuideService.getOrderPaymentStatus(orderNo);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/orders/{orderNo}/complete-info")
    @ApiOperation(value = "获取完整订单信息", notes = "支付成功后返回订单、患者、陪诊师、费用、服务进度等完整信息，用于详情页和服务页。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功"),
            @ApiResponse(code = 404, message = "订单不存在"),
            @ApiResponse(code = 500, message = "查询失败")
    })
    public ResponseEntity<CompleteOrderInfoResponse> getCompleteOrderInfo(
            @ApiParam(value = "订单编号", required = true, example = "ORD202603200001")
            @PathVariable String orderNo) {
        CompleteOrderInfoResponse response = aiGuideService.getCompleteOrderInfo(orderNo);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test/create-order-with-appointment")
    @ApiOperation(value = "创建带预约关联的测试订单", notes = "测试接口：自动生成预约并创建关联订单，仅用于联调和演示，不建议前端正式环境调用。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "创建成功，返回测试订单信息"),
            @ApiResponse(code = 500, message = "创建测试订单失败")
    })
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
    @ApiOperation(value = "获取最新的预约信息用于测试", notes = "测试接口：读取最近创建的一条预约记录，便于联调匹配与下单流程。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功，返回最近预约信息"),
            @ApiResponse(code = 500, message = "获取预约信息失败")
    })
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
