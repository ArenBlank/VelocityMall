package com.velocitymall.admin.context;

public final class AdminContext {

    private static final ThreadLocal<Long> ADMIN_THREAD_LOCAL = new ThreadLocal<>();

    private AdminContext() {
    }

    public static void setAdminId(Long adminId) {
        ADMIN_THREAD_LOCAL.set(adminId);
    }

    public static Long getAdminId() {
        return ADMIN_THREAD_LOCAL.get();
    }

    public static void removeAdminId() {
        ADMIN_THREAD_LOCAL.remove();
    }
}
