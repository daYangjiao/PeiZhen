package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.ResponseResult;
import org.example.model.Attendant;
import org.example.model.Order;
import org.example.model.User;
import org.example.model.request.OrderListQueryRequest;
import org.example.model.response.OrderListResponse;
import org.example.model.response.PagedResponse;
import org.example.model.OrderEvaluation;
import org.example.service.AttendantService;
import org.example.service.OrderEvaluationService;
import org.example.service.OrderService;
import org.example.service.UserService;
import org.example.util.AuthUtil;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 陪诊师端接口控制器
 */
@RestController
@RequestMapping("/attendant")
@Api(tags = "陪诊师端接口")
@RequiredArgsConstructor
@Slf4j
public class AttendantController {

    private final OrderService orderService;
    private final AttendantService attendantService;
    private final UserService userService;
    private final OrderEvaluationService evaluationService;

    /**
     * 获取陪诊师个人资料
     */
    @GetMapping("/profile/{userId}")
    @ApiOperation("获取陪诊师个人资料")
    public ResponseResult<Map<String, Object>> getProfile(@PathVariable Integer userId) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseResult.error("用户不存在");
            }

            Attendant attendant = attendantService.findByUserId(userId);
            
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", user.getId());
            profile.put("username", user.getUsername());
            profile.put("name", user.getName() != null ? user.getName() : user.getUsername());
            profile.put("phone", user.getPhone());
            profile.put("avatarUrl", user.getAvatar());
            
            if (attendant != null) {
                profile.put("certificate", attendant.getCertificate());
                profile.put("score", attendant.getScore());
                profile.put("introduction", attendant.getIntroduction());
                profile.put("professionalField", attendant.getProfessionalField());
                profile.put("experienceYears", attendant.getExperienceYears());
            }
            
            profile.put("totalOrders", 0);
            profile.put("completedOrders", 0);
            profile.put("totalEarnings", 0.00);
            profile.put("balance", 0.00);

            return ResponseResult.success(profile);
        } catch (Exception e) {
            log.error("获取陪诊师资料失败", e);
            return ResponseResult.error("获取资料失败");
        }
    }

    /**
     * 获取待接单订单列表
     * @param serviceType 服务类型（1=普通陪诊 2=术后护理 3=急诊陪同 4=上门陪诊），不传为不限
     * @param expectedDurationMinHours 预计时长下限（小时）：2=大于2小时 3=大于3小时 4=大于4小时，不传为不限
     * @param orderAmountMax 基础费用上限（元）：80/100/150，不传为不限
     */
    @GetMapping("/orders/waiting")
    @ApiOperation("获取待接单订单列表")
    public ResponseResult<PagedResponse<OrderListResponse>> getWaitingOrders(
            @ApiParam("页码") @RequestParam(defaultValue = "0") Integer page,
            @ApiParam("每页大小") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam("服务类型") @RequestParam(required = false) Integer serviceType,
            @ApiParam("预计时长下限(小时)") @RequestParam(required = false) Integer expectedDurationMinHours,
            @ApiParam("基础费用上限(元)") @RequestParam(required = false) BigDecimal orderAmountMax) {
        
        OrderListQueryRequest queryRequest = new OrderListQueryRequest();
        queryRequest.setPage(page);
        queryRequest.setSize(size);
        queryRequest.setOrderStatus(1); // 1=待接单
        if (serviceType != null) queryRequest.setServiceType(serviceType);
        if (expectedDurationMinHours != null) queryRequest.setExpectedDurationMinHours(expectedDurationMinHours);
        if (orderAmountMax != null) queryRequest.setOrderAmountMax(orderAmountMax);
        
        try {
            PagedResponse<OrderListResponse> result = orderService.getUserOrdersWithPagination(null, queryRequest);
            return ResponseResult.success(result);
        } catch (Exception e) {
            log.error("获取待接单订单列表失败", e);
            return ResponseResult.error("获取订单列表失败");
        }
    }

    /**
     * 陪诊师接单
     */
    @PostMapping("/orders/{orderId}/accept")
    @ApiOperation("陪诊师接单")
    public ResponseResult<String> acceptOrder(
            @ApiParam("订单ID") @PathVariable Integer orderId,
            @ApiParam("陪诊师ID") @RequestParam Integer attendantId) {
        
        try {
            String result = orderService.attendantAcceptOrder(orderId, attendantId);
            if ("接单成功".equals(result)) {
                return ResponseResult.success(result);
            } else {
                return ResponseResult.error(result);
            }
        } catch (Exception e) {
            log.error("陪诊师接单失败，订单ID: {}", orderId, e);
            return ResponseResult.error("接单失败");
        }
    }

    /**
     * 开始服务
     */
    @PostMapping("/orders/{orderId}/start")
    @ApiOperation("开始服务")
    public ResponseResult<String> startService(
            @ApiParam("订单ID") @PathVariable Integer orderId) {
        
        try {
            String result = orderService.startService(orderId);
            if ("服务开始成功".equals(result)) {
                return ResponseResult.success(result);
            } else {
                return ResponseResult.error(result);
            }
        } catch (Exception e) {
            log.error("开始服务失败，订单ID: {}", orderId, e);
            return ResponseResult.error("开始服务失败");
        }
    }

    /**
     * 结束服务
     */
    @PostMapping("/orders/{orderId}/end")
    @ApiOperation("结束服务")
    public ResponseResult<String> endService(
            @ApiParam("订单ID") @PathVariable Integer orderId,
            @ApiParam("实际服务时长") @RequestParam BigDecimal actualDuration) {
        
        try {
            String result = orderService.endService(orderId, actualDuration);
            // 只要返回不是明显错误提示，就认为成功
            if (result != null && result.startsWith("服务结束成功")) {
                return ResponseResult.success(result);
            } else {
                return ResponseResult.error(result != null ? result : "结束服务失败");
            }
        } catch (Exception e) {
            log.error("结束服务失败，订单ID: {}", orderId, e);
            return ResponseResult.error("结束服务失败");
        }
    }

    /**
     * 更新服务进度（已到院/候诊中/检查中/就诊完成）
     */
    @PostMapping("/orders/{orderId}/service-progress")
    @ApiOperation("更新服务进度")
    public ResponseResult<String> updateServiceProgress(
            @ApiParam("订单ID") @PathVariable Integer orderId,
            @ApiParam("服务进度步骤：1=已到院,2=候诊中,3=检查中,4=就诊完成") @RequestParam Integer step,
            HttpServletRequest request) {

        try {
            String result = orderService.updateServiceProgress(orderId, step);
            if (result != null && result.startsWith("服务进度已更新")) {
                return ResponseResult.success(result);
            } else {
                return ResponseResult.error(result != null ? result : "更新服务进度失败");
            }
        } catch (Exception e) {
            log.error("更新服务进度失败，订单ID: {}", orderId, e);
            return ResponseResult.error("更新服务进度失败");
        }
    }

    /**
     * 获取陪诊师的订单列表
     */
    @GetMapping("/orders")
    @ApiOperation("获取陪诊师订单列表")
    public ResponseResult<PagedResponse<OrderListResponse>> getAttendantOrders(
            @ApiParam("陪诊师ID") @RequestParam Integer attendantId,
            @ApiParam("页码") @RequestParam(defaultValue = "0") Integer page,
            @ApiParam("每页大小") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam("订单状态") @RequestParam(required = false) Integer orderStatus) {
        
        OrderListQueryRequest queryRequest = new OrderListQueryRequest();
        queryRequest.setPage(page);
        queryRequest.setSize(size);
        queryRequest.setOrderStatus(orderStatus);
        
        try {
            List<Order> allMatchingOrders = orderService.findAllOrders(); // 获取所有订单
            List<OrderListResponse> filteredList = allMatchingOrders.stream()
                .filter(o -> {
                    // 状态过滤
                    if (orderStatus != null && !o.getOrderStatus().equals(orderStatus)) return false;
                    // 陪诊师过滤
                    return o.getAttendantId() != null && o.getAttendantId().equals(attendantId);
                })
                .map(this::convertToOrderListResponse)
                .collect(Collectors.toList());
            
            // 手动分页
            int total = filteredList.size();
            int start = page * size;
            int end = Math.min(start + size, total);
            List<OrderListResponse> pagedList;
            
            if (start >= total) {
                pagedList = java.util.Collections.emptyList();
            } else {
                pagedList = filteredList.subList(start, end);
            }
            
            return ResponseResult.success(new PagedResponse<>(pagedList, total, page, size));
            
        } catch (Exception e) {
            log.error("获取陪诊师订单列表失败", e);
            return ResponseResult.error("获取订单列表失败");
        }
    }
    
    // 辅助方法：转换 Order 为 OrderListResponse（仅接单后填充用户头像）
    private OrderListResponse convertToOrderListResponse(Order order) {
        OrderListResponse res = new OrderListResponse();
        res.setOrderId(order.getOrderId());
        res.setOrderNo(order.getOrderNo());
        res.setHospital(order.getHospital());
        res.setPatientName(order.getPatientName());
        res.setPatientAge(order.getPatientAge());
        res.setPatientSex(order.getPatientSex());
        res.setServiceDate(order.getServiceDate());
        res.setServiceTimeSlot(order.getServiceTimeSlot());
        res.setOrderAmount(order.getOrderAmount());
        res.setOrderStatus(order.getOrderStatus());
        res.setOrderStatusDesc(getOrderStatusDesc(order.getOrderStatus()));
        res.setPaymentStatus(order.getPaymentStatus());
        res.setPaymentStatusDesc(order.getPaymentStatus() == 1 ? "已支付" : "待支付");
        res.setServiceTypeName(order.getServiceContent());
        res.setCreateTime(order.getCreateTime());
        res.setAttendantName(order.getAttendantName());
        res.setSpecialRequirements(order.getSpecialRequirements());
        res.setCustomRequirement(order.getCustomRequirement());
        if (order.getOrderStatus() != null && order.getOrderStatus() >= 2 && order.getUserId() != null) {
            User user = userService.findById(order.getUserId());
            if (user != null && user.getAvatar() != null) {
                res.setUserAvatar(user.getAvatar());
            }
        }
        return res;
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

    /**
     * 获取订单详情
     */
    @GetMapping("/orders/{orderId}")
    @ApiOperation("获取订单详情")
    public ResponseResult<Order> getOrderDetail(
            @ApiParam("订单ID") @PathVariable Integer orderId,
            HttpServletRequest request) {
        
        try {
            Order order = orderService.getOrderById(orderId);
            if (order != null) {
                // 仅接单后返回用户头像（待服务/服务中/已完成都展示），待接单(1)不返回
                if (order.getOrderStatus() != null && order.getOrderStatus() >= 2 && order.getUserId() != null) {
                    User user = userService.findById(order.getUserId());
                    if (user != null && user.getAvatar() != null) {
                        order.setUserAvatar(user.getAvatar());
                    }
                }
                return ResponseResult.success(order);
            } else {
                return ResponseResult.error("订单不存在");
            }
        } catch (Exception e) {
            log.error("获取订单详情失败，订单ID: {}", orderId, e);
            return ResponseResult.error("获取订单详情失败");
        }
    }

    /**
     * 陪诊师取消订单（仅待服务/待核销状态可取消）
     */
    @RequestMapping(value = "/orders/{orderId}/cancel", method = {RequestMethod.POST, RequestMethod.PUT})
    @ApiOperation("陪诊师取消订单；预约开始前取消则订单释放回接单大厅并通知用户，否则按已取消处理")
    public ResponseResult<String> cancelOrder(
            @ApiParam("订单ID") @PathVariable Integer orderId,
            @ApiParam("取消原因") @RequestParam(required = false) String reason,
            @ApiParam("违约金金额") @RequestParam(required = false) BigDecimal penaltyAmount,
            @ApiParam("退款金额") @RequestParam(required = false) BigDecimal refundAmount,
            @ApiParam("违约金比例") @RequestParam(required = false) BigDecimal penaltyRate) {
        try {
            log.info("陪诊师取消订单请求，orderId={}, reason={}, penaltyAmount={}, refundAmount={}, penaltyRate={}",
                    orderId, reason, penaltyAmount, refundAmount, penaltyRate);
            String result = orderService.attendantCancelOrder(orderId, reason, penaltyAmount, refundAmount, penaltyRate);
            return ResponseResult.success(result);
        } catch (Exception e) {
            log.error("取消订单失败，订单ID: {}", orderId, e);
            String msg = e.getMessage() != null ? e.getMessage() : "取消订单失败";
            return ResponseResult.error(msg);
        }
    }

    /**
     * 扫描二维码确认服务开始
     */
    @PostMapping("/orders/{orderId}/scan-qr")
    @ApiOperation("扫描二维码确认服务开始")
    public ResponseResult<String> scanQrCode(
            @ApiParam("订单ID") @PathVariable Integer orderId,
            @ApiParam("二维码内容") @RequestParam String qrCodeContent) {
        
        try {
            String expectedQrCode = "SERVICE_CONFIRM_" + orderId;
            if (!expectedQrCode.equals(qrCodeContent)) {
                return ResponseResult.error("二维码无效");
            }
            
            String result = orderService.startService(orderId);
            if ("服务开始成功".equals(result)) {
                return ResponseResult.success("扫码成功，服务已开始");
            } else {
                return ResponseResult.error(result);
            }
        } catch (Exception e) {
            log.error("扫描二维码失败，订单ID: {}", orderId, e);
            return ResponseResult.error("扫码失败");
        }
    }
    
    /**
     * 陪诊师获取订单评价（仅已完成订单，且当前用户为该订单陪诊师）
     */
    @GetMapping("/orders/{orderId}/evaluation")
    @ApiOperation("陪诊师获取订单评价")
    public ResponseResult<OrderEvaluation> getOrderEvaluation(
            @ApiParam("订单ID") @PathVariable Integer orderId,
            HttpServletRequest request) {
        try {
            Integer currentUserId = AuthUtil.getCurrentUserId(request);
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                return ResponseResult.error("订单不存在");
            }
            if (order.getAttendantId() == null || !order.getAttendantId().equals(currentUserId)) {
                return ResponseResult.unauthorized("无权限查看该订单评价");
            }
            if (order.getOrderStatus() == null || order.getOrderStatus() != 6) {
                return ResponseResult.error("仅已完成的订单可查看评价");
            }
            OrderEvaluation evaluation = evaluationService.getByOrderId(orderId);
            return ResponseResult.success(evaluation);
        } catch (Exception e) {
            log.error("获取订单评价失败, orderId={}", orderId, e);
            return ResponseResult.error("获取评价失败");
        }
    }

    /**
     * 陪诊师回复订单评价
     */
    @PostMapping("/orders/{orderId}/evaluation/reply")
    @ApiOperation("陪诊师回复订单评价")
    public ResponseResult<String> replyToEvaluation(
            @ApiParam("订单ID") @PathVariable Integer orderId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            Integer currentUserId = AuthUtil.getCurrentUserId(request);
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                return ResponseResult.error("订单不存在");
            }
            if (order.getAttendantId() == null || !order.getAttendantId().equals(currentUserId)) {
                return ResponseResult.unauthorized("无权限回复该订单评价");
            }
            if (order.getOrderStatus() == null || order.getOrderStatus() != 6) {
                return ResponseResult.error("仅已完成的订单可回复评价");
            }
            String replyContent = body != null ? body.get("reply") : null;
            if (replyContent == null || replyContent.trim().isEmpty()) {
                return ResponseResult.error("回复内容不能为空");
            }
            evaluationService.replyToEvaluation(orderId, replyContent.trim());
            return ResponseResult.success("回复成功");
        } catch (IllegalArgumentException e) {
            return ResponseResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("回复订单评价失败, orderId={}", orderId, e);
            return ResponseResult.error("回复失败");
        }
    }

    /**
     * 获取推荐陪诊师列表
     */
    @GetMapping("/recommended")
    @ApiOperation("获取推荐陪诊师列表")
    public ResponseResult<List<Map<String, Object>>> getRecommendedAttendants() {
        try {
            List<Attendant> attendants = attendantService.findRecommended();
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            
            for (Attendant attendant : attendants) {
                User user = userService.findById(attendant.getUserId());
                if (user != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("name", user.getName());
                    map.put("avatar", user.getAvatar());
                    map.put("professionalField", attendant.getProfessionalField());
                    map.put("score", attendant.getScore());
                    map.put("experienceYears", attendant.getExperienceYears());
                    result.add(map);
                }
            }
            return ResponseResult.success(result);
        } catch (Exception e) {
            log.error("获取推荐陪诊师失败", e);
            return ResponseResult.error("获取推荐失败");
        }
    }
}