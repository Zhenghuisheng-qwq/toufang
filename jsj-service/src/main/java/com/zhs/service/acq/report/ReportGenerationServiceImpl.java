package com.zhs.service.acq.report;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

/** No scheduler, external client or public endpoint is registered by this shared service. */
@Service
public class ReportGenerationServiceImpl implements ReportGenerationService {
    @Resource private ReportWorkMapper work;
    @Resource private ReportTemplateVersionMapper versions;
    @Resource private TradeReportMapper reports;
    @Resource private PlainTextReportRenderer renderer;

    @Override
    @Transactional(transactionManager="acqTransactionManager",rollbackFor=Exception.class)
    public boolean generate(Long taskId,String owner,Long leaseVersion) {
        if (taskId == null || owner == null || owner.isBlank()) return false;
        var task = work.lockTask(taskId);
        if (task == null || leaseVersion == null || !leaseVersion.equals(task.getLeaseVersion()) || !Integer.valueOf(1).equals(task.getTaskType()) || !Integer.valueOf(1).equals(task.getStatus())
                || !owner.equals(task.getLeaseOwner()) || task.getLeaseUntil() == null || !task.getLeaseUntil().isAfter(work.now())) return false;
        var order = work.lockOrder(task.getOrderNo());
        var pay = order == null ? null : work.lockPayment(task.getOrderNo());
        if (!paid(order,pay)) return blocked(taskId,owner,"PAYMENT_INELIGIBLE");
        var existing = work.lockReport(task.getOrderNo());
        if (existing != null) {
            if (Integer.valueOf(1).equals(existing.getStatus()) && Integer.valueOf(0).equals(existing.getIsDelete())
                    && existing.getContentText() != null && !existing.getContentText().isBlank()) {
                complete(taskId,owner,2,null); return true;
            }
            return blocked(taskId,owner,"EXISTING_REPORT_REQUIRES_REVIEW");
        }
        if (!Integer.valueOf(1).equals(order.getReportInputVersion()) || order.getReportTemplateVersionId() == null || order.getReportTemplateId() == null)
            return blocked(taskId,owner,"SNAPSHOT_MISSING");
        // Historical published versions remain valid even when the parent template is later disabled.
        var version = versions.lockById(order.getReportTemplateVersionId());
        if (version == null || !Objects.equals(order.getReportTemplateId(),version.getTemplateId()) || !Integer.valueOf(1).equals(version.getStatus()))
            return blocked(taskId,owner,"TEMPLATE_VERSION_MISSING");
        if (!Integer.valueOf(1).equals(version.getContentType())) return blocked(taskId,owner,"UNSUPPORTED_GENERATION");
        String content;
        try {
            renderer.validate(version.getContentType(),version.getContentText());
        } catch (BusinessException ex) { return blocked(taskId,owner,"TEMPLATE_INVALID"); }
        try {
            content=renderer.render(version.getContentType(),version.getContentText(),
                    new ReportInput(order.getReportName(),order.getReportGender(),order.getReportBirthDate(),order.getReportBirthTime()));
        } catch (BusinessException ex) { return blocked(taskId,owner,"INPUT_INVALID"); }
        TradeReport report=new TradeReport();
        report.setOrderNo(order.getOrderNo()); report.setTemplateId(order.getReportTemplateId()); report.setTemplateVersionId(order.getReportTemplateVersionId());
        report.setStatus(1); report.setIsDelete(0); report.setContentText(content); report.setGenerationMode("TEXT_TEMPLATE");
        report.setGeneratedTime(work.now());
        reports.insert(report);
        // Lease loss throws so the insert rolls back with task completion. Unique(order_no) is the final guard.
        complete(taskId,owner,2,null);
        return true;
    }
    private boolean blocked(Long id,String owner,String category) { complete(id,owner,4,category); return true; }
    private void complete(Long id,String owner,int status,String error) {
        if(work.finish(id,owner,status,error)!=1) throw new BusinessException("报告任务租约已失效，本次写入回滚");
    }
    static boolean paid(TradeOrder order,PayRecord pay) {
        return order!=null && pay!=null && Integer.valueOf(1).equals(order.getPayStatus()) && Integer.valueOf(1).equals(pay.getPayStatus())
                && pay.getTransactionId()!=null && !pay.getTransactionId().isBlank() && order.getPaidTime()!=null && pay.getSuccessTime()!=null
                && order.getMchId()!=null && !order.getMchId().isBlank() && order.getAppId()!=null && !order.getAppId().isBlank()
                && Objects.equals(order.getOrderNo(),pay.getOrderNo())
                && Objects.equals(order.getOrderNo(),pay.getSuccessOrderNo()) && Objects.equals(order.getMchId(),pay.getMchId())
                && Objects.equals(order.getAppId(),pay.getAppId()) && order.getAmountFen()!=null && order.getAmountFen()>0
                && Objects.equals(order.getAmountFen(),pay.getAmountFen());
    }
}
