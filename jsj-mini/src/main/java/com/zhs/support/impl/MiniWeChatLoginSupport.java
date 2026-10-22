package com.zhs.support.impl;

import com.zhs.dto.wechat.WeChatSessionResult;
import com.zhs.service.support.WeChatLoginSupport;
import com.zhs.service.wechat.WeChatMiniService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class MiniWeChatLoginSupport implements WeChatLoginSupport {

    @Resource
    private WeChatMiniService weChatMiniService;

    @Override
    public WeChatSession getSessionByCode(String code) {
        WeChatSessionResult result = weChatMiniService.getOpenIdByCode(code);
        return new WeChatSession(result.getOpenId(), result.getUnionId());
    }
}
