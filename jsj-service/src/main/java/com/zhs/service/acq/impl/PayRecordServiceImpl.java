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
    @Resource private com.zhs.service.mapper.acq.ReportWorkMapper reportWork;

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
        if (!StringUtils.hasText(transactionId) || !StringUtils.hasText(notifyId) || successTime == null) {
            throw new BusinessException("支付通知缺少交易号、通知号或成功时间");
        }
        TradeOrder order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getMchId().equals(mchId) || !order.getAppId().equals(appId) || !order.getAmountFen().equals(amountFen)) {
            throw new BusinessException("支付通知与订单锁定的商户、应用或金额不一致");
        }
        int rows = payRecordMapper.update(null, new LambdaUpdateWrapper<PayRecord>()
                .set(PayRecord::getPayStatus, AcquisitionCodes.PAY_SUCCESS)
                .set(PayRecord::getSuccessOrderNo, orderNo)
                .set(PayRecord::getTransactionId, transactionId)
                .set(PayRecord::getNotifyId, notifyId)
                .set(PayRecord::getWxTradeState, "SUCCESS")
                .set(PayRecord::getSuccessTime, successTime)
                .eq(PayRecord::getOrderNo, orderNo)
                .eq(PayRecord::getPayStatus, AcquisitionCodes.PAY_UNPAID)
                .isNull(PayRecord::getSuccessOrderNo));
        if (rows == 1) {
            if (!tradeOrderService.markPaid(orderNo, successTime)) {
                throw new BusinessException("订单支付状态未更新");
            }
            reportWork.ensureReport(orderNo);
            return true;
        }
        PayRecord existing = payRecordMapper.selectOne(new LambdaQueryWrapper<PayRecord>()
                .eq(PayRecord::getOrderNo, orderNo));
        if (existing != null
                && Integer.valueOf(AcquisitionCodes.PAY_SUCCESS).equals(existing.getPayStatus())
                && transactionId.equals(existing.getTransactionId())) {
            return false;
        }
        throw new BusinessException("支付记录与已有成功通知冲突");
    }
}
