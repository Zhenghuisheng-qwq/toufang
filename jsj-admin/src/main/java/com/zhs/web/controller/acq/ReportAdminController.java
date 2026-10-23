package com.zhs.web.controller.acq;

import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import com.zhs.service.acq.report.ReportAdminService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/acq/report")
public class ReportAdminController extends BaseController {

    @Resource
    private ReportAdminService reports;

    @GetMapping("/{orderNo}")
    @PreAuthorize("@ss.hasPermi('acq:report:query')")
    public AjaxResult status(@PathVariable("orderNo") String orderNo, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return success(reports.status(orderNo));
    }

    @GetMapping("/{orderNo}/content")
    @PreAuthorize("@ss.hasPermi('acq:report:content')")
    public AjaxResult content(@PathVariable("orderNo") String orderNo, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return AjaxResult.success((Object) reports.content(orderNo));
    }

    @PostMapping("/{orderNo}/retry")
    @PreAuthorize("@ss.hasPermi('acq:report:retry')")
    @Log(title = "付费报告受控补生成", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public AjaxResult retry(@PathVariable("orderNo") String orderNo, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return success(reports.retry(orderNo));
    }
}
