package com.zhs.service.acq;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.model.acq.PayMerchantVersion;

import java.net.URI;
import java.util.Objects;
import java.util.Set;

/** Pure configuration rules. Completeness never implies resolution or platform authorization. */
public final class ConfigRules {

    private ConfigRules() {
    }

    public static String id(String value, String label) {
        String text = AcqTexts.requireText(value, label, 64);
        if (!text.matches("[A-Za-z0-9_.@-]+")) {
            throw new BusinessException(label + "格式不正确");
        }
        return text;
    }

    public static void unchanged(Object oldValue, Object newValue, boolean referenced, String label) {
        if (referenced && !Objects.equals(oldValue, newValue)) {
            throw new BusinessException(label + "已被引用，不能修改身份，请新增配置");
        }
    }

    public static void ownership(Long expectedCorp, Long actualCorp) {
        if (expectedCorp == null || !expectedCorp.equals(actualCorp)) {
            throw new BusinessException("老师不属于所选企业");
        }
    }

    public static String choice(String value, String label, Set<String> allowed) {
        if (value == null || !allowed.contains(value)) {
            throw new BusinessException(label + "取值不正确");
        }
        return value;
    }

    public static String link(String value) {
        String text = AcqTexts.optionalText(value, "获客链接", 512);
        if (text == null) {
            return null;
        }
        try {
            URI uri = URI.create(text);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getRawUserInfo() != null || uri.getFragment() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("获客链接必须是无用户信息的HTTPS地址");
        }
        // Stored only; never fetched by the server.
        return text;
    }

    public static boolean complete(PayMerchantVersion version) {
        if (version == null || blank(version.getApiV3KeyRef()) || blank(version.getPrivateKeyRef())
                || blank(version.getMerchantSerialNo())) {
            return false;
        }
        return "CERTIFICATE".equals(version.getVerifyMode()) ? !blank(version.getPlatformCertRef())
                : "PUBLIC_KEY".equals(version.getVerifyMode()) && !blank(version.getPlatformKeyId())
                && !blank(version.getPlatformPublicKeyRef());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
