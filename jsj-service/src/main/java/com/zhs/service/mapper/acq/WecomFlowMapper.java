package com.zhs.service.mapper.acq;
import org.apache.ibatis.annotations.*;
import com.zhs.service.model.acq.*;
public interface WecomFlowMapper {
    @Select("SELECT * FROM wecom_corp WHERE sid=#{sid} AND is_delete=0") WecomCorp corp(String sid);
    @Insert("INSERT INTO wecom_external_contact(corp_id,external_userid,status) VALUES(#{corp},#{external},1) ON DUPLICATE KEY UPDATE id=id") int customer(@Param("corp") String corp,@Param("external") String external);
    @Insert("INSERT INTO wecom_customer_relation(corp_id,external_userid,teacher_userid,status,event_time) VALUES(#{corp},#{external},#{teacher},#{status},#{time}) ON DUPLICATE KEY UPDATE status=IF(VALUES(event_time)>event_time,VALUES(status),IF(VALUES(event_time)=event_time,LEAST(status,VALUES(status)),status)),event_time=GREATEST(event_time,VALUES(event_time))")
    int relation(@Param("corp") String corp,@Param("external") String external,@Param("teacher") String teacher,@Param("status") int status,@Param("time") long time);
    @Update("UPDATE wecom_external_contact c SET status=IF(EXISTS(SELECT 1 FROM wecom_customer_relation r WHERE r.corp_id=c.corp_id AND r.external_userid=c.external_userid AND r.status=1 AND r.is_delete=0),1,0) WHERE c.corp_id=#{corp} AND c.external_userid=#{external} AND c.is_delete=0") int customerStatus(@Param("corp") String corp,@Param("external") String external);
    @Select("SELECT * FROM wecom_contact_entry WHERE corp_id=#{corp} AND teacher_userid=#{teacher} AND state_token=#{state} AND entry_type=1 AND status=1 AND is_delete=0") WecomContactEntry entry(@Param("corp") String corp,@Param("teacher") String teacher,@Param("state") String state);
}
