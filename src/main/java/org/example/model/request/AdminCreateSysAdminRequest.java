package org.example.model.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class AdminCreateSysAdminRequest {

    @NotBlank(message = "管理员姓名不能为空")
    private String name;

    @NotBlank(message = "管理员手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入正确的手机号")
    private String phone;

    @NotBlank(message = "管理员密码不能为空")
    @Size(min = 6, message = "密码至少 6 位")
    private String password;

    @Pattern(regexp = "^(SUPER_ADMIN|ADMIN)$", message = "账号类型不合法")
    private String role;
}
