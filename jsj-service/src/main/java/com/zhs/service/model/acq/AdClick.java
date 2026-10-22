package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ad_click
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ad_click")
public class AdClick extends AcqEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 平台：1=TENCENT 2=BAIDU 3=KUAISHOU */
    private Integer platform;
    /** 广告账户ID */
    private Long adAccountId;
    /** 落地页ID */
    private Long landingPageId;
    /** 访问域名ID */
    private Long domainId;
    /** 访问域名 */
    private String domainName;
    /** 访问记录ID */
    private Long visitId;
    /** 线索ID */
    private Long leadId;
    /** 随后绑定的订单号 */
    private String orderNo;
    /** 腾讯点击标识 */
    private String tencentClickId;
    /** 腾讯曝光标识 */
    private String tencentImpressionId;
    /** 腾讯请求标识 */
    private String tencentRequestId;
    /** 百度bd_vid */
    private String baiduBdVid;
    /** 百度logidUrl */
    private String baiduLogidUrl;
    /** 快手callback */
    private String kuaishouCallback;
    /** 原始查询串，容纳尚未确认的点击参数 */
    private String rawQuery;
    /** 平台加主点击标识。无稳定标识时为空 */
    private String clickDedupKey;
    /** 点击进入时间 */
    private LocalDateTime clickTime;
}
