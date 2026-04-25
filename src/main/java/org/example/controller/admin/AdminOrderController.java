package org.example.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.common.ResponseResult;
import org.example.model.request.AdminOrderCancelRequest;
import org.example.model.request.AdminOrderDisputeResolutionRequest;
import org.example.model.response.AdminOrderDetailResponse;
import org.example.model.response.AdminOrderListItemResponse;
import org.example.model.response.PagedResponse;
import org.example.service.AdminService;
import org.example.util.AuthUtil;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminService adminService;

    @GetMapping
    public ResponseResult<PagedResponse<AdminOrderListItemResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer orderStatus,
            @RequestParam(required = false) Integer paymentStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ResponseResult.success(adminService.getOrders(keyword, orderStatus, paymentStatus, startDate, endDate, page, pageSize));
    }

    @GetMapping("/{orderId}")
    public ResponseResult<AdminOrderDetailResponse> detail(@PathVariable Integer orderId,
                                                           HttpServletRequest request) {
        try {
            return ResponseResult.success(adminService.getOrderDetail(AuthUtil.getCurrentAdminId(request), orderId));
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseResult<Void> cancel(@PathVariable Integer orderId,
                                       @RequestBody(required = false) AdminOrderCancelRequest request,
                                       HttpServletRequest httpServletRequest) {
        try {
            adminService.cancelOrder(AuthUtil.getCurrentAdminId(httpServletRequest), orderId, request);
            return ResponseResult.success(null);
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }

    @PatchMapping("/{orderId}/dispute-resolution")
    public ResponseResult<Void> resolveDispute(@PathVariable Integer orderId,
                                               @RequestBody(required = false) AdminOrderDisputeResolutionRequest request,
                                               HttpServletRequest httpServletRequest) {
        try {
            adminService.resolveDispute(AuthUtil.getCurrentAdminId(httpServletRequest), orderId, request);
            return ResponseResult.success(null);
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }
}
