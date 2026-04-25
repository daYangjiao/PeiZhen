package org.example.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.common.ResponseResult;
import org.example.model.request.AdminWorkbenchCompleteRequest;
import org.example.model.response.AdminWorkbenchClaimResponse;
import org.example.model.response.AdminWorkbenchSummaryResponse;
import org.example.model.response.AdminWorkbenchTaskResponse;
import org.example.model.response.PagedResponse;
import org.example.service.impl.AdminWorkbenchService;
import org.example.util.AuthUtil;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/workbench")
@RequiredArgsConstructor
public class AdminWorkbenchController {

    private final AdminWorkbenchService workbenchService;

    @GetMapping("/summary")
    public ResponseResult<AdminWorkbenchSummaryResponse> summary(HttpServletRequest request) {
        return ResponseResult.success(workbenchService.getSummary(AuthUtil.getCurrentAdminId(request)));
    }

    @GetMapping("/tasks")
    public ResponseResult<PagedResponse<AdminWorkbenchTaskResponse>> tasks(
            @RequestParam String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        try {
            return ResponseResult.success(workbenchService.getTasks(AuthUtil.getCurrentAdminId(request), type, keyword, page, pageSize));
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }

    @PostMapping("/tasks/{type}/{targetId}/claim")
    public ResponseResult<AdminWorkbenchClaimResponse> claim(@PathVariable String type,
                                                             @PathVariable Integer targetId,
                                                             HttpServletRequest request) {
        try {
            return ResponseResult.success(workbenchService.claimTask(AuthUtil.getCurrentAdminId(request), type, targetId));
        } catch (SecurityException ex) {
            return new ResponseResult<>(403, ex.getMessage(), null);
        } catch (IllegalStateException ex) {
            return new ResponseResult<>(409, ex.getMessage(), null);
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }

    @PostMapping("/tasks/{type}/{targetId}/complete")
    public ResponseResult<Void> complete(@PathVariable String type,
                                         @PathVariable Integer targetId,
                                         @RequestBody(required = false) AdminWorkbenchCompleteRequest completeRequest,
                                         HttpServletRequest request) {
        try {
            workbenchService.completeTask(AuthUtil.getCurrentAdminId(request), type, targetId, completeRequest);
            return ResponseResult.success(null);
        } catch (SecurityException ex) {
            return new ResponseResult<>(403, ex.getMessage(), null);
        } catch (IllegalStateException ex) {
            return new ResponseResult<>(409, ex.getMessage(), null);
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }

    @DeleteMapping("/tasks/{type}/{targetId}/claim")
    public ResponseResult<Void> release(@PathVariable String type,
                                        @PathVariable Integer targetId,
                                        HttpServletRequest request) {
        try {
            workbenchService.releaseTask(AuthUtil.getCurrentAdminId(request), type, targetId);
            return ResponseResult.success(null);
        } catch (IllegalArgumentException ex) {
            return new ResponseResult<>(400, ex.getMessage(), null);
        }
    }
}
