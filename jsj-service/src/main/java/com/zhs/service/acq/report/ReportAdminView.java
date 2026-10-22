package com.zhs.service.acq.report;

import lombok.Data;
import java.time.LocalDateTime;

/** Explicit projection: no input snapshot, credential, voucher or report body. IDs are strings for JS. */
@Data
public class ReportAdminView {
    private String orderNo;
    private Integer payStatus;
    private String templateId;
    private String templateVersionId;
    private String reportId;
    private Integer reportStatus;
    private LocalDateTime generatedTime;
    private String taskId;
    private Integer taskStatus;
    private Integer attemptCount;
    private LocalDateTime leaseUntil;
    private String lastError;
}
