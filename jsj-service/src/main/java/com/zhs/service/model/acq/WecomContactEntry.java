package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * wecom_contact_entry
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wecom_contact_entry")
public class WecomContactEntry extends AcqEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 入口类型：1=联系我二维码 2=获客助手链接 */
    private Integer entryType;
    /** 订单号，一订单一入口 */
    private String orderNo;
    /** 企业行ID */
    private Long corpRowId;
    /** 企业CorpID快照 */
    private String corpId;
    /** 老师UserID快照 */
    private String teacherUserid;
    /** 联系我state，仅类型1使用，最长30 */
    private String stateToken;
    /** 联系我配置ID，仅类型1 */
    private String configId;
    /** 联系我二维码地址，仅类型1 */
    private String qrCodeUrl;
    /** 获客助手链接ID，仅类型2 */
    private String linkId;
    /** 获客助手链接地址，仅类型2 */
    private String linkUrl;
    /** 获客助手自定义渠道参数，仅类型2 */
    private String customerChannel;
    /** 状态：0=待创建 1=已创建 2=创建失败 3=已失效 */
    private Integer status;
}
