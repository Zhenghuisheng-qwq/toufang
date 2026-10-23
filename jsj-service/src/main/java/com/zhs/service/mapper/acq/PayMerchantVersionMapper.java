package com.zhs.service.mapper.acq;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.service.model.acq.PayMerchantVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * pay_merchant_version
 */
@Mapper
public interface PayMerchantVersionMapper extends BaseMapper<PayMerchantVersion> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM pay_merchant_version WHERE id=#{id} AND is_delete=0 FOR UPDATE")
    PayMerchantVersion lockById(Long id);
}
