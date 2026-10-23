package com.zhs.service.acq.flow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.DomainNames;
import com.zhs.service.acq.PayRecordService;
import com.zhs.service.acq.TradeOrderService;
import com.zhs.service.acq.report.ReportGenerationServiceImpl;
import com.zhs.service.mapper.acq.AdAccountMapper;
import com.zhs.service.mapper.acq.AdClickMapper;
import com.zhs.service.mapper.acq.BusinessFlowMapper;
import com.zhs.service.mapper.acq.H5LeadMapper;
import com.zhs.service.mapper.acq.H5VisitMapper;
import com.zhs.service.mapper.acq.ReportWorkMapper;
import com.zhs.service.mapper.acq.WecomContactEntryMapper;
import com.zhs.service.model.acq.AdAccount;
import com.zhs.service.model.acq.AdClick;
import com.zhs.service.model.acq.H5Lead;
import com.zhs.service.model.acq.H5Visit;
import com.zhs.service.model.acq.LandingPage;
import com.zhs.service.model.acq.PayMerchant;
import com.zhs.service.model.acq.PayMerchantVersion;
import com.zhs.service.model.acq.PayRecord;
import com.zhs.service.model.acq.TradeOrder;
import com.zhs.service.model.acq.TradeReport;
import com.zhs.service.model.acq.WecomContactEntry;
import com.zhs.service.model.acq.WecomRoute;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Anonymous ownership and idempotency are enforced in the acquisition database. */
@Service
@Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
public class H5FlowService {

    @Resource
    private BusinessFlowMapper flow;

    @Resource
    private H5CatalogService catalog;

    @Resource
    private H5VisitMapper visits;

    @Resource
    private H5LeadMapper leads;

    @Resource
    private AdClickMapper clicks;

    @Resource
    private AdAccountMapper accounts;

    @Resource
    private TradeOrderService orders;

    @Resource
    private PayRecordService payments;

    @Resource
    private AdEventService events;

    @Resource
    private ObjectMapper json;

    @Resource
    private ReportWorkMapper reportWork;

    @Resource
    private WecomContactEntryMapper entries;

    public Map<String, Object> entry(String token, String host, String no) {
        TradeOrder order = ownedOrder(token, host, no);
        if (!ReportGenerationServiceImpl.paid(order, reportWork.lockPayment(no))) {
            throw new BusinessException("支付尚未确认");
        }
        WecomContactEntry entry = entries.selectOne(new LambdaQueryWrapper<WecomContactEntry>()
                .eq(WecomContactEntry::getOrderNo, no));
        if (entry == null || !Objects.equals(entry.getStatus(), 1)) {
            return Map.of("status", taskStatus(no, 2));
        }
        String url = entry.getEntryType() == 2 ? entry.getLinkUrl() : entry.getQrCodeUrl();
        return Map.of("status", "READY", "type", entry.getEntryType(), "url", url);
    }

    public boolean oauthReady(String token, String host, String no) {
        ownedOrder(token, host, no);
        PayRecord pay = reportWork.lockPayment(no);
        return pay != null && pay.getPayerOpenid() != null;
    }

    public Map<String, Object> report(String token, String host, String no) {
        TradeOrder order = ownedOrder(token, host, no);
        PayRecord pay = reportWork.lockPayment(no);
        if (!ReportGenerationServiceImpl.paid(order, pay)) {
            throw new BusinessException("报告尚未解锁或退款后不可访问");
        }
        TradeReport report = reportWork.lockReport(no);
        if (report == null) {
            return Map.of("status", taskStatus(no, 1));
        }
        if (!Objects.equals(report.getIsDelete(), 0) || !Objects.equals(report.getStatus(), 1)) {
            return Map.of("status", "REVIEW_REQUIRED");
        }
        return Map.of("status", "READY", "content", report.getContentText(), "mode", report.getGenerationMode());
    }

    private String taskStatus(String no, int type) {
        boolean reviewRequired = flow.tasks(no).stream()
                .filter(task -> ((Number) task.get("type")).intValue() == type)
                .anyMatch(task -> ((Number) task.get("status")).intValue() >= 3);
        return reviewRequired ? "REVIEW_REQUIRED" : "PENDING";
    }

    public String session(String host, String existing) {
        host = hostKey(host);
        if (!H5LoopbackPreview.active()) {
            catalog.domain(host);
        }
        if (existing != null && existing.matches("[A-Za-z0-9_-]{43}")
                && flow.lockSession(FlowSecurity.credentialHash(existing), host) != null) {
            return existing;
        }
        String token = FlowSecurity.token();
        flow.session(FlowSecurity.credentialHash(token), host);
        return token;
    }

