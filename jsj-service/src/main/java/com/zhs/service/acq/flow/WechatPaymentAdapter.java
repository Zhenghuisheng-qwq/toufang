package com.zhs.service.acq.flow;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAConfig;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RSANotificationConfig;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.core.util.NonceUtil;
import com.wechat.pay.java.service.payments.h5.H5Service;
import com.wechat.pay.java.service.payments.h5.model.Amount;
import com.wechat.pay.java.service.payments.h5.model.H5Info;
import com.wechat.pay.java.service.payments.h5.model.PrepayRequest;
import com.wechat.pay.java.service.payments.h5.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.h5.model.SceneInfo;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.QueryByOutRefundNoRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.mapper.acq.PayMerchantVersionMapper;
import com.zhs.service.model.acq.PayMerchantVersion;
import com.zhs.service.model.acq.TradeOrder;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Official SDK owns request signing, response verification and callback AEAD decryption. */
@Component
public class WechatPaymentAdapter {

    @Resource
    private PlatformSecrets secrets;

    @Resource
    private PayMerchantVersionMapper versions;

    @Value("${acq.payment.enabled:false}")
    private boolean enabled;

    public void requireEnabled() {
        if (!enabled) {
            throw new BusinessException("真实支付尚未开放");
        }
    }

    private record Client(Config config, NotificationConfig notification) {
    }

    private Client client(TradeOrder order) {
        requireEnabled();
        PayMerchantVersion version = versions.selectById(order.getMerchantVersionId());
        if (version == null || !Objects.equals(version.getPayMerchantId(), order.getPayMerchantId())) {
            throw new BusinessException("订单支付配置不可用");
        }
        String key = secrets.resolve(version.getPrivateKeyRef());
        String api = secrets.resolve(version.getApiV3KeyRef());
        if ("PUBLIC_KEY".equals(version.getVerifyMode())) {
            RSAPublicKeyConfig config = new RSAPublicKeyConfig.Builder()
                    .merchantId(order.getMchId())
                    .privateKey(key)
                    .merchantSerialNumber(version.getMerchantSerialNo())
                    .apiV3Key(api)
                    .publicKey(secrets.resolve(version.getPlatformPublicKeyRef()))
                    .publicKeyId(version.getPlatformKeyId())
                    .build();
            return new Client(config, config);
        }
        if ("CERTIFICATE".equals(version.getVerifyMode())) {
            String certificate = secrets.resolve(version.getPlatformCertRef());
            RSAConfig config = new RSAConfig.Builder()
                    .merchantId(order.getMchId())
                    .privateKey(key)
                    .merchantSerialNumber(version.getMerchantSerialNo())
                    .wechatPayCertificates(certificate)
                    .build();
            RSANotificationConfig notification = new RSANotificationConfig.Builder()
                    .apiV3Key(api)
                    .certificates(certificate)
                    .build();
            return new Client(config, notification);
        }
        throw new BusinessException("支付验签模式尚未确认");
    }

    public String h5(TradeOrder order, String ip) {
        H5Service service = new H5Service.Builder().config(client(order).config()).build();
        PrepayRequest request = new PrepayRequest();
        request.setAppid(order.getAppId());
        request.setMchid(order.getMchId());
        request.setOutTradeNo(order.getOrderNo());
        request.setDescription(order.getPageTitle() == null ? "付费报告" : order.getPageTitle());
        request.setNotifyUrl("https://" + order.getDomainName() + "/api/payments/notify/" + order.getOrderNo());
        Amount amount = new Amount();
        amount.setCurrency("CNY");
        amount.setTotal(order.getAmountFen());
        request.setAmount(amount);
        SceneInfo scene = new SceneInfo();
        scene.setPayerClientIp(ip);
        H5Info info = new H5Info();
        info.setType("Wap");
        info.setAppName("付费报告");
        info.setAppUrl("https://" + order.getDomainName());
        scene.setH5Info(info);
        request.setSceneInfo(scene);
        return service.prepay(request).getH5Url();
    }

    public Transaction query(TradeOrder order) {
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setMchid(order.getMchId());
        request.setOutTradeNo(order.getOrderNo());
        return new H5Service.Builder().config(client(order).config()).build().queryOrderByOutTradeNo(request);
    }

    public Transaction notification(TradeOrder order, RequestParam request) {
        return new NotificationParser(client(order).notification()).parse(request, Transaction.class);
    }

    public Refund refund(TradeOrder order, String transaction, String refundNo, String reason) {
        CreateRequest request = new CreateRequest();
        request.setTransactionId(transaction);
        request.setOutRefundNo(refundNo);
        request.setReason(reason);
        AmountReq amount = new AmountReq();
        amount.setRefund(order.getAmountFen().longValue());
        amount.setTotal(order.getAmountFen().longValue());
        amount.setCurrency("CNY");
        request.setAmount(amount);
        return new RefundService.Builder().config(client(order).config()).build().create(request);
    }

    public Refund queryRefund(TradeOrder order, String refundNo) {
        QueryByOutRefundNoRequest request = new QueryByOutRefundNoRequest();
        request.setOutRefundNo(refundNo);
        return new RefundService.Builder().config(client(order).config()).build().queryByOutRefundNo(request);
    }

    public String jsapi(TradeOrder order, String openid) {
        JsapiService service = new JsapiService.Builder().config(client(order).config()).build();
        com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest request =
                new com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest();
        request.setAppid(order.getAppId());
        request.setMchid(order.getMchId());
        request.setOutTradeNo(order.getOrderNo());
        request.setDescription(order.getPageTitle() == null ? "付费报告" : order.getPageTitle());
        request.setNotifyUrl("https://" + order.getDomainName() + "/api/payments/notify/" + order.getOrderNo());
        com.wechat.pay.java.service.payments.jsapi.model.Amount amount =
                new com.wechat.pay.java.service.payments.jsapi.model.Amount();
        amount.setTotal(order.getAmountFen());
        amount.setCurrency("CNY");
        request.setAmount(amount);
        com.wechat.pay.java.service.payments.jsapi.model.Payer payer =
                new com.wechat.pay.java.service.payments.jsapi.model.Payer();
        payer.setOpenid(openid);
        request.setPayer(payer);
        return service.prepay(request).getPrepayId();
    }

    /** Same message layout as SDK 0.2.17 JsapiServiceExtension; SDK signer performs RSA signing. */
    public Map<String, String> jsapiParameters(TradeOrder order, String prepay) {
        String time = String.valueOf(Instant.now().getEpochSecond());
        String nonce = NonceUtil.createNonce(32);
        String pkg = "prepay_id=" + prepay;
        String message = order.getAppId() + "\n" + time + "\n" + nonce + "\n" + pkg + "\n";
        String signature = client(order).config().createSigner().sign(message).getSign();
        return Map.of("appId", order.getAppId(), "timeStamp", time, "nonceStr", nonce,
                "package", pkg, "signType", "RSA", "paySign", signature);
    }
}
