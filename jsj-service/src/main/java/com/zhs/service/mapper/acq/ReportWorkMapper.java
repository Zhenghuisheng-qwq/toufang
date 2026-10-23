package com.zhs.service.mapper.acq;

import com.zhs.service.model.acq.PayRecord;
import com.zhs.service.model.acq.PostPayTask;
import com.zhs.service.model.acq.TradeOrder;
import com.zhs.service.model.acq.TradeReport;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/** Current reads and database time enforce ownership at the final write boundary. */
public interface ReportWorkMapper {

    @Select("""
        SELECT id FROM post_pay_task WHERE is_delete=0 AND task_type=1 AND next_run_at<=CURRENT_TIMESTAMP
        AND (status=0 OR (status=1 AND lease_until<=CURRENT_TIMESTAMP))
        ORDER BY next_run_at,id LIMIT 10
        """)
    List<Long> candidates();

    @Update("""
        UPDATE post_pay_task SET status=1,lease_owner=#{owner},lease_until=DATE_ADD(CURRENT_TIMESTAMP,INTERVAL 60 SECOND),
        lease_version=lease_version+1 WHERE id=#{id} AND is_delete=0 AND task_type=1
        AND next_run_at<=CURRENT_TIMESTAMP AND (status=0 OR (status=1 AND lease_until<=CURRENT_TIMESTAMP))
        """)
    int claim(@Param("id") Long id, @Param("owner") String owner);

    @Select("SELECT * FROM post_pay_task WHERE id=#{id} AND is_delete=0")
    PostPayTask task(@Param("id") Long id);

    @Insert("""
        INSERT INTO post_pay_task (order_no,task_type,status,attempt_count,next_run_at,lease_version,is_delete)
        VALUES (#{orderNo},1,0,0,CURRENT_TIMESTAMP,0,0)
        ON DUPLICATE KEY UPDATE id=id
        """)
    int ensureReport(@Param("orderNo") String orderNo);

    @Update("""
        UPDATE post_pay_task SET status=IF(attempt_count>=4,4,0),
        attempt_count=attempt_count+1,last_error='TRANSIENT_STORAGE_ERROR',
        lease_owner=NULL,lease_until=NULL,next_run_at=DATE_ADD(CURRENT_TIMESTAMP,INTERVAL 60 SECOND)
        WHERE id=#{id} AND task_type=1 AND status=1 AND is_delete=0 AND lease_owner=#{owner}
        AND lease_version=#{version} AND lease_until>CURRENT_TIMESTAMP
        """)
    int retryLater(@Param("id") Long id, @Param("owner") String owner, @Param("version") Long version);

    @Select("SELECT * FROM post_pay_task WHERE id=#{id} AND is_delete=0 FOR UPDATE")
    PostPayTask lockTask(@Param("id") Long id);

    @Select("SELECT CURRENT_TIMESTAMP")
    LocalDateTime now();

    @Select("SELECT * FROM trade_order WHERE order_no=#{orderNo} AND is_delete=0 FOR UPDATE")
    TradeOrder lockOrder(@Param("orderNo") String orderNo);

    @Select("SELECT * FROM pay_record WHERE order_no=#{orderNo} AND is_delete=0 FOR UPDATE")
    PayRecord lockPayment(@Param("orderNo") String orderNo);

    @Select("SELECT * FROM trade_report WHERE order_no=#{orderNo} FOR UPDATE")
    TradeReport lockReport(@Param("orderNo") String orderNo);

    @Update("""
        UPDATE post_pay_task SET status=#{status},last_error=#{error},lease_owner=NULL,lease_until=NULL,
        attempt_count=attempt_count+1,update_time=CURRENT_TIMESTAMP
        WHERE id=#{id} AND is_delete=0 AND task_type=1 AND status=1
          AND lease_owner=#{owner} AND lease_until>CURRENT_TIMESTAMP
        """)
    int finish(@Param("id") Long id, @Param("owner") String owner, @Param("status") int status, @Param("error") String error);
}
