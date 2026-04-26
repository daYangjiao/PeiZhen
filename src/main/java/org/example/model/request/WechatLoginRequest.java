package org.example.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "微信登录请求")
public class WechatLoginRequest {

    @ApiModelProperty(value = "wx.login 返回的 code", required = true, example = "031m7qGa1b0abcDEF1234567890")
    private String code;

    @ApiModelProperty(value = "当前登录角色，支持 user 或 escort", example = "user")
    private String role;

    @ApiModelProperty(value = "微信登录平台：MINI_PROGRAM、APP、WECHAT_H5", example = "MINI_PROGRAM")
    private String platform;
}
