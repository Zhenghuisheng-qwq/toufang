package com.zhs.service.acq.report;

public interface ReportAdminService {
    ReportAdminView status(String orderNo);
    String content(String orderNo);
    ReportAdminView retry(String orderNo);
}
