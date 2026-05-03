package com.velocitymall.common.context;

/**
 * 当前请求用户上下文。
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_THREAD_LOCAL = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUserId(Long userId) {
        USER_THREAD_LOCAL.set(userId);
    }

    public static Long getUserId() {
        return USER_THREAD_LOCAL.get();
    }

    public static void removeUserId() {
        USER_THREAD_LOCAL.remove();
    }
}
