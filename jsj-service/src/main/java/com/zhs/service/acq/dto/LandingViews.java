package com.zhs.service.acq.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public final class LandingViews {
    private LandingViews() {
    }

    @Data
    public static class Row {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String title;
        private String headline;
        private String buttonText;
        private String priceYuan;
        private Integer priceFen;
        private Integer publishStatus;
        private Integer sortNo;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long reportTemplateId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long adAccountId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long payMerchantId;
        private Integer nameEnabled;
        private Integer genderEnabled;
        private Integer birthDateEnabled;
        private Integer birthTimeEnabled;
    }

    @Data
    public static class Asset {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String assetUrl;
        private Integer sortNo;
        private Integer status;
    }

    @Data
    public static class Detail {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String title;
        private String headline;
        private String sellingText;
        private String buttonText;
        private String priceYuan;
        private Integer priceFen;
        private Integer publishStatus;
        private Integer sortNo;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long reportTemplateId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long adAccountId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long payMerchantId;
        private Integer nameEnabled;
        private Integer genderEnabled;
        private Integer birthDateEnabled;
        private Integer birthTimeEnabled;
        private List<Asset> assets = new ArrayList<>();
        private String previewNote = "这是后台配置预览，不会生成订单、付费报告，也不会调用企业微信。";
    }

    @Data
    public static class Option {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String title;
    }
}
