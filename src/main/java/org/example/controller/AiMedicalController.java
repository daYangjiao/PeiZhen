package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.example.dao.AiMedicalQaMapper;
import org.example.model.AiMedicalQa;
import org.example.model.MedicalQaRequest;
import org.example.service.AiMedicalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/ai/medical")
@Api(tags = "AI医疗问答接口", description = "提供医疗问题的智能问答服务，基于本地DeepSeek模型")
public class AiMedicalController {

    @Autowired
    private AiMedicalService aiMedicalService;

    @Autowired
    private AiMedicalQaMapper aiMedicalQaMapper;

    @PostMapping("/qa")
    @ApiOperation(value = "医疗问题问答", notes = "提交医疗问题后立即返回问答记录。若前端需要展示模型思考过程，可配合 recordId 轮询查询思考过程接口。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "问答成功，返回 AI 问答记录"),
            @ApiResponse(code = 400, message = "请求参数不完整或为空"),
            @ApiResponse(code = 500, message = "AI 服务异常或问答生成失败")
    })
    public ResponseEntity<AiMedicalQa> medicalQa(
            @ApiParam(name = "requestBody", value = "问答请求体，仅需传入 question 字段", required = true)
            @Valid
            @RequestBody MedicalQaRequest request) {

        AiMedicalQa result = aiMedicalService.getMedicalAnswer(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/qa/thinking/{recordId}")
    @ApiOperation(value = "获取思考过程", notes = "前端可轮询该接口读取指定问答记录的思考过程与最新回答状态。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功，返回指定问答记录"),
            @ApiResponse(code = 404, message = "问答记录不存在"),
            @ApiResponse(code = 500, message = "查询过程发生异常")
    })
    public ResponseEntity<AiMedicalQa> getThinkingProcess(
            @ApiParam(name = "recordId", value = "问答记录ID", required = true, example = "1")
            @PathVariable Long recordId) {
        AiMedicalQa qaRecord = aiMedicalQaMapper.selectById(recordId);
        return ResponseEntity.ok(qaRecord);
    }
}
