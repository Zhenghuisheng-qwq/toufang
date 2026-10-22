package com.zhs.service.acq;

import com.zhs.service.model.acq.WecomContactEntry;

/**
 * 获客入口。联系我二维码和获客助手链接不能混用字段。
 */
public interface WecomContactEntryService {

    WecomContactEntry create(WecomContactEntry entry);
}
