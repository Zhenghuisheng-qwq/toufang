package com.zhs.service.acq.impl;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.AcquisitionCodes;
import com.zhs.service.acq.WecomContactEntryService;
import com.zhs.service.mapper.acq.WecomContactEntryMapper;
import com.zhs.service.model.acq.WecomContactEntry;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WecomContactEntryServiceImpl implements WecomContactEntryService {

    @Resource
    private WecomContactEntryMapper wecomContactEntryMapper;

    @Override
    public WecomContactEntry create(WecomContactEntry entry) {
        if (entry == null || !StringUtils.hasText(entry.getOrderNo()) || entry.getEntryType() == null) {
            throw new BusinessException("获客入口缺少订单号或类型");
        }
        if (!StringUtils.hasText(entry.getCorpId()) || !StringUtils.hasText(entry.getTeacherUserid())) {
            throw new BusinessException("获客入口必须锁定企业和老师");
        }
        if (entry.getEntryType() == AcquisitionCodes.ENTRY_CONTACT_QR) {
            if (!StringUtils.hasText(entry.getStateToken()) || entry.getStateToken().length() > 30) {
                throw new BusinessException("联系我二维码必须携带不超过30字的state");
            }
            if (StringUtils.hasText(entry.getLinkId()) || StringUtils.hasText(entry.getLinkUrl())
                    || StringUtils.hasText(entry.getCustomerChannel())) {
                throw new BusinessException("联系我二维码不能写入获客助手字段");
            }
        } else if (entry.getEntryType() == AcquisitionCodes.ENTRY_ACQUISITION_LINK) {
            if (StringUtils.hasText(entry.getStateToken()) || StringUtils.hasText(entry.getConfigId())
                    || StringUtils.hasText(entry.getQrCodeUrl())) {
                throw new BusinessException("获客助手链接不能写入联系我state或二维码");
            }
            if (!StringUtils.hasText(entry.getCustomerChannel()) && !StringUtils.hasText(entry.getLinkId())) {
                throw new BusinessException("获客助手链接必须有独立关联参数");
            }
        } else {
            throw new BusinessException("未知获客入口类型");
        }
        wecomContactEntryMapper.insert(entry);
        return entry;
    }
}
