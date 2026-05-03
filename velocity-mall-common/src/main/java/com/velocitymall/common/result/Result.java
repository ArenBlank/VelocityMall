package com.velocitymall.common.result;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全局统一 API 响应体。
 *
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int SUCCESS_CODE = 20000;

    public static final int FAILED_CODE = 50000;

    private Integer code;

    private String message;

    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> failed(String message) {
        return failed(ResultCode.SYSTEM_ERROR, message);
    }

    public static <T> Result<T> failed(ResultCode resultCode) {
        return failed(resultCode, resultCode.getMessage());
    }

    public static <T> Result<T> failed(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

    public static <T> Result<T> failed(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
