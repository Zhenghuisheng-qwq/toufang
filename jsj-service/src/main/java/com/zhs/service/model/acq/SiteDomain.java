package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * site_domain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("site_domain")
public class SiteDomain extends AcqEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 域名，不含协议和路径 */
    private String domainName;
    /** 状态：0=停用 1=启用 */
    private Integer status;
    /** 人工审核登记：0=未登记 1=已登记通过 2=已登记未通过 */
    private Integer reviewStatus;
    /** 备案登记：0=未登记 1=已登记备案 2=已登记未备案 */
    private Integer icpStatus;
    /** HTTPS登记：0=未登记 1=已登记启用 2=已登记未启用 */
    private Integer httpsStatus;
    /** 实际核验：0=未验证 1=已验证。人工登记不等于核验 */
    private Integer verifyStatus;
    /** 备注 */
    private String remark;
}
