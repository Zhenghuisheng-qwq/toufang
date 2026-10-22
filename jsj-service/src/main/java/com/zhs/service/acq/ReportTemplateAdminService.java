package com.zhs.service.acq;

import com.zhs.service.acq.dto.AcqPage;
import com.zhs.service.acq.dto.ReportCommands;

import java.util.List;

public interface ReportTemplateAdminService {
    java.util.List<java.util.Map<String,Object>> options(String keyword);

    AcqPage<ReportCommands.Row> page(ReportCommands.Query query);

    ReportCommands.Detail detail(Long id);

    ReportCommands.Detail create(ReportCommands.Save command);

    ReportCommands.Detail update(Long id, ReportCommands.Save command);

    void changeStatus(Long id, Integer status);

    void delete(List<Long> ids);

    ReportCommands.Version createDraft(Long templateId, ReportCommands.Draft command);

    ReportCommands.Version updateDraft(Long versionId, ReportCommands.Draft command);

    ReportCommands.Version publish(Long versionId);

    void deleteDraft(Long versionId);

    ReportCommands.PreviewResult preview(ReportCommands.Preview command);
}
