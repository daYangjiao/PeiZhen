package org.example.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.common.ResponseResult;
import org.example.model.request.AdminAttendantReviewRequest;
import org.example.model.request.AdminAttendantStatusUpdateRequest;
import org.example.model.response.AdminAttendantDetailResponse;
import org.example.model.response.AdminAttendantListItemResponse;
import org.example.model.response.PagedResponse;
import org.example.service.AdminService;
import org.example.util.AuthUtil;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/attendants")
@RequiredArgsConstructor
public class AdminAttendantController {

    private final AdminService adminService;

    @GetMapping
    public ResponseResult<PagedResponse<AdminAttendantListItemResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ResponseResult.success(adminService.getAttendants(keyword, auditStatus, page, pageSize));
    }

    @GetMapping("/{userId}")
    public ResponseResult<AdminAttendantDetailResponse> detail(@PathVariable Integer userId) {
        try {
            return ResponseResult.success(adminService.getAttendantDetail(userId));
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }

    @PatchMapping("/{userId}/status")
    public ResponseResult<Void> updateStatus(@PathVariable Integer userId,
                                             @RequestBody AdminAttendantStatusUpdateRequest request,
                                             HttpServletRequest httpServletRequest) {
        try {
            adminService.updateAttendantStatus(AuthUtil.getCurrentUserId(httpServletRequest), userId, request.getStatus(), request.getReason());
            return ResponseResult.success(null);
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }

    @PatchMapping("/{userId}/qualification-review")
    public ResponseResult<Void> review(@PathVariable Integer userId,
                                       @RequestBody AdminAttendantReviewRequest request,
                                       HttpServletRequest httpServletRequest) {
        try {
            adminService.reviewAttendantQualification(AuthUtil.getCurrentUserId(httpServletRequest), userId, request.getAction(), request.getReason());
            return ResponseResult.success(null);
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }
}
