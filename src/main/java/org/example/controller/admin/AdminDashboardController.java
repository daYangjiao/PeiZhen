package org.example.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.common.ResponseResult;
import org.example.model.response.AdminDashboardOverviewResponse;
import org.example.service.AdminService;
import org.example.util.AuthUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminService adminService;

    @GetMapping("/overview")
    public ResponseResult<AdminDashboardOverviewResponse> overview(HttpServletRequest request) {
        return ResponseResult.success(adminService.getDashboardOverview(AuthUtil.getCurrentAdminId(request)));
    }
}
