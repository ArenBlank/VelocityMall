package com.velocitymall.common.exception;

import com.velocitymall.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常。
 * <p>
 * 当使用 {@link #BusinessException(Integer, String)} 构造时，自定义 code 会被
 * {@link GlobalExceptionHandler} 识别并原样透传给调用方，用于跨服务错误码传播。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    /**
     * 自定义错误码，为 null 时回退到 {@link #resultCode} 的 code。
     */
    private final Integer code;

    public BusinessException(String message) {
        this(ResultCode.BIZ_WARNING, message);
    }

    public BusinessException(ResultCode resultCode) {
        this(resultCode, resultCode.getMessage());
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
        this.code = null;
    }

    /**
     * 携带自定义整型错误码的构造器，用于跨服务透传下游真实错误码。
     *
     * @param code    自定义错误码
     * @param message 错误描述
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.resultCode = ResultCode.BIZ_WARNING;
        this.code = code;
    }
}
