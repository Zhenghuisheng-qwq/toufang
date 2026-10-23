package com.zhs.service.acq.flow;

import com.wechat.pay.java.core.*;
import com.wechat.pay.java.core.notification.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.h5.H5Service;
import com.wechat.pay.java.service.payments.h5.model.*;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.mapper.acq.PayMerchantVersionMapper;
import com.zhs.service.model.acq.*;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Objects;

/** Official SDK owns request signing, response verification and callback AEAD decryption. */
@Component
public class WechatPaymentAdapter {
    @Resource private PlatformSecrets secrets;
    @Resource private PayMerchantVersionMapper versions;
    @Value("${acq.payment.enabled:false}") private boolean enabled;
    public void requireEnabled(){if(!enabled)throw new BusinessException("真实支付尚未开放");}
    private record Client(Config config,NotificationConfig notification){}
    private Client client(TradeOrder order){
        requireEnabled();
        var v=versions.selectById(order.getMerchantVersionId());
        if(v==null||!Objects.equals(v.getPayMerchantId(),order.getPayMerchantId()))throw new BusinessException("订单支付配置不可用");
        String key=secrets.resolve(v.getPrivateKeyRef());String api=secrets.resolve(v.getApiV3KeyRef());
        if("PUBLIC_KEY".equals(v.getVerifyMode())){
            var config=new RSAPublicKeyConfig.Builder().merchantId(order.getMchId()).privateKey(key).merchantSerialNumber(v.getMerchantSerialNo())
                    .apiV3Key(api).publicKey(secrets.resolve(v.getPlatformPublicKeyRef())).publicKeyId(v.getPlatformKeyId()).build();
            return new Client(config,config);
        }
        if("CERTIFICATE".equals(v.getVerifyMode())){
            String certificate=secrets.resolve(v.getPlatformCertRef());
            var config=new RSAConfig.Builder().merchantId(order.getMchId()).privateKey(key).merchantSerialNumber(v.getMerchantSerialNo()).wechatPayCertificates(certificate).build();
            var notification=new RSANotificationConfig.Builder().apiV3Key(api).certificates(certificate).build();
            return new Client(config,notification);
        }
        throw new BusinessException("支付验签模式尚未确认");
    }
    public String h5(TradeOrder order,String ip){
        var service=new H5Service.Builder().config(client(order).config()).build();
        var request=new PrepayRequest();request.setAppid(order.getAppId());request.setMchid(order.getMchId());request.setOutTradeNo(order.getOrderNo());
        request.setDescription(order.getPageTitle()==null?"付费报告":order.getPageTitle());request.setNotifyUrl("https://"+order.getDomainName()+"/api/payments/notify/"+order.getOrderNo());
        var amount=new Amount();amount.setCurrency("CNY");amount.setTotal(order.getAmountFen());request.setAmount(amount);
        var scene=new SceneInfo();scene.setPayerClientIp(ip);var info=new H5Info();info.setType("Wap");info.setAppName("付费报告");info.setAppUrl("https://"+order.getDomainName());scene.setH5Info(info);request.setSceneInfo(scene);
        return service.prepay(request).getH5Url();
    }
    public Transaction query(TradeOrder order){
        var request=new QueryOrderByOutTradeNoRequest();request.setMchid(order.getMchId());request.setOutTradeNo(order.getOrderNo());
        return new H5Service.Builder().config(client(order).config()).build().queryOrderByOutTradeNo(request);
    }
    public Transaction notification(TradeOrder order,RequestParam request){return new NotificationParser(client(order).notification()).parse(request,Transaction.class);}
    public com.wechat.pay.java.service.refund.model.Refund refund(TradeOrder order,String transaction,String refundNo,String reason){
        var request=new com.wechat.pay.java.service.refund.model.CreateRequest();request.setTransactionId(transaction);request.setOutRefundNo(refundNo);request.setReason(reason);
        var amount=new com.wechat.pay.java.service.refund.model.AmountReq();amount.setRefund(order.getAmountFen().longValue());amount.setTotal(order.getAmountFen().longValue());amount.setCurrency("CNY");request.setAmount(amount);
        return new com.wechat.pay.java.service.refund.RefundService.Builder().config(client(order).config()).build().create(request);
    }
    public com.wechat.pay.java.service.refund.model.Refund queryRefund(TradeOrder order,String refundNo){
        var request=new com.wechat.pay.java.service.refund.model.QueryByOutRefundNoRequest();request.setOutRefundNo(refundNo);
        return new com.wechat.pay.java.service.refund.RefundService.Builder().config(client(order).config()).build().queryByOutRefundNo(request);
    }
    public String jsapi(TradeOrder order,String openid){
        var service=new com.wechat.pay.java.service.payments.jsapi.JsapiService.Builder().config(client(order).config()).build();
        var request=new com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest();request.setAppid(order.getAppId());request.setMchid(order.getMchId());request.setOutTradeNo(order.getOrderNo());request.setDescription(order.getPageTitle()==null?"付费报告":order.getPageTitle());request.setNotifyUrl("https://"+order.getDomainName()+"/api/payments/notify/"+order.getOrderNo());
        var amount=new com.wechat.pay.java.service.payments.jsapi.model.Amount();amount.setTotal(order.getAmountFen());amount.setCurrency("CNY");request.setAmount(amount);
        var payer=new com.wechat.pay.java.service.payments.jsapi.model.Payer();payer.setOpenid(openid);request.setPayer(payer);return service.prepay(request).getPrepayId();
    }
    /** Same message layout as SDK 0.2.17 JsapiServiceExtension; SDK signer performs RSA signing. */
    public java.util.Map<String,String> jsapiParameters(TradeOrder order,String prepay){
        String time=String.valueOf(java.time.Instant.now().getEpochSecond());String nonce=com.wechat.pay.java.core.util.NonceUtil.createNonce(32);String pkg="prepay_id="+prepay;
        String message=order.getAppId()+"\n"+time+"\n"+nonce+"\n"+pkg+"\n";
        String signature=client(order).config().createSigner().sign(message).getSign();
        return java.util.Map.of("appId",order.getAppId(),"timeStamp",time,"nonceStr",nonce,"package",pkg,"signType","RSA","paySign",signature);
    }
}
