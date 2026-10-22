package com.zhs.service.acq.report;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

/** Only invoked inside order creation. All input and template identifiers come from stored rows. */
@Component
public class OrderReportSnapshot {
    @Resource private LandingPageMapper pages;
    @Resource private H5LeadMapper leads;
    @Resource private ReportTemplateMapper templates;
    @Resource private ReportTemplateVersionMapper versions;
    @Resource private PlainTextReportRenderer renderer;

    @Transactional(transactionManager="acqTransactionManager", propagation=Propagation.MANDATORY)
    public void freeze(TradeOrder order) {
        var page = order.getLandingPageId() == null ? null : pages.selectById(order.getLandingPageId());
        var lead = order.getLeadId() == null ? null : leads.selectById(order.getLeadId());
        if (page == null || !Integer.valueOf(1).equals(page.getPublishStatus()) || lead == null
                || !Objects.equals(page.getId(), lead.getLandingPageId())
                || !Objects.equals(order.getDomainId(), lead.getDomainId())) {
            throw new BusinessException("报告所需页面或线索关联无效");
        }
        var template = page.getReportTemplateId() == null ? null : templates.lockById(page.getReportTemplateId());
        if (template == null || !Integer.valueOf(1).equals(template.getStatus()) || template.getCurrentVersionId() == null)
            throw new BusinessException("页面没有可交付的报告模板");
        var version = versions.lockById(template.getCurrentVersionId());
        if (version == null || !template.getId().equals(version.getTemplateId()) || !Integer.valueOf(1).equals(version.getStatus()))
            throw new BusinessException("报告模板缺少有效发布版本");
        var input = new ReportInput(lead.getRealName(), lead.getGender(), lead.getBirthDate(), lead.getBirthTime());
        renderer.render(version.getContentType(), version.getContentText(), input);
        order.setReportTemplateId(template.getId()); order.setReportTemplateVersionId(version.getId());
        order.setReportInputVersion(1); order.setReportName(input.name()); order.setReportGender(input.gender());
        order.setReportBirthDate(input.birthDate()); order.setReportBirthTime(input.birthTime());
    }
}
