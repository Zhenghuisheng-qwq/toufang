package com.zhs.service.acq.report;

public interface ReportGenerationService {
    /** Returns false if ownership expired. Caller must supply a unique owner per claim. */
    boolean generate(Long taskId, String leaseOwner, Long leaseVersion);
}
