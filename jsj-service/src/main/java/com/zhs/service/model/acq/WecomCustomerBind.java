package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * wecom_customer_bind
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wecom_customer_bind")
public class WecomCustomerBind extends AcqEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private String orderNo;
    /** 企业CorpID */
    private String corpId;
    /** 外部联系人ID */
    private String externalUserid;
    /** 老师UserID */
    private String teacherUserid;
    /** 关联方式：1=联系我state 2=UnionID */
    private Integer matchType;
    /** 命中的联系我state */
    private String stateToken;
    /** 获客入口ID */
    private Long contactEntryId;
}
