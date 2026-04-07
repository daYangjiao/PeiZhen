package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "陪诊师头像上传响应")
public class AttendantAvatarUploadResponse {

    @ApiModelProperty(value = "陪诊师用户ID")
    private Integer userId;

    @ApiModelProperty(value = "头像访问路径")
    private String avatarUrl;
}
