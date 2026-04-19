package org.example.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel(description = "AI预约结构化需求")
public class AiAppointmentStructuredDemand {

    @ApiModelProperty(value = "原始需求文本")
    private String rawDemandText;

    @ApiModelProperty(value = "就诊人姓名", example = "张三")
    private String patientName;

    @ApiModelProperty(value = "就诊人性别", example = "女")
    private String patientSex;
    @ApiModelProperty(value = "服务日期", example = "2026-04-16")
    private String serviceDate;

    @ApiModelProperty(value = "大致时间段", example = "上午")
    private String timePeriod;

    @ApiModelProperty(value = "服务开始时间", example = "09:00")
    private String serviceStartTime;

    @ApiModelProperty(value = "服务结束时间", example = "12:00")
    private String serviceEndTime;

    @ApiModelProperty(value = "医院", example = "华西医院")
    private String hospital;

    @ApiModelProperty(value = "科室关键词", example = "心内科")
    private String department;

    @ApiModelProperty(value = "患者画像", example = "80岁老人")
    private String patientProfile;

    @ApiModelProperty(value = "陪诊师性别偏好", example = "男")
    private String attendantGender;

    @ApiModelProperty(value = "服务类型：1普通陪诊 2术后护理 3急诊陪同 4上门陪诊", example = "1")
    private Integer serviceTypeNumber;

    @ApiModelProperty(value = "偏好标签")
    private List<String> preferenceTags = new ArrayList<>();

    @ApiModelProperty(value = "症状关键词")
    private List<String> symptomTags = new ArrayList<>();

    @ApiModelProperty(value = "症状描述", example = "心内科复诊，近期胸闷头晕")
    private String symptomDescription;

    @ApiModelProperty(value = "其他需求", example = "需要轮椅协助，优先男陪诊师")
    private String otherRequirement;
}
