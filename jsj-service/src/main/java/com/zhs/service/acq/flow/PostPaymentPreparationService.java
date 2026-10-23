package com.zhs.service.acq.flow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.report.ReportGenerationServiceImpl;
import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

/** Local task preparation only. No external service is called while holding a database lock. */
@Service
public class PostPaymentPreparationService {
    @Resource private BusinessFlowMapper flow;
    @Resource private ReportWorkMapper locks;
    @Resource private WecomContactEntryMapper entries;
    @Resource private AdEventService events;
    @Transactional(transactionManager="acqTransactionManager",rollbackFor=Exception.class)
    public void prepare(Long id,String owner,Long version){
        var task=locks.lockTask(id);
        if(task==null||!Objects.equals(task.getStatus(),1)||!Objects.equals(task.getLeaseOwner(),owner)||!Objects.equals(task.getLeaseVersion(),version)||!task.getLeaseUntil().isAfter(locks.now()))return;
        var o=locks.lockOrder(task.getOrderNo());var p=locks.lockPayment(task.getOrderNo());int status=2;String error=null;
        if(!ReportGenerationServiceImpl.paid(o,p)){status=4;error="PAYMENT_INELIGIBLE";}
        else if(Objects.equals(task.getTaskType(),3)){events.register(1,"ORDER",o.getId(),o.getOrderNo(),o.getAdClickId(),o.getAmountFen());}
        else if(Objects.equals(task.getTaskType(),2)){
            var existing=entries.selectOne(new LambdaQueryWrapper<WecomContactEntry>().eq(WecomContactEntry::getOrderNo,o.getOrderNo()));
            if(existing==null){
                if(Objects.equals(o.getEntryType(),2)&&o.getEntryLinkUrl()!=null){
                    var entry=new WecomContactEntry();entry.setOrderNo(o.getOrderNo());entry.setEntryType(2);entry.setCorpRowId(o.getCorpRowId());entry.setCorpId(o.getCorpId());entry.setTeacherUserid(o.getTeacherUserid());
                    entry.setLinkId(o.getEntryLinkId());entry.setLinkUrl(com.zhs.service.acq.ConfigRules.link(o.getEntryLinkUrl()));entry.setStatus(1);entries.insert(entry);
                }else{status=4;error="DYNAMIC_WECOM_ENTRY_NOT_IMPLEMENTED";}
            }
        }else throw new BusinessException("任务类型不匹配");
        if(flow.finish(id,owner,version,status,error)!=1)throw new BusinessException("任务租约失效");
    }
}
