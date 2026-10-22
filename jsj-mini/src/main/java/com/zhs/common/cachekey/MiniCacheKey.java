package com.zhs.common.cachekey;

import java.util.concurrent.TimeUnit;

/**
 * Redis CacheKey 抽象基类
 * <p>
 * 业务层通过继承模块级基类（如 {@link UserModuleCacheKey}）
 * 实现具体 CacheKey，只需声明 suffixKey 与过期时间，无需手写字符串前缀。
 * <p>
 * 最终 Redis key 自动按 <b>{appName}:{module}:{className}:{suffix}</b> 组装，
 * 保证跨模块、跨类的 key 天然唯一，避免碰撞与人为笔误。
 */
public abstract class MiniCacheKey {

    /** 统一分隔符 */
    public static final String CACHE_KEY_SEPARATOR = ":";

    /** 应用名（用 spring.application.name 的值），作为所有 key 的一级前缀 */
    public static final String APP_NAME = "jsj-mini";

    /** 从类名剥离的冗余后缀 */
    private static final String CLASS_NAME_SUFFIX = "CacheKey";

    /**
     * 模块前缀，例如 "user"。由模块级抽象类实现。
     */
    protected abstract String getModulePrefix();

    /**
     * 业务后缀 — 拼入 key 末尾的动态部分（userId / token …）。
     * 可返回多个字段时自行用 {@link #CACHE_KEY_SEPARATOR} 连接。
     */
    protected abstract String getSuffixKey();

    /** 过期时间（&lt;=0 表示永不过期） */
    public abstract long getExpireTime();

    /** 过期时间单位 */
    public abstract TimeUnit getExpireTimeUnit();

    /**
     * 最终 key 组装：<code>{APP_NAME}:{module}:{class}:{suffix}</code>
     * <p>例如 <code>jsj-mini:user:Token:abc</code>
     * <p>类名会自动剥离 "CacheKey" 后缀（如有），让 key 更简洁。
     */
    public final String buildKey() {
        String className = getClass().getSimpleName();
        if (className.endsWith(CLASS_NAME_SUFFIX)) {
            className = className.substring(0, className.length() - CLASS_NAME_SUFFIX.length());
        }
        return APP_NAME + CACHE_KEY_SEPARATOR
                + getModulePrefix() + CACHE_KEY_SEPARATOR
                + className + CACHE_KEY_SEPARATOR
                + getSuffixKey();
    }
}
