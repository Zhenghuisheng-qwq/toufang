package com.zhs.service.acq.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.AcquisitionCodes;
import com.zhs.service.acq.PostPayTaskService;
import com.zhs.service.mapper.acq.PostPayTaskMapper;
import com.zhs.service.model.acq.PostPayTask;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class PostPayTaskServiceImpl implements PostPayTaskService {

    @Resource
    private PostPayTaskMapper postPayTaskMapper;
    @Resource private com.zhs.service.mapper.acq.BusinessFlowMapper flow;

    @Override
    @org.springframework.transaction.annotation.Transactional(transactionManager="acqTransactionManager",rollbackFor=Exception.class)
    public void ensureTasks(String orderNo, LocalDateTime nextRunAt) {
        if (!StringUtils.hasText(orderNo) || nextRunAt == null) {
            throw new BusinessException("任务缺少订单号或执行时间");
        }
        for (int taskType : AcquisitionCodes.POST_PAY_TASK_TYPES) flow.taskAt(orderNo,taskType,nextRunAt);

    }

    @Override
    public boolean claim(Long taskId, String owner, LocalDateTime now, int leaseSeconds) {
        if (taskId == null || !StringUtils.hasText(owner) || now == null || leaseSeconds < 1) {
            throw new BusinessException("领取任务的参数不完整");
        }
        int rows = postPayTaskMapper.update(null, new LambdaUpdateWrapper<PostPayTask>()
                .set(PostPayTask::getStatus, AcquisitionCodes.TASK_RUNNING)
                .set(PostPayTask::getLeaseOwner, owner)
                .set(PostPayTask::getLeaseUntil, now.plusSeconds(leaseSeconds))
                .setSql("lease_version = lease_version + 1")
                .eq(PostPayTask::getId, taskId)
                .le(PostPayTask::getNextRunAt, now)
                .and(wrapper -> wrapper.eq(PostPayTask::getStatus, AcquisitionCodes.TASK_PENDING)
                        .or(running -> running.eq(PostPayTask::getStatus, AcquisitionCodes.TASK_RUNNING)
                                .le(PostPayTask::getLeaseUntil, now))));
        return rows == 1;
    }
}
