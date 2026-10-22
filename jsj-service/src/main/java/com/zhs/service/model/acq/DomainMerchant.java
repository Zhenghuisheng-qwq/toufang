package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * domain_merchant
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("domain_merchant")
public class DomainMerchant extends AcqEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 域名ID */
    private Long domainId;
    /** 支付商户ID */
    private Long payMerchantId;
    /** 人工授权登记：0=停用 1=已登记。不等于平台核验通过 */
    private Integer status;
    /** 支付授权核验：0=仅人工登记 1=已通过平台核验 */
    private Integer verifyStatus;
}
