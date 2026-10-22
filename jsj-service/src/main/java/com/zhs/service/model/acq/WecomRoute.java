package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * wecom_route
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wecom_route")
public class WecomRoute extends AcqEntity implements Serializable {
    private Integer entryType;
    private String linkId;
    private String linkUrl;
    private String capabilityNote;
    private String remark;

    @Serial
    private static final long serialVersionUID = 1L;

    /** 范围：1=全局当前 2=落地页指定 */
    private Integer scopeType;
    /** 落地页ID，全局路由为空 */
    private Long landingPageId;
    /** 企业行ID */
    private Long corpRowId;
    /** 老师行ID */
    private Long teacherRowId;
    /** 企业CorpID */
    private String corpId;
    /** 老师UserID */
    private String teacherUserid;
    /** 状态：0=停用 1=启用 */
    private Integer status;
    /** 当前启用占位：G=全局，L:{落地页ID}=落地页。停用必须为NULL */
    private String activeKey;
    /** 同范围排序 */
    private Integer sortNo;
}
