package org.example.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel(description = "App 更新检查请求")
public class AppUpgradeCheckRequest {

    @ApiModelProperty(value = "uni-app AppID", example = "__UNI__73289DB")
    private String appid;

    @ApiModelProperty(value = "平台", example = "android")
    private String platform;

    @ApiModelProperty(value = "安装包版本名称", example = "1.0.1")
    private String appVersion;

    @ApiModelProperty(value = "安装包版本号", example = "101")
    private Integer appVersionCode;

    @ApiModelProperty(value = "当前 WGT 资源版本", example = "1.0.1")
    private String wgtVersion;
}
