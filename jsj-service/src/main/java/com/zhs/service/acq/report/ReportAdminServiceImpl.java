package com.zhs.service.acq.report;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.mapper.acq.ReportAdminMapper;
import com.zhs.service.mapper.acq.ReportTemplateVersionMapper;
import com.zhs.service.mapper.acq.ReportWorkMapper;
import com.zhs.service.model.acq.PayRecord;
import com.zhs.service.model.acq.PostPayTask;
import com.zhs.service.model.acq.ReportTemplateVersion;
import com.zhs.service.model.acq.TradeOrder;
import com.zhs.service.model.acq.TradeReport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ReportAdminServiceImpl implements ReportAdminService {

    @Resource
    private ReportAdminMapper admin;

    @Resource
    private ReportWorkMapper work;

    @Resource
    private ReportTemplateVersionMapper versions;

    @Resource
    private PlainTextReportRenderer renderer;

    @Resource
    private ReportGenerationService generation;

    private static final Set<String> ERRORS = Set.of("PAYMENT_INELIGIBLE", "EXISTING_REPORT_REQUIRES_REVIEW",
            "SNAPSHOT_MISSING", "TEMPLATE_VERSION_MISSING", "UNSUPPORTED_GENERATION", "TEMPLATE_INVALID",
            "INPUT_INVALID", "TRANSIENT_STORAGE_ERROR");

    private void requireOrderNo(String orderNo) {
        if (orderNo == null || !orderNo.matches("[A-Za-z0-9_-]{1,32}")) {
            throw new BusinessException("订单号格式不正确");
        }
    }

    @Override
    public ReportAdminView status(String orderNo) {
        requireOrderNo(orderNo);
        ReportAdminView view = admin.status(orderNo);
        if (view == null) {
            throw new BusinessException("订单不存在");
        }
        if (view.getLastError() != null && !ERRORS.contains(view.getLastError())) {
            view.setLastError("REQUIRES_REVIEW");
        }
        return view;
    }

    @Override
    public String content(String orderNo) {
        status(orderNo);
        String content = admin.content(orderNo);
        if (content == null || content.isBlank()) {
            throw new BusinessException("尚无可查看的成功报告");
        }
        return content;
    }

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public ReportAdminView retry(String orderNo) {
        requireOrderNo(orderNo);
        // Insert-or-lock the unique report task before locking the order, matching the worker lock order.
        // Invalid orders/payments throw below, rolling back even a newly inserted task.
        work.ensureReport(orderNo);
        PostPayTask task = admin.lockReportTask(orderNo);
        if (task == null || !Integer.valueOf(0).equals(task.getIsDelete())) {
            throw new BusinessException("报告任务已删除，需要人工核查");
        }
        TradeOrder order = work.lockOrder(orderNo);
        PayRecord pay = order == null ? null : work.lockPayment(orderNo);
        if (!ReportGenerationServiceImpl.paid(order, pay)) {
            throw new BusinessException("订单不具备可信支付资格，拒绝补生成");
        }
        TradeReport report = work.lockReport(orderNo);
        if (report != null) {
            if (Integer.valueOf(1).equals(report.getStatus()) && Integer.valueOf(0).equals(report.getIsDelete())
                    && report.getContentText() != null && !report.getContentText().isBlank()) {
                return status(orderNo);
            }
            throw new BusinessException("已有报告记录需人工核查，禁止覆盖");
        }
        if (Integer.valueOf(1).equals(task.getStatus()) && (task.getLeaseUntil() == null || task.getLeaseUntil().isAfter(work.now()))) {
            throw new BusinessException("报告任务正在执行或租约异常，请稍后核查");
        }
        if (!Integer.valueOf(1).equals(order.getReportInputVersion()) || order.getReportTemplateId() == null || order.getReportTemplateVersionId() == null) {
            throw new BusinessException("历史订单缺少冻结快照，禁止使用最新配置补齐");
        }
        ReportTemplateVersion version = versions.lockById(order.getReportTemplateVersionId());
        if (version == null || !Integer.valueOf(1).equals(version.getStatus()) || !Objects.equals(order.getReportTemplateId(), version.getTemplateId())) {
            throw new BusinessException("冻结模板版本不可用");
        }
        renderer.validate(version.getContentType(), version.getContentText());
        renderer.render(version.getContentType(), version.getContentText(), new ReportInput(order.getReportName(), order.getReportGender(), order.getReportBirthDate(), order.getReportBirthTime()));
        String owner = "admin-" + UUID.randomUUID();
        Long previousVersion = task.getLeaseVersion();
        if (previousVersion == null || admin.claimRetry(task.getId(), previousVersion, owner) != 1) {
            throw new BusinessException("报告任务状态已变化或不可重试");
        }
        if (!generation.generate(task.getId(), owner, previousVersion + 1)) {
            throw new BusinessException("报告任务领取失败");
        }
        return status(orderNo);
    }
}
