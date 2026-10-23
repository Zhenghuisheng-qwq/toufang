package com.zhs.config;

import com.zhs.service.acq.flow.PostPaymentPreparationService;
import com.zhs.service.acq.flow.WecomEntryJob;
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

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "acq.post-payment.worker", name = "enabled", havingValue = "true")
public class PostPaymentWorker {

    @Resource
    private BusinessFlowMapper flow;

    @Resource
    private ReportWorkMapper locks;

    @Resource
    private PostPaymentPreparationService preparation;

    @Resource
    private WecomEntryJob wecom;

    @Value("${acq.post-payment.worker.since:}")
    private String since;

    @Scheduled(fixedDelayString = "${acq.post-payment.worker.delay-ms:5000}")
    public void poll() {
        if (since.isBlank()) {
            return;
        }
        for (int type = 2; type <= 3; type++) {
            for (Long id : flow.candidates(type, LocalDateTime.parse(since))) {
                String owner = UUID.randomUUID().toString();
                if (flow.claim(id, owner) != 1) {
                    continue;
                }
                PostPayTask task = locks.task(id);
                if (task == null || !owner.equals(task.getLeaseOwner())) {
                    continue;
                }
                try {
                    if (type == 2) {
                        wecom.run(id, owner, task.getLeaseVersion());
                    } else {
                        preparation.prepare(id, owner, task.getLeaseVersion());
                    }
                } catch (RuntimeException e) {
                    int status = task.getAttemptCount() >= 4 ? 4 : 0;
                    flow.finish(id, owner, task.getLeaseVersion(), status, "LOCAL_PREPARATION_FAILED");
                }
            }
        }
    }
}
