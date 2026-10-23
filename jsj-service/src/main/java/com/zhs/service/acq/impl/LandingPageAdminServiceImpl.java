package com.zhs.service.acq.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.AcqTexts;
import com.zhs.service.acq.AcquisitionCodes;
import com.zhs.service.acq.AssetUrls;
import com.zhs.service.acq.LandingPageAdminService;
import com.zhs.service.acq.MoneyAmounts;
import com.zhs.service.acq.dto.AcqPage;
import com.zhs.service.acq.dto.LandingAssetCommand;
import com.zhs.service.acq.dto.LandingPageQuery;
import com.zhs.service.acq.dto.LandingPageSaveCommand;
import com.zhs.service.acq.dto.LandingViews;
import com.zhs.service.mapper.acq.AdAccountMapper;
import com.zhs.service.mapper.acq.LandingAssetMapper;
import com.zhs.service.mapper.acq.LandingDomainMapper;
import com.zhs.service.mapper.acq.LandingPageMapper;
import com.zhs.service.mapper.acq.PayMerchantMapper;
import com.zhs.service.mapper.acq.ReportTemplateMapper;
import com.zhs.service.model.acq.LandingAsset;
import com.zhs.service.model.acq.LandingDomain;
import com.zhs.service.model.acq.LandingPage;
import com.zhs.service.model.acq.ReportTemplate;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LandingPageAdminServiceImpl implements LandingPageAdminService {

    private static final int MAX_ASSETS = 8;

    @Resource
    private LandingPageMapper landingPageMapper;
    @Resource
    private LandingAssetMapper landingAssetMapper;
    @Resource
    private LandingDomainMapper landingDomainMapper;
    @Resource
    private ReportTemplateMapper reportTemplateMapper;

    @Resource
    private AdAccountMapper adAccounts;

    @Resource
    private PayMerchantMapper merchants;

    @Override
    public AcqPage<LandingViews.Row> page(LandingPageQuery query) {
        LambdaQueryWrapper<LandingPage> wrapper = new LambdaQueryWrapper<>();
        String title = AcqTexts.likePattern(query == null ? null : query.getTitle());
        if (title != null) {
            wrapper.apply("title LIKE {0} ESCAPE '\\\\'", title);
        }
        if (query != null && query.getPublishStatus() != null) {
            wrapper.eq(LandingPage::getPublishStatus, query.getPublishStatus());
        }
        wrapper.orderByAsc(LandingPage::getSortNo).orderByDesc(LandingPage::getId);
        Page<LandingPage> result = landingPageMapper.selectPage(
                new Page<>(AcqTexts.pageNum(query == null ? null : query.getPageNum()),
                        AcqTexts.pageSize(query == null ? null : query.getPageSize())),
                wrapper);
        List<LandingViews.Row> rows = new ArrayList<>();
        for (LandingPage page : result.getRecords()) {
            rows.add(toRow(page));
        }
        return new AcqPage<>(result.getTotal(), rows);
    }

    @Override
    public LandingViews.Detail detail(Long id) {
        return toDetail(requirePage(id));
    }

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public LandingViews.Detail create(LandingPageSaveCommand command) {
        LandingPage page = new LandingPage();
        page.setId(null);
        page.setPublishStatus(AcquisitionCodes.PAGE_DRAFT);
        apply(page, command);
        landingPageMapper.insert(page);
        if (page.getId() == null) {
            throw new BusinessException("落地页保存后没有取得主键");
        }
        replaceAssets(page.getId(), command.getAssets(), true);
        return detail(page.getId());
    }

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public LandingViews.Detail update(Long id, LandingPageSaveCommand command) {
        LandingPage existing = requirePage(id);
        apply(existing, command);
        landingPageMapper.update(null, new LambdaUpdateWrapper<LandingPage>()
                .set(LandingPage::getTitle, existing.getTitle())
                .set(LandingPage::getHeadline, existing.getHeadline())
                .set(LandingPage::getSellingText, existing.getSellingText())
                .set(LandingPage::getButtonText, existing.getButtonText())
                .set(LandingPage::getPriceFen, existing.getPriceFen())
                .set(LandingPage::getSortNo, existing.getSortNo())
                .set(LandingPage::getReportTemplateId, existing.getReportTemplateId())
                .set(LandingPage::getAdAccountId, existing.getAdAccountId()).set(LandingPage::getPayMerchantId,existing.getPayMerchantId())
                .set(LandingPage::getNameEnabled, existing.getNameEnabled())
                .set(LandingPage::getGenderEnabled, existing.getGenderEnabled())
                .set(LandingPage::getBirthDateEnabled, existing.getBirthDateEnabled())
                .set(LandingPage::getBirthTimeEnabled, existing.getBirthTimeEnabled())
                .eq(LandingPage::getId, id));
        replaceAssets(id, command.getAssets(), false);
        return detail(id);
    }

    @Override
    public void changeStatus(Long id, Integer publishStatus) {
        requirePage(id);
        int target = AcqTexts.requireChoice(publishStatus, "发布状态",
                AcquisitionCodes.PAGE_PUBLISHED, AcquisitionCodes.PAGE_OFFLINE);
        int rows = landingPageMapper.update(null, new LambdaUpdateWrapper<LandingPage>()
                .set(LandingPage::getPublishStatus, target)
                .eq(LandingPage::getId, id));
        if (rows != 1) {
            throw new BusinessException("落地页状态更新失败");
        }
    }

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的落地页");
        }
        if (ids.size() > 50) {
            throw new BusinessException("一次最多删除50条");
        }
        for (Long id : ids) {
            deleteOne(id);
        }
    }

    @Override
    public List<LandingViews.Option> options() {
        List<LandingPage> pages = landingPageMapper.selectList(new LambdaQueryWrapper<LandingPage>()
                .orderByAsc(LandingPage::getSortNo)
                .orderByDesc(LandingPage::getId));
        List<LandingViews.Option> options = new ArrayList<>();
        for (LandingPage page : pages) {
            LandingViews.Option option = new LandingViews.Option();
            option.setId(page.getId());
            option.setTitle(page.getTitle());
            options.add(option);
        }
        return options;
    }

    private void deleteOne(Long id) {
        requirePage(id);
        if (landingPageMapper.countTradeOrderRef(id) > 0 || landingPageMapper.countVisitRef(id) > 0
                || landingPageMapper.countLeadRef(id) > 0 || landingPageMapper.countClickRef(id) > 0) {
            throw new BusinessException("落地页已有访问、线索、点击或订单引用，不能删除，请改为停用");
        }
        if (landingPageMapper.countRouteRef(id) > 0) {
            throw new BusinessException("落地页仍被企微路由引用，不能删除，请改为停用");
        }
        List<LandingAsset> assets = landingAssetMapper.selectList(new LambdaQueryWrapper<LandingAsset>()
                .eq(LandingAsset::getLandingPageId, id));
        for (LandingAsset asset : assets) {
            landingAssetMapper.deleteById(asset.getId());
        }
        List<LandingDomain> links = landingDomainMapper.selectList(new LambdaQueryWrapper<LandingDomain>()
                .eq(LandingDomain::getLandingPageId, id));
        for (LandingDomain link : links) {
            landingDomainMapper.deleteById(link.getId());
        }
        landingPageMapper.deleteById(id);
    }

    private void apply(LandingPage page, LandingPageSaveCommand command) {
        if (command == null) {
            throw new BusinessException("请填写落地页配置");
        }
        page.setTitle(AcqTexts.requireText(command.getTitle(), "页面标题", 128));
        page.setHeadline(AcqTexts.optionalText(command.getHeadline(), "主宣传文案", 255));
        page.setSellingText(AcqTexts.optionalText(command.getSellingText(), "补充文案", 500));
        page.setButtonText(AcqTexts.requireText(command.getButtonText(), "按钮文案", 64));
        page.setPriceFen(MoneyAmounts.yuanToFen(command.getPriceYuan()));
        int sortNo = command.getSortNo() == null ? 0 : command.getSortNo();
        if (sortNo < 0 || sortNo > 999999) {
            throw new BusinessException("展示顺序必须在0到999999之间");
        }
        page.setSortNo(sortNo);
        page.setNameEnabled(AcqTexts.requireFlag(command.getNameEnabled(), "姓名采集"));
        page.setGenderEnabled(AcqTexts.requireFlag(command.getGenderEnabled(), "性别采集"));
        page.setBirthDateEnabled(AcqTexts.requireFlag(command.getBirthDateEnabled(), "出生日期采集"));
        page.setBirthTimeEnabled(AcqTexts.requireFlag(command.getBirthTimeEnabled(), "出生时间采集"));
        if (command.getReportTemplateId() != null) {
            ReportTemplate template = reportTemplateMapper.lockById(command.getReportTemplateId());
            if (template == null) {
                throw new BusinessException("报告模板不存在");
            }
        }
        page.setReportTemplateId(command.getReportTemplateId());
        if (command.getAdAccountId() != null && adAccounts.lockById(command.getAdAccountId()) == null) {
            throw new BusinessException("广告账户不存在");
        }
        page.setAdAccountId(command.getAdAccountId());
        if (command.getPayMerchantId() != null && merchants.lockById(command.getPayMerchantId()) == null) {
            throw new BusinessException("支付商户不存在");
        }
        page.setPayMerchantId(command.getPayMerchantId());
    }

    private void replaceAssets(Long pageId, List<LandingAssetCommand> assets, boolean creating) {
        List<LandingAssetCommand> items = assets == null ? List.of() : assets;
        if (items.size() > MAX_ASSETS) {
            throw new BusinessException("同一落地页最多8张图片");
        }
        Set<Long> kept = new HashSet<>();
        for (int index = 0; index < items.size(); index++) {
            LandingAssetCommand item = items.get(index);
            if (item == null || item.getAssetUrl() == null || item.getAssetUrl().isBlank()) {
                throw new BusinessException("图片地址不能为空");
            }
            String url = AssetUrls.normalize(item.getAssetUrl());
            if (item.getId() == null) {
                LandingAsset asset = new LandingAsset();
                asset.setId(null);
                asset.setLandingPageId(pageId);
                asset.setAssetType(AcquisitionCodes.ASSET_IMAGE);
                asset.setAssetUrl(url);
                asset.setSortNo(index);
                asset.setStatus(AcquisitionCodes.RECORD_ENABLED);
                landingAssetMapper.insert(asset);
                if (asset.getId() == null) {
                    throw new BusinessException("素材保存后没有取得主键");
                }
                kept.add(asset.getId());
                continue;
            }
            if (creating) {
                throw new BusinessException("新建落地页不能绑定已有素材");
            }
            LandingAsset existing = landingAssetMapper.selectById(item.getId());
            if (existing == null || !pageId.equals(existing.getLandingPageId())) {
                throw new BusinessException("素材不属于当前落地页");
            }
            int rows = landingAssetMapper.update(null, new LambdaUpdateWrapper<LandingAsset>()
                    .set(LandingAsset::getAssetUrl, url)
                    .set(LandingAsset::getSortNo, index)
                    .set(LandingAsset::getAssetType, AcquisitionCodes.ASSET_IMAGE)
                    .set(LandingAsset::getStatus, AcquisitionCodes.RECORD_ENABLED)
                    .eq(LandingAsset::getId, existing.getId())
                    .eq(LandingAsset::getLandingPageId, pageId));
            if (rows != 1) {
                throw new BusinessException("素材保存失败");
            }
            kept.add(existing.getId());
        }
        if (creating) {
            return;
        }
        List<LandingAsset> current = landingAssetMapper.selectList(new LambdaQueryWrapper<LandingAsset>()
                .eq(LandingAsset::getLandingPageId, pageId));
        for (LandingAsset asset : current) {
            if (!kept.contains(asset.getId())) {
                landingAssetMapper.deleteById(asset.getId());
            }
        }
    }

    private LandingPage requirePage(Long id) {
        if (id == null) {
            throw new BusinessException("落地页不存在");
        }
        LandingPage page = landingPageMapper.selectById(id);
        if (page == null) {
            throw new BusinessException("落地页不存在");
        }
        return page;
    }

    private LandingViews.Row toRow(LandingPage page) {
        LandingViews.Row row = new LandingViews.Row();
        row.setId(page.getId());
        row.setTitle(page.getTitle());
        row.setHeadline(page.getHeadline());
        row.setButtonText(page.getButtonText());
        row.setPriceFen(page.getPriceFen());
        row.setPriceYuan(MoneyAmounts.fenToYuan(page.getPriceFen()));
        row.setPublishStatus(page.getPublishStatus());
        row.setSortNo(page.getSortNo());
        row.setReportTemplateId(page.getReportTemplateId());
        row.setAdAccountId(page.getAdAccountId());
        row.setPayMerchantId(page.getPayMerchantId());
        row.setNameEnabled(page.getNameEnabled());
        row.setGenderEnabled(page.getGenderEnabled());
        row.setBirthDateEnabled(page.getBirthDateEnabled());
        row.setBirthTimeEnabled(page.getBirthTimeEnabled());
        return row;
    }

    private LandingViews.Detail toDetail(LandingPage page) {
        LandingViews.Detail detail = new LandingViews.Detail();
        LandingViews.Row row = toRow(page);
        detail.setId(row.getId());
        detail.setTitle(row.getTitle());
        detail.setHeadline(page.getHeadline());
        detail.setSellingText(page.getSellingText());
        detail.setButtonText(row.getButtonText());
        detail.setPriceYuan(row.getPriceYuan());
        detail.setPriceFen(row.getPriceFen());
        detail.setPublishStatus(row.getPublishStatus());
        detail.setSortNo(row.getSortNo());
        detail.setReportTemplateId(row.getReportTemplateId());
        detail.setAdAccountId(row.getAdAccountId());
        detail.setPayMerchantId(row.getPayMerchantId());
        detail.setNameEnabled(row.getNameEnabled());
        detail.setGenderEnabled(row.getGenderEnabled());
        detail.setBirthDateEnabled(row.getBirthDateEnabled());
        detail.setBirthTimeEnabled(row.getBirthTimeEnabled());
        List<LandingAsset> assets = landingAssetMapper.selectList(new LambdaQueryWrapper<LandingAsset>()
                .eq(LandingAsset::getLandingPageId, page.getId())
                .orderByAsc(LandingAsset::getSortNo)
                .orderByAsc(LandingAsset::getId));
        for (LandingAsset asset : assets) {
            LandingViews.Asset view = new LandingViews.Asset();
            view.setId(asset.getId());
            view.setAssetUrl(asset.getAssetUrl());
            view.setSortNo(asset.getSortNo());
            view.setStatus(asset.getStatus());
            detail.getAssets().add(view);
        }
        return detail;
    }
}
