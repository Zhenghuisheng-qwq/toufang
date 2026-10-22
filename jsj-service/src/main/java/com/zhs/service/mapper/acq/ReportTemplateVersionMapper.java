package com.zhs.service.mapper.acq;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.service.model.acq.ReportTemplateVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * report_template_version
 */
@Mapper
public interface ReportTemplateVersionMapper extends BaseMapper<ReportTemplateVersion> {
    @Select("SELECT * FROM report_template_version WHERE id = #{id} AND is_delete = 0 FOR UPDATE")
    ReportTemplateVersion lockById(@Param("id") Long id);

    @Select("SELECT IFNULL(MAX(version_no), 0) FROM report_template_version WHERE template_id = #{templateId}")
    Integer selectMaxVersionNo(@Param("templateId") Long templateId);

    @Select("SELECT COUNT(*) FROM trade_report WHERE template_version_id = #{id}")
    long countReportRef(@Param("id") Long id);

    @Select("""
            SELECT COUNT(*) FROM report_template_version
             WHERE template_id = #{templateId} AND status = 1 AND is_delete = 0
            """)
    long countPublished(@Param("templateId") Long templateId);
}
