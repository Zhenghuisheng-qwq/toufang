package com.zhs.service.acq.flow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.ConfigRules;
import com.zhs.service.acq.report.ReportGenerationServiceImpl;
import com.zhs.service.mapper.acq.BusinessFlowMapper;
import com.zhs.service.mapper.acq.ReportWorkMapper;
import com.zhs.service.mapper.acq.WecomContactEntryMapper;
import com.zhs.service.model.acq.PayRecord;
import com.zhs.service.model.acq.PostPayTask;
import com.zhs.service.model.acq.TradeOrder;
import com.zhs.service.model.acq.WecomContactEntry;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/** Local task preparation only. No external service is called while holding a database lock. */
@Service
public class PostPaymentPreparationService {

    @Resource
    private BusinessFlowMapper flow;

    @Resource
    private ReportWorkMapper locks;

    @Resource
    private WecomContactEntryMapper entries;

    @Resource
    private AdEventService events;

    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public void prepare(Long id, String owner, Long version) {
        PostPayTask task = locks.lockTask(id);
        if (task == null || !Objects.equals(task.getStatus(), 1) || !Objects.equals(task.getLeaseOwner(), owner)
                || !Objects.equals(task.getLeaseVersion(), version) || !task.getLeaseUntil().isAfter(locks.now())) {
            return;
        }
        TradeOrder order = locks.lockOrder(task.getOrderNo());
        PayRecord payment = locks.lockPayment(task.getOrderNo());
        int status = 2;
        String error = null;
        if (!ReportGenerationServiceImpl.paid(order, payment)) {
            status = 4;
            error = "PAYMENT_INELIGIBLE";
        } else if (Objects.equals(task.getTaskType(), 3)) {
            events.register(1, "ORDER", order.getId(), order.getOrderNo(), order.getAdClickId(), order.getAmountFen());
        } else if (Objects.equals(task.getTaskType(), 2)) {
            WecomContactEntry existing = entries.selectOne(new LambdaQueryWrapper<WecomContactEntry>()
                    .eq(WecomContactEntry::getOrderNo, order.getOrderNo()));
            if (existing == null) {
                if (Objects.equals(order.getEntryType(), 2) && order.getEntryLinkUrl() != null) {
                    WecomContactEntry entry = new WecomContactEntry();
                    entry.setOrderNo(order.getOrderNo());
                    entry.setEntryType(2);
                    entry.setCorpRowId(order.getCorpRowId());
                    entry.setCorpId(order.getCorpId());
                    entry.setTeacherUserid(order.getTeacherUserid());
                    entry.setLinkId(order.getEntryLinkId());
                    entry.setLinkUrl(ConfigRules.link(order.getEntryLinkUrl()));
                    entry.setStatus(1);
                    entries.insert(entry);
                } else {
                    status = 4;
                    error = "DYNAMIC_WECOM_ENTRY_NOT_IMPLEMENTED";
                }
            }
        } else {
            throw new BusinessException("任务类型不匹配");
        }
        if (flow.finish(id, owner, version, status, error) != 1) {
            throw new BusinessException("任务租约失效");
        }
    }
}
