package com.zhs.service.acq.report;

import com.zhs.service.mapper.acq.H5LeadMapper;
import com.zhs.service.mapper.acq.LandingPageMapper;
import com.zhs.service.mapper.acq.ReportTemplateMapper;
import com.zhs.service.mapper.acq.ReportTemplateVersionMapper;
import com.zhs.service.model.acq.H5Lead;
import com.zhs.service.model.acq.LandingPage;
import com.zhs.service.model.acq.ReportTemplate;
import com.zhs.service.model.acq.ReportTemplateVersion;
import com.zhs.service.model.acq.TradeOrder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static com.zhs.service.acq.report.ReportGenerationTest.fake;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderReportSnapshotTest {

    @Test
    void frozenInputsAndVersionDoNotFollowLaterEdits() throws Exception {
        OrderReportSnapshot service = new OrderReportSnapshot();
        LandingPage page = new LandingPage();
        page.setId(1L);
        page.setPublishStatus(1);
        page.setReportTemplateId(2L);
        H5Lead lead = new H5Lead();
        lead.setId(3L);
        lead.setLandingPageId(1L);
        lead.setDomainId(4L);
        lead.setRealName("原姓名");
        lead.setBirthDate(LocalDate.of(2000, 1, 1));
        ReportTemplate template = new ReportTemplate();
        template.setId(2L);
        template.setStatus(1);
        template.setCurrentVersionId(5L);
        ReportTemplateVersion version = new ReportTemplateVersion();
        version.setId(5L);
        version.setTemplateId(2L);
        version.setStatus(1);
        version.setContentType(1);
        version.setContentText("{{name}} {{birthTime}}");
        TradeOrder order = new TradeOrder();
        order.setLandingPageId(1L);
        order.setLeadId(3L);
        order.setDomainId(4L);
        order.setReportTemplateVersionId(999L);
        order.setReportName("不可信传参");
        inject(service, "pages", fake(LandingPageMapper.class, (m, a) -> page));
        inject(service, "leads", fake(H5LeadMapper.class, (m, a) -> lead));
        inject(service, "templates", fake(ReportTemplateMapper.class, (m, a) -> template));
        inject(service, "versions", fake(ReportTemplateVersionMapper.class, (m, a) -> version));
        inject(service, "renderer", new PlainTextReportRenderer());
        service.freeze(order);
        lead.setRealName("新姓名");
        template.setCurrentVersionId(6L);
        assertEquals("原姓名", order.getReportName());
        assertEquals(5L, order.getReportTemplateVersionId());
        assertNull(order.getReportBirthTime());
        assertEquals(1, order.getReportInputVersion());
    }

    private void inject(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
