package com.zhs.service.acq;

/**
 * 有稳定点击标识时才生成去重键。没有标识时返回空，禁止编造点击。
 */
public final class AdClickKeys {

    private AdClickKeys() {
    }

    public static String dedupKey(Integer platform, String primaryClickId) {
        if (platform == null || primaryClickId == null || primaryClickId.isBlank()) {
            return null;
        }
        return platform + ":" + primaryClickId.trim();
    }
}
