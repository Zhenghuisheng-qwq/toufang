package com.zhs.service.acq.impl;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.AdConversionService;
import com.zhs.service.mapper.acq.AdConversionMapper;
import com.zhs.service.model.acq.AdConversion;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdConversionServiceImpl implements AdConversionService {

    @Resource
    private AdConversionMapper adConversionMapper;

    @Override
    public AdConversion createPending(AdConversion conversion) {
        if (conversion == null || !StringUtils.hasText(conversion.getOrderNo())
                || conversion.getPlatform() == null || conversion.getEventType() == null) {
            throw new BusinessException("转化事件缺少订单、平台或事件类型");
        }
        if (conversion.getAdClickId() == null) {
            throw new BusinessException("没有广告点击记录时不能创建回传");
        }
        conversion.setStatus(0);
        if (conversion.getRetryCount() == null) {
            conversion.setRetryCount(0);
        }
        adConversionMapper.insert(conversion);
        return conversion;
    }
}
