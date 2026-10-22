package com.zhs.service.acq;

import com.zhs.service.acq.dto.AcqPage;
import com.zhs.service.acq.dto.LandingPageQuery;
import com.zhs.service.acq.dto.LandingPageSaveCommand;
import com.zhs.service.acq.dto.LandingViews;

import java.util.List;

public interface LandingPageAdminService {

    AcqPage<LandingViews.Row> page(LandingPageQuery query);

    LandingViews.Detail detail(Long id);

    LandingViews.Detail create(LandingPageSaveCommand command);

    LandingViews.Detail update(Long id, LandingPageSaveCommand command);

    void changeStatus(Long id, Integer publishStatus);

    void delete(List<Long> ids);

    List<LandingViews.Option> options();
}
