package com.zhs.service.acq.flow;

import com.zhs.common.exception.BusinessException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class FlowSecurity {

    private static final SecureRandom RANDOM = new SecureRandom();

    private FlowSecurity() {
    }

    public static String token() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static String key(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{16,64}")) {
            throw new BusinessException("请求标识无效，请刷新后重试");
        }
        return value;
    }

    public static String credentialHash(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
            throw new BusinessException("访问授权缺失或已过期");
        }
        return hash(token);
    }
}
