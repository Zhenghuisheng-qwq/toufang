package com.zhs.service.support;

/**
 * 认证相关 Redis / 外部能力由应用层实现，Service 层只依赖接口。
 */
public interface AuthTokenSupport {

    void storeToken(String token, Long userId);

    Long removeTokenAndGetUserId(String token);

    boolean tryAcquireSmsSendLimit(String phone);

    void storeSmsCode(String phone, String code);

    String consumeSmsCode(String phone);

    boolean tryAcquireNewUserLock(String lockKey);

    void releaseNewUserLock(String lockKey);

    boolean isDevProfile();
}
