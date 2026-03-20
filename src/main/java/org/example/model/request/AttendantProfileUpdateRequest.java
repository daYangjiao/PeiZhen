package org.example.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "陪诊师个人中心资料更新请求")
public class AttendantProfileUpdateRequest {

    @ApiModelProperty(value = "姓名", example = "李怀")
    private String name;

    @ApiModelProperty(value = "手机号", example = "13900139002")
    private String phone;

    @ApiModelProperty(value = "头像地址", example = "/uploads/avatar_20260320.png")
    private String avatarUrl;

    @ApiModelProperty(value = "个人简介", example = "擅长三甲医院门诊陪诊，沟通耐心细致。")
    private String introduction;

    @ApiModelProperty(value = "擅长领域", example = "肿瘤科, 心内科")
    private String professionalField;

    @ApiModelProperty(value = "从业年限", example = "5")
    private Integer experienceYears;

    @ApiModelProperty(value = "常驻医院", example = "北京协和医院")
    private String hospitalName;

    @ApiModelProperty(value = "资质证书编号", example = "CERT-2026-0001")
    private String certificate;
}
