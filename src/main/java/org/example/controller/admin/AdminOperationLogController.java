package org.example.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.common.ResponseResult;
import org.example.model.response.AdminOperationLogResponse;
import org.example.model.response.PagedResponse;
import org.example.service.impl.AdminOperationLogService;
import org.example.util.AuthUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/operation-logs")
@RequiredArgsConstructor
public class AdminOperationLogController {

    private final AdminOperationLogService operationLogService;

    @GetMapping
    public ResponseResult<PagedResponse<AdminOperationLogResponse>> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String operatorRole,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        try {
            return ResponseResult.success(operationLogService.getLogs(
                    AuthUtil.getCurrentAdminId(request),
                    module,
                    action,
                    operatorRole,
                    keyword,
                    startTime,
                    endTime,
                    page,
                    pageSize
            ));
        } catch (SecurityException ex) {
            return new ResponseResult<>(403, ex.getMessage(), null);
        }
    }
}
