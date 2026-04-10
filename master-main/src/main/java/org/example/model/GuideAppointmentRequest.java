package org.example.model;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 步骤1预约请求VO - 适配新JSON格式
 */
@Data
public class GuideAppointmentRequest {
    @NotBlank(message = "医院名称不能为空")
    private String hospital; // 医院名称
    
    @NotBlank(message = "服务日期不能为空")
    private String serviceDate; // 服务日期
    
    @NotBlank(message = "服务开始时间不能为空")
    private String serviceStartTime; // 服务开始时间
    
    @NotBlank(message = "服务结束时间不能为空")
    private String serviceEndTime; // 服务结束时间
    
    @NotNull(message = "服务类型编号不能为空")
    private Integer serviceTypeNumber; // 服务类型编号
    
    @NotBlank(message = "患者姓名不能为空")
    private String patientName; // 患者姓名
    
    @NotBlank(message = "患者电话不能为空")
    private String patientPhone; // 患者电话
    
    private List<String> symptoms; // 症状列表
    
    private String otherRequirement; // 其他需求
}