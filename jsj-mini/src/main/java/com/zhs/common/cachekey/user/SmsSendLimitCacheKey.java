package com.zhs.common.cachekey.user;

import com.zhs.common.cachekey.UserModuleCacheKey;

import java.util.concurrent.TimeUnit;

/**
 * 手机验证码发送频率限制 CacheKey
 * <p>最终 key：<code>jsj-mini:user:SmsSendLimit:{phone}</code>
 * <p>过期时间：60 秒（防刷）
 */
public class SmsSendLimitCacheKey extends UserModuleCacheKey {

    private static final long EXPIRE_SECONDS = 60;

    private final String phone;

    public SmsSendLimitCacheKey(String phone) {
        this.phone = phone;
    }

    @Override
    protected String getSuffixKey() {
        return phone;
    }

    @Override
    public long getExpireTime() {
        return EXPIRE_SECONDS;
    }

    @Override
    public TimeUnit getExpireTimeUnit() {
        return TimeUnit.SECONDS;
    }
}
