package com.zhs.service.acq.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.AcquisitionCodes;
import com.zhs.service.acq.PayRecordService;
import com.zhs.service.acq.TradeOrderService;
import com.zhs.service.mapper.acq.PayRecordMapper;
import com.zhs.service.mapper.acq.TradeOrderMapper;
import com.zhs.service.model.acq.PayRecord;
import com.zhs.service.model.acq.TradeOrder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class PayRecordServiceImpl implements PayRecordService {

    @Resource
    private PayRecordMapper payRecordMapper;

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Resource
    private TradeOrderService tradeOrderService;
    @Resource private com.zhs.service.acq.flow.PaymentConfirmationService confirmations;

    @Override
    public PayRecord savePending(PayRecord record) {
        if (record == null || !StringUtils.hasText(record.getOrderNo()) || !StringUtils.hasText(record.getOutTradeNo())) {
            throw new BusinessException("支付记录缺少订单号");
        }
        record.setPayStatus(AcquisitionCodes.PAY_UNPAID);
        record.setSuccessOrderNo(null);
        record.setTransactionId(null);
        record.setNotifyId(null);
        payRecordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public boolean markSuccess(String orderNo, String transactionId, String notifyId, Integer amountFen,
                               String mchId, String appId, LocalDateTime successTime) {
        String digest = com.zhs.service.acq.flow.FlowSecurity.hash(String.join("|", orderNo, transactionId, notifyId,
                String.valueOf(amountFen), mchId, appId, String.valueOf(successTime)));
        return confirmations.confirm(new com.zhs.service.acq.flow.PaymentConfirmationService.Evidence(
                com.zhs.service.acq.flow.PaymentConfirmationService.Source.NOTIFY, notifyId, digest,
                orderNo, transactionId, mchId, appId, amountFen, "CNY", "SUCCESS", successTime));
    }
}
