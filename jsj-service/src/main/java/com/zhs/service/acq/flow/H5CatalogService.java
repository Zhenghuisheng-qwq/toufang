package com.zhs.service.acq.flow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.ConfigRules;
import com.zhs.service.acq.DomainNames;
import com.zhs.service.acq.RouteEligibility;
import com.zhs.service.acq.report.PlainTextReportRenderer;
import com.zhs.service.mapper.acq.BusinessFlowMapper;
import com.zhs.service.mapper.acq.LandingAssetMapper;
import com.zhs.service.mapper.acq.PayMerchantVersionMapper;
import com.zhs.service.mapper.acq.ReportTemplateMapper;
import com.zhs.service.mapper.acq.ReportTemplateVersionMapper;
import com.zhs.service.mapper.acq.WecomCorpMapper;
import com.zhs.service.mapper.acq.WecomTeacherMapper;
import com.zhs.service.model.acq.LandingAsset;
import com.zhs.service.model.acq.LandingDomain;
import com.zhs.service.model.acq.LandingPage;
import com.zhs.service.model.acq.PayMerchant;
import com.zhs.service.model.acq.PayMerchantVersion;
import com.zhs.service.model.acq.ReportTemplate;
import com.zhs.service.model.acq.ReportTemplateVersion;
import com.zhs.service.model.acq.SiteDomain;
import com.zhs.service.model.acq.WecomRoute;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class H5CatalogService {

    @Resource
    private BusinessFlowMapper flow;

    @Resource
    private LandingAssetMapper assets;

    @Resource
    private PayMerchantVersionMapper versions;

    @Resource
    private ReportTemplateMapper templates;

    @Resource
    private ReportTemplateVersionMapper reportVersions;

    @Resource
    private WecomCorpMapper corps;

    @Resource
    private WecomTeacherMapper teachers;

    @Resource
    private PlainTextReportRenderer renderer;

    public record Page(String id, String title, String headline, String sellingText, String buttonText, int priceFen,
                       boolean nameEnabled, boolean genderEnabled, boolean birthDateEnabled, boolean birthTimeEnabled,
                       List<String> images) {
    }

    public record Configuration(LandingPage page, SiteDomain domain, PayMerchant merchant, PayMerchantVersion version,
                                WecomRoute route) {
    }

    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public Page publicPage(Long id, String host) {
        LandingPage page = H5LoopbackPreview.active() ? published(id) : resolve(id, host).page();
        List<String> images = assets.selectList(new LambdaQueryWrapper<LandingAsset>()
                        .eq(LandingAsset::getLandingPageId, id)
                        .eq(LandingAsset::getStatus, 1)
                        .orderByAsc(LandingAsset::getSortNo))
                .stream()
                .map(LandingAsset::getAssetUrl)
                .toList();
        return new Page(page.getId().toString(), page.getTitle(), page.getHeadline(), page.getSellingText(),
                page.getButtonText(), page.getPriceFen(), on(page.getNameEnabled()), on(page.getGenderEnabled()),
                on(page.getBirthDateEnabled()), on(page.getBirthTimeEnabled()), images);
    }

    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public LandingPage published(Long id) {
        LandingPage page = flow.page(id);
        if (page == null) {
            throw new BusinessException("页面不存在");
        }
        if (!on(page.getPublishStatus())) {
            throw new BusinessException("页面暂未开放");
        }
        if (page.getPriceFen() == null || page.getPriceFen() < 1) {
            throw new BusinessException("页面价格尚未配置");
        }
        return page;
    }

    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public SiteDomain domain(String host) {
        SiteDomain domain = flow.domain(DomainNames.canonicalize(host));
        if (domain == null || !on(domain.getStatus()) || !on(domain.getVerifyStatus())) {
            throw new BusinessException("访问域名未启用或未核验");
        }
        return domain;
    }

    /** Caller and this proxied method participate in the same short acquisition transaction. */
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public Configuration resolve(Long id, String host) {
        LandingPage page = flow.page(id);
        if (page == null) {
            throw new BusinessException("页面不存在");
        }
        if (!on(page.getPublishStatus())) {
            throw new BusinessException("页面暂未开放");
        }
        SiteDomain domain = domain(host);
        LandingDomain relation = flow.pageDomain(id, domain.getId());
        if (relation == null || !on(relation.getStatus())) {
            throw new BusinessException("页面未关联当前域名");
        }
        if (page.getPriceFen() == null || page.getPriceFen() < 1) {
            throw new BusinessException("页面价格尚未配置");
        }
        List<PayMerchant> merchants = flow.merchants(domain.getId());
        if (page.getPayMerchantId() != null) {
            merchants = merchants.stream()
                    .filter(merchant -> Objects.equals(merchant.getId(), page.getPayMerchantId()))
                    .toList();
        }
        if (merchants.size() != 1) {
            throw new BusinessException("域名必须有唯一的已核验承接商户，请检查商户关联");
        }
        PayMerchant merchant = merchants.getFirst();
        PayMerchantVersion version = merchant.getCurrentVersionId() == null
                ? null : versions.lockById(merchant.getCurrentVersionId());
        if (version == null || !on(version.getStatus()) || !Objects.equals(version.getPayMerchantId(), merchant.getId())
                || !ConfigRules.complete(version)) {
            throw new BusinessException("商户缺少可用配置版本");
        }
        ReportTemplate template = page.getReportTemplateId() == null ? null : templates.lockById(page.getReportTemplateId());
        ReportTemplateVersion reportVersion = template == null || template.getCurrentVersionId() == null
                ? null : reportVersions.lockById(template.getCurrentVersionId());
        if (template == null || !on(template.getStatus()) || reportVersion == null || !on(reportVersion.getStatus())
                || !Objects.equals(reportVersion.getTemplateId(), template.getId())) {
            throw new BusinessException("页面缺少已发布报告模板");
        }
        renderer.validate(reportVersion.getContentType(), reportVersion.getContentText());
        if ((reportVersion.getContentText().contains("{{name}}") && !on(page.getNameEnabled()))
                || (reportVersion.getContentText().contains("{{gender}}") && !on(page.getGenderEnabled()))
                || (reportVersion.getContentText().contains("{{birthDate}}") && !on(page.getBirthDateEnabled()))) {
            throw new BusinessException("报告模板需要的资料字段未启用");
        }
        List<WecomRoute> all = flow.routes(id);
        List<WecomRoute> specific = all.stream()
                .filter(route -> Objects.equals(route.getLandingPageId(), id) && Objects.equals(route.getScopeType(), 2))
                .toList();
        List<WecomRoute> scope = specific.isEmpty()
                ? all.stream().filter(route -> Objects.equals(route.getScopeType(), 1)).toList()
                : specific;
        List<WecomRoute> active = scope.stream().filter(route -> on(route.getStatus())).toList();
        if (active.size() != 1) {
            throw new BusinessException("指定承接配置缺失或停用，不会自动更换企业");
        }
        WecomRoute route = active.getFirst();
        RouteEligibility.require(route, corps.lockById(route.getCorpRowId()), teachers.lockById(route.getTeacherRowId()),
                route.getLandingPageId() == null ? null : page);
        return new Configuration(page, domain, merchant, version, route);
    }

    private static boolean on(Integer flag) {
        return Integer.valueOf(1).equals(flag);
    }
}
