package org.example.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 */
@ApiModel(description = "订单状态枚举")
@AllArgsConstructor
@Getter
public enum OrderStatusEnum {
    
    @ApiModelProperty(value = "待支付定金", example = "0")
    PENDING_DEPOSIT(0, "待支付定金"),
    
    @ApiModelProperty(value = "待接单", example = "1")
    WAITING_ACCEPT(1, "待接单"),
    
    @ApiModelProperty(value = "已接单", example = "2")
    ACCEPTED(2, "已接单"),
    
    @ApiModelProperty(value = "服务中", example = "3")
    IN_SERVICE(3, "服务中"),
    
    @ApiModelProperty(value = "待确认时长", example = "4")
    WAITING_CONFIRM_DURATION(4, "待确认时长"),
    
    @ApiModelProperty(value = "待支付差价", example = "5")
    WAITING_PAY_BALANCE(5, "待支付差价"),
    
    @ApiModelProperty(value = "已完成", example = "6")
    COMPLETED(6, "已完成"),
    
    @ApiModelProperty(value = "已取消", example = "7")
    CANCELLED(7, "已取消");

    private final Integer code;
    private final String description;

    /**
     * 根据状态码获取枚举
     */
    public static OrderStatusEnum fromCode(Integer code) {
        if (code == null) return null;
        for (OrderStatusEnum status : OrderStatusEnum.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否为有效状态
     */
    public static boolean isValidStatus(Integer code) {
        return fromCode(code) != null;
    }
}