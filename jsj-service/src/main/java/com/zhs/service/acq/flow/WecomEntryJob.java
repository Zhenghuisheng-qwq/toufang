package com.zhs.service.acq.flow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.ConfigRules;
import com.zhs.service.acq.report.ReportGenerationServiceImpl;
import com.zhs.service.mapper.acq.BusinessFlowMapper;
import com.zhs.service.mapper.acq.ReportWorkMapper;
import com.zhs.service.mapper.acq.WecomContactEntryMapper;
import com.zhs.service.mapper.acq.WecomCorpMapper;
import com.zhs.service.mapper.acq.WecomTeacherMapper;
import com.zhs.service.model.acq.PostPayTask;
import com.zhs.service.model.acq.TradeOrder;
import com.zhs.service.model.acq.WecomContactEntry;
import com.zhs.service.model.acq.WecomCorp;
import com.zhs.service.model.acq.WecomTeacher;
import jakarta.annotation.Resource;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.bean.external.WxCpContactWayInfo;
import me.chanjar.weixin.cp.bean.external.WxCpContactWayResult;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;

/** An uncertain create is never automatically sent again. Reconcile it before operator retry. */
@Service
public class WecomEntryJob {

    private final TransactionTemplate transaction;

    public WecomEntryJob(@Qualifier("acqTransactionManager") PlatformTransactionManager manager) {
        transaction = new TransactionTemplate(manager);
    }

    @Resource
    private ReportWorkMapper locks;

    @Resource
    private BusinessFlowMapper flow;

    @Resource
    private WecomContactEntryMapper entries;

    @Resource
    private WecomCorpMapper corps;

    @Resource
    private WecomTeacherMapper teachers;

    @Resource
    private PlatformSecrets secrets;

    @Resource
    private PostPaymentPreparationService local;

    @Value("${acq.wecom.outbound.enabled:false}")
    private boolean enabled;

    private record Request(WecomContactEntry entry, WxCpServiceImpl client) {
    }

    public void run(Long id, String owner, Long version) {
        PostPayTask task = locks.task(id);
        if (task == null) {
            return;
        }
        TradeOrder order = locks.lockOrder(task.getOrderNo());
        if (order != null && Objects.equals(order.getEntryType(), 2)) {
            local.prepare(id, owner, version);
            return;
        }
        Request request = transaction.execute(status -> {
            PostPayTask locked = locks.lockTask(id);
            if (!valid(locked, owner, version)) {
                return null;
            }
            TradeOrder o = locks.lockOrder(locked.getOrderNo());
            if (!ReportGenerationServiceImpl.paid(o, locks.lockPayment(locked.getOrderNo()))) {
                finish(id, owner, version, 4, "PAYMENT_INELIGIBLE");
                return null;
            }
            if (!enabled) {
                finish(id, owner, version, 4, "WECOM_OUTBOUND_DISABLED");
                return null;
            }
            WecomCorp corp = corps.lockById(o.getCorpRowId());
            WecomTeacher teacher = teachers.lockById(o.getTeacherRowId());
            if (corp == null || teacher == null || !Objects.equals(corp.getStatus(), 1) || !Objects.equals(teacher.getStatus(), 1)
                    || !Objects.equals(teacher.getCorpRowId(), corp.getId()) || !Objects.equals(corp.getCorpId(), o.getCorpId())
                    || !Objects.equals(teacher.getTeacherUserid(), o.getTeacherUserid()) || !"SELF_BUILT".equals(corp.getAuthMode())) {
                finish(id, owner, version, 4, "WECOM_CONFIGURATION_INELIGIBLE");
                return null;
            }
            WxCpDefaultConfigImpl config = new WxCpDefaultConfigImpl();
            config.setCorpId(corp.getCorpId());
            try {
                config.setCorpSecret(secrets.resolve(corp.getContactSecretRef()));
            } catch (BusinessException e) {
                finish(id, owner, version, 4, "WECOM_CREDENTIAL_REQUIRED");
                return null;
            }
            WxCpServiceImpl client = new WxCpServiceImpl();
            client.setWxCpConfigStorage(config);
            client.setMaxRetryTimes(0);
            WecomContactEntry entry = entries.selectOne(new LambdaQueryWrapper<WecomContactEntry>()
                    .eq(WecomContactEntry::getOrderNo, o.getOrderNo()));
            if (entry != null) {
                finish(id, owner, version, Objects.equals(entry.getStatus(), 1) ? 2 : 4,
                        Objects.equals(entry.getStatus(), 1) ? null : "ENTRY_RESULT_REQUIRES_RECONCILIATION");
                return null;
            }
            entry = new WecomContactEntry();
            entry.setEntryType(1);
            entry.setOrderNo(o.getOrderNo());
            entry.setCorpRowId(o.getCorpRowId());
            entry.setCorpId(o.getCorpId());
            entry.setTeacherUserid(o.getTeacherUserid());
            entry.setStateToken(FlowSecurity.token().substring(0, 24));
            entry.setStatus(4);
            entries.insert(entry);
            return new Request(entry, client);
        });
        if (request == null) {
            return;
        }
        try {
            WxCpContactWayInfo.ContactWay detail = new WxCpContactWayInfo.ContactWay();
            detail.setType(WxCpContactWayInfo.TYPE.SINGLE);
            detail.setScene(WxCpContactWayInfo.SCENE.QRCODE);
            detail.setState(request.entry().getStateToken());
            detail.setUsers(List.of(request.entry().getTeacherUserid()));
            detail.setSkipVerify(true);
            WxCpContactWayInfo input = new WxCpContactWayInfo();
            input.setContactWay(detail);
            WxCpContactWayResult result = request.client().getExternalContactService().addContactWay(input);
            if (result.getConfigId() == null || result.getQrCode() == null) {
                throw new BusinessException("企微未返回入口");
            }
            transaction.executeWithoutResult(status -> {
                if (!valid(locks.lockTask(id), owner, version)) {
                    throw new BusinessException("任务租约失效");
                }
                WecomContactEntry entry = request.entry();
                entry.setConfigId(result.getConfigId());
                entry.setQrCodeUrl(ConfigRules.link(result.getQrCode()));
                entry.setStatus(1);
                entries.updateById(entry);
                finish(id, owner, version, 2, null);
            });
        } catch (Exception e) {
            transaction.executeWithoutResult(status -> {
                if (valid(locks.lockTask(id), owner, version)) {
                    finish(id, owner, version, 4, "ENTRY_RESULT_UNKNOWN");
                }
            });
        }
    }

    private boolean valid(PostPayTask task, String owner, Long version) {
        return task != null && Objects.equals(task.getTaskType(), 2) && Objects.equals(task.getStatus(), 1)
                && Objects.equals(task.getLeaseOwner(), owner) && Objects.equals(task.getLeaseVersion(), version)
                && task.getLeaseUntil() != null && task.getLeaseUntil().isAfter(locks.now());
    }

    private void finish(Long id, String owner, Long version, int state, String error) {
        if (flow.finish(id, owner, version, state, error) != 1) {
            throw new BusinessException("任务租约失效");
        }
    }
}
