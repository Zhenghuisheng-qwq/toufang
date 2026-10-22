package com.zhs.service.acq.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.*;
import com.zhs.service.acq.dto.*;
import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.zhs.service.acq.ConfigViews.of;

@Service
@Transactional(transactionManager="acqTransactionManager", rollbackFor=Exception.class)
public class MerchantAdminServiceImpl implements MerchantAdminService {
    @Resource private PayMerchantMapper merchants;
    @Resource private PayMerchantVersionMapper versions;
    @Resource private DomainMerchantMapper links;
    @Resource private SiteDomainMapper domains;
    @Resource private ConfigReferenceMapper refs;
    @Resource private PayMerchantVersionService versionService;

    public AcqPage<Map<String,Object>> page(ConfigCommands.Query q) {
        var w=new LambdaQueryWrapper<PayMerchant>();
        if(q.getKeyword()!=null) w.like(PayMerchant::getMchName, q.getKeyword());
        if(q.getStatus()!=null) w.eq(PayMerchant::getStatus,q.getStatus());
        var p=merchants.selectPage(new Page<>(AcqTexts.pageNum(q.getPageNum()),AcqTexts.pageSize(q.getPageSize())),w.orderByDesc(PayMerchant::getId));
        return new AcqPage<>(p.getTotal(),p.getRecords().stream().map(this::view).toList());
    }
    public Map<String,Object> detail(Long id, boolean secrets) {
        PayMerchant m=require(id); var result=view(m);
        result.put("versions",versions.selectList(new LambdaQueryWrapper<PayMerchantVersion>().eq(PayMerchantVersion::getPayMerchantId,id).orderByDesc(PayMerchantVersion::getVersionNo)).stream().map(v->{
            var row=of("id",v.getId(),"versionNo",v.getVersionNo(),"status",v.getStatus(),"complete",ConfigRules.complete(v),"current",Objects.equals(id==null?null:m.getCurrentVersionId(),v.getId()),"remark",v.getRemark());
            if(secrets){row.put("apiV3KeyRef",v.getApiV3KeyRef());row.put("privateKeyRef",v.getPrivateKeyRef());row.put("merchantSerialNo",v.getMerchantSerialNo());row.put("verifyMode",v.getVerifyMode());row.put("platformCertRef",v.getPlatformCertRef());row.put("platformKeyId",v.getPlatformKeyId());row.put("platformPublicKeyRef",v.getPlatformPublicKeyRef());}
            return row;
        }).toList());
        result.put("domains",links.selectList(new LambdaQueryWrapper<DomainMerchant>().eq(DomainMerchant::getPayMerchantId,id)).stream().map(l->{var d=domains.selectById(l.getDomainId());return of("id",l.getDomainId(),"domainName",d==null?"已删除域名":d.getDomainName(),"verifyStatus",l.getVerifyStatus());}).toList());
        return result;
    }
    public Long save(Long id, ConfigCommands.Merchant c) {
        if(c==null) throw new BusinessException("请填写商户");
        PayMerchant m=id==null?new PayMerchant():require(id);
        String mch=AcqTexts.requireText(c.mchId(),"商户号",32),app=AcqTexts.requireText(c.appId(),"AppID",32);
        ConfigRules.id(mch,"商户号");ConfigRules.id(app,"AppID");
        boolean used=id!=null&&refs.merchant(id)>0;
        ConfigRules.unchanged(m.getMchId(),mch,used,"商户号");ConfigRules.unchanged(m.getAppId(),app,used,"AppID");
        m.setMchName(AcqTexts.requireText(c.mchName(),"商户名称",64));m.setMchId(mch);m.setAppId(app);m.setRemark(AcqTexts.optionalText(c.remark(),"备注",255));
        try {if(id==null){m.setStatus(0);merchants.insert(m);} else merchants.update(null,new LambdaUpdateWrapper<PayMerchant>().eq(PayMerchant::getId,id).set(PayMerchant::getMchName,m.getMchName()).set(PayMerchant::getMchId,mch).set(PayMerchant::getAppId,app).set(PayMerchant::getRemark,m.getRemark()));}
        catch(RuntimeException e){duplicate(e,"商户号与AppID组合已存在（包括历史删除记录），请使用原记录");}
        return m.getId();
    }
    public void status(Long id,Integer status){require(id);merchants.update(null,new LambdaUpdateWrapper<PayMerchant>().eq(PayMerchant::getId,id).set(PayMerchant::getStatus,AcqTexts.requireFlag(status,"状态")));}
    public void delete(Long id){require(id);if(refs.merchant(id)>0)throw new BusinessException("商户存在版本、域名或订单引用，请停用");merchants.deleteById(id);}
    public Long saveVersion(Long merchantId,Long versionId,ConfigCommands.MerchantVersion c){
        require(merchantId);if(c==null)throw new BusinessException("请填写版本");
        PayMerchantVersion v=versionId==null?new PayMerchantVersion():versions.selectById(versionId);
        if(v==null || (versionId!=null&&!merchantId.equals(v.getPayMerchantId())))throw new BusinessException("版本不属于该商户");
        if(versionId!=null&&(!Integer.valueOf(0).equals(v.getStatus())||refs.version(versionId)>0))throw new BusinessException("已启用或已使用版本不可覆盖，请新建版本");
        v.setApiV3KeyRef(reference("APIv3引用",c.apiV3KeyRef()));v.setPrivateKeyRef(reference("商户私钥引用",c.privateKeyRef()));
        v.setMerchantSerialNo(AcqTexts.optionalText(c.merchantSerialNo(),"商户证书序列号",128));
        v.setVerifyMode(ConfigRules.choice(c.verifyMode(),"验签模式",Set.of("UNCONFIRMED","CERTIFICATE","PUBLIC_KEY")));
        v.setPlatformCertRef(reference("平台证书引用",c.platformCertRef()));v.setPlatformPublicKeyRef(reference("平台公钥引用",c.platformPublicKeyRef()));
        v.setPlatformKeyId(AcqTexts.optionalText(c.platformKeyId(),"平台公钥ID",128));v.setRemark(AcqTexts.optionalText(c.remark(),"说明",255));
        if("CERTIFICATE".equals(v.getVerifyMode())&&(v.getPlatformPublicKeyRef()!=null||v.getPlatformKeyId()!=null))throw new BusinessException("证书模式不能填写公钥模式字段");
        if("PUBLIC_KEY".equals(v.getVerifyMode())&&v.getPlatformCertRef()!=null)throw new BusinessException("公钥模式不能填写平台证书引用");
        if(versionId==null){v.setPayMerchantId(merchantId);versionService.save(v);}else{
            versions.update(null,new LambdaUpdateWrapper<PayMerchantVersion>().eq(PayMerchantVersion::getId,versionId).eq(PayMerchantVersion::getStatus,0)
                .set(PayMerchantVersion::getApiV3KeyRef,v.getApiV3KeyRef()).set(PayMerchantVersion::getPrivateKeyRef,v.getPrivateKeyRef())
                .set(PayMerchantVersion::getMerchantSerialNo,v.getMerchantSerialNo()).set(PayMerchantVersion::getVerifyMode,v.getVerifyMode())
                .set(PayMerchantVersion::getPlatformCertRef,v.getPlatformCertRef()).set(PayMerchantVersion::getPlatformPublicKeyRef,v.getPlatformPublicKeyRef())
                .set(PayMerchantVersion::getPlatformKeyId,v.getPlatformKeyId()).set(PayMerchantVersion::getRemark,v.getRemark()));
        }return v.getId();
    }
    public void activate(Long merchantId,Long versionId){
        require(merchantId);var v=versions.selectById(versionId);
        if(v==null||!merchantId.equals(v.getPayMerchantId()))throw new BusinessException("版本不属于该商户");
        if(!ConfigRules.complete(v))throw new BusinessException("必要签名、解密或验签配置不完整");
        versions.update(null,new LambdaUpdateWrapper<PayMerchantVersion>().eq(PayMerchantVersion::getPayMerchantId,merchantId).eq(PayMerchantVersion::getStatus,1).set(PayMerchantVersion::getStatus,2));
        versions.update(null,new LambdaUpdateWrapper<PayMerchantVersion>().eq(PayMerchantVersion::getId,versionId).set(PayMerchantVersion::getStatus,1));
        merchants.update(null,new LambdaUpdateWrapper<PayMerchant>().eq(PayMerchant::getId,merchantId).set(PayMerchant::getCurrentVersionId,versionId));
    }
    public void deactivate(Long merchantId){require(merchantId);versions.update(null,new LambdaUpdateWrapper<PayMerchantVersion>().eq(PayMerchantVersion::getPayMerchantId,merchantId).eq(PayMerchantVersion::getStatus,1).set(PayMerchantVersion::getStatus,2));merchants.update(null,new LambdaUpdateWrapper<PayMerchant>().eq(PayMerchant::getId,merchantId).set(PayMerchant::getCurrentVersionId,null));}
    private PayMerchant require(Long id){var m=id==null?null:merchants.lockById(id);if(m==null)throw new BusinessException("商户不存在");return m;}
    private Map<String,Object> view(PayMerchant m){return of("id",m.getId(),"mchName",m.getMchName(),"mchId",m.getMchId(),"appId",m.getAppId(),"status",m.getStatus(),"remark",m.getRemark(),"currentVersionId",m.getCurrentVersionId());}
    private String reference(String name,String value){String ref=AcqTexts.optionalText(value,name,128);SecretRefGuard.requireRef(name,ref);return ref;}
    private void duplicate(RuntimeException e,String message){if(AcqDuplicate.isDuplicate(e))throw new BusinessException(message);throw e;}
}
