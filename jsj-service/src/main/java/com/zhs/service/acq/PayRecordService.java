package com.zhs.service.acq;

import com.zhs.service.model.acq.PayRecord;

import java.time.LocalDateTime;

/**
 * 支付记录。成功写入 success_order_no，重复的成功通知保持一次。
 */
public interface PayRecordService {

    PayRecord savePending(PayRecord record);

    /**
     * @return true 表示本次把记录推进为成功；false 表示同一交易号的重复通知
     */
    boolean markSuccess(String orderNo, String transactionId, String notifyId, Integer amountFen,
                        String mchId, String appId, LocalDateTime successTime);
}
