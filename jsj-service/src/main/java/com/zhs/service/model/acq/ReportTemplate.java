package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * report_template
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report_template")
public class ReportTemplate extends AcqEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 模板名称 */
    private String templateName;
    /** 状态：0=草稿 1=启用 2=停用 */
    private Integer status;
    /** 当前已发布版本ID，空表示尚未发布 */
    private Long currentVersionId;
    /** 说明，不包含未确认的计分规则 */
    private String remark;
}
