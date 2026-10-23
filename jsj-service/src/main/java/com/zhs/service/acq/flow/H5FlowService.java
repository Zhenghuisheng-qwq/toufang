package com.zhs.service.acq.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.*;
import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

/** Anonymous ownership and idempotency are enforced in the acquisition database. */
@Service
@Transactional(transactionManager="acqTransactionManager",rollbackFor=Exception.class)
public class H5FlowService {
    @Resource private BusinessFlowMapper flow;
    @Resource private H5CatalogService catalog;
    @Resource private H5VisitMapper visits;
    @Resource private H5LeadMapper leads;
    @Resource private AdClickMapper clicks;
    @Resource private AdAccountMapper accounts;
    @Resource private TradeOrderService orders;
    @Resource private PayRecordService payments;
    @Resource private AdEventService events;
    @Resource private ObjectMapper json;
    @Resource private ReportWorkMapper reportWork;
    @Resource private WecomContactEntryMapper entries;

    public Map<String,Object> entry(String token,String host,String no){
        var order=ownedOrder(token,host,no);
        if(!com.zhs.service.acq.report.ReportGenerationServiceImpl.paid(order,reportWork.lockPayment(no)))throw new BusinessException("支付尚未确认");
        var entry=entries.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WecomContactEntry>().eq(WecomContactEntry::getOrderNo,no));
        if(entry==null||!Objects.equals(entry.getStatus(),1))return Map.of("status",taskStatus(no,2));
        return Map.of("status","READY","type",entry.getEntryType(),"url",entry.getEntryType()==2?entry.getLinkUrl():entry.getQrCodeUrl());
    }
    public boolean oauthReady(String token,String host,String no){ownedOrder(token,host,no);var pay=reportWork.lockPayment(no);return pay!=null&&pay.getPayerOpenid()!=null;}

    public Map<String,Object> report(String token,String host,String no) {
        var order=ownedOrder(token,host,no);var pay=reportWork.lockPayment(no);
        if(!com.zhs.service.acq.report.ReportGenerationServiceImpl.paid(order,pay))throw new BusinessException("报告尚未解锁或退款后不可访问");
        var report=reportWork.lockReport(no);
        if(report==null)return Map.of("status",taskStatus(no,1));
        if(!Objects.equals(report.getIsDelete(),0)||!Objects.equals(report.getStatus(),1))return Map.of("status","REVIEW_REQUIRED");
        return Map.of("status","READY","content",report.getContentText(),"mode",report.getGenerationMode());
    }
    private String taskStatus(String no,int type){
        return flow.tasks(no).stream().filter(t->((Number)t.get("type")).intValue()==type)
                .anyMatch(t->((Number)t.get("status")).intValue()>=3)?"REVIEW_REQUIRED":"PENDING";
    }

    public String session(String host,String existing) {
        host=DomainNames.canonicalize(host);catalog.domain(host);
        if(existing!=null && existing.matches("[A-Za-z0-9_-]{43}") && flow.lockSession(FlowSecurity.credentialHash(existing),host)!=null)return existing;
        String token=FlowSecurity.token();flow.session(FlowSecurity.credentialHash(token),host);return token;
    }
    private Long owner(String token,String host) {
        Long id=flow.lockSession(FlowSecurity.credentialHash(token),DomainNames.canonicalize(host));
        if(id==null)throw new BusinessException("访问凭证失效，请重新打开页面");return id;
    }
    private String fingerprint(Object command) {
        try{return FlowSecurity.hash(json.writeValueAsString(command));}
        catch(JsonProcessingException e){throw new BusinessException("请求格式不正确");}
    }
    private String previous(Long sid,String kind,String key,Object command) {
        FlowSecurity.key(key);var row=flow.request(sid,kind,key);
        if(row==null)return null;
        if(!Objects.equals(row.get("fingerprint"),fingerprint(command)))throw new BusinessException("重复请求的内容不一致");
        return row.get("result").toString();
    }
    private String remember(Long sid,String kind,String key,Object command,Object result) {
        String value=result.toString();flow.remember(sid,kind,key,fingerprint(command),value);return value;
    }
    public String visit(String token,String host,H5Commands.Visit input) {
        Long sid=owner(token,host);String prior=previous(sid,"VISIT",input.requestKey(),input);if(prior!=null)return prior;
        var c=catalog.resolve(input.pageId(),host);
        int platform=input.platform()==null?0:input.platform();if(platform<0||platform>3)throw new BusinessException("来源平台不正确");
        var v=new H5Visit();v.setSessionId(sid);v.setVisitNo(UUID.randomUUID().toString().replace("-",""));v.setLandingPageId(c.page().getId());
        v.setDomainId(c.domain().getId());v.setDomainName(c.domain().getDomainName());v.setPlatform(platform);v.setVisitTime(LocalDateTime.now());visits.insert(v);
        if(platform>0&&input.clickId()!=null&&!input.clickId().isBlank()) {
            if(input.clickId().length()>1024||input.clickId().codePoints().anyMatch(Character::isISOControl))throw new BusinessException("点击参数不正确");
            var a=new AdClick();a.setPlatform(platform);a.setLandingPageId(v.getLandingPageId());a.setVisitId(v.getId());a.setDomainId(v.getDomainId());a.setDomainName(v.getDomainName());a.setClickTime(v.getVisitTime());
            var account=c.page().getAdAccountId()==null?null:accounts.lockById(c.page().getAdAccountId());
            if(account!=null&&Objects.equals(account.getStatus(),1)&&Objects.equals(account.getPlatform(),platform))a.setAdAccountId(account.getId());
            if(platform==1)a.setTencentClickId(input.clickId());if(platform==2)a.setBaiduLogidUrl(input.clickId());if(platform==3)a.setKuaishouCallback(input.clickId());
            clicks.insert(a);v.setAdClickId(a.getId());visits.updateById(v);
        }
        return remember(sid,"VISIT",input.requestKey(),input,v.getId());
    }
    public String lead(String token,String host,H5Commands.Lead input) {
        Long sid=owner(token,host);String prior=previous(sid,"LEAD",input.requestKey(),input);if(prior!=null)return prior;
        var v=visits.selectById(input.visitId());if(v==null||!Objects.equals(v.getSessionId(),sid))throw new BusinessException("访问记录不存在");
        var p=catalog.resolve(v.getLandingPageId(),host).page();
        if(on(p.getNameEnabled())&&(input.name()==null||input.name().isBlank()))throw new BusinessException("请填写姓名");
        if(input.name()!=null&&(input.name().length()>64||input.name().codePoints().anyMatch(Character::isISOControl)))throw new BusinessException("姓名不符合要求");
        if(on(p.getGenderEnabled())&&(input.gender()==null||input.gender()<1||input.gender()>2))throw new BusinessException("请选择性别");
        if(on(p.getBirthDateEnabled())&&(input.birthDate()==null||input.birthDate().isAfter(LocalDate.now())||input.birthDate().isBefore(LocalDate.of(1900,1,1))))throw new BusinessException("出生日期不符合要求");
        var l=new H5Lead();l.setSessionId(sid);l.setVisitId(v.getId());l.setLandingPageId(v.getLandingPageId());l.setDomainId(v.getDomainId());l.setDomainName(v.getDomainName());l.setPlatform(v.getPlatform());l.setAdClickId(v.getAdClickId());
        l.setRealName(on(p.getNameEnabled())?input.name().trim():null);l.setGender(on(p.getGenderEnabled())?input.gender():0);
        l.setBirthDate(on(p.getBirthDateEnabled())?input.birthDate():null);l.setBirthTime(on(p.getBirthTimeEnabled())?input.birthTime():null);l.setSubmitTime(LocalDateTime.now());leads.insert(l);
        events.register(AdEventService.SUBMIT,"LEAD",l.getId(),null,l.getAdClickId(),null);
        return remember(sid,"LEAD",input.requestKey(),input,l.getId());
    }
    public String order(String token,String host,H5Commands.Order input) {
        Long sid=owner(token,host);String prior=previous(sid,"ORDER",input.requestKey(),input);if(prior!=null)return prior;
        var l=leads.selectById(input.leadId());if(l==null||!Objects.equals(l.getSessionId(),sid))throw new BusinessException("资料不存在");
        if(!Objects.equals(input.tradeType(),1)&&!Objects.equals(input.tradeType(),2))throw new BusinessException("支付类型不正确");
        var c=catalog.resolve(l.getLandingPageId(),host);var r=c.route();var m=c.merchant();var v=c.version();
        var o=new TradeOrder();o.setOrderNo(UUID.randomUUID().toString().replace("-",""));o.setSessionId(sid);o.setLeadId(l.getId());o.setVisitId(l.getVisitId());o.setLandingPageId(l.getLandingPageId());o.setAdClickId(l.getAdClickId());o.setAdSnapshot(events.freeze(l.getAdClickId()));
        o.setDomainId(c.domain().getId());o.setDomainName(c.domain().getDomainName());o.setAmountFen(c.page().getPriceFen());o.setTradeType(input.tradeType());o.setPayMerchantId(m.getId());o.setMerchantVersionId(v.getId());o.setMerchantVersionNo(v.getVersionNo());o.setMchId(m.getMchId());o.setAppId(m.getAppId());
        o.setRouteId(r.getId());o.setCorpRowId(r.getCorpRowId());o.setTeacherRowId(r.getTeacherRowId());o.setCorpId(r.getCorpId());o.setTeacherUserid(r.getTeacherUserid());o.setEntryType(r.getEntryType());o.setEntryLinkId(r.getLinkId());o.setEntryLinkUrl(r.getLinkUrl());o.setPageTitle(c.page().getTitle());o.setReturnPath("/orders/"+o.getOrderNo());orders.create(o);
        var pay=new PayRecord();pay.setOrderNo(o.getOrderNo());pay.setOutTradeNo(o.getOrderNo());pay.setMchId(o.getMchId());pay.setAppId(o.getAppId());pay.setAmountFen(o.getAmountFen());pay.setTradeType(o.getTradeType());payments.savePending(pay);
        return remember(sid,"ORDER",input.requestKey(),input,o.getOrderNo());
    }
    public TradeOrder ownedOrder(String token,String host,String no) {
        var o=flow.ownedOrder(no,owner(token,host));if(o==null)throw new BusinessException("订单不存在或访问凭证不匹配");return o;
    }
    private static boolean on(Integer n){return Objects.equals(n,1);}
}
