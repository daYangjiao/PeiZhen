package org.example.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.common.ResponseResult;
import org.example.model.request.AdminUserStatusUpdateRequest;
import org.example.model.response.AdminUserDetailResponse;
import org.example.model.response.AdminUserListItemResponse;
import org.example.model.response.PagedResponse;
import org.example.service.AdminService;
import org.example.util.AuthUtil;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService adminService;

    @GetMapping
    public ResponseResult<PagedResponse<AdminUserListItemResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer userType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ResponseResult.success(adminService.getUsers(keyword, userType, status, page, pageSize));
    }

    @GetMapping("/{userId}")
    public ResponseResult<AdminUserDetailResponse> detail(@PathVariable Integer userId) {
        try {
            return ResponseResult.success(adminService.getUserDetail(userId));
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }

    @PatchMapping("/{userId}/status")
    public ResponseResult<Void> updateStatus(@PathVariable Integer userId,
                                             @RequestBody AdminUserStatusUpdateRequest request,
                                             HttpServletRequest httpServletRequest) {
        try {
            adminService.updateUserStatus(AuthUtil.getCurrentAdminId(httpServletRequest), userId, request.getStatus());
            return ResponseResult.success(null);
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }
}
