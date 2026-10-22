package com.zhs.service.mapper.acq;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.service.model.acq.ReportTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * report_template
 */
@Mapper
public interface ReportTemplateMapper extends BaseMapper<ReportTemplate> {

    @Select("SELECT * FROM report_template WHERE id = #{id} AND is_delete = 0 FOR UPDATE")
    ReportTemplate lockById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM trade_report WHERE template_id = #{id}")
    long countReportRef(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM landing_page WHERE report_template_id = #{id} AND is_delete = 0")
    long countLandingRef(@Param("id") Long id);
}
