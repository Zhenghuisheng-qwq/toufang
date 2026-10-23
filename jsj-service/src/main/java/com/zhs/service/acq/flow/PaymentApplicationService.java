package com.zhs.service.acq.flow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

/** Deliberately not transactional: platform I/O happens after the ownership transaction commits. */
@Service
public class PaymentApplicationService {
    @Resource private H5FlowService ownership;
    @Resource private WechatPaymentAdapter adapter;
    @Resource private BusinessFlowMapper work;
    @Resource private PayRecordMapper payments;
    @Resource private PaymentConfirmationService confirmations;
    public Map<String,String> jsapi(String token,String host,String no){
        var order=ownership.ownedOrder(token,host,no);adapter.requireEnabled();
        if(!Objects.equals(order.getPayStatus(),0)||!Objects.equals(order.getTradeType(),2))throw new BusinessException("订单不支持此支付方式");
        var pay=payments.selectOne(new LambdaQueryWrapper<PayRecord>().eq(PayRecord::getOrderNo,no));
        if(pay==null||pay.getPayerOpenid()==null)throw new BusinessException("请先完成公众号授权");
        if("READY".equals(pay.getAttemptState())&&pay.getPrepayId()!=null)return adapter.jsapiParameters(order,pay.getPrepayId());
        String owner=UUID.randomUUID().toString();if(work.paymentClaim(no,owner)!=1)throw new BusinessException("支付请求结果未确认，请先查单");
        try{String prepay=adapter.jsapi(order,pay.getPayerOpenid());if(work.jsapiFinish(no,owner,prepay)!=1)throw new BusinessException("订单状态已变化");return adapter.jsapiParameters(order,prepay);}
        catch(RuntimeException e){work.paymentFinish(no,owner,"UNKNOWN",null);throw new BusinessException("支付请求未确认，请查询后处理");}
    }
    public String start(String token,String host,String no,String ip){
        var order=ownership.ownedOrder(token,host,no);
        adapter.requireEnabled();
        if(!Objects.equals(order.getPayStatus(),0))throw new BusinessException("订单已支付或关闭");
        if(!Objects.equals(order.getTradeType(),1))throw new BusinessException("请先完成公众号授权后使用 JSAPI 支付");
        var pay=payments.selectOne(new LambdaQueryWrapper<PayRecord>().eq(PayRecord::getOrderNo,no));
        if(pay!=null&&"READY".equals(pay.getAttemptState()))return pay.getPaymentUrl();
        String owner=UUID.randomUUID().toString();
        if(work.paymentClaim(no,owner)!=1)throw new BusinessException("支付请求处理中或结果未确认，请先查单");
        try{
            String url=adapter.h5(order,ip);
            var parsed=java.net.URI.create(url);
            if(!"https".equals(parsed.getScheme())||!"wx.tenpay.com".equalsIgnoreCase(parsed.getHost())||parsed.getUserInfo()!=null)throw new BusinessException("支付链接校验失败");
            if(work.paymentFinish(no,owner,"READY",url)!=1)throw new BusinessException("订单状态已变化，请刷新");return url;
        }catch(RuntimeException e){work.paymentFinish(no,owner,"UNKNOWN",null);throw new BusinessException("支付请求未确认，请查询支付状态后处理");}
    }
    public boolean query(String token,String host,String no){
        var order=ownership.ownedOrder(token,host,no);var tx=adapter.query(order);
        if(tx.getTradeState()!=Transaction.TradeStateEnum.SUCCESS)return false;
        confirm(order,tx,PaymentConfirmationService.Source.QUERY,tx.getTransactionId(),digest(tx));return true;
    }
    public void confirm(TradeOrder expected,Transaction tx,PaymentConfirmationService.Source source,String key,String digest){
        if(!Objects.equals(expected.getOrderNo(),tx.getOutTradeNo())||tx.getAmount()==null)throw new BusinessException("支付返回订单不匹配");
        confirmations.confirm(new PaymentConfirmationService.Evidence(source,key,digest,tx.getOutTradeNo(),tx.getTransactionId(),tx.getMchid(),tx.getAppid(),tx.getAmount().getTotal(),tx.getAmount().getCurrency(),String.valueOf(tx.getTradeState()),OffsetDateTime.parse(tx.getSuccessTime()).atZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime()));
    }
    private String digest(Transaction tx){return FlowSecurity.hash(String.join("|",String.valueOf(tx.getOutTradeNo()),String.valueOf(tx.getTransactionId()),String.valueOf(tx.getMchid()),String.valueOf(tx.getAppid()),String.valueOf(tx.getAmount().getTotal()),String.valueOf(tx.getAmount().getCurrency()),String.valueOf(tx.getSuccessTime())));}
}
