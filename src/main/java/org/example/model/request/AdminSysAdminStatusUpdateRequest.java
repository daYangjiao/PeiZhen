package org.example.model.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AdminSysAdminStatusUpdateRequest {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
