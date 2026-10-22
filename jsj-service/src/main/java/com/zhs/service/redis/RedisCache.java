package com.zhs.service.redis;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Redis 底层操作封装（jsj-service）。
 * <p>
 * Bean 名刻意避开 Admin 侧 {@code com.zhs.common.core.redis.RedisCache}。
 */
@Component("jsjServiceRedisCache")
public class RedisCache {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public <T> void setCacheObject(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public <T> void setCacheObject(String key, T value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public <T> boolean setIfAbsent(String key, T value, long timeout, TimeUnit unit) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
        return Boolean.TRUE.equals(ok);
    }

    @SuppressWarnings("unchecked")
    public <T> T getCacheObject(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAndDelete(String key) {
        return (T) redisTemplate.opsForValue().getAndDelete(key);
    }

    public long increment(String key, long delta) {
        Long v = redisTemplate.opsForValue().increment(key, delta);
        return v == null ? 0L : v;
    }

    public boolean deleteObject(String key) {
        Boolean result = redisTemplate.delete(key);
        return Boolean.TRUE.equals(result);
    }

    public long deleteObjects(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        Long count = redisTemplate.delete(keys);
        return count == null ? 0L : count;
    }

    public boolean hasKey(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        Boolean result = redisTemplate.expire(key, timeout, unit);
        return Boolean.TRUE.equals(result);
    }

    public long getExpire(String key, TimeUnit unit) {
        Long v = redisTemplate.getExpire(key, unit);
        return v == null ? -2L : v;
    }
}
