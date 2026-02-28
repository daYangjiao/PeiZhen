package org.example.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单列表查询请求参数
 */
@Data
@ApiModel(description = "订单列表查询请求参数")
public class OrderListQueryRequest {

    @ApiModelProperty(value = "页码（从0开始）", example = "0")
    private Integer page = 0;

    @ApiModelProperty(value = "每页大小", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "排序字段（createTime, serviceDate等）", example = "createTime")
    private String sortBy = "createTime";

    @ApiModelProperty(value = "排序方向（asc, desc）", example = "desc")
    private String sortDirection = "desc";

    @ApiModelProperty(value = "订单状态筛选（0=待接单, 1=已接单, 2=已完成, 3=已取消）", example = "0")
    private Integer orderStatus;

    @ApiModelProperty(value = "支付状态筛选（0=待支付, 1=已支付）", example = "0")
    private Integer paymentStatus;

    @ApiModelProperty(value = "关键词搜索（医院名称、患者姓名等）", example = "协和")
    private String keyword;

    @ApiModelProperty(value = "服务类型筛选", example = "1")
    private Integer serviceType;

    @ApiModelProperty(value = "开始日期（yyyy-MM-dd）", example = "2025-01-01")
    private String startDate;

    @ApiModelProperty(value = "结束日期（yyyy-MM-dd）", example = "2025-12-31")
    private String endDate;

    @ApiModelProperty(value = "预计时长下限（小时），如2表示大于2小时", example = "2")
    private Integer expectedDurationMinHours;

    @ApiModelProperty(value = "基础费用上限（元），如80表示小于80元", example = "80")
    private BigDecimal orderAmountMax;
}