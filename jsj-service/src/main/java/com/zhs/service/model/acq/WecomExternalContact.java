package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * wecom_external_contact
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wecom_external_contact")
public class WecomExternalContact extends AcqEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 企业CorpID */
    private String corpId;
    /** 外部联系人ID，只在本企业内有效 */
    private String externalUserid;
    /** 微信UnionID，老客户匹配时使用 */
    private String unionId;
    /** 外部联系人名称 */
    private String contactName;
    /** 状态：0=已删除 1=跟进中 */
    private Integer status;
}
