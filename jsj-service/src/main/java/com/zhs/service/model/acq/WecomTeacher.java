package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * wecom_teacher
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wecom_teacher")
public class WecomTeacher extends AcqEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 企业行ID */
    private Long corpRowId;
    /** 企业CorpID */
    private String corpId;
    /** 老师UserID */
    private String teacherUserid;
    /** 老师名称 */
    private String teacherName;
    /** 状态：0=停用 1=启用 */
    private Integer status;
}
