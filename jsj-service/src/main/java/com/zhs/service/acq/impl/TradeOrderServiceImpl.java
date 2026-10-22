package com.zhs.service.acq.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.AcquisitionCodes;
import com.zhs.service.acq.TradeOrderService;
import com.zhs.service.mapper.acq.DomainMerchantMapper;
import com.zhs.service.mapper.acq.TradeOrderMapper;
import com.zhs.service.model.acq.DomainMerchant;
import com.zhs.service.model.acq.TradeOrder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class TradeOrderServiceImpl implements TradeOrderService {

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Resource
    private DomainMerchantMapper domainMerchantMapper;
    @Resource private com.zhs.service.acq.report.OrderReportSnapshot reportSnapshot;

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public TradeOrder create(TradeOrder order) {
        if (order == null || !StringUtils.hasText(order.getOrderNo())) {
            throw new BusinessException("订单号不能为空");
        }
        if (order.getDomainId() == null || order.getPayMerchantId() == null) {
            throw new BusinessException("订单必须锁定域名和商户");
        }
        if (order.getMerchantVersionId() == null || order.getMerchantVersionNo() == null
                || !StringUtils.hasText(order.getMchId()) || !StringUtils.hasText(order.getAppId())) {
            throw new BusinessException("订单必须锁定商户配置版本");
        }
        if (order.getAmountFen() == null || order.getAmountFen() < 1) {
            throw new BusinessException("订单金额必须大于0");
        }
        if (order.getTradeType() == null
                || (order.getTradeType() != AcquisitionCodes.TRADE_H5 && order.getTradeType() != AcquisitionCodes.TRADE_JSAPI)) {
            throw new BusinessException("支付类型只支持H5或JSAPI");
        }
        DomainMerchant authorized = domainMerchantMapper.selectOne(new LambdaQueryWrapper<DomainMerchant>()
                .eq(DomainMerchant::getDomainId, order.getDomainId())
                .eq(DomainMerchant::getPayMerchantId, order.getPayMerchantId())
                .last("LIMIT 1"));
        if (authorized == null || authorized.getStatus() == null
                || authorized.getStatus() != AcquisitionCodes.DOMAIN_MERCHANT_ENABLED) {
            throw new BusinessException("该域名未授权使用此支付商户");
        }
        if (authorized.getVerifyStatus() == null
                || authorized.getVerifyStatus() != AcquisitionCodes.DOMAIN_MERCHANT_VERIFIED) {
            throw new BusinessException("该域名的支付商户尚未完成核验，不能用于下单");
        }
        order.setPayStatus(AcquisitionCodes.PAY_UNPAID);
        order.setPaidTime(null);
        order.setId(null);
        order.setIsDelete(0);
        reportSnapshot.freeze(order);
        tradeOrderMapper.insert(order);
        return order;
    }

    @Override
    public boolean markPaid(String orderNo, LocalDateTime paidTime) {
        if (!StringUtils.hasText(orderNo) || paidTime == null) {
            throw new BusinessException("支付成功缺少订单号或时间");
        }
        int rows = tradeOrderMapper.update(null, new LambdaUpdateWrapper<TradeOrder>()
                .set(TradeOrder::getPayStatus, AcquisitionCodes.PAY_SUCCESS)
                .set(TradeOrder::getPaidTime, paidTime)
                .eq(TradeOrder::getOrderNo, orderNo)
                .eq(TradeOrder::getPayStatus, AcquisitionCodes.PAY_UNPAID));
        return rows == 1;
    }
}
