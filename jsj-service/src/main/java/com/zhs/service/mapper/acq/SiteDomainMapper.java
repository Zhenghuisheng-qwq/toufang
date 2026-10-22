package com.zhs.service.mapper.acq;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.service.model.acq.SiteDomain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * site_domain
 */
@Mapper
public interface SiteDomainMapper extends BaseMapper<SiteDomain> {

    @Select("SELECT * FROM site_domain WHERE domain_name = #{domainName} LIMIT 1")
    SiteDomain selectAnyByDomainName(@Param("domainName") String domainName);

    @Update("""
            UPDATE site_domain
               SET is_delete = 0,
                   status = #{status},
                   review_status = #{reviewStatus},
                   icp_status = #{icpStatus},
                   https_status = #{httpsStatus},
                   verify_status = 0,
                   remark = #{remark},
                   update_time = NOW()
             WHERE id = #{id}
            """)
    int restore(SiteDomain domain);

    @Select("SELECT COUNT(*) FROM trade_order WHERE domain_id = #{id}")
    long countTradeOrderRef(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM h5_visit WHERE domain_id = #{id}")
    long countVisitRef(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM h5_lead WHERE domain_id = #{id}")
    long countLeadRef(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM ad_click WHERE domain_id = #{id}")
    long countClickRef(@Param("id") Long id);
}
