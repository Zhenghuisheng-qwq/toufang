package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * report_template_version
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report_template_version")
public class ReportTemplateVersion extends AcqEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 报告模板ID */
    private Long templateId;
    /** 版本号，从1递增 */
    private Integer versionNo;
    /** 状态：0=草稿 1=已发布 2=停用 */
    private Integer status;
    /** 版本说明，不写入未确认的测试规则 */
    private String changeNote;
    /** 正文类型：1=纯文本 */
    private Integer contentType;
    /** 模板正文。开发配置内容，不是正式算法输出 */
    private String contentText;
    /** 发布时间 */
    private LocalDateTime publishedTime;
}
