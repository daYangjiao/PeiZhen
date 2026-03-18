package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.ResponseResult;
import org.example.model.Attendant;
import org.example.model.AttendantQualification;
import org.example.model.Order;
import org.example.model.User;
import org.example.model.request.AttendantQualificationUpdateRequest;
import org.example.model.request.AttendantProfileUpdateRequest;
import org.example.model.request.OrderListQueryRequest;
import org.example.model.response.AttendantProfileResponse;
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
    public ResponseResult<AttendantProfileResponse> getProfile(@PathVariable Integer userId) {
        try {
            AttendantProfileResponse profile = attendantService.getProfile(userId);
            if (profile == null) {
                return ResponseResult.error("用户不存在");
            }

            return ResponseResult.success(profile);
        } catch (Exception e) {
            log.error("获取陪诊师资料失败", e);
            return ResponseResult.error("获取资料失败");
        }
    }

    /**
     * 更新陪诊师个人资料
     */
    @PutMapping("/profile/{userId}")
    @ApiOperation("更新陪诊师个人资料")
    public ResponseResult<String> updateProfile(
            @PathVariable Integer userId,
            @RequestBody AttendantProfileUpdateRequest request) {
        try {
            if (request == null) {
                return ResponseResult.error("请求参数不能为空");
            }
            User existUser = userService.findById(userId);
            if (existUser == null) {
                return ResponseResult.error("用户不存在");
            }

            boolean hasUserUpdates = request.getName() != null
                    || request.getPhone() != null
                    || request.getAvatarUrl() != null;
            if (hasUserUpdates) {
                User user = new User();
                user.setId(userId);
                user.setName(request.getName());
                user.setPhone(request.getPhone());
                user.setAvatar(request.getAvatarUrl());
                userService.update(user);
            }

            boolean hasAttendantUpdates = request.getIntroduction() != null
                    || request.getProfessionalField() != null
                    || request.getExperienceYears() != null
                    || request.getHospitalName() != null
                    || request.getCertificate() != null;
            if (hasAttendantUpdates) {
                Attendant attendant = new Attendant();
                attendant.setUserId(userId);
                attendant.setIntroduction(request.getIntroduction());
                attendant.setProfessionalField(request.getProfessionalField());
                attendant.setExperienceYears(request.getExperienceYears());
                attendant.setHospitalName(request.getHospitalName());
                attendant.setCertificate(request.getCertificate());
                attendantService.update(attendant);
            }

            return ResponseResult.success("更新成功");
        } catch (Exception e) {
            log.error("更新陪诊师资料失败，userId={}", userId, e);
            return ResponseResult.error("更新失败");
        }
    }

    /**
     * 更新陪诊师三证资质信息
     */
    @PutMapping("/qualification/{userId}")
    @ApiOperation("更新陪诊师三证资质信息")
    public ResponseResult<String> updateQualification(
            @PathVariable Integer userId,
            @RequestBody AttendantQualificationUpdateRequest request) {
        try {
            if (request == null) {
                return ResponseResult.error("请求参数不能为空");
            }
            User existUser = userService.findById(userId);
            if (existUser == null) {
                return ResponseResult.error("用户不存在");
            }

            AttendantQualification qualification = new AttendantQualification();
            qualification.setUserId(userId);
            qualification.setIdCardUploaded(request.getIdCardUploaded());
            qualification.setPracticeCertUploaded(request.getPracticeCertUploaded());
            qualification.setHealthCertUploaded(request.getHealthCertUploaded());
            qualification.setIdCardFileUrl(request.getIdCardFileUrl());
            qualification.setIdCardFrontFileUrl(request.getIdCardFrontFileUrl());
            qualification.setIdCardBackFileUrl(request.getIdCardBackFileUrl());
            qualification.setPracticeCertFileUrl(request.getPracticeCertFileUrl());
            qualification.setHealthCertFileUrl(request.getHealthCertFileUrl());

            attendantService.updateQualification(userId, qualification);
            return ResponseResult.success("资质信息更新成功");
        } catch (Exception e) {
            log.error("更新陪诊师资质失败，userId={}", userId, e);
            return ResponseResult.error("资质信息更新失败");
        }
    }

    /**
     * 提交资质审核
     */
    @PostMapping("/qualification/{userId}/submit")
    @ApiOperation("提交资质审核")
    public ResponseResult<String> submitQualification(@PathVariable Integer userId) {
        try {
            User existUser = userService.findById(userId);
            if (existUser == null) {
                return ResponseResult.error("用户不存在");
            }
            String result = attendantService.submitQualification(userId);
            return ResponseResult.success(result);
        } catch (IllegalArgumentException e) {
            return ResponseResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("提交资质审核失败，userId={}", userId, e);
            return ResponseResult.error("提交资质审核失败");
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
            @ApiParam("陪诊师 ID") @RequestParam Integer attendantId,
            @ApiParam("页码") @RequestParam(defaultValue = "0") Integer page,
            @ApiParam("每页大小") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam("订单状态") @RequestParam(required = false) String orderStatusStr) {
        
        // 处理前端传来的 "null" 字符串
       Integer orderStatus= null;
      if (orderStatusStr != null && !"null".equals(orderStatusStr)) {
            try {
              orderStatus = Integer.valueOf(orderStatusStr);
            } catch (NumberFormatException e) {
                log.warn("无效的 orderStatus 值：{}", orderStatusStr);
            }
        }
        
        // 使用 final 变量供 lambda 表达式使用
       final Integer finalOrderStatus = orderStatus;
        
        OrderListQueryRequest queryRequest = new OrderListQueryRequest();
        queryRequest.setPage(page);
        queryRequest.setSize(size);
        queryRequest.setOrderStatus(orderStatus);
        
        try {
            List<Order> allMatchingOrders = orderService.findAllOrders(); // 获取所有订单
           List<OrderListResponse> filteredList = allMatchingOrders.stream()
               .filter(o -> {
                    // 状态过滤
                  if (finalOrderStatus != null && !o.getOrderStatus().equals(finalOrderStatus)) return false;
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
