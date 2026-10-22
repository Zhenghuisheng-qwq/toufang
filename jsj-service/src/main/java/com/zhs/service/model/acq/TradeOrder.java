package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * trade_order
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_order")
public class TradeOrder extends AcqEntity implements Serializable {
    private Long reportTemplateId;
    private Long reportTemplateVersionId;
    private Integer reportInputVersion;
    private String reportName;
    private Integer reportGender;
    private java.time.LocalDate reportBirthDate;
    private java.time.LocalTime reportBirthTime;

    @Override public String toString() { return "TradeOrder[redacted]"; }

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private String orderNo;
    /** 线索ID */
    private Long leadId;
    /** 访问记录ID */
    private Long visitId;
    /** 下单时落地页ID */
    private Long landingPageId;
    /** 下单时锁定的广告点击ID */
    private Long adClickId;
    /** 下单时访问域名ID */
    private Long domainId;
    /** 下单时访问域名快照 */
    private String domainName;
    /** 订单金额，单位分 */
    private Integer amountFen;
    /** 支付类型：1=H5 2=JSAPI */
    private Integer tradeType;
    /** 支付状态：0=待支付 1=已支付 2=已退款 3=已关闭 */
    private Integer payStatus;
    /** 锁定的商户ID */
    private Long payMerchantId;
    /** 锁定的商户配置版本ID */
    private Long merchantVersionId;
    /** 锁定的商户配置版本号 */
    private Integer merchantVersionNo;
    /** 锁定的商户号快照 */
    private String mchId;
    /** 锁定的AppID快照 */
    private String appId;
    /** 下单时命中的企微路由ID */
    private Long routeId;
    /** 下单时锁定的企业行ID */
    private Long corpRowId;
    /** 下单时锁定的老师行ID */
    private Long teacherRowId;
    /** 下单时锁定的企业CorpID快照 */
    private String corpId;
    /** 下单时锁定的老师UserID快照 */
    private String teacherUserid;
    /** 支付成功时间 */
    private LocalDateTime paidTime;
    /** 关闭时间 */
    private LocalDateTime closeTime;
}
