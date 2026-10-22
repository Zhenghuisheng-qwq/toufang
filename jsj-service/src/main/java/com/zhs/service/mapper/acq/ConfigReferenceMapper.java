package com.zhs.service.mapper.acq;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/** Counts include historical rows: logical deletion does not erase identity ownership. */
public interface ConfigReferenceMapper {
    @Select("SELECT (SELECT COUNT(*) FROM pay_merchant_version WHERE pay_merchant_id=#{id}) + (SELECT COUNT(*) FROM domain_merchant WHERE pay_merchant_id=#{id}) + (SELECT COUNT(*) FROM trade_order WHERE pay_merchant_id=#{id})")
    long merchant(@Param("id") Long id);
    @Select("SELECT COUNT(*) FROM trade_order WHERE merchant_version_id=#{id}")
    long version(@Param("id") Long id);
    @Select("SELECT IFNULL(MAX(version_no),0) FROM pay_merchant_version WHERE pay_merchant_id=#{id}")
    int maxVersion(@Param("id") Long id);
    @Select("SELECT (SELECT COUNT(*) FROM wecom_teacher WHERE corp_row_id=#{id}) + (SELECT COUNT(*) FROM wecom_tag WHERE corp_id=#{corpId}) + (SELECT COUNT(*) FROM wecom_route WHERE corp_row_id=#{id}) + (SELECT COUNT(*) FROM trade_order WHERE corp_row_id=#{id} OR corp_id=#{corpId}) + (SELECT COUNT(*) FROM wecom_contact_entry WHERE corp_id=#{corpId}) + (SELECT COUNT(*) FROM wecom_customer_bind WHERE corp_id=#{corpId}) + (SELECT COUNT(*) FROM wecom_external_contact WHERE corp_id=#{corpId}) + (SELECT COUNT(*) FROM wecom_contact_event WHERE corp_id=#{corpId})")
    long corp(@Param("id") Long id, @Param("corpId") String corpId);
    @Select("SELECT (SELECT COUNT(*) FROM wecom_route WHERE teacher_row_id=#{id}) + (SELECT COUNT(*) FROM trade_order WHERE teacher_row_id=#{id}) + (SELECT COUNT(*) FROM wecom_contact_entry WHERE corp_id=#{corpId} AND teacher_userid=#{userid}) + (SELECT COUNT(*) FROM wecom_customer_bind WHERE corp_id=#{corpId} AND teacher_userid=#{userid})")
    long teacher(@Param("id") Long id, @Param("corpId") String corpId, @Param("userid") String userid);
    @Select("SELECT COUNT(*) FROM trade_order WHERE route_id=#{id}")
    long route(@Param("id") Long id);
    @Select("SELECT (SELECT COUNT(*) FROM ad_click WHERE ad_account_id=#{id}) + (SELECT COUNT(*) FROM ad_conversion WHERE ad_account_id=#{id})")
    long ad(@Param("id") Long id);
    @Select("SELECT id FROM wecom_route ORDER BY id FOR UPDATE")
    List<Long> lockRoutes();
}
