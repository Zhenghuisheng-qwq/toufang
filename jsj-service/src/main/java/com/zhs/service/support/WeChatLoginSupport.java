package com.zhs.service.support;

/**
 * 微信 code2Session，由 jsj-mini 调用微信 HTTP 接口实现。
 */
public interface WeChatLoginSupport {

    record WeChatSession(String openId, String unionId) {
    }

    WeChatSession getSessionByCode(String code);
}
