package com.zhs.service.mapper.acq;

import com.zhs.service.acq.report.ReportAdminView;
import com.zhs.service.model.acq.PostPayTask;
import org.apache.ibatis.annotations.*;

public interface ReportAdminMapper {
    @Select("""
        SELECT o.order_no,o.pay_status,o.report_template_id AS template_id,
               o.report_template_version_id AS template_version_id,r.id AS report_id,r.status AS report_status,
               r.generated_time,t.id AS task_id,t.status AS task_status,t.attempt_count,t.lease_until,t.last_error
        FROM trade_order o LEFT JOIN trade_report r ON r.order_no=o.order_no AND r.is_delete=0
        LEFT JOIN post_pay_task t ON t.order_no=o.order_no AND t.task_type=1 AND t.is_delete=0
        WHERE o.order_no=#{orderNo} AND o.is_delete=0
        """)
    ReportAdminView status(@Param("orderNo") String orderNo);

    @Select("SELECT content_text FROM trade_report WHERE order_no=#{orderNo} AND status=1 AND is_delete=0")
    String content(@Param("orderNo") String orderNo);

    @Select("SELECT * FROM post_pay_task WHERE order_no=#{orderNo} AND task_type=1 FOR UPDATE")
    PostPayTask lockReportTask(@Param("orderNo") String orderNo);

    @Update("""
        UPDATE post_pay_task SET status=1,lease_owner=#{owner},lease_until=DATE_ADD(CURRENT_TIMESTAMP,INTERVAL 60 SECOND),
            lease_version=lease_version+1,update_time=CURRENT_TIMESTAMP
        WHERE id=#{id} AND task_type=1 AND is_delete=0 AND lease_version=#{version}
          AND (status IN (0,3,4) OR (status=1 AND lease_until<=CURRENT_TIMESTAMP))
        """)
    int claimRetry(@Param("id") Long id,@Param("version") Long version,@Param("owner") String owner);
}
