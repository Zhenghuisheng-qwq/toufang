package com.zhs.service.mapper.acq;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.service.model.acq.DomainMerchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * domain_merchant
 */
@Mapper
public interface DomainMerchantMapper extends BaseMapper<DomainMerchant> {

    @Select("""
            SELECT * FROM domain_merchant
             WHERE domain_id = #{domainId} AND pay_merchant_id = #{payMerchantId}
             LIMIT 1
            """)
    DomainMerchant selectAny(@Param("domainId") Long domainId, @Param("payMerchantId") Long payMerchantId);

    @Update("""
            UPDATE domain_merchant
               SET is_delete = 0, status = 1, verify_status = 0, update_time = NOW()
             WHERE id = #{id}
            """)
    int restore(@Param("id") Long id);
}
