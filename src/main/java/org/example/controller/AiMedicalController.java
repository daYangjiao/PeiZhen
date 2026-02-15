package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
    private AiMedicalQaMapper AiMedicalQaMapperi;

    @PostMapping("/qa")
    @ApiOperation(value = "医疗问题问答", notes = "用户提交医疗问题，获取AI生成的专业回答（包含思考过程推送）")
    public ResponseEntity<AiMedicalQa> medicalQa(
            @ApiParam(name = "requestBody", value = "包含问题的请求体", required = true)
            @Valid
            @RequestBody MedicalQaRequest request) {

        AiMedicalQa result = aiMedicalService.getMedicalAnswer(request);
        return ResponseEntity.ok(result);
    }

    // 新增：前端轮询获取思考过程的接口
    @GetMapping("/qa/thinking/{recordId}")
    @ApiOperation(value = "获取思考过程", notes = "轮询该接口获取AI处理中的思考过程")
    public ResponseEntity<AiMedicalQa> getThinkingProcess(
            @ApiParam(name = "recordId", value = "问答记录ID", required = true)
            @PathVariable Long recordId) {
        AiMedicalQa qaRecord = AiMedicalQaMapperi.selectById(recordId); // 需确保Mapper有该方法
        return ResponseEntity.ok(qaRecord);
    }
}