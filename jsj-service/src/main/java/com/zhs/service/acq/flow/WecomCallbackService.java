package com.zhs.service.acq.flow;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.Objects;

@Service
public class WecomCallbackService {
    public record Event(String corp,String change,String external,String teacher,String state,long epoch){@Override public String toString(){return "WecomEvent[redacted]";}}
    @Resource private WecomCorpMapper corps;
    @Resource private WecomFlowMapper flow;
    @Resource private WecomContactEventMapper logs;
    @Resource private WecomCustomerBindMapper binds;
    @Resource private ReportWorkMapper orders;
    @Resource private AdEventService ads;
    @Resource private WecomAutomationService automation;
    @Transactional(transactionManager="acqTransactionManager",rollbackFor=Exception.class)
    public void accept(Long corpId,Event input){
        var corp=corps.lockById(corpId);if(corp==null||!Objects.equals(corp.getCorpId(),input.corp()))throw new BusinessException("企微回调主体不匹配");
        int change=switch(input.change()){case "add_external_contact"->1;case "del_external_contact","del_follow_user"->2;default->0;};if(change==0)return;
        if(input.external()==null||input.external().length()>64||input.teacher()==null||input.teacher().length()>64||input.epoch()<1)throw new BusinessException("企微事件字段不完整");
        String key=FlowSecurity.hash(String.join("|",input.corp(),input.change(),input.external(),input.teacher(),String.valueOf(input.epoch()),String.valueOf(input.state())));
        if(logs.selectCount(new LambdaQueryWrapper<WecomContactEvent>().eq(WecomContactEvent::getDedupKey,key))>0)return;
        flow.customer(input.corp(),input.external());flow.relation(input.corp(),input.external(),input.teacher(),change==1?1:0,input.epoch());flow.customerStatus(input.corp(),input.external());
        var event=new WecomContactEvent();event.setDedupKey(key);event.setCorpId(input.corp());event.setChangeType(change);event.setExternalUserid(input.external());event.setTeacherUserid(input.teacher());event.setEventTime(LocalDateTime.ofInstant(Instant.ofEpochSecond(input.epoch()),ZoneId.of("Asia/Shanghai")));event.setRemarkStatus(2);event.setTagStatus(2);
        if(input.state()!=null&&input.state().length()<=30)event.setStateToken(input.state());
        var entry=change==1&&event.getStateToken()!=null?flow.entry(input.corp(),input.teacher(),event.getStateToken()):null;
        if(entry!=null){
            var order=orders.lockOrder(entry.getOrderNo());var payment=orders.lockPayment(entry.getOrderNo());
            if(order!=null&&Objects.equals(order.getCorpId(),input.corp())&&Objects.equals(order.getTeacherUserid(),input.teacher())&&com.zhs.service.acq.report.ReportGenerationServiceImpl.paid(order,payment)){
                var bound=binds.selectOne(new LambdaQueryWrapper<WecomCustomerBind>().eq(WecomCustomerBind::getOrderNo,order.getOrderNo()));
                if(bound==null){bound=new WecomCustomerBind();bound.setOrderNo(order.getOrderNo());bound.setCorpId(input.corp());bound.setExternalUserid(input.external());bound.setTeacherUserid(input.teacher());bound.setMatchType(1);bound.setStateToken(input.state());bound.setContactEntryId(entry.getId());binds.insert(bound);}
                if(Objects.equals(bound.getCorpId(),input.corp())&&Objects.equals(bound.getExternalUserid(),input.external())&&Objects.equals(bound.getTeacherUserid(),input.teacher())){
                    event.setOrderNo(order.getOrderNo());ads.register(3,"ORDER",order.getId(),order.getOrderNo(),order.getAdClickId(),null);event.setLastError("AUTOMATION_RULES_NOT_CONFIGURED");
                }else event.setLastError("BINDING_CONFLICT_REQUIRES_REVIEW");
            }else event.setLastError("ORDER_NOT_ELIGIBLE");
        }else if(change==1)event.setLastError("UNLINKED_NO_UNIQUE_STATE");
        logs.insert(event);
        if(event.getOrderNo()!=null){automation.register(corp,event);logs.updateById(event);}
    }
}
