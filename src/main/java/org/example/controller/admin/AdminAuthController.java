package org.example.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.common.ResponseResult;
import org.example.model.request.AdminLoginRequest;
import org.example.model.response.AdminLoginResponse;
import org.example.service.AdminService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminService adminService;

    @PostMapping("/login")
    public ResponseResult<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        try {
            return ResponseResult.success(adminService.login(request));
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }
}
