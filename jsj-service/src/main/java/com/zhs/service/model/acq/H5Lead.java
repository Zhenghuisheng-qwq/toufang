package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * h5_lead
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("h5_lead")
public class H5Lead extends AcqEntity implements Serializable {
    @Override public String toString(){return "H5Lead[redacted]";}

    private Long sessionId;

    @Serial
    private static final long serialVersionUID = 1L;

    /** 访问记录ID */
    private Long visitId;
    /** 落地页ID */
    private Long landingPageId;
    /** 域名ID */
    private Long domainId;
    /** 提交时的原始访问域名 */
    private String domainName;
    /** 来源平台：0=未知 1=TENCENT 2=BAIDU 3=KUAISHOU */
    private Integer platform;
    /** 广告点击ID */
    private Long adClickId;
    /** 姓名 */
    private String realName;
    /** 性别：0=未知 1=男 2=女 */
    private Integer gender;
    /** 出生日期 */
    private LocalDate birthDate;
    /** 出生时间，未采集时为空 */
    private LocalTime birthTime;
    /** 用户提交时间 */
    private LocalDateTime submitTime;
}
