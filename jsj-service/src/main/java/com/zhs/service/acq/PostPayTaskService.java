package com.zhs.service.acq;

import java.time.LocalDateTime;

/**
 * 支付后任务的写入与领取。不启动调度器。
 */
public interface PostPayTaskService {

    void ensureTasks(String orderNo, LocalDateTime nextRunAt);

    boolean claim(Long taskId, String owner, LocalDateTime now, int leaseSeconds);
}