    private Long owner(String token, String host) {
        Long id = flow.lockSession(FlowSecurity.credentialHash(token), hostKey(host));
        if (id == null) {
            throw new BusinessException("访问凭证失效，请重新打开页面");
        }
        return id;
    }

    private String fingerprint(Object command) {
        try {
            return FlowSecurity.hash(json.writeValueAsString(command));
        } catch (JsonProcessingException e) {
            throw new BusinessException("请求格式不正确");
        }
    }

    private String previous(Long sid, String kind, String key, Object command) {
        FlowSecurity.key(key);
        Map<String, Object> row = flow.request(sid, kind, key);
        if (row == null) {
            return null;
        }
        if (!Objects.equals(row.get("fingerprint"), fingerprint(command))) {
            throw new BusinessException("重复请求的内容不一致");
        }
        return row.get("result").toString();
    }

    private String remember(Long sid, String kind, String key, Object command, Object result) {
        String value = result.toString();
        flow.remember(sid, kind, key, fingerprint(command), value);
        return value;
    }

    public String visit(String token, String host, H5Commands.Visit input) {
        Long sid = owner(token, host);
        String prior = previous(sid, "VISIT", input.requestKey(), input);
        if (prior != null) {
            return prior;
        }
        LandingPage page;
        Long domainId;
        String domainName;
        if (H5LoopbackPreview.active()) {
            page = catalog.published(input.pageId());
            domainId = null;
            domainName = H5LoopbackPreview.hostKey(host);
        } else {
            H5CatalogService.Configuration configuration = catalog.resolve(input.pageId(), host);
            page = configuration.page();
            domainId = configuration.domain().getId();
            domainName = configuration.domain().getDomainName();
        }
        int platform = input.platform() == null ? 0 : input.platform();
        if (platform < 0 || platform > 3) {
            throw new BusinessException("来源平台不正确");
        }
        H5Visit visit = new H5Visit();
        visit.setSessionId(sid);
        visit.setVisitNo(UUID.randomUUID().toString().replace("-", ""));
        visit.setLandingPageId(page.getId());
        visit.setDomainId(domainId);
        visit.setDomainName(domainName);
        visit.setPlatform(platform);
        visit.setVisitTime(LocalDateTime.now());
        visits.insert(visit);
        if (platform > 0 && input.clickId() != null && !input.clickId().isBlank()) {
            if (input.clickId().length() > 1024 || input.clickId().codePoints().anyMatch(Character::isISOControl)) {
                throw new BusinessException("点击参数不正确");
            }
            AdClick click = new AdClick();
            click.setPlatform(platform);
            click.setLandingPageId(visit.getLandingPageId());
            click.setVisitId(visit.getId());
            click.setDomainId(visit.getDomainId());
            click.setDomainName(visit.getDomainName());
            click.setClickTime(visit.getVisitTime());
            AdAccount account = page.getAdAccountId() == null ? null : accounts.lockById(page.getAdAccountId());
            if (account != null && Objects.equals(account.getStatus(), 1) && Objects.equals(account.getPlatform(), platform)) {
                click.setAdAccountId(account.getId());
            }
            if (platform == 1) {
                click.setTencentClickId(input.clickId());
            }
            if (platform == 2) {
                click.setBaiduLogidUrl(input.clickId());
            }
            if (platform == 3) {
                click.setKuaishouCallback(input.clickId());
            }
            clicks.insert(click);
            visit.setAdClickId(click.getId());
            visits.updateById(visit);
        }
        return remember(sid, "VISIT", input.requestKey(), input, visit.getId());
    }

