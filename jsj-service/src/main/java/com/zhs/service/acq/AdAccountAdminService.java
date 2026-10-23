package com.zhs.service.acq;

import com.zhs.service.acq.dto.AcqPage;
import com.zhs.service.acq.dto.ConfigCommands;

import java.util.Map;

public interface AdAccountAdminService {

    AcqPage<Map<String, Object>> page(ConfigCommands.Query query);

    Map<String, Object> detail(Long id, boolean secrets);

    Long save(Long id, ConfigCommands.AdAccount command);

    void secret(Long id, ConfigCommands.AdSecret command);

    void status(Long id, Integer status);

    void delete(Long id);
}
