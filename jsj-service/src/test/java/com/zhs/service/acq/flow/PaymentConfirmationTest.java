package com.zhs.service.acq.flow;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.TradeOrderService;
import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PaymentConfirmationTest {
    final LocalDateTime now=LocalDateTime.of(2026,10,23,1,0);PaymentConfirmationService service;
    TradeOrder order;PayRecord pay;int tasks,eventCount;Map<String,Object> evidence;
    @BeforeEach void setup()throws Exception{
        service=new PaymentConfirmationService();order=new TradeOrder();order.setId(1L);order.setOrderNo("order");order.setPayStatus(0);order.setAmountFen(10);order.setMchId("m");order.setAppId("a");
        pay=new PayRecord();pay.setOrderNo("order");pay.setPayStatus(0);pay.setAmountFen(10);pay.setMchId("m");pay.setAppId("a");
        set("locks",fake(ReportWorkMapper.class,(m,a)->switch(m){case "lockOrder"->order;case "lockPayment"->pay;case "now"->now;default->throw new AssertionError(m);}));
        set("flow",fake(BusinessFlowMapper.class,(m,a)->switch(m){case "evidence"->{evidence=Map.of("orderNo",a[0],"transactionId",a[4],"evidenceHash",a[3]);yield 1;}case "evidenceRow"->evidence;case "task"->{tasks++;yield 1;}default->throw new AssertionError(m);}));
        set("payments",fake(PayRecordMapper.class,(m,a)->1));
        set("orders",fake(TradeOrderService.class,(m,a)->{order.setPayStatus(1);order.setPaidTime(now);return true;}));
        set("events",new AdEventService(){@Override public void register(int t,String s,Long id,String no,Long click,Integer amount){eventCount++;}});
    }
    PaymentConfirmationService.Evidence event(int amount,String currency,String tx){return new PaymentConfirmationService.Evidence(PaymentConfirmationService.Source.QUERY,tx,"a".repeat(64),"order",tx,"m","a",amount,currency,"SUCCESS",now);}
    @Test void queryUsesNoFakeNotificationAndRegistersAllIndependentTasks(){assertTrue(service.confirm(event(10,"CNY","tx")));assertNull(pay.getNotifyId());assertEquals("QUERY",pay.getConfirmationSource());assertEquals(3,tasks);assertEquals(1,eventCount);assertFalse(service.confirm(event(10,"CNY","tx")));}
    @Test void mismatchedFactsCannotAdvancePayment(){assertThrows(BusinessException.class,()->service.confirm(event(11,"CNY","tx")));assertThrows(BusinessException.class,()->service.confirm(event(10,"USD","tx")));assertEquals(0,tasks);assertEquals(0,pay.getPayStatus());}
    @Test void closedOrRefundedOrdersCannotBeResurrected(){order.setPayStatus(2);assertThrows(BusinessException.class,()->service.confirm(event(10,"CNY","tx")));order.setPayStatus(3);assertThrows(BusinessException.class,()->service.confirm(event(10,"CNY","tx")));}
    @Test void conflictingTransactionFailsAfterSuccess(){service.confirm(event(10,"CNY","tx"));assertThrows(BusinessException.class,()->service.confirm(event(10,"CNY","other")));}
    @Test void lateSuccessAfterConfirmedRefundDoesNotRestoreEntitlementOrTasks(){service.confirm(event(10,"CNY","tx"));order.setPayStatus(2);pay.setPayStatus(3);assertFalse(service.confirm(event(10,"CNY","tx")));assertEquals(2,order.getPayStatus());assertEquals(3,pay.getPayStatus());assertEquals(3,tasks);}
    void set(String name,Object value)throws Exception{var f=service.getClass().getDeclaredField(name);f.setAccessible(true);f.set(service,value);}
    interface Call{Object invoke(String name,Object[] args);}
    @SuppressWarnings("unchecked") static <T>T fake(Class<T> type,Call call){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(o,m,a)->call.invoke(m.getName(),a));}
}
