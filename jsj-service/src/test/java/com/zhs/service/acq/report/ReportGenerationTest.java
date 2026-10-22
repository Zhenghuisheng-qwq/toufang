package com.zhs.service.acq.report;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.Proxy;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

/** Mapper fakes prove service branches; do not substitute for MySQL isolation/rollback verification. */
class ReportGenerationTest {
    final LocalDateTime now=LocalDateTime.of(2026,10,22,12,0);
    ReportGenerationServiceImpl service;
    TradeOrder order; PayRecord pay; PostPayTask task; ReportTemplateVersion version;
    TradeReport stored; int inserts; String error; int resultStatus; boolean loseLease;
    @BeforeEach void setup() throws Exception {
        service=new ReportGenerationServiceImpl();
        order=new TradeOrder();order.setOrderNo("R3_TEST_ORDER");order.setPayStatus(1);order.setPaidTime(now);
        order.setAmountFen(10);order.setMchId("TEST_MCH");order.setAppId("TEST_APP");
        order.setReportTemplateId(1L);order.setReportTemplateVersionId(2L);order.setReportInputVersion(1);
        order.setReportName("测试");order.setReportGender(0);order.setReportBirthDate(LocalDate.of(2000,1,1));
        pay=new PayRecord();pay.setOrderNo(order.getOrderNo());pay.setSuccessOrderNo(order.getOrderNo());pay.setPayStatus(1);
        pay.setAmountFen(10);pay.setMchId("TEST_MCH");pay.setAppId("TEST_APP");pay.setTransactionId("TEST_TRANSACTION");pay.setSuccessTime(now);
        task=new PostPayTask();task.setId(3L);task.setOrderNo(order.getOrderNo());task.setTaskType(1);task.setStatus(1);
        task.setLeaseOwner("test-lease");task.setLeaseVersion(1L);task.setLeaseUntil(now.plusSeconds(60));
        version=new ReportTemplateVersion();version.setId(2L);version.setTemplateId(1L);version.setStatus(1);version.setContentType(1);
        version.setContentText("姓名{{name}}\n出生时间{{birthTime}}，提供{{birthTimeProvided}}");
        inject("renderer",new PlainTextReportRenderer());
        inject("work",fake(ReportWorkMapper.class,(m,a)->switch(m) {
            case "lockTask" -> task;
            case "now" -> now;
            case "lockOrder" -> order;
            case "lockPayment" -> pay;
            case "lockReport" -> stored;
            case "finish" -> { resultStatus=(int)a[2];error=(String)a[3];yield loseLease?0:1; }
            default -> throw new AssertionError(m);
        }));
        inject("versions",fake(ReportTemplateVersionMapper.class,(m,a)->{
            assertEquals("lockById",m);assertEquals(2L,a[0]);return version;
        }));
        inject("reports",fake(TradeReportMapper.class,(m,a)->{
            assertEquals("insert",m);stored=(TradeReport)a[0];stored.setId(4L);inserts++;return 1;
        }));
    }
    @Test void generatedTextUsesOnlyFrozenInputAndPreservesMissingTime() {
        assertTrue(run());assertEquals(1,inserts);assertEquals(2,resultStatus);
        assertEquals("姓名测试\n出生时间未提供，提供否",stored.getContentText());
        assertEquals(2L,stored.getTemplateVersionId());
    }
    @Test void unpaidOrMismatchedPaymentCannotGenerate() {
        pay.setAmountFen(9);assertTrue(run());assertEquals(0,inserts);assertEquals("PAYMENT_INELIGIBLE",error);
        pay.setAmountFen(10);order.setPayStatus(0);assertTrue(run());assertEquals(0,inserts);
    }
    @Test void oldLeaseOwnerAndOldRevisionAreBothRejected() {
        assertFalse(service.generate(3L,"old-worker",1L));task.setLeaseVersion(2L);
        assertFalse(run());assertEquals(0,inserts);
    }
    @Test void expiredLeaseIsRejected() {
        task.setLeaseUntil(now);assertFalse(run());assertEquals(0,inserts);
    }
    @Test void existingSuccessfulReportIsReusedWithoutReRendering() {
        assertTrue(run());String text=stored.getContentText();
        version.setContentText("should never replace a saved report");
        assertTrue(run());assertEquals(1,inserts);assertEquals(text,stored.getContentText());
    }
    @Test void unknownVariablesBlockWithoutChangingPayment() {
        version.setContentText("{{unknown}}");assertTrue(run());assertEquals("TEMPLATE_INVALID",error);
        assertEquals(4,resultStatus);assertEquals(0,inserts);assertEquals(1,order.getPayStatus());
    }
    @Test void historicalMissingSnapshotDoesNotUseLatestTemplate() {
        order.setReportInputVersion(null);assertTrue(run());assertEquals("SNAPSHOT_MISSING",error);assertEquals(0,inserts);
    }
    @Test void leaseLossDuringFinalWriteThrowsForTransactionRollback() {
        loseLease=true;assertThrows(BusinessException.class,this::run);
    }
    @Test void nonReportTasksRemainUntouched() {
        task.setTaskType(2);assertFalse(run());assertEquals(0,resultStatus);assertEquals(0,inserts);
    }
    boolean run(){return service.generate(3L,"test-lease",1L);}
    void inject(String name,Object value)throws Exception {var f=service.getClass().getDeclaredField(name);f.setAccessible(true);f.set(service,value);}
    interface Handler{Object invoke(String name,Object[] args);}
    @SuppressWarnings("unchecked") static <T>T fake(Class<T> type,Handler h){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->h.invoke(m.getName(),a));}
}
