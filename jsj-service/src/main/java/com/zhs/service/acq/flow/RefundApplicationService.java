package com.zhs.service.acq.flow;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.*;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;

/** Full refund only. Requires explicit entitlement-policy acknowledgement before any platform I/O. */
@Service
public class RefundApplicationService {
    private final TransactionTemplate transaction;
    public RefundApplicationService(@Qualifier("acqTransactionManager") PlatformTransactionManager manager){transaction=new TransactionTemplate(manager);}
    @Resource private ReportWorkMapper locks;
    @Resource private PayRefundMapper refunds;
    @Resource private TradeOrderMapper orders;
    @Resource private PayRecordMapper payments;
    @Resource private WechatPaymentAdapter adapter;
    @Value("${acq.refund.enabled:false}") private boolean enabled;
    @Value("${acq.refund.entitlement-policy:UNCONFIRMED}") private String policy;
    private record Intent(TradeOrder order,PayRecord payment,PayRefund refund,boolean created){}
    public Map<String,Object> refund(String no,String reason){
        if(!enabled||!"FULL_REVOKE".equals(policy))throw new BusinessException("退款未启用或退款后报告权限规则尚未确认");adapter.requireEnabled();
        if(reason==null||reason.isBlank()||reason.length()>80||reason.codePoints().anyMatch(Character::isISOControl))throw new BusinessException("请填写不超过80字的退款原因");
        Intent intent=transaction.execute(status->{
            var order=locks.lockOrder(no);var payment=locks.lockPayment(no);if(order==null||payment==null)throw new BusinessException("订单不存在");
            var previous=refunds.selectOne(new LambdaQueryWrapper<PayRefund>().eq(PayRefund::getOrderNo,no));
            if(previous!=null)return new Intent(order,payment,previous,false);
            if(!com.zhs.service.acq.report.ReportGenerationServiceImpl.paid(order,payment))throw new BusinessException("订单支付状态不支持退款");
            var refund=new PayRefund();refund.setRefundNo(FlowSecurity.hash("full-refund:"+no).substring(0,32));refund.setOrderNo(no);refund.setPayRecordId(payment.getId());refund.setRefundFen(order.getAmountFen());refund.setStatus(0);refunds.insert(refund);
            return new Intent(order,payment,refund,true);
        });
        if(intent==null)throw new BusinessException("退款登记失败");
        if(Objects.equals(intent.refund().getStatus(),1))return result(intent.refund());
        try{
            var evidence=intent.created()?adapter.refund(intent.order(),intent.payment().getTransactionId(),intent.refund().getRefundNo(),reason):adapter.queryRefund(intent.order(),intent.refund().getRefundNo());
            return transaction.execute(status->{
                var order=locks.lockOrder(no);var payment=locks.lockPayment(no);var refund=refunds.selectById(intent.refund().getId());
                if(refund==null||evidence.getAmount()==null||!Objects.equals(evidence.getOutTradeNo(),no)||!Objects.equals(evidence.getTransactionId(),payment.getTransactionId())||!Objects.equals(evidence.getOutRefundNo(),refund.getRefundNo())||!Objects.equals(evidence.getAmount().getTotal(),order.getAmountFen().longValue())||!Objects.equals(evidence.getAmount().getRefund(),refund.getRefundFen().longValue())||!"CNY".equals(evidence.getAmount().getCurrency()))throw new BusinessException("退款凭据与原支付不一致");
                if(Objects.equals(refund.getStatus(),1))return result(refund);
                refund.setWxRefundId(evidence.getRefundId());
                if(evidence.getStatus()==com.wechat.pay.java.service.refund.model.Status.SUCCESS){
                    if(evidence.getSuccessTime()==null||evidence.getRefundId()==null)throw new BusinessException("退款成功凭据不完整");
                    refund.setStatus(1);order.setPayStatus(2);payment.setPayStatus(3);orders.updateById(order);payments.updateById(payment);
                }else if(evidence.getStatus()==com.wechat.pay.java.service.refund.model.Status.CLOSED)refund.setStatus(2);
                else if(evidence.getStatus()==com.wechat.pay.java.service.refund.model.Status.ABNORMAL)refund.setStatus(3);
                refunds.updateById(refund);return result(refund);
            });
        }catch(RuntimeException e){throw new BusinessException("退款结果未确认，已保留原退款单；再次操作将查单，不重新发起退款");}
    }
    private Map<String,Object> result(PayRefund refund){return Map.of("refundNo",refund.getRefundNo(),"status",refund.getStatus(),"refundFen",refund.getRefundFen());}
}
