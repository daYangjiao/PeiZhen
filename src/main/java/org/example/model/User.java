package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "基础用户实体，包含所有角色的通用信息")
public class User {

    @ApiModelProperty(value = "用户ID", example = "1")
    private Integer id;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ApiModelProperty(value = "登录密码，仅请求时传入，响应中不会回传", required = true, example = "123456")
    private String password;

    @ApiModelProperty(value = "真实姓名或昵称", example = "李怀")
    private String name;

    @ApiModelProperty(value = "手机号", example = "13900139002")
    private String phone;

    @ApiModelProperty(value = "性别", example = "男")
    private String sex;

    @ApiModelProperty(value = "年龄", example = "28")
    private Integer age;

    @ApiModelProperty(value = "头像URL", example = "/static/avatar/doc1.png")
    private String avatar;

    @ApiModelProperty(value = "角色：0=普通用户, 1=陪诊师, 2=管理员", required = true, example = "1")
    private Integer userType;

    @ApiModelProperty(value = "账号状态：1=正常, 0=禁用", example = "1")
    private Integer status;

    @ApiModelProperty(value = "陪诊师个人简介", example = "熟悉三甲医院就诊流程，擅长术后护理与普通陪诊。")
    private String introduction;

    @ApiModelProperty(value = "陪诊师擅长领域", example = "普通陪诊,术后护理")
    private String professionalField;

    @ApiModelProperty(value = "陪诊师从业年限", example = "3")
    private Integer experienceYears;

    @ApiModelProperty(value = "陪诊师常驻医院", example = "福建医科大学附属协和医院")
    private String hospitalName;

    @ApiModelProperty(value = "陪诊师资格证编号", example = "CD-PZ-2026-021")
    private String certificate;

    @ApiModelProperty(value = "微信OpenID")
    private String openid;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;
}
