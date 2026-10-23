package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ad_conversion
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ad_conversion")
public class AdConversion extends AcqEntity implements Serializable {

    private String eventKey;
    private String sourceType;
    private Long sourceId;
    private LocalDateTime eventTime;
    private String deliverySnapshot;
    private LocalDateTime nextRunAt;
    private String leaseOwner;
    private LocalDateTime leaseUntil;
    private Long leaseVersion;

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private String orderNo;
    /** 平台：1=TENCENT 2=BAIDU 3=KUAISHOU */
    private Integer platform;
    /** 转化类型：1=支付 */
    private Integer eventType;
    /** 广告账户ID */
    private Long adAccountId;
    /** 广告点击ID */
    private Long adClickId;
    /** 转化金额，单位分 */
    private Integer amountFen;
    /** 回传状态：0=待回传 1=已接收 2=跳过 3=失败 4=结果未知 */
    private Integer status;
    /** 已尝试次数 */
    private Integer retryCount;
    /** 最近一次请求时间 */
    private LocalDateTime requestTime;
    /** 平台响应码 */
    private String responseCode;
    /** 平台响应摘要 */
    private String responseBody;
    /** 最近一次失败说明 */
    private String lastError;
}
