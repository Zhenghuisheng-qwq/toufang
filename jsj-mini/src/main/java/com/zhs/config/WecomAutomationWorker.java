package com.zhs.config;
import com.zhs.service.acq.flow.WecomAutomationService;
import com.zhs.service.mapper.acq.WecomActionMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import java.time.LocalDateTime;
import java.util.UUID;
@Configuration @EnableScheduling
@ConditionalOnProperty(prefix="acq.wecom.automation.worker",name="enabled",havingValue="true")
public class WecomAutomationWorker {
    @Resource private WecomActionMapper work;
    @Resource private WecomAutomationService automation;
    @Value("${acq.wecom.automation.worker.since:}") private String since;
    @Scheduled(fixedDelayString="${acq.wecom.automation.worker.delay-ms:5000}") public void poll(){if(since.isBlank())return;var start=LocalDateTime.parse(since);work.expire(start);for(var id:work.candidates(start))automation.run(id,UUID.randomUUID().toString());}
}
