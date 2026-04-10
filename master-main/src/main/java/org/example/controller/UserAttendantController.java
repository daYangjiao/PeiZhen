package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.ResponseResult;
import org.example.model.Attendant;
import org.example.model.User;
import org.example.service.AttendantService;
import org.example.service.UserService;
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
    private final UserService userService;

    /**
     * 根据陪诊师ID获取详细信息
     */
    @GetMapping("/{attendantId}")
    @ApiOperation("获取陪诊师详细信息")
    public ResponseResult<Map<String, Object>> getAttendantDetail(
            @ApiParam("陪诊师ID") @PathVariable Integer attendantId) {
        
        try {
            // 1. 获取基础用户信息
            User user = userService.findById(attendantId);
            if (user == null) {
                return ResponseResult.error("陪诊师不存在");
            }

            // 2. 获取陪诊师扩展信息
            Attendant attendant = attendantService.findByUserId(attendantId);
            
            // 3. 组装返回数据
            Map<String, Object> detail = new HashMap<>();
            detail.put("id", user.getId());
            detail.put("name", user.getName());
            detail.put("avatar", user.getAvatar());
            detail.put("phone", user.getPhone());
            
            if (attendant != null) {
                detail.put("certificate", attendant.getCertificate());
                detail.put("score", attendant.getScore());
                detail.put("introduction", attendant.getIntroduction());
                detail.put("professionalField", attendant.getProfessionalField());
                detail.put("experienceYears", attendant.getExperienceYears());
                detail.put("hospitalName", attendant.getHospitalName());
            }
            
            return ResponseResult.success(detail);
        } catch (Exception e) {
            log.error("获取陪诊师详情失败，ID: {}", attendantId, e);
            return ResponseResult.error("查询失败");
        }
    }
}