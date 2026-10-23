package com.zhs.service.acq.flow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.*;
import com.zhs.service.acq.report.PlainTextReportRenderer;
import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class H5CatalogService {
    @Resource private BusinessFlowMapper flow;
    @Resource private LandingAssetMapper assets;
    @Resource private PayMerchantVersionMapper versions;
    @Resource private ReportTemplateMapper templates;
    @Resource private ReportTemplateVersionMapper reportVersions;
    @Resource private WecomCorpMapper corps;
    @Resource private WecomTeacherMapper teachers;
    @Resource private PlainTextReportRenderer renderer;
    public record Page(String id,String title,String headline,String sellingText,String buttonText,int priceFen,
                       boolean nameEnabled,boolean genderEnabled,boolean birthDateEnabled,boolean birthTimeEnabled,List<String> images) { }
    public record Configuration(LandingPage page,SiteDomain domain,PayMerchant merchant,PayMerchantVersion version,WecomRoute route) { }

    @Transactional(transactionManager="acqTransactionManager",rollbackFor=Exception.class)
    public Page publicPage(Long id,String host) {
        var c=resolve(id,host);var p=c.page();
        var images=assets.selectList(new LambdaQueryWrapper<LandingAsset>().eq(LandingAsset::getLandingPageId,id)
                .eq(LandingAsset::getStatus,1).orderByAsc(LandingAsset::getSortNo)).stream().map(LandingAsset::getAssetUrl).toList();
        return new Page(p.getId().toString(),p.getTitle(),p.getHeadline(),p.getSellingText(),p.getButtonText(),p.getPriceFen(),
                on(p.getNameEnabled()),on(p.getGenderEnabled()),on(p.getBirthDateEnabled()),on(p.getBirthTimeEnabled()),images);
    }
    @Transactional(transactionManager="acqTransactionManager",rollbackFor=Exception.class)
    public SiteDomain domain(String host) {
        var d=flow.domain(DomainNames.canonicalize(host));
        if(d==null||!on(d.getStatus())||!on(d.getVerifyStatus())) throw new BusinessException("访问域名未启用或未核验");
        return d;
    }
    /** Caller and this proxied method participate in the same short acquisition transaction. */
    @Transactional(transactionManager="acqTransactionManager",rollbackFor=Exception.class)
    public Configuration resolve(Long id,String host) {
        var p=flow.page(id);if(p==null)throw new BusinessException("页面不存在");
        if(!on(p.getPublishStatus()))throw new BusinessException("页面暂未开放");
        var d=domain(host);var relation=flow.pageDomain(id,d.getId());
        if(relation==null||!on(relation.getStatus()))throw new BusinessException("页面未关联当前域名");
        if(p.getPriceFen()==null||p.getPriceFen()<1)throw new BusinessException("页面价格尚未配置");
        var merchants=flow.merchants(d.getId());
        if(p.getPayMerchantId()!=null)merchants=merchants.stream().filter(m->Objects.equals(m.getId(),p.getPayMerchantId())).toList();
        if(merchants.size()!=1)throw new BusinessException("域名必须有唯一的已核验承接商户，请检查商户关联");
        var merchant=merchants.getFirst();var v=merchant.getCurrentVersionId()==null?null:versions.lockById(merchant.getCurrentVersionId());
        if(v==null||!on(v.getStatus())||!Objects.equals(v.getPayMerchantId(),merchant.getId())||!ConfigRules.complete(v))throw new BusinessException("商户缺少可用配置版本");
        var template=p.getReportTemplateId()==null?null:templates.lockById(p.getReportTemplateId());
        var rv=template==null||template.getCurrentVersionId()==null?null:reportVersions.lockById(template.getCurrentVersionId());
        if(template==null||!on(template.getStatus())||rv==null||!on(rv.getStatus())||!Objects.equals(rv.getTemplateId(),template.getId()))throw new BusinessException("页面缺少已发布报告模板");
        renderer.validate(rv.getContentType(),rv.getContentText());
        if((rv.getContentText().contains("{{name}}")&&!on(p.getNameEnabled()))||(rv.getContentText().contains("{{gender}}")&&!on(p.getGenderEnabled()))
                ||(rv.getContentText().contains("{{birthDate}}")&&!on(p.getBirthDateEnabled())))throw new BusinessException("报告模板需要的资料字段未启用");
        var all=flow.routes(id);var specific=all.stream().filter(r->Objects.equals(r.getLandingPageId(),id)&&Objects.equals(r.getScopeType(),2)).toList();
        var scope=specific.isEmpty()?all.stream().filter(r->Objects.equals(r.getScopeType(),1)).toList():specific;
        var active=scope.stream().filter(r->on(r.getStatus())).toList();
        if(active.size()!=1)throw new BusinessException("指定承接配置缺失或停用，不会自动更换企业");
        var route=active.getFirst();RouteEligibility.require(route,corps.lockById(route.getCorpRowId()),teachers.lockById(route.getTeacherRowId()),route.getLandingPageId()==null?null:p);
        return new Configuration(p,d,merchant,v,route);
    }
    private static boolean on(Integer flag){return Integer.valueOf(1).equals(flag);}
}
