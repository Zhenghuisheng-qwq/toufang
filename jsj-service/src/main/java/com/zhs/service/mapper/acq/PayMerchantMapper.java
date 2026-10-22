package com.zhs.service.mapper.acq;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.service.model.acq.PayMerchant;
import org.apache.ibatis.annotations.Mapper;

/**
 * pay_merchant
 */
@Mapper
public interface PayMerchantMapper extends BaseMapper<PayMerchant> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM pay_merchant WHERE id = #{id} AND is_delete = 0 FOR UPDATE")
    PayMerchant lockById(@org.apache.ibatis.annotations.Param("id") Long id);
}
