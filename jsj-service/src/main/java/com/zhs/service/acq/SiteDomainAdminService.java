package com.zhs.service.acq;

import com.zhs.service.acq.dto.AcqPage;
import com.zhs.service.acq.dto.DomainCommands;

import java.util.List;

public interface SiteDomainAdminService {

    AcqPage<DomainCommands.Row> page(DomainCommands.Query query);

    DomainCommands.Detail detail(Long id);

    DomainCommands.Detail create(DomainCommands.Save command);

    DomainCommands.Detail update(Long id, DomainCommands.Save command);

    void changeStatus(Long id, Integer status);

    void delete(List<Long> ids);

    void linkLanding(Long domainId, Long landingPageId);

    void unlinkLanding(Long domainId, Long landingPageId);

    void linkMerchant(Long domainId, Long payMerchantId);

    void unlinkMerchant(Long domainId, Long payMerchantId);

    List<DomainCommands.MerchantOption> merchantOptions();
}
