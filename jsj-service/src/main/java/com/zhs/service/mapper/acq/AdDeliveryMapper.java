package com.zhs.service.mapper.acq;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;
public interface AdDeliveryMapper {
    @Select("SELECT id FROM ad_conversion WHERE is_delete=0 AND event_key IS NOT NULL AND create_time>=#{since} AND status IN (0,3) AND retry_count<5 AND next_run_at<=NOW() ORDER BY id LIMIT 10") List<Long> candidates(LocalDateTime since);
    @Update("UPDATE ad_conversion SET status=6,lease_owner=#{owner},lease_until=DATE_ADD(NOW(),INTERVAL 60 SECOND),lease_version=lease_version+1,retry_count=retry_count+1,request_time=NOW() WHERE id=#{id} AND is_delete=0 AND status IN(0,3) AND retry_count<5 AND next_run_at<=NOW()") int claim(@Param("id") Long id,@Param("owner") String owner);
    @Update("UPDATE ad_conversion SET status=#{status},response_code=#{code},last_error=#{code},lease_owner=NULL,lease_until=NULL,next_run_at=DATE_ADD(NOW(),INTERVAL 5 MINUTE) WHERE id=#{id} AND status=6 AND lease_owner=#{owner} AND lease_version=#{version} AND lease_until>NOW() AND is_delete=0") int finish(@Param("id") Long id,@Param("owner") String owner,@Param("version") Long version,@Param("status") int status,@Param("code") String code);
    @Update("UPDATE ad_conversion SET status=4,last_error='LEASE_EXPIRED_RESULT_UNKNOWN',lease_owner=NULL,lease_until=NULL WHERE status=6 AND lease_until<=NOW() AND create_time>=#{since} AND is_delete=0") int expire(LocalDateTime since);
}
