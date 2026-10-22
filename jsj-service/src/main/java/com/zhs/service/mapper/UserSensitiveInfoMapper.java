package com.zhs.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.service.model.UserSensitiveInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户敏感信息 Mapper
 */
@Mapper
public interface UserSensitiveInfoMapper extends BaseMapper<UserSensitiveInfo> {
}
