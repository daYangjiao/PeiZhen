package org.example.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "微信登录绑定手机号请求")
public class WechatBindPhoneRequest {

    @ApiModelProperty(value = "微信绑定短期凭证", required = true)
    private String wechatBindToken;

    @ApiModelProperty(value = "手机号", required = true, example = "15520765697")
    private String phone;

    @ApiModelProperty(value = "密码", required = true, example = "123456")
    private String password;

    @ApiModelProperty(value = "展示姓名/昵称", required = true, example = "范涵伶")
    private String name;

    @ApiModelProperty(value = "当前登录角色，支持 user 或 escort", example = "user")
    private String role;
}
