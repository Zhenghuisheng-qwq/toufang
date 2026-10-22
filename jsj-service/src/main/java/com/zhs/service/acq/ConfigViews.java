package com.zhs.service.acq;

import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit view construction; entity fields never automatically become public API fields. */
public final class ConfigViews {
    private ConfigViews() { }
    public static Map<String, Object> of(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            Object value = pairs[i + 1];
            result.put((String) pairs[i], value instanceof Long ? value.toString() : value);
        }
        result.put("verificationNote", "仅登记配置，凭据尚未解析，未通过平台联调");
        return result;
    }
}
