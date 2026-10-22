package com.zhs.common.cachekey.user;

import com.zhs.common.cachekey.UserModuleCacheKey;

import java.util.concurrent.TimeUnit;

/**
 * 手机验证码 CacheKey
 * <p>最终 key：<code>jsj-mini:user:SmsCode:{phone}</code>
 * <p>过期时间：5 分钟
 */
public class SmsCodeCacheKey extends UserModuleCacheKey {

    private static final long EXPIRE_MINUTES = 5;

    private final String phone;

    public SmsCodeCacheKey(String phone) {
        this.phone = phone;
    }

    @Override
    protected String getSuffixKey() {
        return phone;
    }

    @Override
    public long getExpireTime() {
        return EXPIRE_MINUTES;
    }

    @Override
    public TimeUnit getExpireTimeUnit() {
        return TimeUnit.MINUTES;
    }
}
