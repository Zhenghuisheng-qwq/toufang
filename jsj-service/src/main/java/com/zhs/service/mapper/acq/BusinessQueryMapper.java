package com.zhs.service.mapper.acq;
import org.apache.ibatis.annotations.*;
import java.util.*;
public interface BusinessQueryMapper {
    @Select("SELECT CAST(id AS CHAR) id,order_no orderNo,CAST(lead_id AS CHAR) leadId,domain_name domainName,amount_fen amountFen,pay_status payStatus,mch_id mchId,corp_id corpId,teacher_userid teacherUserid,create_time createTime FROM trade_order WHERE is_delete=0 ORDER BY id DESC LIMIT 30 OFFSET #{offset}") List<Map<String,Object>> orders(int offset);
    @Select("SELECT CAST(id AS CHAR) id,CAST(landing_page_id AS CHAR) pageId,domain_name domainName,platform,submit_time submitTime FROM h5_lead WHERE is_delete=0 ORDER BY id DESC LIMIT 30 OFFSET #{offset}") List<Map<String,Object>> leads(int offset);
    @Select("SELECT CAST(id AS CHAR) id,order_no orderNo,event_key eventKey,platform,event_type eventType,status,retry_count retryCount,last_error lastError,event_time eventTime FROM ad_conversion WHERE is_delete=0 ORDER BY id DESC LIMIT 30 OFFSET #{offset}") List<Map<String,Object>> events(int offset);
    @Select("SELECT COUNT(*) orders,COALESCE(SUM(pay_status=1),0) paidOrders,COALESCE(SUM(IF(pay_status=1,amount_fen,0)),0) paidAmountFen FROM trade_order WHERE is_delete=0") Map<String,Object> stats();
}
