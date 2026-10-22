package com.zhs.common.cachekey.user;

import com.zhs.common.cachekey.UserModuleCacheKey;

import java.util.concurrent.TimeUnit;

/**
 * 新用户注册互斥锁 —— 按 phone / openId 拼 key，避免并发创建重复账号。
 * <p>最终 key：<code>jsj-mini:user:NewUserLock:{phone|openId}</code>
 */
public class NewUserLockCacheKey extends UserModuleCacheKey {

    private static final long EXPIRE_SECONDS = 10;

    private final String identifier;

    public NewUserLockCacheKey(String identifier) {
        this.identifier = identifier;
    }

    @Override
    protected String getSuffixKey() {
        return identifier;
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
