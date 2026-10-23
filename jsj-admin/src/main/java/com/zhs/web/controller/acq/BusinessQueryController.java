package com.zhs.web.controller.acq;

import com.zhs.common.annotation.Log;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.flow.RefundApplicationService;
import com.zhs.service.mapper.acq.BusinessQueryMapper;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/acq/business")
public class BusinessQueryController {

    @Resource
    private BusinessQueryMapper query;

    @Resource
    private RefundApplicationService refunds;

    public record RefundRequest(String reason) {
        @Override
        public String toString() {
            return "RefundRequest[redacted]";
        }
    }

    @PostMapping("/orders/{no}/refund")
    @PreAuthorize("@ss.hasPermi('acq:order:refund')")
    @Log(title = "全额退款申请或查单", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public AjaxResult refund(@PathVariable("no") String no, @RequestBody RefundRequest request) {
        return AjaxResult.success(refunds.refund(no, request.reason()));
    }

    @GetMapping("/orders")
    @PreAuthorize("@ss.hasPermi('acq:order:query')")
    public AjaxResult orders(@RequestParam(name = "page", defaultValue = "1") int page) {
        return AjaxResult.success(query.orders(offset(page)));
    }

    @GetMapping("/leads")
    @PreAuthorize("@ss.hasPermi('acq:lead:query')")
    public AjaxResult leads(@RequestParam(name = "page", defaultValue = "1") int page) {
        return AjaxResult.success(query.leads(offset(page)));
    }

    @GetMapping("/events")
    @PreAuthorize("@ss.hasPermi('acq:conversion:query')")
    public AjaxResult events(@RequestParam(name = "page", defaultValue = "1") int page) {
        return AjaxResult.success(query.events(offset(page)));
    }

    @GetMapping("/stats")
    @PreAuthorize("@ss.hasPermi('acq:stats:query')")
    public AjaxResult stats() {
        return AjaxResult.success(query.stats());
    }

    private int offset(int page) {
        if (page < 1 || page > 10000) {
            throw new BusinessException("页码超出范围");
        }
        return (page - 1) * 30;
    }
}
