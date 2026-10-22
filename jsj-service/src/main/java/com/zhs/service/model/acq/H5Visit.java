package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * h5_visit
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("h5_visit")
public class H5Visit extends AcqEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 访问号 */
    private String visitNo;
    /** 落地页ID */
    private Long landingPageId;
    /** 域名ID */
    private Long domainId;
    /** 原始访问域名 */
    private String domainName;
    /** 来源平台：0=未知 1=TENCENT 2=BAIDU 3=KUAISHOU */
    private Integer platform;
    /** 广告点击ID */
    private Long adClickId;
    /** 访问时间 */
    private LocalDateTime visitTime;
}
