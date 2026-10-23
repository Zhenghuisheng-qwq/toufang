package com.zhs.config;

import com.zhs.service.acq.report.ReportGenerationService;
import com.zhs.service.mapper.acq.BusinessFlowMapper;
import com.zhs.service.mapper.acq.ReportWorkMapper;
import com.zhs.service.model.acq.PostPayTask;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.UUID;

/** Mini-only, disabled by default. Processes reports only; never contacts a platform. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "acq.report.worker", name = "enabled", havingValue = "true")
public class ReportWorker {

    @Resource
    private ReportWorkMapper work;

    @Resource
    private ReportGenerationService generation;

    @Resource
    private BusinessFlowMapper scopedWork;

    @Value("${acq.report.worker.since:}")
    private String since;

    @Scheduled(fixedDelayString = "${acq.report.worker.delay-ms:5000}")
    public void poll() {
        if (since == null || since.isBlank()) {
            return;
        }
        for (Long id : scopedWork.candidates(1, LocalDateTime.parse(since))) {
            String owner = UUID.randomUUID().toString();
            if (work.claim(id, owner) != 1) {
                continue;
            }
            PostPayTask task = work.task(id);
            if (task == null || !owner.equals(task.getLeaseOwner())) {
                continue;
            }
            try {
                generation.generate(id, owner, task.getLeaseVersion());
            } catch (RuntimeException ex) {
                // Persist only a stable category. Never log inputs, report text or SQL exception details.
                work.retryLater(id, owner, task.getLeaseVersion());
            }
        }
    }
}
