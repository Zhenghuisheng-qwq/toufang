package com.zhs.service.mapper.acq;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.service.model.acq.WecomTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * wecom_tag
 */
@Mapper
public interface WecomTagMapper extends BaseMapper<WecomTag> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM wecom_tag WHERE id = #{id} AND is_delete = 0 FOR UPDATE")
    WecomTag lockById(@org.apache.ibatis.annotations.Param("id") Long id);
}
