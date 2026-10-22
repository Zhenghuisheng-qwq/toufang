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
@ApiModel(value = "BannerConfigDetailResp")
public class BannerConfigDetailResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @ApiModelProperty(value = "Banner标题", required = true)
        private String title;

        @ApiModelProperty(value = "Banner图片URL", required = true)
        private String imageUrl;

        @ApiModelProperty(value = "跳转类型：0=不跳转 1=商品 2=分类 3=链接", required = true)
        private Integer jumpType;

        @ApiModelProperty(value = "跳转地址")
        private String jumpUrl;

        @ApiModelProperty(value = "展示位置：1=首页顶部 2=分类页", required = true)
        private Integer position;

        @ApiModelProperty(value = "状态：0=停用 1=启用", required = true)
        private Integer status;

        @ApiModelProperty(value = "排序值，越小越靠前", required = true)
        private Integer sort;

        @ApiModelProperty(value = "开始时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date startTime;

        @ApiModelProperty(value = "结束时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date endTime;

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
