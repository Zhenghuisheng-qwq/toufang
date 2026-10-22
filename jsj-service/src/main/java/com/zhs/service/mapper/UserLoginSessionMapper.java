package com.zhs.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.service.model.UserLoginSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserLoginSessionMapper extends BaseMapper<UserLoginSession> {
}
