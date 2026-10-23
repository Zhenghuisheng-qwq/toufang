package com.zhs.config;
import com.zhs.service.acq.flow.AdDeliveryAdapter;
import com.zhs.service.mapper.acq.*;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import java.time.LocalDateTime;
import java.util.UUID;
@Configuration @EnableScheduling
@ConditionalOnProperty(prefix="acq.ads.worker",name="enabled",havingValue="true")
public class AdDeliveryWorker {
    @Resource private AdDeliveryMapper work;
    @Resource private AdConversionMapper events;
    @Resource private AdAccountMapper accounts;
    @Resource private AdDeliveryAdapter adapter;
    @Value("${acq.ads.worker.since:}") private String since;
    @Scheduled(fixedDelayString="${acq.ads.worker.delay-ms:5000}") public void poll(){
        if(since.isBlank())return;var boundary=LocalDateTime.parse(since);work.expire(boundary);
        for(var id:work.candidates(boundary)){
            String owner=UUID.randomUUID().toString();if(work.claim(id,owner)!=1)continue;var event=events.selectById(id);
            if(event==null||!owner.equals(event.getLeaseOwner()))continue;
            var result=adapter.deliver(event,event.getAdAccountId()==null?null:accounts.selectById(event.getAdAccountId()));
            boolean exhausted=result.status()==3&&event.getRetryCount()>=5;
            work.finish(id,owner,event.getLeaseVersion(),exhausted?5:result.status(),exhausted?"MAX_ATTEMPTS_EXHAUSTED":result.code());
        }
    }
}
