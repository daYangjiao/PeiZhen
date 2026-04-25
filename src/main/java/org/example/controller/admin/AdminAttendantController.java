package org.example.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.common.ResponseResult;
import org.example.model.request.AdminAttendantReviewRequest;
import org.example.model.request.AdminAttendantStatusUpdateRequest;
import org.example.model.response.AdminAttendantDetailResponse;
import org.example.model.response.AdminAttendantQualificationLogResponse;
import org.example.model.response.AdminAttendantListItemResponse;
import org.example.model.response.PagedResponse;
import org.example.service.AdminService;
import org.example.util.AuthUtil;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    @GetMapping("/next-pending")
    public ResponseResult<AdminAttendantDetailResponse> nextPending(@RequestParam(required = false) Integer excludeId,
                                                                    HttpServletRequest request) {
        return ResponseResult.success(adminService.getNextPendingAttendant(AuthUtil.getCurrentAdminId(request), excludeId));
    }

    @GetMapping("/{userId}")
    public ResponseResult<AdminAttendantDetailResponse> detail(@PathVariable Integer userId,
                                                              HttpServletRequest request) {
        try {
            return ResponseResult.success(adminService.getAttendantDetail(AuthUtil.getCurrentAdminId(request), userId));
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }

    @GetMapping("/{userId}/qualification-logs")
    public ResponseResult<List<AdminAttendantQualificationLogResponse>> qualificationLogs(@PathVariable Integer userId,
                                                                                         @RequestParam(defaultValue = "20") Integer limit,
                                                                                         HttpServletRequest request) {
        try {
            return ResponseResult.success(adminService.getAttendantQualificationLogs(AuthUtil.getCurrentAdminId(request), userId, limit));
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }

    @PatchMapping("/{userId}/status")
    public ResponseResult<Void> updateStatus(@PathVariable Integer userId,
                                             @RequestBody AdminAttendantStatusUpdateRequest request,
                                             HttpServletRequest httpServletRequest) {
        try {
            adminService.updateAttendantStatus(AuthUtil.getCurrentAdminId(httpServletRequest), userId, request.getStatus(), request.getReason());
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
            adminService.reviewAttendantQualification(AuthUtil.getCurrentAdminId(httpServletRequest), userId, request.getAction(), request.getReason());
            return ResponseResult.success(null);
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }
}
