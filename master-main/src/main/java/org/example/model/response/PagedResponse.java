package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 分页响应封装
 */
@Data
@ApiModel(description = "分页响应封装")
public class PagedResponse<T> {

    @ApiModelProperty(value = "数据列表")
    private List<T> content;

    @ApiModelProperty(value = "总记录数", example = "100")
    private long totalElements;

    @ApiModelProperty(value = "总页数", example = "10")
    private int totalPages;

    @ApiModelProperty(value = "当前页码（从0开始）", example = "0")
    private int pageNumber;

    @ApiModelProperty(value = "每页大小", example = "10")
    private int pageSize;

    @ApiModelProperty(value = "是否第一页", example = "true")
    private boolean first;

    @ApiModelProperty(value = "是否最后一页", example = "false")
    private boolean last;

    @ApiModelProperty(value = "是否有下一页", example = "true")
    private boolean hasNext;

    @ApiModelProperty(value = "是否有上一页", example = "false")
    private boolean hasPrevious;

    public PagedResponse() {}

    public PagedResponse(List<T> content, long totalElements, int pageNumber, int pageSize) {
        this.content = content;
        this.totalElements = totalElements;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
        this.first = pageNumber == 0;
        this.last = pageNumber >= totalPages - 1;
        this.hasNext = !last;
        this.hasPrevious = !first;
    }
}