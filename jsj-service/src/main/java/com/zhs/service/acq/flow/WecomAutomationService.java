package com.zhs.service.acq.flow;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.*;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import me.chanjar.weixin.cp.bean.external.WxCpUpdateRemarkRequest;
import java.util.*;

@Service
public class WecomAutomationService {
    private final org.springframework.transaction.support.TransactionTemplate transaction;
    public WecomAutomationService(@org.springframework.beans.factory.annotation.Qualifier("acqTransactionManager") org.springframework.transaction.PlatformTransactionManager manager){transaction=new org.springframework.transaction.support.TransactionTemplate(manager);}
    @Resource private WecomActionMapper actions;
    @Resource private WecomCorpMapper corps;
    @Resource private WecomTagMapper tags;
    @Resource private PlatformSecrets secrets;
    @Resource private Environment env;
    @Resource private ObjectMapper json;
    @Transactional(transactionManager="acqTransactionManager",propagation=Propagation.MANDATORY)
    public void register(WecomCorp corp,WecomContactEvent event){
        String root="acq.wecom.automation."+corp.getCorpId()+".";
        String policy=env.getProperty(root+"remark-policy","UNCONFIRMED");String remark=env.getProperty(root+"remark");
        if("EMPTY_ONLY".equals(policy)&&remark!=null&&!remark.isBlank()&&remark.length()<=20&&!remark.codePoints().anyMatch(Character::isISOControl)){
            actions.register(event.getId(),corp.getId(),corp.getCorpId(),event.getExternalUserid(),event.getTeacherUserid(),"REMARK",json.createObjectNode().put("policy",policy).put("remark",remark).toString());
            event.setRemarkStatus(0);
        }
        String configured=env.getProperty(root+"tag-ids","");
        if(!configured.isBlank()){
            var ids=Arrays.stream(configured.split(",")).map(String::trim).filter(s->!s.isEmpty()).distinct().toList();
            if(!ids.isEmpty()&&ids.size()<=20&&tags.selectCount(new LambdaQueryWrapper<WecomTag>().eq(WecomTag::getCorpId,corp.getCorpId()).eq(WecomTag::getStatus,1).in(WecomTag::getTagId,ids))==ids.size()){
                var payload=json.createObjectNode();payload.set("tags",json.valueToTree(ids));actions.register(event.getId(),corp.getId(),corp.getCorpId(),event.getExternalUserid(),event.getTeacherUserid(),"TAG",payload.toString());event.setTagStatus(0);
            }
        }
        if(Objects.equals(event.getRemarkStatus(),0)&&Objects.equals(event.getTagStatus(),0))event.setLastError(null);
        else if(Objects.equals(event.getRemarkStatus(),0)||Objects.equals(event.getTagStatus(),0))event.setLastError("AUTOMATION_RULES_PARTIAL");
    }
    public void run(Long id,String owner){
        if(!env.getProperty("acq.wecom.outbound.enabled",Boolean.class,false))return;
        if(actions.claim(id,owner)!=1)return;var action=actions.action(id);if(action==null)return;
        long version=((Number)action.get("lease_version")).longValue();int status=5;String error="CONFIGURATION_REQUIRED";
        try{
            String corpId=(String)action.get("corp_id"),external=(String)action.get("external_userid"),teacher=(String)action.get("teacher_userid");
            var corp=corps.selectById(((Number)action.get("corp_row_id")).longValue());
            if(corp==null||!Objects.equals(corp.getCorpId(),corpId)||!Objects.equals(corp.getStatus(),1)||!"SELF_BUILT".equals(corp.getAuthMode())||actions.active(corpId,external,teacher)!=1){error="RELATION_INELIGIBLE";}
            else{
                var config=new WxCpDefaultConfigImpl();config.setCorpId(corpId);config.setCorpSecret(secrets.resolve(corp.getContactSecretRef()));var client=new WxCpServiceImpl();client.setWxCpConfigStorage(config);client.setMaxRetryTimes(0);var api=client.getExternalContactService();var payload=json.readTree((String)action.get("payload"));
                if("REMARK".equals(action.get("action_type"))&&"EMPTY_ONLY".equals(payload.path("policy").asText())){
                    var customer=api.getContactDetail(external,null);var follows=customer.getFollowedUsers();var follow=follows==null?null:follows.stream().filter(f->teacher.equals(f.getUserId())).findFirst().orElse(null);
                    if(follow==null){status=5;error="TEACHER_RELATION_NOT_VISIBLE";}
                    else if(follow.getRemark()!=null&&!follow.getRemark().isBlank()){status=2;error="EXISTING_REMARK_PRESERVED";}
                    else{api.updateRemark(new WxCpUpdateRemarkRequest().setUserId(teacher).setExternalUserId(external).setRemark(payload.path("remark").asText()));status=1;error=null;}
                }else if("TAG".equals(action.get("action_type"))){
                    var ids=new ArrayList<String>();payload.path("tags").forEach(n->ids.add(n.asText()));
                    if(ids.isEmpty()||tags.selectCount(new LambdaQueryWrapper<WecomTag>().eq(WecomTag::getCorpId,corpId).eq(WecomTag::getStatus,1).in(WecomTag::getTagId,ids))!=ids.size()){error="TAG_SCOPE_CHANGED";}
                    else{api.markTag(teacher,external,ids.toArray(String[]::new),new String[0]);status=1;error=null;}
                }
            }
        }catch(com.zhs.common.exception.BusinessException e){error="CREDENTIAL_REQUIRED";}
        catch(Exception e){status=4;error="RESULT_UNKNOWN";}
        final int resultStatus=status;final String resultError=error;
        transaction.executeWithoutResult(tx->{if(actions.finish(id,owner,version,resultStatus,resultError)==1)actions.eventStatus(((Number)action.get("event_id")).longValue(),(String)action.get("action_type"),resultStatus==1?1:resultStatus==2?2:3);});
    }
}
