package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * landing_domain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("landing_domain")
public class LandingDomain extends AcqEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 落地页ID */
    private Long landingPageId;
    /** 域名ID */
    private Long domainId;
    /** 状态：0=停用 1=启用 */
    private Integer status;
}
