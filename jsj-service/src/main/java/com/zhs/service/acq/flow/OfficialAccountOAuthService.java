package com.zhs.service.acq.flow;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.mapper.acq.BusinessFlowMapper;
import com.zhs.service.model.acq.TradeOrder;
import jakarta.annotation.Resource;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.service.WxOAuth2Service;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class OfficialAccountOAuthService {

    @Resource
    private H5FlowService ownership;

    @Resource
    private BusinessFlowMapper flow;

    @Resource
    private PlatformSecrets secrets;

    @Resource
    private Environment environment;

    private WxOAuth2Service client(String appId) {
        if (!environment.getProperty("acq.oauth.enabled", Boolean.class, false)) {
            throw new BusinessException("公众号授权尚未开放");
        }
        if (appId == null || !appId.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new BusinessException("公众号配置不正确");
        }
        WxMpDefaultConfigImpl config = new WxMpDefaultConfigImpl();
        config.setAppId(appId);
        config.setSecret(secrets.resolve(environment.getProperty("acq.oauth." + appId + ".secret-ref")));
        WxMpServiceImpl service = new WxMpServiceImpl();
        service.setWxMpConfigStorage(config);
        return service.getOAuth2Service();
    }

    public String begin(String token, String host, String no) {
        TradeOrder order = ownership.ownedOrder(token, host, no);
        if (!Objects.equals(order.getTradeType(), 2) || !Objects.equals(order.getPayStatus(), 0)) {
            throw new BusinessException("订单不需要公众号授权");
        }
        WxOAuth2Service client = client(order.getAppId());
        String state = FlowSecurity.token();
        flow.oauth(FlowSecurity.hash(state), order.getSessionId(), no, order.getAppId());
        return client.buildAuthorizationUrl("https://" + order.getDomainName() + "/api/h5/oauth/callback", "snsapi_base", state);
    }

    public String callback(String token, String host, String state, String code) {
        String hash = FlowSecurity.credentialHash(state);
        String no = flow.oauthOrder(hash);
        if (no == null) {
            throw new BusinessException("授权状态过期或已使用");
        }
        TradeOrder order = ownership.ownedOrder(token, host, no);
        if (flow.consumeOauth(hash, order.getSessionId(), no, order.getAppId()) != 1) {
            throw new BusinessException("授权状态不匹配");
        }
        if (code == null || code.isBlank() || code.length() > 256) {
            throw new BusinessException("公众号授权未完成");
        }
        try {
            WxOAuth2AccessToken result = client(order.getAppId()).getAccessToken(code);
            String openid = result.getOpenId();
            if (openid == null || openid.isBlank() || openid.length() > 64 || flow.openid(no, order.getAppId(), openid) != 1) {
                throw new BusinessException("授权身份与订单不匹配");
            }
        } catch (WxErrorException e) {
            throw new BusinessException("公众号授权失败，请返回订单重试");
        }
        return order.getReturnPath();
    }
}
