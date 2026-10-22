package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * ad_account
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ad_account")
public class AdAccount extends AcqEntity implements Serializable {
    private String interfaceMode;
    private String dataSourceId;
    private String payEvent;
    private String amountUnit;
    private String remark;

    @Serial
    private static final long serialVersionUID = 1L;

    /** 平台：1=TENCENT 2=BAIDU 3=KUAISHOU */
    private Integer platform;
    /** 平台广告账户ID */
    private String externalAccountId;
    /** 账户名称 */
    private String accountName;
    /** API授权引用，格式ref:，不存明文令牌 */
    private String credentialRef;
    /** 状态：0=停用 1=启用 */
    private Integer status;
}
