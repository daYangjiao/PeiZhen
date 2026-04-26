package org.example.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.common.ResponseResult;
import org.example.model.request.AdminLoginRequest;
import org.example.model.request.WechatAdminBindRequest;
import org.example.model.response.AdminLoginResponse;
import org.example.service.SysAdminService;
import org.example.service.WechatAuthService;
import org.example.entity.SysAdmin;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final SysAdminService sysAdminService;
    private final WechatAuthService wechatAuthService;

    @PostMapping("/login")
    public ResponseResult<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        try {
            return ResponseResult.success(sysAdminService.login(request.getAccount(), request.getPassword()));
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }

    @GetMapping("/current")
    public ResponseResult<SysAdmin> current(HttpServletRequest request) {
        Integer adminId = (Integer) request.getAttribute("currentAdminId");
        SysAdmin admin = sysAdminService.findById(adminId);
        return ResponseResult.success(admin);
    }

    @GetMapping("/wechat/oauth-url")
    public ResponseResult<Map<String, Object>> getWechatOAuthUrl(
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl) {
        try {
            return ResponseResult.success(Map.of("url", wechatAuthService.buildAdminOAuthUrl(redirectUrl)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new ResponseResult<>(400, e.getMessage(), null);
        }
    }

    @GetMapping("/wechat/oauth-callback")
    public ResponseEntity<Void> handleWechatOAuthCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state) {
        String redirectUrl = wechatAuthService.handleAdminOAuthCallback(code, state);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, redirectUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/wechat/bind")
    public ResponseResult<Void> bindWechat(HttpServletRequest request, @RequestBody WechatAdminBindRequest bindRequest) {
        try {
            Integer adminId = (Integer) request.getAttribute("currentAdminId");
            sysAdminService.bindWechatIdentityToken(adminId, bindRequest.getWechatBindToken());
            return ResponseResult.success(null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new ResponseResult<>(400, e.getMessage(), null);
        }
    }
}
