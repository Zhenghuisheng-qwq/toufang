package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * pay_merchant
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pay_merchant")
public class PayMerchant extends AcqEntity implements Serializable {
    private String remark;
    private Long currentVersionId;

    @Serial
    private static final long serialVersionUID = 1L;

    /** 微信支付商户号 */
    private String mchId;
    /** 关联AppID */
    private String appId;
    /** 商户名称 */
    private String mchName;
    /** 状态：0=停用 1=启用 */
    private Integer status;
}
