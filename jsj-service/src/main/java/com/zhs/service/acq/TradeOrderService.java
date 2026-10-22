package com.zhs.service.acq;

import com.zhs.service.model.acq.TradeOrder;

/**
 * 订单创建与支付状态推进。不提供改写商户、域名、企业、老师快照的方法。
 */
public interface TradeOrderService {

    TradeOrder create(TradeOrder order);

    boolean markPaid(String orderNo, java.time.LocalDateTime paidTime);
}
