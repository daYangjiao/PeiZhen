package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.example.common.ResponseResult;
import org.example.model.MedicalQaRequest;
import org.example.model.MedicalQaResponse;
import org.example.service.AiMedicalService;
import org.example.util.AuthUtil;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/ai/medical")
@Api(tags = "AI医疗问答接口", description = "提供 AI 导诊问答服务")
public class AiMedicalController {

    private final AiMedicalService aiMedicalService;

    public AiMedicalController(AiMedicalService aiMedicalService) {
        this.aiMedicalService = aiMedicalService;
    }

    @PostMapping("/qa")
    @ApiOperation(value = "提交 AI 导诊问题", notes = "创建一条问答记录并异步生成导诊建议，前端拿到 recordId 后可轮询状态。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "提交成功"),
            @ApiResponse(code = 400, message = "请求参数错误"),
            @ApiResponse(code = 500, message = "AI 服务异常")
    })
    public ResponseResult<MedicalQaResponse> medicalQa(
            @ApiParam(name = "requestBody", value = "问答请求体", required = true)
            @Valid @RequestBody MedicalQaRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = getCurrentUserId(httpRequest);
        return ResponseResult.success(aiMedicalService.submitQuestion(userId, request));
    }

    @GetMapping("/qa/{recordId}")
    @ApiOperation(value = "获取问答状态", notes = "前端轮询该接口获取指定问答记录的处理状态与最终回答。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功"),
            @ApiResponse(code = 404, message = "问答记录不存在")
    })
    public ResponseResult<MedicalQaResponse> getRecord(
            @ApiParam(name = "recordId", value = "问答记录ID", required = true, example = "1")
            @PathVariable Long recordId,
            HttpServletRequest httpRequest) {
        Integer userId = getCurrentUserId(httpRequest);
        MedicalQaResponse response = aiMedicalService.getRecord(userId, recordId);
        if (response == null) {
            return ResponseResult.error("问答记录不存在");
        }
        return ResponseResult.success(response);
    }

    @GetMapping("/qa/conversation/{conversationId}")
    @ApiOperation(value = "获取当前会话问答记录", notes = "按 conversationId 返回当前会话全部问答，用于刷新恢复页面。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功")
    })
    public ResponseResult<List<MedicalQaResponse>> getConversation(
            @ApiParam(name = "conversationId", value = "会话ID", required = true, example = "conv-20260413-001")
            @PathVariable String conversationId,
            HttpServletRequest httpRequest) {
        Integer userId = getCurrentUserId(httpRequest);
        return ResponseResult.success(aiMedicalService.getConversation(userId, conversationId));
    }

    @GetMapping("/qa/latest")
    @ApiOperation(value = "获取最近一次 AI 导诊会话", notes = "按当前登录用户返回最近一次会话的完整问答记录，用于进入页面自动恢复。")
    public ResponseResult<List<MedicalQaResponse>> getLatestConversation(HttpServletRequest httpRequest) {
        Integer userId = getCurrentUserId(httpRequest);
        return ResponseResult.success(aiMedicalService.getLatestConversation(userId));
    }

    @GetMapping("/qa/thinking/{recordId}")
    @ApiOperation(value = "兼容旧版思考过程查询", notes = "兼容旧前端轮询接口，实际返回与问答状态接口一致。")
    public ResponseResult<MedicalQaResponse> getThinkingProcess(@PathVariable Long recordId, HttpServletRequest httpRequest) {
        return getRecord(recordId, httpRequest);
    }

    private Integer getCurrentUserId(HttpServletRequest request) {
        Integer userId = AuthUtil.getCurrentUserId(request);
        if (userId == null) {
            throw new IllegalArgumentException("请先登录后再使用 AI 导诊");
        }
        return userId;
    }
}
