package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * wecom_corp
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wecom_corp")
public class WecomCorp extends AcqEntity implements Serializable {
    private String sid;
    private String authMode;
    private String remark;

    @Serial
    private static final long serialVersionUID = 1L;

    /** 企业微信CorpID */
    private String corpId;
    /** 企业名称 */
    private String corpName;
    /** 状态：0=停用 1=启用 */
    private Integer status;
    /** 回调Token引用，格式ref: */
    private String tokenRef;
    /** EncodingAESKey引用，格式ref: */
    private String encodingAesKeyRef;
    /** 客户联系Secret引用，格式ref: */
    private String contactSecretRef;
}
