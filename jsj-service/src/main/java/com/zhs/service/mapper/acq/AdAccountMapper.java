package com.zhs.service.mapper.acq;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.service.model.acq.AdAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * ad_account
 */
@Mapper
public interface AdAccountMapper extends BaseMapper<AdAccount> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM ad_account WHERE id = #{id} AND is_delete = 0 FOR UPDATE")
    AdAccount lockById(@org.apache.ibatis.annotations.Param("id") Long id);
}
