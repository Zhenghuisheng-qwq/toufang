package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * pay_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pay_record")
public class PayRecord extends AcqEntity implements Serializable {
    @Override public String toString(){return "PayRecord[redacted]";}

    private String confirmationSource;
    private String evidenceHash;
    private String paymentUrl;
    private String attemptState;
    private String attemptOwner;
    private LocalDateTime attemptUntil;

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private String orderNo;
    /** 商户订单号，首期与订单号相同 */
    private String outTradeNo;
    /** 微信支付订单号 */
    private String transactionId;
    /** 支付成功时写入订单号；为空表示尚未成功。用于保证一笔订单一条成功支付 */
    private String successOrderNo;
    /** 微信通知ID，重复通知靠唯一约束拦截 */
    private String notifyId;
    /** 商户号，须与订单快照一致 */
    private String mchId;
    /** AppID，须与订单快照一致 */
    private String appId;
    /** 支付金额，单位分 */
    private Integer amountFen;
    /** 支付类型：1=H5 2=JSAPI */
    private Integer tradeType;
    /** 支付状态：0=待支付 1=支付成功 2=已关闭 3=转入退款 */
    private Integer payStatus;
    /** 微信原始交易状态 */
    private String wxTradeState;
    /** 预支付标识 */
    private String prepayId;
    /** JSAPI付款人openid */
    private String payerOpenid;
    /** 支付成功时间 */
    private LocalDateTime successTime;
}
