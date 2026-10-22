package com.zhs.service.acq.impl;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.PayMerchantVersionService;
import com.zhs.service.acq.SecretRefGuard;
import com.zhs.service.mapper.acq.PayMerchantVersionMapper;
import com.zhs.service.model.acq.PayMerchantVersion;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.zhs.service.mapper.acq.PayMerchantMapper;
import com.zhs.service.mapper.acq.ConfigReferenceMapper;

@Service
public class PayMerchantVersionServiceImpl implements PayMerchantVersionService {

    @Resource
    private PayMerchantVersionMapper payMerchantVersionMapper;
    @Resource private PayMerchantMapper payMerchantMapper;
    @Resource private ConfigReferenceMapper references;

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public PayMerchantVersion save(PayMerchantVersion version) {
        if (version == null || version.getPayMerchantId() == null) {
            throw new BusinessException("商户配置版本不完整");
        }
        if (payMerchantMapper.lockById(version.getPayMerchantId()) == null) {
            throw new BusinessException("商户不存在");
        }
        version.setId(null);
        version.setIsDelete(0);
        version.setStatus(0);
        version.setVersionNo(Math.addExact(references.maxVersion(version.getPayMerchantId()), 1));
        SecretRefGuard.requireRef("apiV3KeyRef", version.getApiV3KeyRef());
        SecretRefGuard.requireRef("privateKeyRef", version.getPrivateKeyRef());
        SecretRefGuard.requireRef("platformCertRef", version.getPlatformCertRef());
        SecretRefGuard.requireRef("platformPublicKeyRef", version.getPlatformPublicKeyRef());
        payMerchantVersionMapper.insert(version);
        return version;
    }
}
