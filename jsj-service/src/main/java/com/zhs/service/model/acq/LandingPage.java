package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * landing_page
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("landing_page")
public class LandingPage extends AcqEntity implements Serializable {

    private Long adAccountId;
    private Long payMerchantId;

    @Serial
    private static final long serialVersionUID = 1L;

    /** 页面标题 */
    private String title;
    /** 主宣传文案 */
    private String headline;
    /** 补充文案 */
    private String sellingText;
    /** 按钮文案 */
    private String buttonText;
    /** 展示价格，单位分 */
    private Integer priceFen;
    /** 发布状态：0=草稿 1=已发布 2=已下线 */
    private Integer publishStatus;
    /** 展示顺序，小的在前 */
    private Integer sortNo;
    /** 报告模板ID，算法未定时可空 */
    private Long reportTemplateId;
    /** 是否采集姓名：0=否 1=是 */
    private Integer nameEnabled;
    /** 是否采集性别：0=否 1=是 */
    private Integer genderEnabled;
    /** 是否采集出生日期：0=否 1=是 */
    private Integer birthDateEnabled;
    /** 是否采集出生时间：0=否 1=是 */
    private Integer birthTimeEnabled;
}
