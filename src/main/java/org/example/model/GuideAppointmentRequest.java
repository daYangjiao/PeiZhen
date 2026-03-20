package org.example.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 步骤1预约请求VO - 适配新JSON格式
 */
@Data
@ApiModel(description = "AI导诊预约创建请求")
public class GuideAppointmentRequest {
    @ApiModelProperty(value = "医院名称", required = true, example = "北京协和医院")
    @NotBlank(message = "医院名称不能为空")
    private String hospital;

    @ApiModelProperty(value = "服务日期", required = true, example = "2026-03-20")
    @NotBlank(message = "服务日期不能为空")
    private String serviceDate;

    @ApiModelProperty(value = "服务开始时间", required = true, example = "09:00")
    @NotBlank(message = "服务开始时间不能为空")
    private String serviceStartTime;

    @ApiModelProperty(value = "服务结束时间", required = true, example = "11:00")
    @NotBlank(message = "服务结束时间不能为空")
    private String serviceEndTime;

    @ApiModelProperty(value = "服务类型编号：1=普通陪诊，2=术后护理，3=急诊陪同，4=上门陪诊", required = true, example = "1")
    @NotNull(message = "服务类型编号不能为空")
    private Integer serviceTypeNumber;

    @ApiModelProperty(value = "患者姓名", required = true, example = "张三")
    @NotBlank(message = "患者姓名不能为空")
    private String patientName;

    @ApiModelProperty(value = "患者电话", required = true, example = "13800138000")
    @NotBlank(message = "患者电话不能为空")
    private String patientPhone;

    @ApiModelProperty(value = "症状列表", example = "[\"发热\",\"咳嗽\"]")
    private List<String> symptoms;

    @ApiModelProperty(value = "其他需求", example = "需要轮椅协助")
    private String otherRequirement;
}
