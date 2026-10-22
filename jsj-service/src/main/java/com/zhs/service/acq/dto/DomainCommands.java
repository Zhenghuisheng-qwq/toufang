package com.zhs.service.acq.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public final class DomainCommands {
    private DomainCommands() {
    }

    @Data
    public static class Query {
        private String domainName;
        private Integer status;
        private Long pageNum;
        private Long pageSize;
    }

    @Data
    public static class Save {
        private String domainName;
        private Integer status;
        private Integer reviewStatus;
        private Integer icpStatus;
        private Integer httpsStatus;
        private String remark;
    }

    @Data
    public static class Link {
        private Long landingPageId;
        private Long payMerchantId;
    }

    @Data
    public static class Row {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String domainName;
        private Integer status;
        private Integer reviewStatus;
        private Integer icpStatus;
        private Integer httpsStatus;
        private Integer verifyStatus;
        private String remark;
        private String verifyNote = "人工登记不等于已经通过平台核验。";
    }

    @Data
    public static class LandingLink {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long landingPageId;
        private String title;
        private Integer status;
    }

    @Data
    public static class MerchantLink {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long payMerchantId;
        private String mchName;
        private String mchId;
        private String appId;
        private Integer merchantStatus;
        private Integer status;
        private Integer verifyStatus;
        private boolean paymentReady;
        private String paymentNote = "该关联只是人工登记，不代表已经具备真实收款能力。";
    }

    @Data
    public static class Detail {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String domainName;
        private Integer status;
        private Integer reviewStatus;
        private Integer icpStatus;
        private Integer httpsStatus;
        private Integer verifyStatus;
        private String remark;
        private String verifyNote = "审核、备案、HTTPS 和支付授权均为人工登记，本阶段不调用外部平台核验。";
        private List<LandingLink> landings = new ArrayList<>();
        private List<MerchantLink> merchants = new ArrayList<>();
    }

    @Data
    public static class MerchantOption {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String mchName;
        private String mchId;
        private String appId;
        private Integer status;
        private boolean paymentReady;
        private String paymentNote = "列表中的商户只供配置关联，不代表已经具备真实收款能力。";
    }
}
