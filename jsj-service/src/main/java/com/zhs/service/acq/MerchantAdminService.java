package com.zhs.service.acq;

import com.zhs.service.acq.dto.AcqPage;
import com.zhs.service.acq.dto.ConfigCommands;
import java.util.Map;

public interface MerchantAdminService {
    AcqPage<Map<String,Object>> page(ConfigCommands.Query query);
    Map<String,Object> detail(Long id, boolean secrets);
    Long save(Long id, ConfigCommands.Merchant command);
    void status(Long id, Integer status);
    void delete(Long id);
    Long saveVersion(Long merchantId, Long versionId, ConfigCommands.MerchantVersion command);
    void activate(Long merchantId, Long versionId);
    void deactivate(Long merchantId);
}
