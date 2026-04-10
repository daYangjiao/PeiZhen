package org.example.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@ApiModel(description = "通用接口响应结果")
public class ResponseResult<T> {
    @ApiModelProperty(value = "响应状态码（200:成功，400:参数错误，500:服务器错误）", example = "200")
    private int code;

    @ApiModelProperty(value = "响应消息", example = "操作成功")
    private String message;

    @ApiModelProperty(value = "响应数据（泛型）")
    private T data;

    public ResponseResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 通用错误响应方法
    public static <T> ResponseResult<T> error(String message) {
        return new ResponseResult<>(500, message, null);
    }

    // 通用成功响应方法
    public static <T> ResponseResult<T> success(T data) {
        return new ResponseResult<>(200, "操作成功", data);
    }
    
    // 添加unauthorized方法
    public static <T> ResponseResult<T> unauthorized(String message) {
        return new ResponseResult<>(401, message, null);
    }
}