package com.zhs.service.acq;

import com.zhs.common.exception.BusinessException;

/**
 * 密钥只允许保存外部引用，拒绝私钥正文和裸密钥。
 */
public final class SecretRefGuard {

    private SecretRefGuard() {
    }

    public static void requireRef(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!value.matches("ref:[A-Za-z0-9][A-Za-z0-9_:/.-]{0,123}") || value.contains("..")
                || value.contains("BEGIN") || value.length() > 128) {
            throw new BusinessException(fieldName + "只能保存ref:引用");
        }
    }
}
