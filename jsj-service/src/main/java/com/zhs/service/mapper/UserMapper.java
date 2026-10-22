package com.zhs.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.service.model.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
