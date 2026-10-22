package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * wecom_contact_event
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wecom_contact_event")
public class WecomContactEvent extends AcqEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 回调幂等键 */
    private String dedupKey;
    /** 企业CorpID */
    private String corpId;
    /** 变更类型：1=添加好友 2=删除好友 */
    private Integer changeType;
    /** 外部联系人ID */
    private String externalUserid;
    /** 老师UserID */
    private String teacherUserid;
    /** 回调中的state */
    private String stateToken;
    /** 匹配到的订单号 */
    private String orderNo;
    /** 回调事件时间 */
    private LocalDateTime eventTime;
    /** 备注执行：0=待处理 1=成功 2=跳过 3=失败 */
    private Integer remarkStatus;
    /** 打标签执行：0=待处理 1=成功 2=跳过 3=失败 */
    private Integer tagStatus;
    /** 最近一次失败说明 */
    private String lastError;
}
