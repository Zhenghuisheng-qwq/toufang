package com.zhs.service.acq.flow;

import com.zhs.common.exception.BusinessException;

import java.util.Locale;

/**
 * 本地联调标记。默认关闭，只接受回环来源加回环 Host，不接受任意 Host 伪装。
 */
public final class H5LoopbackPreview {

    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();

    private H5LoopbackPreview() {
    }

    public static boolean eligible(String remoteAddress, String host) {
        return loopbackAddress(remoteAddress) && loopbackHost(host);
    }

    public static void openIf(boolean enabled, String remoteAddress, String host) {
        if (enabled && eligible(remoteAddress, host)) {
            ACTIVE.set(Boolean.TRUE);
        }
    }

    public static void close() {
        ACTIVE.remove();
    }

    public static boolean active() {
        return Boolean.TRUE.equals(ACTIVE.get());
    }

    public static String hostKey(String raw) {
        if (!active()) {
            throw new IllegalStateException("loopback preview is inactive");
        }
        String value = normalize(raw);
        if (!loopbackHost(value)) {
            throw new BusinessException("访问域名未启用或未核验");
        }
        return value;
    }

    private static boolean loopbackAddress(String address) {
        String value = normalize(address);
        return "127.0.0.1".equals(value) || "::1".equals(value) || "0:0:0:0:0:0:0:1".equals(value);
    }

    private static boolean loopbackHost(String host) {
        String value = normalize(host);
        return "127.0.0.1".equals(value) || "localhost".equals(value) || "::1".equals(value)
                || "0:0:0:0:0:0:0:1".equals(value);
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
