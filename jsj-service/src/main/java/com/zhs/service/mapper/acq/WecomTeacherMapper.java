package com.zhs.service.mapper.acq;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.service.model.acq.WecomTeacher;
import org.apache.ibatis.annotations.Mapper;

/**
 * wecom_teacher
 */
@Mapper
public interface WecomTeacherMapper extends BaseMapper<WecomTeacher> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM wecom_teacher WHERE id = #{id} AND is_delete = 0 FOR UPDATE")
    WecomTeacher lockById(@org.apache.ibatis.annotations.Param("id") Long id);
}
