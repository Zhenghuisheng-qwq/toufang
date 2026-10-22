package com.zhs.service.mapper.acq;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.service.model.acq.LandingPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * landing_page
 */
@Mapper
public interface LandingPageMapper extends BaseMapper<LandingPage> {

    @Select("SELECT COUNT(*) FROM trade_order WHERE landing_page_id = #{id}")
    long countTradeOrderRef(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM h5_visit WHERE landing_page_id = #{id}")
    long countVisitRef(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM h5_lead WHERE landing_page_id = #{id}")
    long countLeadRef(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM ad_click WHERE landing_page_id = #{id}")
    long countClickRef(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM wecom_route WHERE landing_page_id = #{id}")
    long countRouteRef(@Param("id") Long id);
}
