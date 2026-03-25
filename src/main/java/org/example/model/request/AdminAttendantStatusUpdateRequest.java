package org.example.model.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AdminAttendantStatusUpdateRequest {

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String reason;
}
