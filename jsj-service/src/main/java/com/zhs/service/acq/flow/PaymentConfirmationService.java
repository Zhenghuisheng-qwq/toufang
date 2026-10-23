package com.zhs.service.acq.flow;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.TradeOrderService;
import com.zhs.service.mapper.acq.BusinessFlowMapper;
import com.zhs.service.mapper.acq.PayRecordMapper;
import com.zhs.service.mapper.acq.ReportWorkMapper;
import com.zhs.service.model.acq.PayRecord;
import com.zhs.service.model.acq.TradeOrder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/** Accept only evidence already authenticated by the platform adapter; never expose as an HTTP command. */
@Service
public class PaymentConfirmationService {

    public enum Source {
        NOTIFY, QUERY
    }

    public record Evidence(Source source, String evidenceId, String digest, String orderNo, String transactionId,
                           String mchId, String appId, Integer amountFen, String currency, String state,
                           LocalDateTime paidAt) {
        @Override
        public String toString() {
            return "PaymentEvidence[redacted]";
        }
    }

    @Resource
    private ReportWorkMapper locks;

    @Resource
    private BusinessFlowMapper flow;

    @Resource
    private PayRecordMapper payments;

    @Resource
    private TradeOrderService orders;

    @Resource
    private AdEventService events;

    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public boolean confirm(Evidence evidence) {
        if (evidence == null || evidence.source() == null || blank(evidence.evidenceId()) || evidence.evidenceId().length() > 128
                || blank(evidence.transactionId()) || evidence.transactionId().length() > 64
                || evidence.digest() == null || !evidence.digest().matches("[a-f0-9]{64}")
                || evidence.paidAt() == null || !"CNY".equals(evidence.currency()) || !"SUCCESS".equals(evidence.state())) {
            throw new BusinessException("支付凭据不完整");
        }
        TradeOrder order = locks.lockOrder(evidence.orderNo());
        PayRecord pay = locks.lockPayment(evidence.orderNo());
        if (order == null || pay == null || !Objects.equals(order.getMchId(), evidence.mchId())
                || !Objects.equals(order.getAppId(), evidence.appId()) || !Objects.equals(order.getAmountFen(), evidence.amountFen())
                || !Objects.equals(pay.getMchId(), evidence.mchId()) || !Objects.equals(pay.getAppId(), evidence.appId())
                || !Objects.equals(pay.getAmountFen(), evidence.amountFen())) {
            throw new BusinessException("支付凭据与订单快照不一致");
        }
        if (evidence.paidAt().isAfter(locks.now().plusMinutes(5))) {
            throw new BusinessException("支付成功时间不正确");
        }
        boolean duplicate = Objects.equals(pay.getPayStatus(), 1) && Objects.equals(order.getPayStatus(), 1);
        boolean refunded = Objects.equals(pay.getPayStatus(), 3) && Objects.equals(order.getPayStatus(), 2)
                && Objects.equals(pay.getSuccessOrderNo(), evidence.orderNo());
        if ((duplicate || refunded) && !Objects.equals(pay.getTransactionId(), evidence.transactionId())) {
            throw new BusinessException("支付交易号冲突");
        }
        if (!duplicate && !refunded && (!Objects.equals(pay.getPayStatus(), 0) || !Objects.equals(order.getPayStatus(), 0))) {
            throw new BusinessException("订单已关闭或进入退款流程");
        }
        flow.evidence(evidence.orderNo(), evidence.source().name(), evidence.evidenceId(), evidence.digest(), evidence.transactionId());
        Map<String, Object> previous = flow.evidenceRow(evidence.source().name(), evidence.evidenceId());
        if (previous == null || !Objects.equals(previous.get("orderNo"), evidence.orderNo())
                || !Objects.equals(previous.get("transactionId"), evidence.transactionId())
                || !Objects.equals(previous.get("evidenceHash"), evidence.digest())) {
            throw new BusinessException("支付凭据冲突");
        }
        if (refunded) {
            return false;
        }
        if (!duplicate) {
            pay.setPayStatus(1);
            pay.setSuccessOrderNo(evidence.orderNo());
            pay.setTransactionId(evidence.transactionId());
            pay.setWxTradeState("SUCCESS");
            pay.setSuccessTime(evidence.paidAt());
            pay.setNotifyId(evidence.source() == Source.NOTIFY ? evidence.evidenceId() : null);
            pay.setConfirmationSource(evidence.source().name());
            pay.setEvidenceHash(evidence.digest());
            if (payments.updateById(pay) != 1 || !orders.markPaid(evidence.orderNo(), evidence.paidAt())) {
                throw new BusinessException("支付入账失败");
            }
        }
        for (int type = 1; type <= 3; type++) {
            flow.task(evidence.orderNo(), type);
        }
        events.register(AdEventService.PAY, "ORDER", order.getId(), order.getOrderNo(), order.getAdClickId(), order.getAmountFen());
        return !duplicate;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
