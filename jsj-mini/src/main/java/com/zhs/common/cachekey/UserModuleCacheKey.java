package com.zhs.common.cachekey;

/**
 * 用户模块 CacheKey 基类 — 登录 / token / 个人信息等。
 * 模块前缀固定为 "user"，最终 key 形如 "jsj-mini:user:xxx:yyy"。
 */
public abstract class UserModuleCacheKey extends MiniCacheKey {

    @Override
    protected final String getModulePrefix() {
        return "user";
    }
}
