package com.zhs.service.mapper.acq;

import com.zhs.service.model.acq.*;
import org.apache.ibatis.annotations.*;
import java.util.*;

/** All statements use the existing acquisition SqlSessionFactory and transaction manager. */
public interface BusinessFlowMapper {
    @Insert("INSERT INTO acq_oauth_state(state_hash,session_id,order_no,app_id,expires_at) VALUES(#{hash},#{sid},#{no},#{app},DATE_ADD(NOW(),INTERVAL 10 MINUTE))")
    int oauth(@Param("hash") String hash,@Param("sid") Long sid,@Param("no") String no,@Param("app") String app);
    @Update("UPDATE acq_oauth_state SET consumed=1 WHERE state_hash=#{hash} AND session_id=#{sid} AND order_no=#{no} AND app_id=#{app} AND consumed=0 AND expires_at>NOW() AND is_delete=0")
    int consumeOauth(@Param("hash") String hash,@Param("sid") Long sid,@Param("no") String no,@Param("app") String app);
    @Select("SELECT order_no FROM acq_oauth_state WHERE state_hash=#{hash} AND consumed=0 AND expires_at>NOW() AND is_delete=0") String oauthOrder(String hash);
    @Update("UPDATE pay_record SET payer_openid=#{openid} WHERE order_no=#{no} AND app_id=#{app} AND pay_status=0 AND is_delete=0 AND (payer_openid IS NULL OR payer_openid=#{openid})") int openid(@Param("no") String no,@Param("app") String app,@Param("openid") String openid);
    @Update("UPDATE pay_record SET attempt_state='READY',prepay_id=#{prepay},attempt_owner=NULL,attempt_until=NULL WHERE order_no=#{no} AND attempt_owner=#{owner} AND attempt_state='IN_FLIGHT' AND pay_status=0 AND is_delete=0") int jsapiFinish(@Param("no") String no,@Param("owner") String owner,@Param("prepay") String prepay);
    @Update("UPDATE pay_record SET attempt_state='IN_FLIGHT',attempt_owner=#{owner},attempt_until=DATE_ADD(NOW(),INTERVAL 60 SECOND) WHERE order_no=#{no} AND pay_status=0 AND is_delete=0 AND (attempt_state='NEW' OR attempt_state IS NULL)")
    int paymentClaim(@Param("no") String no,@Param("owner") String owner);
    @Update("UPDATE pay_record SET attempt_state=#{state},payment_url=#{url},attempt_owner=NULL,attempt_until=NULL WHERE order_no=#{no} AND attempt_owner=#{owner} AND attempt_state='IN_FLIGHT' AND pay_status=0 AND is_delete=0")
    int paymentFinish(@Param("no") String no,@Param("owner") String owner,@Param("state") String state,@Param("url") String url);
    @Insert("INSERT INTO acq_h5_session(token_hash,domain_name,expires_at) VALUES(#{hash},#{host},DATE_ADD(NOW(),INTERVAL 30 DAY))")
    int session(@Param("hash") String hash,@Param("host") String host);
    @Select("SELECT id FROM acq_h5_session WHERE token_hash=#{hash} AND domain_name=#{host} AND revoked=0 AND is_delete=0 AND expires_at>NOW() FOR UPDATE")
    Long lockSession(@Param("hash") String hash,@Param("host") String host);
    @Select("SELECT request_hash AS fingerprint,result_id AS result FROM acq_h5_request WHERE session_id=#{sid} AND request_kind=#{kind} AND request_key=#{key} AND is_delete=0")
    Map<String,Object> request(@Param("sid") Long sid,@Param("kind") String kind,@Param("key") String key);
    @Insert("INSERT INTO acq_h5_request(session_id,request_kind,request_key,request_hash,result_id) VALUES(#{sid},#{kind},#{key},#{hash},#{result})")
    int remember(@Param("sid") Long sid,@Param("kind") String kind,@Param("key") String key,@Param("hash") String hash,@Param("result") String result);
    @Select("SELECT * FROM landing_page WHERE id=#{id} AND is_delete=0 FOR UPDATE") LandingPage page(Long id);
    @Select("SELECT * FROM site_domain WHERE domain_name=#{host} AND is_delete=0 FOR UPDATE") SiteDomain domain(String host);
    @Select("SELECT * FROM landing_domain WHERE landing_page_id=#{page} AND domain_id=#{domain} AND is_delete=0 FOR UPDATE")
    LandingDomain pageDomain(@Param("page") Long page,@Param("domain") Long domain);
    @Select("SELECT m.* FROM pay_merchant m JOIN domain_merchant d ON d.pay_merchant_id=m.id WHERE d.domain_id=#{domain} AND d.status=1 AND d.verify_status=1 AND d.is_delete=0 AND m.status=1 AND m.is_delete=0 ORDER BY m.id FOR UPDATE")
    List<PayMerchant> merchants(Long domain);
    @Select("SELECT * FROM wecom_route WHERE is_delete=0 AND (scope_type=1 OR landing_page_id=#{page}) ORDER BY id FOR UPDATE") List<WecomRoute> routes(Long page);
    @Select("SELECT * FROM trade_order WHERE order_no=#{no} AND session_id=#{sid} AND is_delete=0")
    TradeOrder ownedOrder(@Param("no") String no,@Param("sid") Long sid);
    @Insert("INSERT INTO post_pay_task(order_no,task_type,status,attempt_count,next_run_at,lease_version) VALUES(#{no},#{type},0,0,NOW(),0) ON DUPLICATE KEY UPDATE id=id")
    int task(@Param("no") String no,@Param("type") int type);
    @Insert("INSERT INTO post_pay_task(order_no,task_type,status,attempt_count,next_run_at,lease_version) VALUES(#{no},#{type},0,0,#{next},0) ON DUPLICATE KEY UPDATE id=id")
    int taskAt(@Param("no") String no,@Param("type") int type,@Param("next") java.time.LocalDateTime next);
    @Select("SELECT task_type AS type,status,last_error AS error FROM post_pay_task WHERE order_no=#{no} AND is_delete=0 ORDER BY task_type") List<Map<String,Object>> tasks(String no);
    @Insert("INSERT INTO acq_pay_evidence(order_no,source,evidence_key,evidence_hash,transaction_id) VALUES(#{no},#{source},#{key},#{hash},#{tx}) ON DUPLICATE KEY UPDATE id=id")
    int evidence(@Param("no") String no,@Param("source") String source,@Param("key") String key,@Param("hash") String hash,@Param("tx") String tx);
    @Select("SELECT order_no AS orderNo,transaction_id AS transactionId,evidence_hash AS evidenceHash FROM acq_pay_evidence WHERE source=#{source} AND evidence_key=#{key}")
    Map<String,Object> evidenceRow(@Param("source") String source,@Param("key") String key);
    @Select("SELECT id FROM post_pay_task WHERE task_type=#{type} AND is_delete=0 AND create_time>=#{since} AND next_run_at<=NOW() AND (status=0 OR (status=1 AND lease_until<=NOW())) ORDER BY next_run_at,id LIMIT 10")
    List<Long> candidates(@Param("type") int type,@Param("since") java.time.LocalDateTime since);
    @Update("UPDATE post_pay_task SET status=1,lease_owner=#{owner},lease_until=DATE_ADD(NOW(),INTERVAL 60 SECOND),lease_version=lease_version+1 WHERE id=#{id} AND is_delete=0 AND next_run_at<=NOW() AND (status=0 OR (status=1 AND lease_until<=NOW()))")
    int claim(@Param("id") Long id,@Param("owner") String owner);
    @Update("UPDATE post_pay_task SET status=#{status},last_error=#{error},attempt_count=attempt_count+1,lease_owner=NULL,lease_until=NULL,next_run_at=DATE_ADD(NOW(),INTERVAL 60 SECOND) WHERE id=#{id} AND status=1 AND lease_owner=#{owner} AND lease_version=#{version} AND lease_until>NOW() AND is_delete=0")
    int finish(@Param("id") Long id,@Param("owner") String owner,@Param("version") Long version,@Param("status") int status,@Param("error") String error);
}
