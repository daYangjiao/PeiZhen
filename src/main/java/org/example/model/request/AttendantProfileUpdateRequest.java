package org.example.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "陪诊师个人中心资料更新请求")
public class AttendantProfileUpdateRequest {

    @ApiModelProperty("姓名")
    private String name;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("头像地址")
    private String avatarUrl;

    @ApiModelProperty("个人简介")
    private String introduction;

    @ApiModelProperty("擅长领域")
    private String professionalField;

    @ApiModelProperty("从业年限")
    private Integer experienceYears;

    @ApiModelProperty("常驻医院")
    private String hospitalName;

    @ApiModelProperty("资质证书编号")
    private String certificate;
}
