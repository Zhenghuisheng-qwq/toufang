package com.zhs.service.mapper.acq;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.service.model.acq.WecomCorp;
import org.apache.ibatis.annotations.Mapper;

/**
 * wecom_corp
 */
@Mapper
public interface WecomCorpMapper extends BaseMapper<WecomCorp> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM wecom_corp WHERE id = #{id} AND is_delete = 0 FOR UPDATE")
    WecomCorp lockById(@org.apache.ibatis.annotations.Param("id") Long id);
}
