package com.zhs.service.acq.flow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhs.service.mapper.acq.AdAccountMapper;
import com.zhs.service.mapper.acq.AdClickMapper;
import com.zhs.service.mapper.acq.AdConversionMapper;
import com.zhs.service.mapper.acq.TradeOrderMapper;
import com.zhs.service.model.acq.AdAccount;
import com.zhs.service.model.acq.AdClick;
import com.zhs.service.model.acq.AdConversion;
import com.zhs.service.model.acq.TradeOrder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdEventService {

    public static final int PAY = 1;
    public static final int SUBMIT = 2;
    public static final int ADD_CONTACT = 3;

    @Resource
    private AdConversionMapper events;

    @Resource
    private AdClickMapper clicks;

    @Resource
    private AdAccountMapper accounts;

    @Resource
    private ObjectMapper json;

    @Resource
    private TradeOrderMapper orders;

    public String freeze(Long clickId) {
        AdClick click = clickId == null ? null : clicks.selectById(clickId);
        AdAccount account = click == null || click.getAdAccountId() == null ? null : accounts.lockById(click.getAdAccountId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("platform", click == null ? 0 : click.getPlatform());
        map.put("accountId", account == null ? null : account.getId());
        if (click != null) {
            map.put("url", "https://" + click.getDomainName() + "/pages/" + click.getLandingPageId());
        }
        if (click != null) {
            map.put("tencentClickId", click.getTencentClickId());
            map.put("baiduLogidUrl", click.getBaiduLogidUrl());
            map.put("kuaishouCallback", click.getKuaishouCallback());
        }
        if (account != null) {
            map.put("interfaceMode", account.getInterfaceMode());
            map.put("externalAccountId", account.getExternalAccountId());
            map.put("dataSourceId", account.getDataSourceId());
            map.put("pay", account.getPayEvent());
            map.put("submit", mappedEvent(account.getEventMap(), SUBMIT));
            map.put("addContact", mappedEvent(account.getEventMap(), ADD_CONTACT));
            map.put("amountUnit", account.getAmountUnit());
        }
        try {
            return json.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot freeze attribution");
        }
    }

    @Transactional(transactionManager = "acqTransactionManager", propagation = Propagation.MANDATORY)
    public void register(int type, String sourceType, Long sourceId, String orderNo, Long clickId, Integer amount) {
        String key = sourceType + ":" + sourceId + ":" + type;
        if (events.selectCount(new LambdaQueryWrapper<AdConversion>().eq(AdConversion::getEventKey, key)) > 0) {
            return;
        }
        AdClick click = clickId == null ? null : clicks.selectById(clickId);
        AdAccount account = click == null || click.getAdAccountId() == null ? null : accounts.selectById(click.getAdAccountId());
        AdConversion event = new AdConversion();
        event.setEventKey(key);
        event.setSourceType(sourceType);
        event.setSourceId(sourceId);
        event.setOrderNo(orderNo);
        event.setPlatform(click == null ? 0 : click.getPlatform());
        event.setEventType(type);
        event.setAdClickId(clickId);
        event.setAdAccountId(account == null ? null : account.getId());
        event.setAmountFen(amount);
        event.setEventTime(LocalDateTime.now());
        event.setNextRunAt(LocalDateTime.now());
        event.setRetryCount(0);
        event.setLeaseVersion(0L);
        event.setStatus(click == null ? 2 : account == null ? 5 : 0);
        event.setLastError(click == null ? "NO_CLICK" : account == null ? "ACCOUNT_NOT_CONFIGURED" : null);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (click != null) {
            snapshot.put("url", "https://" + click.getDomainName() + "/pages/" + click.getLandingPageId());
        }
        if (click != null) {
            snapshot.put("tencentClickId", click.getTencentClickId());
            snapshot.put("baiduLogidUrl", click.getBaiduLogidUrl());
            snapshot.put("kuaishouCallback", click.getKuaishouCallback());
        }
        if (account != null) {
            snapshot.put("interfaceMode", account.getInterfaceMode());
            snapshot.put("externalAccountId", account.getExternalAccountId());
            snapshot.put("dataSourceId", account.getDataSourceId());
            snapshot.put("event", type == PAY ? account.getPayEvent() : mappedEvent(account.getEventMap(), type));
            snapshot.put("amountUnit", account.getAmountUnit());
        }
        try {
            if (orderNo != null) {
                TradeOrder order = orders.selectOne(new LambdaQueryWrapper<TradeOrder>().eq(TradeOrder::getOrderNo, orderNo));
                if (order == null || order.getAdSnapshot() == null) {
                    event.setStatus(5);
                    event.setLastError("HISTORICAL_ATTRIBUTION_SNAPSHOT_MISSING");
                } else {
                    JsonNode frozen = json.readTree(order.getAdSnapshot());
                    snapshot = json.convertValue(frozen, new TypeReference<Map<String, Object>>() {
                    });
                    String eventName = type == PAY ? "pay" : type == SUBMIT ? "submit" : "addContact";
                    snapshot.put("event", frozen.path(eventName).asText(null));
                    event.setPlatform(frozen.path("platform").asInt(0));
                    event.setAdAccountId(frozen.path("accountId").isNumber() ? frozen.path("accountId").longValue() : null);
                }
            }
            event.setDeliverySnapshot(json.writeValueAsString(snapshot));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot freeze event configuration", ex);
        }
        events.insert(event);
    }

    private String mappedEvent(String mapping, int type) {
        if (mapping == null) {
            return null;
        }
        try {
            return json.readTree(mapping).path(type == SUBMIT ? "submit" : "addContact").asText(null);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }
}
