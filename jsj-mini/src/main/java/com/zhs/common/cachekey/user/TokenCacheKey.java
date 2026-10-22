package com.zhs.common.cachekey.user;

import com.zhs.common.cachekey.UserModuleCacheKey;

import java.util.concurrent.TimeUnit;

/**
 * 登录 Token 的 CacheKey
 * <p>最终 key：<code>jsj-mini:user:Token:{token}</code>
 * <p>过期时间：30 天
 */
public class TokenCacheKey extends UserModuleCacheKey {

    private static final long EXPIRE_DAYS = 30;

    private final String token;

    public TokenCacheKey(String token) {
        this.token = token;
    }

    @Override
    protected String getSuffixKey() {
        return token;
    }

    @Override
    public long getExpireTime() {
        return EXPIRE_DAYS;
    }

    @Override
    public TimeUnit getExpireTimeUnit() {
        return TimeUnit.DAYS;
    }
}
