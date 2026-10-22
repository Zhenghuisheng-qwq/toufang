package com.zhs.service.mapper.acq;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.service.model.acq.LandingDomain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * landing_domain
 */
@Mapper
public interface LandingDomainMapper extends BaseMapper<LandingDomain> {

    @Select("""
            SELECT * FROM landing_domain
             WHERE landing_page_id = #{landingPageId} AND domain_id = #{domainId}
             LIMIT 1
            """)
    LandingDomain selectAny(@Param("landingPageId") Long landingPageId, @Param("domainId") Long domainId);

    @Update("UPDATE landing_domain SET is_delete = 0, status = 1, update_time = NOW() WHERE id = #{id}")
    int restore(@Param("id") Long id);
}
