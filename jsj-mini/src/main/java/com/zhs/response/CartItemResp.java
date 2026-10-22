package com.zhs.response;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 *
 * @author 
 * @since 2026-08-09
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel(value = "CartItemResp")
public class CartItemResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "用户ID（关联 user.id）")
        private Long userId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "仓库ID（关联 warehouse.id）")
        private Long warehouseId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "SPU ID（冗余，方便查询）")
        private Long spuId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "SKU ID")
        private Long skuId;

        @ApiModelProperty(value = "购买数量", required = true)
        private Integer quantity;

        @ApiModelProperty(value = "是否选中：0=未选中 1=已选中", required = true)
        private Integer selected;

        @ApiModelProperty(value = "删除标志：0=未删除 1=已删除", required = true)
        private Integer isDelete;

        @ApiModelProperty(value = "创建时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @ApiModelProperty(value = "更新时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
