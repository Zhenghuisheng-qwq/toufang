package com.zhs.service.acq;

import com.zhs.common.exception.BusinessException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 域名只保存主机名。协议、路径和端口直接拒绝，不在服务端拼接。
 */
public final class DomainNames {

    private static final Pattern HOST = Pattern.compile(
            "^(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\\.(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))+$");

    private DomainNames() {
    }

    public static String canonicalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("域名不能为空");
        }
        String value = raw.trim();
        if (value.contains("://") || value.contains("/") || value.contains("?") || value.contains("#")
                || value.contains("\\") || value.contains(" ")) {
            throw new BusinessException("请只填写域名，不要包含协议或路径");
        }
        if (value.indexOf(':') >= 0) {
            throw new BusinessException("请只填写域名，不要包含端口");
        }
        if (value.endsWith(".")) {
            throw new BusinessException("域名不能以点结尾");
        }
        value = value.toLowerCase(Locale.ROOT);
        if (!HOST.matcher(value).matches()) {
            throw new BusinessException("域名格式不正确");
        }
        return value;
    }
}
