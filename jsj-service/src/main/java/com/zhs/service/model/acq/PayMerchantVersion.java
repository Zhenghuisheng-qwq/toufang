package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * pay_merchant_version
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pay_merchant_version")
public class PayMerchantVersion extends AcqEntity implements Serializable {
    private String merchantSerialNo;
    private String verifyMode;
    private String platformKeyId;
    private String platformPublicKeyRef;

    @Serial
    private static final long serialVersionUID = 1L;

    /** 支付商户ID */
    private Long payMerchantId;
    /** 配置版本号，从1递增 */
    private Integer versionNo;
    /** 状态：0=草稿 1=启用 2=停用 */
    private Integer status;
    /** APIv3密钥引用，格式ref:，不存明文 */
    private String apiV3KeyRef;
    /** 商户私钥引用，格式ref:，不存明文 */
    private String privateKeyRef;
    /** 平台证书或公钥引用，格式ref:，不存明文 */
    private String platformCertRef;
    /** 版本说明 */
    private String remark;
}
