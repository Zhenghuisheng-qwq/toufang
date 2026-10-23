package com.zhs.service.mapper.acq;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface WecomActionMapper {

    @Update("UPDATE wecom_contact_event SET remark_status=IF(#{type}='REMARK',#{status},remark_status),tag_status=IF(#{type}='TAG',#{status},tag_status) WHERE id=#{event} AND is_delete=0")
    int eventStatus(@Param("event") Long event, @Param("type") String type, @Param("status") int status);

    @Insert("INSERT INTO acq_wecom_action(event_id,corp_row_id,corp_id,external_userid,teacher_userid,action_type,payload) VALUES(#{event},#{corpId},#{corp},#{external},#{teacher},#{type},#{payload}) ON DUPLICATE KEY UPDATE id=id")
    int register(@Param("event") Long event, @Param("corpId") Long corpId, @Param("corp") String corp, @Param("external") String external, @Param("teacher") String teacher, @Param("type") String type, @Param("payload") String payload);

    @Select("SELECT id FROM acq_wecom_action WHERE status=0 AND is_delete=0 AND create_time>=#{since} AND attempt_count<5 ORDER BY id LIMIT 10")
    List<Long> candidates(LocalDateTime since);

    @Update("UPDATE acq_wecom_action SET status=6,attempt_count=attempt_count+1,lease_owner=#{owner},lease_until=DATE_ADD(NOW(),INTERVAL 60 SECOND),lease_version=lease_version+1 WHERE id=#{id} AND status=0 AND is_delete=0 AND attempt_count<5")
    int claim(@Param("id") Long id, @Param("owner") String owner);

    @Select("SELECT * FROM acq_wecom_action WHERE id=#{id} AND is_delete=0")
    Map<String, Object> action(Long id);

    @Update("UPDATE acq_wecom_action SET status=#{status},last_error=#{error},lease_owner=NULL,lease_until=NULL WHERE id=#{id} AND status=6 AND lease_owner=#{owner} AND lease_version=#{version} AND lease_until>NOW() AND is_delete=0")
    int finish(@Param("id") Long id, @Param("owner") String owner, @Param("version") Long version, @Param("status") int status, @Param("error") String error);

    @Update("UPDATE acq_wecom_action SET status=4,last_error='RESULT_UNKNOWN',lease_owner=NULL,lease_until=NULL WHERE status=6 AND lease_until<=NOW() AND create_time>=#{since} AND is_delete=0")
    int expire(LocalDateTime since);

    @Select("SELECT COUNT(*) FROM wecom_customer_relation WHERE corp_id=#{corp} AND external_userid=#{external} AND teacher_userid=#{teacher} AND status=1 AND is_delete=0")
    int active(@Param("corp") String corp, @Param("external") String external, @Param("teacher") String teacher);
}
