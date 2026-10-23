package com.zhs.service.acq.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.*;
import com.zhs.service.acq.dto.*;
import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.AdAccount;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.zhs.service.acq.ConfigViews.of;

@Service
@Transactional(transactionManager="acqTransactionManager",rollbackFor=Exception.class)
public class AdAccountAdminServiceImpl implements AdAccountAdminService {
    @Resource private AdAccountMapper accounts;
    @Resource private ConfigReferenceMapper refs;
    @Resource private com.fasterxml.jackson.databind.ObjectMapper json;
    public AcqPage<Map<String,Object>> page(ConfigCommands.Query q){
        var w=new LambdaQueryWrapper<AdAccount>();
        if(q.getKeyword()!=null)w.like(AdAccount::getAccountName,q.getKeyword());
        if(q.getPlatform()!=null)w.eq(AdAccount::getPlatform,q.getPlatform());
        if(q.getStatus()!=null)w.eq(AdAccount::getStatus,q.getStatus());
        var p=accounts.selectPage(new Page<>(AcqTexts.pageNum(q.getPageNum()),AcqTexts.pageSize(q.getPageSize())),w.orderByDesc(AdAccount::getId));
        return new AcqPage<>(p.getTotal(),p.getRecords().stream().map(this::view).toList());
    }
    public Map<String,Object> detail(Long id,boolean secrets){var a=require(id);var row=view(a);if(secrets)row.put("credentialRef",a.getCredentialRef());return row;}
    public Long save(Long id,ConfigCommands.AdAccount c){
        if(c==null)throw new BusinessException("请填写广告账户");var a=id==null?new AdAccount():require(id);
        int platform=AcqTexts.requireChoice(c.platform(),"平台",1,2,3);String identity=ConfigRules.id(c.externalAccountId(),"账户ID");
        boolean used=id!=null&&refs.ad(id)>0;ConfigRules.unchanged(a.getPlatform(),platform,used,"平台");ConfigRules.unchanged(a.getExternalAccountId(),identity,used,"账户ID");
        String mode=ConfigRules.choice(c.interfaceMode(),"接口模式",platform==1?Set.of("UNCONFIRMED","TENCENT_MARKETING","TENCENT_CONVERSION"):platform==2?Set.of("UNCONFIRMED","BAIDU_CONVERSION"):Set.of("UNCONFIRMED","KUAISHOU_CONVERSION"));
        var mappings=json.createObjectNode();
        mappings.put("submit",AcqTexts.optionalText(c.submitEvent(),"提交事件",64));mappings.put("addContact",AcqTexts.optionalText(c.addContactEvent(),"加老师事件",64));a.setEventMap(mappings.toString());
        a.setPlatform(platform);a.setExternalAccountId(identity);a.setAccountName(AcqTexts.requireText(c.accountName(),"账户名称",128));a.setInterfaceMode(mode);
        a.setDataSourceId(AcqTexts.optionalText(c.dataSourceId(),"数据源标识",128));a.setPayEvent(AcqTexts.optionalText(c.payEvent(),"支付事件映射",128));
        a.setAmountUnit(ConfigRules.choice(c.amountUnit(),"金额单位",Set.of("UNCONFIRMED","FEN","YUAN")));a.setRemark(AcqTexts.optionalText(c.remark(),"备注",255));
        try{if(id==null){a.setStatus(0);accounts.insert(a);}else accounts.update(null,new LambdaUpdateWrapper<AdAccount>().eq(AdAccount::getId,id).set(AdAccount::getPlatform,platform).set(AdAccount::getExternalAccountId,identity).set(AdAccount::getAccountName,a.getAccountName()).set(AdAccount::getInterfaceMode,mode).set(AdAccount::getDataSourceId,a.getDataSourceId()).set(AdAccount::getPayEvent,a.getPayEvent()).set(AdAccount::getEventMap,a.getEventMap()).set(AdAccount::getAmountUnit,a.getAmountUnit()).set(AdAccount::getRemark,a.getRemark()));}
        catch(RuntimeException e){if(AcqDuplicate.isDuplicate(e))throw new BusinessException("该平台账户已存在，包括历史删除记录");throw e;}return a.getId();
    }
    public void secret(Long id,ConfigCommands.AdSecret c){require(id);if(c==null)throw new BusinessException("请填写引用");String ref=AcqTexts.optionalText(c.credentialRef(),"授权引用",128);SecretRefGuard.requireRef("授权引用",ref);accounts.update(null,new LambdaUpdateWrapper<AdAccount>().eq(AdAccount::getId,id).set(AdAccount::getCredentialRef,ref));}
    public void status(Long id,Integer status){require(id);accounts.update(null,new LambdaUpdateWrapper<AdAccount>().eq(AdAccount::getId,id).set(AdAccount::getStatus,AcqTexts.requireFlag(status,"状态")));}
    public void delete(Long id){require(id);if(refs.ad(id)>0)throw new BusinessException("账户已有点击或转化引用，请停用");accounts.deleteById(id);}
    private AdAccount require(Long id){var a=id==null?null:accounts.lockById(id);if(a==null)throw new BusinessException("广告账户不存在");return a;}
    private String mapping(AdAccount a,String key){try{return a.getEventMap()==null?null:json.readTree(a.getEventMap()).path(key).asText(null);}catch(Exception e){return null;}}
    private Map<String,Object> view(AdAccount a){return of("id",a.getId(),"platform",a.getPlatform(),"externalAccountId",a.getExternalAccountId(),"accountName",a.getAccountName(),"interfaceMode",a.getInterfaceMode(),"dataSourceId",a.getDataSourceId(),"payEvent",a.getPayEvent(),"submitEvent",mapping(a,"submit"),"addContactEvent",mapping(a,"addContact"),"amountUnit",a.getAmountUnit(),"remark",a.getRemark(),"status",a.getStatus(),"referenceRegistered",a.getCredentialRef()!=null);}
}
