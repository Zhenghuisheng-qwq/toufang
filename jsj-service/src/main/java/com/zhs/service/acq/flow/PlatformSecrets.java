package com.zhs.service.acq.flow;

import com.zhs.common.exception.BusinessException;
import jakarta.annotation.Resource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Database values are aliases, never arbitrary paths or property expressions. */
@Component
public class PlatformSecrets {

    @Resource
    private Environment environment;

    public String resolve(String alias) {
        if (alias != null && alias.startsWith("ref:")) {
            alias = alias.substring(4);
        }
        if (alias == null || !alias.matches("[A-Za-z0-9_-]{1,100}")) {
            throw new BusinessException("平台凭据引用未配置");
        }
        String value = environment.getProperty("acq.secrets." + alias);
        if (value == null || value.isBlank()) {
            throw new BusinessException("平台凭据尚未在运行环境配置");
        }
        return value;
    }
}