    public String lead(String token, String host, H5Commands.Lead input) {
        Long sid = owner(token, host);
        String prior = previous(sid, "LEAD", input.requestKey(), input);
        if (prior != null) {
            return prior;
        }
        H5Visit visit = visits.selectById(input.visitId());
        if (visit == null || !Objects.equals(visit.getSessionId(), sid)) {
            throw new BusinessException("访问记录不存在");
        }
        LandingPage page = H5LoopbackPreview.active()
                ? catalog.published(visit.getLandingPageId())
                : catalog.resolve(visit.getLandingPageId(), host).page();
        if (on(page.getNameEnabled()) && (input.name() == null || input.name().isBlank())) {
            throw new BusinessException("请填写姓名");
        }
        if (input.name() != null && (input.name().length() > 64 || input.name().codePoints().anyMatch(Character::isISOControl))) {
            throw new BusinessException("姓名不符合要求");
        }
        if (on(page.getGenderEnabled()) && (input.gender() == null || input.gender() < 1 || input.gender() > 2)) {
            throw new BusinessException("请选择性别");
        }
        if (on(page.getBirthDateEnabled()) && (input.birthDate() == null || input.birthDate().isAfter(LocalDate.now())
                || input.birthDate().isBefore(LocalDate.of(1900, 1, 1)))) {
            throw new BusinessException("出生日期不符合要求");
        }
        H5Lead lead = new H5Lead();
        lead.setSessionId(sid);
        lead.setVisitId(visit.getId());
        lead.setLandingPageId(visit.getLandingPageId());
        lead.setDomainId(visit.getDomainId());
        lead.setDomainName(visit.getDomainName());
        lead.setPlatform(visit.getPlatform());
        lead.setAdClickId(visit.getAdClickId());
        lead.setRealName(on(page.getNameEnabled()) ? input.name().trim() : null);
        lead.setGender(on(page.getGenderEnabled()) ? input.gender() : 0);
        lead.setBirthDate(on(page.getBirthDateEnabled()) ? input.birthDate() : null);
        lead.setBirthTime(on(page.getBirthTimeEnabled()) ? input.birthTime() : null);
        lead.setSubmitTime(LocalDateTime.now());
        leads.insert(lead);
        events.register(AdEventService.SUBMIT, "LEAD", lead.getId(), null, lead.getAdClickId(), null);
        return remember(sid, "LEAD", input.requestKey(), input, lead.getId());
    }

    public String order(String token, String host, H5Commands.Order input) {
        Long sid = owner(token, host);
        String prior = previous(sid, "ORDER", input.requestKey(), input);
        if (prior != null) {
            return prior;
        }
        H5Lead lead = leads.selectById(input.leadId());
        if (lead == null || !Objects.equals(lead.getSessionId(), sid)) {
            throw new BusinessException("资料不存在");
        }
        if (!Objects.equals(input.tradeType(), 1) && !Objects.equals(input.tradeType(), 2)) {
            throw new BusinessException("支付类型不正确");
        }
        if (H5LoopbackPreview.active()) {
            throw new BusinessException("本地预览已开放页面和资料。当前页面还没有支付商户、报告模板和承接路由，不能创建订单");
        }
        H5CatalogService.Configuration configuration = catalog.resolve(lead.getLandingPageId(), host);
        WecomRoute route = configuration.route();
        PayMerchant merchant = configuration.merchant();
        PayMerchantVersion version = configuration.version();
        TradeOrder order = new TradeOrder();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setSessionId(sid);
        order.setLeadId(lead.getId());
        order.setVisitId(lead.getVisitId());
        order.setLandingPageId(lead.getLandingPageId());
        order.setAdClickId(lead.getAdClickId());
        order.setAdSnapshot(events.freeze(lead.getAdClickId()));
        order.setDomainId(configuration.domain().getId());
        order.setDomainName(configuration.domain().getDomainName());
        order.setAmountFen(configuration.page().getPriceFen());
        order.setTradeType(input.tradeType());
        order.setPayMerchantId(merchant.getId());
        order.setMerchantVersionId(version.getId());
        order.setMerchantVersionNo(version.getVersionNo());
        order.setMchId(merchant.getMchId());
        order.setAppId(merchant.getAppId());
        order.setRouteId(route.getId());
        order.setCorpRowId(route.getCorpRowId());
        order.setTeacherRowId(route.getTeacherRowId());
        order.setCorpId(route.getCorpId());
        order.setTeacherUserid(route.getTeacherUserid());
        order.setEntryType(route.getEntryType());
        order.setEntryLinkId(route.getLinkId());
        order.setEntryLinkUrl(route.getLinkUrl());
        order.setPageTitle(configuration.page().getTitle());
        order.setReturnPath("/orders/" + order.getOrderNo());
        orders.create(order);
        PayRecord pay = new PayRecord();
        pay.setOrderNo(order.getOrderNo());
        pay.setOutTradeNo(order.getOrderNo());
        pay.setMchId(order.getMchId());
        pay.setAppId(order.getAppId());
        pay.setAmountFen(order.getAmountFen());
        pay.setTradeType(order.getTradeType());
        payments.savePending(pay);
        return remember(sid, "ORDER", input.requestKey(), input, order.getOrderNo());
    }

    public TradeOrder ownedOrder(String token, String host, String no) {
        TradeOrder order = flow.ownedOrder(no, owner(token, host));
        if (order == null) {
            throw new BusinessException("订单不存在或访问凭证不匹配");
        }
        return order;
    }

    private static String hostKey(String host) {
        return H5LoopbackPreview.active() ? H5LoopbackPreview.hostKey(host) : DomainNames.canonicalize(host);
    }

    private static boolean on(Integer flag) {
        return Objects.equals(flag, 1);
    }
}
