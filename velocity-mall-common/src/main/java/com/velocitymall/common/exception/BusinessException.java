package com.velocitymall.common.exception;

import com.velocitymall.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(String message) {
        this(ResultCode.BIZ_WARNING, message);
    }

    public BusinessException(ResultCode resultCode) {
        this(resultCode, resultCode.getMessage());
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
