package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * trade_report
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_report")
public class TradeReport extends AcqEntity implements Serializable {
    private String contentText;
    private String generationMode;
    private java.time.LocalDateTime generatedTime;
    @Override
    public String toString() {
        return "TradeReport[redacted]";
    }

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private String orderNo;
    /** 生成时使用的模板ID */
    private Long templateId;
    /** 生成时锁定的模板版本ID */
    private Long templateVersionId;
    /** 状态：0=待生成 1=已生成 2=生成失败 */
    private Integer status;
    /** 访问凭证哈希，不存明文凭证 */
    private String voucherHash;
    /** 最近一次失败说明 */
    private String failReason;
}
