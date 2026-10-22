package com.zhs.service.acq.report;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.TradeReport;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.zhs.service.acq.report.ReportGenerationTest.fake;

/** Offline mapper fixtures; invokes the real shared generator. MySQL rollback/isolation needs separate acceptance. */
class ReportAdminServiceTest {
    ReportGenerationTest rig;
    ReportAdminServiceImpl service;
    int claims;
    boolean claimLost;
    @BeforeEach void setup() throws Exception {
        rig=new ReportGenerationTest(); rig.setup();
        rig.task.setIsDelete(0); rig.task.setStatus(4); rig.task.setLeaseUntil(null);
        service=new ReportAdminServiceImpl();
        var work=fake(ReportWorkMapper.class,(m,a)->switch(m) {
            case "ensureReport" -> 1;
            case "lockTask" -> rig.task;
            case "lockOrder" -> rig.order;
            case "lockPayment" -> rig.pay;
            case "lockReport" -> rig.stored;
            case "now" -> rig.now;
            case "finish" -> { rig.task.setStatus((int)a[2]);rig.task.setLastError((String)a[3]);yield 1; }
            default -> throw new AssertionError(m);
        });
        rig.inject("work",work); inject("work",work);
        inject("versions",fake(ReportTemplateVersionMapper.class,(m,a)->rig.version));
        inject("renderer",new PlainTextReportRenderer()); inject("generation",rig.service);
        inject("admin",fake(ReportAdminMapper.class,(m,a)->switch(m) {
            case "lockReportTask" -> rig.task;
            case "claimRetry" -> {
                claims++; if(claimLost) yield 0;
                assertEquals(rig.task.getLeaseVersion(),a[1]);
                rig.task.setStatus(1);rig.task.setLeaseOwner((String)a[2]);rig.task.setLeaseVersion(rig.task.getLeaseVersion()+1);
                rig.task.setLeaseUntil(rig.now.plusSeconds(60));yield 1;
            }
            case "status" -> {
                if(rig.order==null) yield null;
                var view=new ReportAdminView();view.setOrderNo(rig.order.getOrderNo());view.setTaskStatus(rig.task.getStatus());
                view.setReportStatus(rig.stored==null?null:rig.stored.getStatus());view.setLastError(rig.task.getLastError());yield view;
            }
            case "content" -> rig.stored==null?null:rig.stored.getContentText();
            default -> throw new AssertionError(m);
        }));
    }
    @Test void retryUsesRealGeneratorAndDuplicateDoesNotOverwrite() {
        assertEquals(1,retry().getReportStatus());assertEquals(1,rig.inserts);assertEquals(1,claims);
        String text=rig.stored.getContentText();rig.version.setContentText("changed");
        assertEquals(1,retry().getReportStatus());assertEquals(text,rig.stored.getContentText());assertEquals(1,claims);
    }
    @Test void missingOrderAndUnpaidAndRefundedAreRejected() {
        rig.order.setPayStatus(0);rejected();rig.order.setPayStatus(2);rejected();rig.order=null;rejected();assertEquals(0,claims);
    }
    @Test void paymentIdentityAndAmountMustMatch() {
        rig.pay.setAmountFen(1);rejected();rig.pay.setAmountFen(10);
        rig.pay.setMchId("other");rejected();rig.pay.setMchId("TEST_MCH");
        rig.pay.setOrderNo("other");rejected();rig.pay.setOrderNo("R3_TEST_ORDER");
        rig.order.setAppId(null);rig.pay.setAppId(null);rejected();assertEquals(0,claims);
    }
    @Test void liveLeaseAndUnknownLeaseAreRejected() {
        rig.task.setStatus(1);rig.task.setLeaseUntil(rig.now.plusSeconds(1));rejected();
        rig.task.setLeaseUntil(null);rejected();assertEquals(0,claims);
    }
    @Test void expiredLeaseCanBeReclaimed() {
        rig.task.setStatus(1);rig.task.setLeaseUntil(rig.now);assertEquals(1,retry().getReportStatus());
    }
    @Test void missingSnapshotAndUnpublishedVersionAreRejected() {
        rig.order.setReportInputVersion(null);rejected();rig.order.setReportInputVersion(1);
        rig.version.setStatus(0);rejected();rig.version.setStatus(1);
        rig.version.setContentText("{{unknown}}");rejected();assertEquals(0,claims);
    }
    @Test void failedOrDeletedReportCannotBeOverwritten() {
        rig.stored=new TradeReport();rig.stored.setStatus(2);rejected();
        rig.stored.setStatus(1);rig.stored.setIsDelete(1);rig.stored.setContentText("old");rejected();assertEquals(0,claims);
    }
    @Test void failedConditionalClaimCannotGenerate() {
        claimLost=true;rejected();assertEquals(0,rig.inserts);
    }
    @Test void deletedTaskAndInvalidOrderNoAreRejected() {
        rig.task.setIsDelete(1);rejected();assertThrows(BusinessException.class,()->service.retry("../order"));assertEquals(0,claims);
    }
    @Test void unknownErrorIsRedactedAndMissingBodyIsRejected() {
        rig.task.setLastError("untrusted-detail");assertEquals("REQUIRES_REVIEW",service.status("R3_TEST_ORDER").getLastError());
        assertThrows(BusinessException.class,()->service.content("R3_TEST_ORDER"));
    }
    ReportAdminView retry(){return service.retry("R3_TEST_ORDER");}
    void rejected(){assertThrows(BusinessException.class,this::retry);assertEquals(0,rig.inserts);}
    void inject(String name,Object value)throws Exception {var f=service.getClass().getDeclaredField(name);f.setAccessible(true);f.set(service,value);}
}
