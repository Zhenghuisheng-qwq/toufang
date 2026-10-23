package com.zhs.service.acq.flow;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.TradeOrderService;
import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.PayRecord;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;

/** Accept only evidence already authenticated by the platform adapter; never expose as an HTTP command. */
@Service
public class PaymentConfirmationService {
    public enum Source { NOTIFY, QUERY }
    public record Evidence(Source source,String evidenceId,String digest,String orderNo,String transactionId,
                           String mchId,String appId,Integer amountFen,String currency,String state,LocalDateTime paidAt) {
        @Override public String toString(){return "PaymentEvidence[redacted]";}
    }
    @Resource private ReportWorkMapper locks;
    @Resource private BusinessFlowMapper flow;
    @Resource private PayRecordMapper payments;
    @Resource private TradeOrderService orders;
    @Resource private AdEventService events;

    @Transactional(transactionManager="acqTransactionManager",rollbackFor=Exception.class)
    public boolean confirm(Evidence e) {
        if(e==null||e.source()==null||blank(e.evidenceId())||e.evidenceId().length()>128||blank(e.transactionId())||e.transactionId().length()>64
                ||e.digest()==null||!e.digest().matches("[a-f0-9]{64}")||e.paidAt()==null||!"CNY".equals(e.currency())||!"SUCCESS".equals(e.state()))throw new BusinessException("支付凭据不完整");
        var order=locks.lockOrder(e.orderNo());var pay=locks.lockPayment(e.orderNo());
        if(order==null||pay==null||!Objects.equals(order.getMchId(),e.mchId())||!Objects.equals(order.getAppId(),e.appId())||!Objects.equals(order.getAmountFen(),e.amountFen())
                ||!Objects.equals(pay.getMchId(),e.mchId())||!Objects.equals(pay.getAppId(),e.appId())||!Objects.equals(pay.getAmountFen(),e.amountFen()))throw new BusinessException("支付凭据与订单快照不一致");
        if(e.paidAt().isAfter(locks.now().plusMinutes(5)))throw new BusinessException("支付成功时间不正确");
        boolean duplicate=Objects.equals(pay.getPayStatus(),1)&&Objects.equals(order.getPayStatus(),1);
        boolean refunded=Objects.equals(pay.getPayStatus(),3)&&Objects.equals(order.getPayStatus(),2)&&Objects.equals(pay.getSuccessOrderNo(),e.orderNo());
        if((duplicate||refunded)&&!Objects.equals(pay.getTransactionId(),e.transactionId()))throw new BusinessException("支付交易号冲突");
        if(!duplicate&&!refunded&&(!Objects.equals(pay.getPayStatus(),0)||!Objects.equals(order.getPayStatus(),0)))throw new BusinessException("订单已关闭或进入退款流程");
        flow.evidence(e.orderNo(),e.source().name(),e.evidenceId(),e.digest(),e.transactionId());
        var previous=flow.evidenceRow(e.source().name(),e.evidenceId());
        if(previous==null||!Objects.equals(previous.get("orderNo"),e.orderNo())||!Objects.equals(previous.get("transactionId"),e.transactionId())||!Objects.equals(previous.get("evidenceHash"),e.digest()))throw new BusinessException("支付凭据冲突");
        if(refunded)return false; // Acknowledge old success evidence without restoring entitlement or tasks.
        if(!duplicate){
            pay.setPayStatus(1);pay.setSuccessOrderNo(e.orderNo());pay.setTransactionId(e.transactionId());pay.setWxTradeState("SUCCESS");pay.setSuccessTime(e.paidAt());
            pay.setNotifyId(e.source()==Source.NOTIFY?e.evidenceId():null);pay.setConfirmationSource(e.source().name());pay.setEvidenceHash(e.digest());
            if(payments.updateById(pay)!=1||!orders.markPaid(e.orderNo(),e.paidAt()))throw new BusinessException("支付入账失败");
        }
        for(int type=1;type<=3;type++)flow.task(e.orderNo(),type);
        events.register(AdEventService.PAY,"ORDER",order.getId(),order.getOrderNo(),order.getAdClickId(),order.getAmountFen());
        return !duplicate;
    }
    private static boolean blank(String value){return value==null||value.isBlank();}
}
