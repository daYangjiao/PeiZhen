package org.example.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel(description = "陪诊师三证资质信息")
public class AttendantQualification {

    @ApiModelProperty(value = "用户ID")
    private Integer userId;

    @ApiModelProperty(value = "身份证是否上传：0=未上传,1=已上传")
    private Integer idCardUploaded;

    @ApiModelProperty(value = "执业证书是否上传：0=未上传,1=已上传")
    private Integer practiceCertUploaded;

    @ApiModelProperty(value = "健康证是否上传：0=未上传,1=已上传")
    private Integer healthCertUploaded;

    @ApiModelProperty(value = "身份证文件地址")
    private String idCardFileUrl;

    @ApiModelProperty(value = "身份证正面文件地址")
    private String idCardFrontFileUrl;

    @ApiModelProperty(value = "身份证背面文件地址")
    private String idCardBackFileUrl;

    @ApiModelProperty(value = "执业证书文件地址")
    private String practiceCertFileUrl;

    @ApiModelProperty(value = "健康证文件地址")
    private String healthCertFileUrl;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    private Date updateTime;
}
