package com.velocitymall.common.exception;

import com.velocitymall.common.result.Result;
import com.velocitymall.common.result.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理基础结构。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常。
     *
     * @param exception 业务异常
     * @return 统一响应体
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        log.warn("业务异常：{}", exception.getMessage());
        if (exception.getCode() != null) {
            return Result.failed(exception.getCode(), exception.getMessage());
        }
        return Result.failed(exception.getResultCode(), exception.getMessage());
    }

    /**
     * 处理 JSON 请求体参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 统一响应体
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        return Result.failed(ResultCode.PARAM_ERROR, resolveValidationMessage(exception));
    }

    /**
     * 处理表单、查询参数绑定异常。
     *
     * @param exception 参数绑定异常
     * @return 统一响应体
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException exception) {
        return Result.failed(ResultCode.PARAM_ERROR, resolveValidationMessage(exception));
    }

    /**
     * 处理路径变量、查询参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 统一响应体
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .findFirst()
                .orElse(ResultCode.PARAM_ERROR.getMessage());
        return Result.failed(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理业务入参非法异常。
     *
     * @param exception 参数异常
     * @return 统一响应体
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        String message = StringUtils.hasText(exception.getMessage()) ? exception.getMessage() : "请求参数不合法";
        return Result.failed(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 兜底异常处理，避免内部异常细节直接暴露给调用方。
     *
     * @param exception 系统异常
     * @return 统一响应体
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return Result.failed(ResultCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
    }

    private String resolveValidationMessage(BindException exception) {
        return exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("请求参数校验失败");
    }
}
