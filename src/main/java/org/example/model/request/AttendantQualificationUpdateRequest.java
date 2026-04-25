package org.example.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "陪诊师三证资质更新请求")
public class AttendantQualificationUpdateRequest {

    @ApiModelProperty(value = "身份证是否上传：0=未上传,1=已上传", example = "1")
    private Integer idCardUploaded;

    @ApiModelProperty(value = "执业证书是否上传：0=未上传,1=已上传", example = "1")
    private Integer practiceCertUploaded;

    @ApiModelProperty(value = "健康证是否上传：0=未上传,1=已上传", example = "1")
    private Integer healthCertUploaded;

    @ApiModelProperty(value = "身份证文件地址", example = "/uploads/qualification/id_card.pdf")
    private String idCardFileUrl;

    @ApiModelProperty(value = "身份证正面文件地址", example = "/uploads/qualification/id_card_front.jpg")
    private String idCardFrontFileUrl;

    @ApiModelProperty(value = "身份证背面文件地址", example = "/uploads/qualification/id_card_back.jpg")
    private String idCardBackFileUrl;

    @ApiModelProperty(value = "执业证书文件地址", example = "/uploads/qualification/practice_cert.jpg")
    private String practiceCertFileUrl;

    @ApiModelProperty(value = "健康证文件地址", example = "/uploads/qualification/health_cert.jpg")
    private String healthCertFileUrl;

    @ApiModelProperty(value = "执业证书有效期，格式 yyyy-MM-dd", example = "2027-12-31")
    private String practiceCertExpireDate;

    @ApiModelProperty(value = "健康证有效期，格式 yyyy-MM-dd", example = "2027-12-31")
    private String healthCertExpireDate;
}
