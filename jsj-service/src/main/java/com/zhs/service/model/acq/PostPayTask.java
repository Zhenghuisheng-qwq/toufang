package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * post_pay_task
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("post_pay_task")
public class PostPayTask extends AcqEntity implements Serializable {
    private Long leaseVersion;

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private String orderNo;
    /** 任务类型：1=REPORT_GENERATE 2=WECOM_CONTACT_CREATE 3=AD_CONVERSION_REPORT */
    private Integer taskType;
    /** 任务状态：0=待执行 1=执行中 2=成功 3=失败待重试 4=结果未知 */
    private Integer status;
    /** 已执行次数 */
    private Integer attemptCount;
    /** 下次执行时间 */
    private LocalDateTime nextRunAt;
    /** 租约持有者 */
    private String leaseOwner;
    /** 租约截止时间 */
    private LocalDateTime leaseUntil;
    /** 最后错误 */
    private String lastError;
}
