package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * wecom_tag
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wecom_tag")
public class WecomTag extends AcqEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 企业CorpID */
    private String corpId;
    /** 企业标签ID */
    private String tagId;
    /** 标签名称 */
    private String tagName;
    /** 标签组ID */
    private String groupId;
    /** 标签组名称 */
    private String groupName;
    /** 状态：0=停用 1=启用 */
    private Integer status;
}
