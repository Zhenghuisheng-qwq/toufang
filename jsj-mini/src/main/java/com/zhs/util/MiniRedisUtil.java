package com.zhs.util;

import com.zhs.common.cachekey.MiniCacheKey;
import com.zhs.service.redis.RedisCache;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 小程序 Redis 工具类：桥接 MiniCacheKey 与 jsj-service RedisCache。
 */
@Component
public class MiniRedisUtil {

    @Resource(name = "jsjServiceRedisCache")
    private RedisCache redisCache;

    public <T> void set(final MiniCacheKey cacheKey, final T value) {
        long expire = cacheKey.getExpireTime();
        if (expire <= 0) {
            throw new IllegalArgumentException(
                    "CacheKey 未配置过期时间，若确需永久 key 请调用 setForever(): " + cacheKey.buildKey());
        }
        redisCache.setCacheObject(cacheKey.buildKey(), value, expire, cacheKey.getExpireTimeUnit());
    }

    public <T> void setForever(final MiniCacheKey cacheKey, final T value) {
        redisCache.setCacheObject(cacheKey.buildKey(), value);
    }

    public <T> boolean setIfAbsent(final MiniCacheKey cacheKey, final T value) {
        long expire = cacheKey.getExpireTime();
        if (expire <= 0) {
            throw new IllegalArgumentException("setIfAbsent 要求 CacheKey 必须配置过期时间: " + cacheKey.buildKey());
        }
        return redisCache.setIfAbsent(cacheKey.buildKey(), value, expire, cacheKey.getExpireTimeUnit());
    }

    public <T> T get(final MiniCacheKey cacheKey) {
        return redisCache.getCacheObject(cacheKey.buildKey());
    }

    public <T> T getAndDelete(final MiniCacheKey cacheKey) {
        return redisCache.getAndDelete(cacheKey.buildKey());
    }

    public long increment(final MiniCacheKey cacheKey, long delta) {
        return redisCache.increment(cacheKey.buildKey(), delta);
    }

    public boolean delete(final MiniCacheKey cacheKey) {
        return redisCache.deleteObject(cacheKey.buildKey());
    }

    public boolean hasKey(final MiniCacheKey cacheKey) {
        return redisCache.hasKey(cacheKey.buildKey());
    }

    public boolean expire(final MiniCacheKey cacheKey) {
        return redisCache.expire(cacheKey.buildKey(), cacheKey.getExpireTime(), cacheKey.getExpireTimeUnit());
    }

    public long getExpire(final MiniCacheKey cacheKey) {
        return redisCache.getExpire(cacheKey.buildKey(), cacheKey.getExpireTimeUnit());
    }
}
