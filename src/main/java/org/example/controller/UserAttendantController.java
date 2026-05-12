package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.ResponseResult;
import org.example.model.response.AttendantProfileResponse;
import org.example.service.AttendantService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户端陪诊师相关接口控制器
 */
@RestController
@RequestMapping("/user/attendants")
@Api(tags = "用户端陪诊师接口")
@RequiredArgsConstructor
@Slf4j
public class UserAttendantController {

    private final AttendantService attendantService;

    /**
     * 根据陪诊师ID获取详细信息
     */
    @GetMapping("/{attendantId}")
    @ApiOperation(value = "获取陪诊师详细信息", notes = "用户端查看指定陪诊师详情，返回基础用户信息与陪诊师扩展资料。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功"),
            @ApiResponse(code = 404, message = "陪诊师不存在"),
            @ApiResponse(code = 500, message = "查询失败")
    })
    public ResponseResult<Map<String, Object>> getAttendantDetail(
            @ApiParam(value = "陪诊师ID", required = true, example = "21")
            @PathVariable Integer attendantId) {
        
        try {
            AttendantProfileResponse profile = attendantService.getProfile(attendantId);
            if (profile == null) {
                return ResponseResult.error("陪诊师不存在");
            }

            Map<String, Object> detail = new HashMap<>();
            detail.put("id", profile.getId());
            detail.put("name", profile.getName());
            detail.put("avatar", profile.getAvatarUrl());
            detail.put("avatarUrl", profile.getAvatarUrl());
            detail.put("phone", profile.getPhone());
            detail.put("certificate", profile.getCertificate());
            detail.put("score", profile.getScore());
            detail.put("evaluationCount", profile.getEvaluationCount() == null ? 0 : profile.getEvaluationCount());
            detail.put("praiseRate", profile.getPraiseRate() == null ? 0 : profile.getPraiseRate());
            detail.put("introduction", profile.getIntroduction());
            detail.put("professionalField", profile.getProfessionalField());
            detail.put("experienceYears", profile.getExperienceYears());
            detail.put("hospitalName", profile.getHospitalName());
            detail.put("qualificationStatusCode", profile.getQualificationStatusCode());
            detail.put("qualificationStatusText", profile.getQualificationStatusText());
            detail.put("idCardUploaded", profile.getIdCardUploaded());
            detail.put("practiceCertUploaded", profile.getPracticeCertUploaded());
            detail.put("healthCertUploaded", profile.getHealthCertUploaded());
            detail.put("idCardFileUrl", profile.getIdCardFileUrl());
            detail.put("idCardFrontFileUrl", profile.getIdCardFrontFileUrl());
            detail.put("idCardFrontScanFileUrl", profile.getIdCardFrontScanFileUrl());
            detail.put("idCardBackFileUrl", profile.getIdCardBackFileUrl());
            detail.put("idCardBackScanFileUrl", profile.getIdCardBackScanFileUrl());
            detail.put("practiceCertFileUrl", profile.getPracticeCertFileUrl());
            detail.put("practiceCertScanFileUrl", profile.getPracticeCertScanFileUrl());
            detail.put("healthCertFileUrl", profile.getHealthCertFileUrl());
            detail.put("healthCertScanFileUrl", profile.getHealthCertScanFileUrl());
            detail.put("practiceCertExpireDate", profile.getPracticeCertExpireDate());
            detail.put("healthCertExpireDate", profile.getHealthCertExpireDate());
            
            return ResponseResult.success(detail);
        } catch (Exception e) {
            log.error("获取陪诊师详情失败，ID: {}", attendantId, e);
            return ResponseResult.error("查询失败");
        }
    }
}
