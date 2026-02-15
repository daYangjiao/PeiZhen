package org.example.model.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 订单创建请求对象
 */
@Data
public class CreateOrderRequest {
    @NotBlank(message = "预约编号不能为空")
    private String appointmentNo;
    
    @NotBlank(message = "陪诊师ID不能为空")
    private String attendantId;
    
    // 新增：用户ID（由后端Controller填充，前端无需传递）
    private Integer userId;
}