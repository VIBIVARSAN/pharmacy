package com.pharmacy.util;

/** Per-request data (client IP) made available to the service layer for audit logging. */
public final class RequestContext {
    private static final ThreadLocal<String> IP = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void setIpAddress(String ip) {
        IP.set(ip);
    }

    public static String getIpAddress() {
        return IP.get();
    }

    public static void clear() {
        IP.remove();
    }
}
