package com.zhs.service.acq.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.AcqDuplicate;
import com.zhs.service.acq.AcqTexts;
import com.zhs.service.acq.AcquisitionCodes;
import com.zhs.service.acq.DomainNames;
import com.zhs.service.acq.SiteDomainAdminService;
import com.zhs.service.acq.dto.AcqPage;
import com.zhs.service.acq.dto.DomainCommands;
import com.zhs.service.mapper.acq.DomainMerchantMapper;
import com.zhs.service.mapper.acq.LandingDomainMapper;
import com.zhs.service.mapper.acq.LandingPageMapper;
import com.zhs.service.mapper.acq.PayMerchantMapper;
import com.zhs.service.mapper.acq.SiteDomainMapper;
import com.zhs.service.model.acq.DomainMerchant;
import com.zhs.service.model.acq.LandingDomain;
import com.zhs.service.model.acq.LandingPage;
import com.zhs.service.model.acq.PayMerchant;
import com.zhs.service.model.acq.SiteDomain;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SiteDomainAdminServiceImpl implements SiteDomainAdminService {

    @Resource
    private SiteDomainMapper siteDomainMapper;
    @Resource
    private LandingDomainMapper landingDomainMapper;
    @Resource
    private DomainMerchantMapper domainMerchantMapper;
    @Resource
    private LandingPageMapper landingPageMapper;
    @Resource
    private PayMerchantMapper payMerchantMapper;

    @Override
    public AcqPage<DomainCommands.Row> page(DomainCommands.Query query) {
        LambdaQueryWrapper<SiteDomain> wrapper = new LambdaQueryWrapper<>();
        String name = AcqTexts.likePattern(query == null ? null : query.getDomainName());
        if (name != null) {
            wrapper.apply("domain_name LIKE {0} ESCAPE '\\\\'", name);
        }
        if (query != null && query.getStatus() != null) {
            wrapper.eq(SiteDomain::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SiteDomain::getId);
        Page<SiteDomain> result = siteDomainMapper.selectPage(
                new Page<>(AcqTexts.pageNum(query == null ? null : query.getPageNum()),
                        AcqTexts.pageSize(query == null ? null : query.getPageSize())),
                wrapper);
        List<DomainCommands.Row> rows = new ArrayList<>();
        for (SiteDomain domain : result.getRecords()) {
            rows.add(toRow(domain));
        }
        return new AcqPage<>(result.getTotal(), rows);
    }

    @Override
    public DomainCommands.Detail detail(Long id) {
        return toDetail(requireDomain(id));
    }

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public DomainCommands.Detail create(DomainCommands.Save command) {
        Values values = valuesOf(command);
        SiteDomain existing = siteDomainMapper.selectAnyByDomainName(values.domainName);
        if (existing != null && !deleted(existing.getIsDelete())) {
            throw new BusinessException("域名已存在");
        }
        if (existing != null) {
            existing.setStatus(0);
            existing.setReviewStatus(values.reviewStatus);
            existing.setIcpStatus(values.icpStatus);
            existing.setHttpsStatus(values.httpsStatus);
            existing.setRemark(values.remark);
            if (siteDomainMapper.restore(existing) != 1) {
                throw new BusinessException("已删除的同名域名恢复失败");
            }
            return detail(existing.getId());
        }
        SiteDomain created = new SiteDomain();
        created.setId(null);
        created.setDomainName(values.domainName);
        created.setStatus(0);
        created.setReviewStatus(values.reviewStatus);
        created.setIcpStatus(values.icpStatus);
        created.setHttpsStatus(values.httpsStatus);
        created.setVerifyStatus(0);
        created.setRemark(values.remark);
        try {
            siteDomainMapper.insert(created);
        } catch (RuntimeException ex) {
            if (AcqDuplicate.isDuplicate(ex)) {
                throw new BusinessException("域名已存在");
            }
            throw ex;
        }
        if (created.getId() == null) {
            throw new BusinessException("域名保存后没有取得主键");
        }
        return detail(created.getId());
    }

    @Override
    public DomainCommands.Detail update(Long id, DomainCommands.Save command) {
        requireDomain(id);
        Values values = valuesOf(command);
        SiteDomain sameName = siteDomainMapper.selectAnyByDomainName(values.domainName);
        if (sameName != null && !id.equals(sameName.getId())) {
            throw new BusinessException("域名已存在");
        }
        int rows = siteDomainMapper.update(null, new LambdaUpdateWrapper<SiteDomain>()
                .set(SiteDomain::getDomainName, values.domainName)
                .set(SiteDomain::getReviewStatus, values.reviewStatus)
                .set(SiteDomain::getIcpStatus, values.icpStatus)
                .set(SiteDomain::getHttpsStatus, values.httpsStatus)
                .set(SiteDomain::getVerifyStatus, 0)
                .set(SiteDomain::getRemark, values.remark)
                .eq(SiteDomain::getId, id));
        if (rows != 1) {
            throw new BusinessException("域名不存在");
        }
        return detail(id);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        requireDomain(id);
        int target = AcqTexts.requireChoice(status, "域名状态",
                AcquisitionCodes.RECORD_DISABLED, AcquisitionCodes.RECORD_ENABLED);
        int rows = siteDomainMapper.update(null, new LambdaUpdateWrapper<SiteDomain>()
                .set(SiteDomain::getStatus, target)
                .eq(SiteDomain::getId, id));
        if (rows != 1) {
            throw new BusinessException("域名状态更新失败");
        }
    }

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的域名");
        }
        if (ids.size() > 50) {
            throw new BusinessException("一次最多删除50条");
        }
        for (Long id : ids) {
            deleteOne(id);
        }
    }

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public void linkLanding(Long domainId, Long landingPageId) {
        requireDomain(domainId);
        if (landingPageId == null || landingPageMapper.selectById(landingPageId) == null) {
            throw new BusinessException("落地页不存在");
        }
        LandingDomain existing = landingDomainMapper.selectAny(landingPageId, domainId);
        if (existing != null && !deleted(existing.getIsDelete())) {
            throw new BusinessException("已经关联该落地页");
        }
        if (existing != null) {
            if (landingDomainMapper.restore(existing.getId()) != 1) {
                throw new BusinessException("落地页关联恢复失败");
            }
            return;
        }
        LandingDomain link = new LandingDomain();
        link.setId(null);
        link.setDomainId(domainId);
        link.setLandingPageId(landingPageId);
        link.setStatus(AcquisitionCodes.RECORD_ENABLED);
        try {
            landingDomainMapper.insert(link);
        } catch (RuntimeException ex) {
            if (AcqDuplicate.isDuplicate(ex)) {
                throw new BusinessException("已经关联该落地页");
            }
            throw ex;
        }
    }

    @Override
    public void unlinkLanding(Long domainId, Long landingPageId) {
        requireDomain(domainId);
        LandingDomain existing = landingDomainMapper.selectOne(new LambdaQueryWrapper<LandingDomain>()
                .eq(LandingDomain::getDomainId, domainId)
                .eq(LandingDomain::getLandingPageId, landingPageId)
                .last("LIMIT 1"));
        if (existing == null) {
            throw new BusinessException("落地页关联不存在");
        }
        landingDomainMapper.deleteById(existing.getId());
    }

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public void linkMerchant(Long domainId, Long payMerchantId) {
        requireDomain(domainId);
        PayMerchant merchant = payMerchantId == null ? null : payMerchantMapper.selectById(payMerchantId);
        if (merchant == null) {
            throw new BusinessException("支付商户不存在");
        }
        DomainMerchant existing = domainMerchantMapper.selectAny(domainId, payMerchantId);
        if (existing != null && !deleted(existing.getIsDelete())) {
            throw new BusinessException("已经关联该支付商户");
        }
        if (existing != null) {
            if (domainMerchantMapper.restore(existing.getId()) != 1) {
                throw new BusinessException("商户关联恢复失败");
            }
            return;
        }
        DomainMerchant link = new DomainMerchant();
        link.setId(null);
        link.setDomainId(domainId);
        link.setPayMerchantId(payMerchantId);
        link.setStatus(AcquisitionCodes.DOMAIN_MERCHANT_ENABLED);
        link.setVerifyStatus(0);
        try {
            domainMerchantMapper.insert(link);
        } catch (RuntimeException ex) {
            if (AcqDuplicate.isDuplicate(ex)) {
                throw new BusinessException("已经关联该支付商户");
            }
            throw ex;
        }
    }

    @Override
    public void unlinkMerchant(Long domainId, Long payMerchantId) {
        requireDomain(domainId);
        DomainMerchant existing = domainMerchantMapper.selectOne(new LambdaQueryWrapper<DomainMerchant>()
                .eq(DomainMerchant::getDomainId, domainId)
                .eq(DomainMerchant::getPayMerchantId, payMerchantId)
                .last("LIMIT 1"));
        if (existing == null) {
            throw new BusinessException("商户关联不存在");
        }
        domainMerchantMapper.deleteById(existing.getId());
    }

    @Override
    public List<DomainCommands.MerchantOption> merchantOptions() {
        List<PayMerchant> merchants = payMerchantMapper.selectList(new LambdaQueryWrapper<PayMerchant>()
                .orderByDesc(PayMerchant::getId));
        List<DomainCommands.MerchantOption> options = new ArrayList<>();
        for (PayMerchant merchant : merchants) {
            DomainCommands.MerchantOption option = new DomainCommands.MerchantOption();
            option.setId(merchant.getId());
            option.setMchName(merchant.getMchName());
            option.setMchId(merchant.getMchId());
            option.setAppId(merchant.getAppId());
            option.setStatus(merchant.getStatus());
            option.setPaymentReady(false);
            options.add(option);
        }
        return options;
    }

    private void deleteOne(Long id) {
        requireDomain(id);
        if (siteDomainMapper.countTradeOrderRef(id) > 0 || siteDomainMapper.countVisitRef(id) > 0
                || siteDomainMapper.countLeadRef(id) > 0 || siteDomainMapper.countClickRef(id) > 0) {
            throw new BusinessException("域名已有访问、线索、点击或订单引用，不能删除，请改为停用");
        }
        List<LandingDomain> landings = landingDomainMapper.selectList(new LambdaQueryWrapper<LandingDomain>()
                .eq(LandingDomain::getDomainId, id));
        for (LandingDomain link : landings) {
            landingDomainMapper.deleteById(link.getId());
        }
        List<DomainMerchant> merchants = domainMerchantMapper.selectList(new LambdaQueryWrapper<DomainMerchant>()
                .eq(DomainMerchant::getDomainId, id));
        for (DomainMerchant link : merchants) {
            domainMerchantMapper.deleteById(link.getId());
        }
        siteDomainMapper.deleteById(id);
    }

    private Values valuesOf(DomainCommands.Save command) {
        if (command == null) {
            throw new BusinessException("请填写域名");
        }
        Values values = new Values();
        values.domainName = DomainNames.canonicalize(command.getDomainName());
        values.status = AcqTexts.requireChoice(command.getStatus(), "域名状态",
                AcquisitionCodes.RECORD_DISABLED, AcquisitionCodes.RECORD_ENABLED);
        values.reviewStatus = AcqTexts.requireChoice(command.getReviewStatus(), "审核登记", 0, 1, 2);
        values.icpStatus = AcqTexts.requireChoice(command.getIcpStatus(), "备案登记", 0, 1, 2);
        values.httpsStatus = AcqTexts.requireChoice(command.getHttpsStatus(), "HTTPS登记", 0, 1, 2);
        values.remark = AcqTexts.optionalText(command.getRemark(), "备注", 255);
        return values;
    }

    private SiteDomain requireDomain(Long id) {
        if (id == null) {
            throw new BusinessException("域名不存在");
        }
        SiteDomain domain = siteDomainMapper.selectById(id);
        if (domain == null) {
            throw new BusinessException("域名不存在");
        }
        return domain;
    }

    private boolean deleted(Integer isDelete) {
        return isDelete != null && isDelete == 1;
    }

    private DomainCommands.Row toRow(SiteDomain domain) {
        DomainCommands.Row row = new DomainCommands.Row();
        row.setId(domain.getId());
        row.setDomainName(domain.getDomainName());
        row.setStatus(domain.getStatus());
        row.setReviewStatus(domain.getReviewStatus());
        row.setIcpStatus(domain.getIcpStatus());
        row.setHttpsStatus(domain.getHttpsStatus());
        row.setVerifyStatus(domain.getVerifyStatus() == null ? 0 : domain.getVerifyStatus());
        row.setRemark(domain.getRemark());
        return row;
    }

    private DomainCommands.Detail toDetail(SiteDomain domain) {
        DomainCommands.Detail detail = new DomainCommands.Detail();
        DomainCommands.Row row = toRow(domain);
        detail.setId(row.getId());
        detail.setDomainName(row.getDomainName());
        detail.setStatus(row.getStatus());
        detail.setReviewStatus(row.getReviewStatus());
        detail.setIcpStatus(row.getIcpStatus());
        detail.setHttpsStatus(row.getHttpsStatus());
        detail.setVerifyStatus(row.getVerifyStatus());
        detail.setRemark(row.getRemark());
        List<LandingDomain> links = landingDomainMapper.selectList(new LambdaQueryWrapper<LandingDomain>()
                .eq(LandingDomain::getDomainId, domain.getId())
                .orderByAsc(LandingDomain::getId));
        for (LandingDomain link : links) {
            LandingPage page = landingPageMapper.selectById(link.getLandingPageId());
            DomainCommands.LandingLink view = new DomainCommands.LandingLink();
            view.setId(link.getId());
            view.setLandingPageId(link.getLandingPageId());
            view.setTitle(page == null ? "落地页已删除" : page.getTitle());
            view.setStatus(link.getStatus());
            detail.getLandings().add(view);
        }
        List<DomainMerchant> merchantLinks = domainMerchantMapper.selectList(new LambdaQueryWrapper<DomainMerchant>()
                .eq(DomainMerchant::getDomainId, domain.getId())
                .orderByAsc(DomainMerchant::getId));
        for (DomainMerchant link : merchantLinks) {
            PayMerchant merchant = payMerchantMapper.selectById(link.getPayMerchantId());
            DomainCommands.MerchantLink view = new DomainCommands.MerchantLink();
            view.setId(link.getId());
            view.setPayMerchantId(link.getPayMerchantId());
            view.setMchName(merchant == null ? "商户已删除" : merchant.getMchName());
            view.setMchId(merchant == null ? "" : merchant.getMchId());
            view.setAppId(merchant == null ? "" : merchant.getAppId());
            view.setMerchantStatus(merchant == null ? null : merchant.getStatus());
            view.setStatus(link.getStatus());
            view.setVerifyStatus(link.getVerifyStatus() == null ? 0 : link.getVerifyStatus());
            view.setPaymentReady(false);
            detail.getMerchants().add(view);
        }
        return detail;
    }

    private static final class Values {
        private String domainName;
        private int status;
        private int reviewStatus;
        private int icpStatus;
        private int httpsStatus;
        private String remark;
    }
}
