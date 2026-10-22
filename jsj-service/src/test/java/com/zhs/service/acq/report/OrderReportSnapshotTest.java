package com.zhs.service.acq.report;

import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static com.zhs.service.acq.report.ReportGenerationTest.fake;

class OrderReportSnapshotTest {
    @Test void frozenInputsAndVersionDoNotFollowLaterEdits() throws Exception {
        var service=new OrderReportSnapshot();
        var page=new LandingPage();page.setId(1L);page.setPublishStatus(1);page.setReportTemplateId(2L);
        var lead=new H5Lead();lead.setId(3L);lead.setLandingPageId(1L);lead.setDomainId(4L);lead.setRealName("原姓名");lead.setBirthDate(LocalDate.of(2000,1,1));
        var template=new ReportTemplate();template.setId(2L);template.setStatus(1);template.setCurrentVersionId(5L);
        var version=new ReportTemplateVersion();version.setId(5L);version.setTemplateId(2L);version.setStatus(1);version.setContentType(1);version.setContentText("{{name}} {{birthTime}}");
        var order=new TradeOrder();order.setLandingPageId(1L);order.setLeadId(3L);order.setDomainId(4L);
        order.setReportTemplateVersionId(999L);order.setReportName("不可信传参");
        inject(service,"pages",fake(LandingPageMapper.class,(m,a)->page));
        inject(service,"leads",fake(H5LeadMapper.class,(m,a)->lead));
        inject(service,"templates",fake(ReportTemplateMapper.class,(m,a)->template));
        inject(service,"versions",fake(ReportTemplateVersionMapper.class,(m,a)->version));
        inject(service,"renderer",new PlainTextReportRenderer());
        service.freeze(order);
        lead.setRealName("新姓名");template.setCurrentVersionId(6L);
        assertEquals("原姓名",order.getReportName());assertEquals(5L,order.getReportTemplateVersionId());
        assertNull(order.getReportBirthTime());assertEquals(1,order.getReportInputVersion());
    }
    private void inject(Object target,String name,Object value)throws Exception {
        var f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);
    }
}
