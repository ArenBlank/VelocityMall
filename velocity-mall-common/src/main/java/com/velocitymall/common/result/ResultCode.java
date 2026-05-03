package com.velocitymall.common.result;

import lombok.Getter;

/**
 * 全局统一响应码。
 */
@Getter
public enum ResultCode {

    SUCCESS(20000, "操作成功"),

    PARAM_ERROR(40000, "请求参数错误"),

    UNAUTHORIZED(40100, "未认证或登录已失效"),

    FORBIDDEN(40300, "无访问权限"),

    SYSTEM_ERROR(50000, "系统异常"),

    BIZ_WARNING(50001, "业务处理失败");

    private final Integer code;

    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
