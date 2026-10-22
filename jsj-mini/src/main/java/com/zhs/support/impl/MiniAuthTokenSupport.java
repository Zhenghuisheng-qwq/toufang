package com.zhs.support.impl;

import com.zhs.common.cachekey.user.NewUserLockCacheKey;
import com.zhs.common.cachekey.user.SmsCodeCacheKey;
import com.zhs.common.cachekey.user.SmsSendLimitCacheKey;
import com.zhs.common.cachekey.user.TokenCacheKey;
import com.zhs.service.support.AuthTokenSupport;
import com.zhs.util.MiniRedisUtil;
import jakarta.annotation.Resource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class MiniAuthTokenSupport implements AuthTokenSupport {

    @Resource
    private MiniRedisUtil miniRedisUtil;

    @Resource
    private Environment environment;

    @Override
    public void storeToken(String token, Long userId) {
        miniRedisUtil.set(new TokenCacheKey(token), userId);
    }

    @Override
    public Long removeTokenAndGetUserId(String token) {
        Object userIdObj = miniRedisUtil.getAndDelete(new TokenCacheKey(token));
        return userIdObj != null ? Long.valueOf(userIdObj.toString()) : null;
    }

    @Override
    public boolean tryAcquireSmsSendLimit(String phone) {
        return miniRedisUtil.setIfAbsent(new SmsSendLimitCacheKey(phone), 1);
    }

    @Override
    public void storeSmsCode(String phone, String code) {
        miniRedisUtil.set(new SmsCodeCacheKey(phone), code);
    }

    @Override
    public String consumeSmsCode(String phone) {
        return miniRedisUtil.getAndDelete(new SmsCodeCacheKey(phone));
    }

    @Override
    public boolean tryAcquireNewUserLock(String lockKey) {
        return miniRedisUtil.setIfAbsent(new NewUserLockCacheKey(lockKey), 1);
    }

    @Override
    public void releaseNewUserLock(String lockKey) {
        miniRedisUtil.delete(new NewUserLockCacheKey(lockKey));
    }

    @Override
    public boolean isDevProfile() {
        String[] active = environment.getActiveProfiles();
        if (active.length == 0) {
            active = environment.getDefaultProfiles();
        }
        return Arrays.stream(active).anyMatch(p -> p.equalsIgnoreCase("dev") || p.equalsIgnoreCase("local"));
    }
}
