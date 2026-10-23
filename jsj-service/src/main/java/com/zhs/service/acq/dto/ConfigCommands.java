package com.zhs.service.acq.dto;

import lombok.Data;

/** Explicit configuration commands. No IDs, audit fields or verification results may be bound. */
public final class ConfigCommands {
    private ConfigCommands() { }
    @Data
    public static class Query {
        private Long pageNum;
        private Long pageSize;
        private String keyword;
        private Integer status;
        private Integer platform;
        private Long corpRowId;
    }
    public record Merchant(String mchName, String mchId, String appId, String remark) { }
    public record MerchantVersion(String apiV3KeyRef, String privateKeyRef, String merchantSerialNo,
                                  String verifyMode, String platformCertRef, String platformKeyId,
                                  String platformPublicKeyRef, String remark) {
        @Override public String toString() { return "MerchantVersion[redacted]"; }
    }
    public record Corp(String corpName, String corpId, String sid, String authMode, String remark) { }
    public record CorpSecrets(String contactSecretRef, String tokenRef, String encodingAesKeyRef) {
        @Override public String toString() { return "CorpSecrets[redacted]"; }
    }
    public record Teacher(Long corpRowId, String teacherUserid, String teacherName) { }
    public record Tag(Long corpRowId, String tagId, String tagName, String groupId, String groupName) { }
    public record Route(Integer scopeType, Long landingPageId, Long corpRowId, Long teacherRowId,
                        Integer entryType, String linkId, String linkUrl, String capabilityNote,
                        Integer sortNo, String remark) { }
    public record AdAccount(Integer platform, String externalAccountId, String accountName,
                            String interfaceMode, String dataSourceId, String payEvent,
                            String amountUnit, String remark, String submitEvent, String addContactEvent) {
        public AdAccount(Integer platform,String externalAccountId,String accountName,String interfaceMode,String dataSourceId,String payEvent,String amountUnit,String remark){this(platform,externalAccountId,accountName,interfaceMode,dataSourceId,payEvent,amountUnit,remark,null,null);}
    }
    public record AdSecret(String credentialRef) {
        @Override public String toString() { return "AdSecret[redacted]"; }
    }
}
