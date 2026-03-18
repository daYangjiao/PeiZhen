package org.example.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "陪诊师三证资质更新请求")
public class AttendantQualificationUpdateRequest {

    @ApiModelProperty("身份证是否上传：0=未上传,1=已上传")
    private Integer idCardUploaded;

    @ApiModelProperty("执业证书是否上传：0=未上传,1=已上传")
    private Integer practiceCertUploaded;

    @ApiModelProperty("健康证是否上传：0=未上传,1=已上传")
    private Integer healthCertUploaded;

    @ApiModelProperty("身份证文件地址")
    private String idCardFileUrl;

    @ApiModelProperty("身份证正面文件地址")
    private String idCardFrontFileUrl;

    @ApiModelProperty("身份证背面文件地址")
    private String idCardBackFileUrl;

    @ApiModelProperty("执业证书文件地址")
    private String practiceCertFileUrl;

    @ApiModelProperty("健康证文件地址")
    private String healthCertFileUrl;
}
