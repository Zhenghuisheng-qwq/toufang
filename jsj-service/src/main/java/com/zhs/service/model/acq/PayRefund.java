package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * pay_refund
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pay_refund")
public class PayRefund extends AcqEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 商户退款单号 */
    private String refundNo;
    /** 原订单号 */
    private String orderNo;
    /** 原支付记录ID */
    private Long payRecordId;
    /** 退款金额，单位分 */
    private Integer refundFen;
    /** 状态：0=处理中 1=成功 2=关闭 3=异常 */
    private Integer status;
    /** 微信退款单号 */
    private String wxRefundId;
}
