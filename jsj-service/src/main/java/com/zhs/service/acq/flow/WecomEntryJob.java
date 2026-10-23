package com.zhs.service.acq.flow;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.*;
import jakarta.annotation.Resource;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import me.chanjar.weixin.cp.bean.external.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;

/** An uncertain create is never automatically sent again. Reconcile it before operator retry. */
@Service
public class WecomEntryJob {
    private final TransactionTemplate transaction;
    public WecomEntryJob(@Qualifier("acqTransactionManager") PlatformTransactionManager manager){transaction=new TransactionTemplate(manager);}
    @Resource private ReportWorkMapper locks;
    @Resource private BusinessFlowMapper flow;
    @Resource private WecomContactEntryMapper entries;
    @Resource private WecomCorpMapper corps;
    @Resource private WecomTeacherMapper teachers;
    @Resource private PlatformSecrets secrets;
    @Resource private PostPaymentPreparationService local;
    @Value("${acq.wecom.outbound.enabled:false}") private boolean enabled;
    private record Request(WecomContactEntry entry,WxCpServiceImpl client){}
    public void run(Long id,String owner,Long version){
        var task=locks.task(id);if(task==null)return;var order=locks.lockOrder(task.getOrderNo());
        if(order!=null&&Objects.equals(order.getEntryType(),2)){local.prepare(id,owner,version);return;}
        Request request=transaction.execute(status->{
            var locked=locks.lockTask(id);if(!valid(locked,owner,version))return null;
            var o=locks.lockOrder(locked.getOrderNo());
            if(!com.zhs.service.acq.report.ReportGenerationServiceImpl.paid(o,locks.lockPayment(locked.getOrderNo()))){finish(id,owner,version,4,"PAYMENT_INELIGIBLE");return null;}
            if(!enabled){finish(id,owner,version,4,"WECOM_OUTBOUND_DISABLED");return null;}
            var corp=corps.lockById(o.getCorpRowId());var teacher=teachers.lockById(o.getTeacherRowId());
            if(corp==null||teacher==null||!Objects.equals(corp.getStatus(),1)||!Objects.equals(teacher.getStatus(),1)||!Objects.equals(teacher.getCorpRowId(),corp.getId())||!Objects.equals(corp.getCorpId(),o.getCorpId())||!Objects.equals(teacher.getTeacherUserid(),o.getTeacherUserid())||!"SELF_BUILT".equals(corp.getAuthMode())){finish(id,owner,version,4,"WECOM_CONFIGURATION_INELIGIBLE");return null;}
            var config=new WxCpDefaultConfigImpl();config.setCorpId(corp.getCorpId());
            try{config.setCorpSecret(secrets.resolve(corp.getContactSecretRef()));}catch(BusinessException e){finish(id,owner,version,4,"WECOM_CREDENTIAL_REQUIRED");return null;}
            var client=new WxCpServiceImpl();client.setWxCpConfigStorage(config);client.setMaxRetryTimes(0);
            var entry=entries.selectOne(new LambdaQueryWrapper<WecomContactEntry>().eq(WecomContactEntry::getOrderNo,o.getOrderNo()));
            if(entry!=null){finish(id,owner,version,Objects.equals(entry.getStatus(),1)?2:4,Objects.equals(entry.getStatus(),1)?null:"ENTRY_RESULT_REQUIRES_RECONCILIATION");return null;}
            entry=new WecomContactEntry();entry.setEntryType(1);entry.setOrderNo(o.getOrderNo());entry.setCorpRowId(o.getCorpRowId());entry.setCorpId(o.getCorpId());entry.setTeacherUserid(o.getTeacherUserid());entry.setStateToken(FlowSecurity.token().substring(0,24));entry.setStatus(4);entries.insert(entry);
            return new Request(entry,client);
        });
        if(request==null)return;
        try{
            var detail=new WxCpContactWayInfo.ContactWay();detail.setType(WxCpContactWayInfo.TYPE.SINGLE);detail.setScene(WxCpContactWayInfo.SCENE.QRCODE);detail.setState(request.entry().getStateToken());detail.setUsers(List.of(request.entry().getTeacherUserid()));detail.setSkipVerify(true);
            var input=new WxCpContactWayInfo();input.setContactWay(detail);var result=request.client().getExternalContactService().addContactWay(input);
            if(result.getConfigId()==null||result.getQrCode()==null)throw new BusinessException("企微未返回入口");
            transaction.executeWithoutResult(status->{if(!valid(locks.lockTask(id),owner,version))throw new BusinessException("任务租约失效");var entry=request.entry();entry.setConfigId(result.getConfigId());entry.setQrCodeUrl(com.zhs.service.acq.ConfigRules.link(result.getQrCode()));entry.setStatus(1);entries.updateById(entry);finish(id,owner,version,2,null);});
        }catch(Exception e){transaction.executeWithoutResult(status->{if(valid(locks.lockTask(id),owner,version))finish(id,owner,version,4,"ENTRY_RESULT_UNKNOWN");});}
    }
    private boolean valid(PostPayTask task,String owner,Long version){return task!=null&&Objects.equals(task.getTaskType(),2)&&Objects.equals(task.getStatus(),1)&&Objects.equals(task.getLeaseOwner(),owner)&&Objects.equals(task.getLeaseVersion(),version)&&task.getLeaseUntil()!=null&&task.getLeaseUntil().isAfter(locks.now());}
    private void finish(Long id,String owner,Long version,int state,String error){if(flow.finish(id,owner,version,state,error)!=1)throw new BusinessException("任务租约失效");}
}
