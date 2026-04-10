package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel(description = "App 更新检查响应")
public class AppUpgradeCheckResponse {
    public static final int UPDATE_NONE = 0;
    public static final int UPDATE_WGT = 101;
    public static final int UPDATE_APK = 102;

    @ApiModelProperty(value = "更新类型：0 无更新，101 WGT，102 整包", example = "101")
    private Integer updateType = UPDATE_NONE;

    @ApiModelProperty(value = "更新标题", example = "发现新版本")
    private String title = "发现新版本";

    @ApiModelProperty(value = "更新说明", example = "修复头像显示与订单时间线问题")
    private String notes = "";

    @ApiModelProperty(value = "是否强制更新", example = "false")
    private Boolean forceUpdate = false;

    @ApiModelProperty(value = "最新安装包版本名称", example = "1.0.1")
    private String latestVersion;

    @ApiModelProperty(value = "最新安装包版本号", example = "101")
    private Integer latestVersionCode;

    @ApiModelProperty(value = "最新 WGT 版本", example = "1.0.1-hotfix.1")
    private String wgtVersion;

    @ApiModelProperty(value = "下载地址", example = "https://example.com/uploads/app-updates/android/app-latest.apk")
    private String downloadUrl;
}
