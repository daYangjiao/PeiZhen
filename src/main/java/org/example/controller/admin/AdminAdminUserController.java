package org.example.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.common.ResponseResult;
import org.example.entity.SysAdmin;
import org.example.model.request.AdminCreateSysAdminRequest;
import org.example.model.request.AdminSysAdminStatusUpdateRequest;
import org.example.model.response.PagedResponse;
import org.example.service.SysAdminService;
import org.example.util.AuthUtil;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin/admin-users")
@RequiredArgsConstructor
public class AdminAdminUserController {

    private final SysAdminService sysAdminService;

    @GetMapping
    public ResponseResult<PagedResponse<SysAdmin>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ResponseResult.success(sysAdminService.getAdmins(keyword, status, page, pageSize));
    }

    @PostMapping
    public ResponseResult<SysAdmin> create(@Valid @RequestBody AdminCreateSysAdminRequest request,
                                           HttpServletRequest httpServletRequest) {
        try {
            return ResponseResult.success(sysAdminService.createAdmin(AuthUtil.getCurrentAdminId(httpServletRequest), request));
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }

    @PatchMapping("/{adminId}/status")
    public ResponseResult<Void> updateStatus(@PathVariable Integer adminId,
                                             @Valid @RequestBody AdminSysAdminStatusUpdateRequest request,
                                             HttpServletRequest httpServletRequest) {
        try {
            sysAdminService.updateStatus(AuthUtil.getCurrentAdminId(httpServletRequest), adminId, request.getStatus());
            return ResponseResult.success(null);
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }
}
