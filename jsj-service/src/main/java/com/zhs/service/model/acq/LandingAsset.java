package com.zhs.service.model.acq;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * landing_asset
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("landing_asset")
public class LandingAsset extends AcqEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 落地页ID */
    private Long landingPageId;
    /** 素材类型：1=图片 */
    private Integer assetType;
    /** 素材地址 */
    private String assetUrl;
    /** 展示顺序，小的在前 */
    private Integer sortNo;
    /** 状态：0=停用 1=启用 */
    private Integer status;
}
