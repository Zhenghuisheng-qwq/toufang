package com.zhs.web.controller.acq;

import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import com.zhs.service.acq.AdAccountAdminService;
import com.zhs.service.acq.dto.ConfigCommands;
import com.zhs.service.acq.dto.StatusCommand;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/acq/ad")
public class AdAccountController extends BaseController {

    @Resource
    private AdAccountAdminService service;

    @PreAuthorize("@ss.hasPermi('acq:ad:list')")
    @GetMapping("/list")
    public AjaxResult list(ConfigCommands.Query q) {
        return success(service.page(q));
    }

    @PreAuthorize("@ss.hasPermi('acq:ad:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable("id") Long id) {
        return success(service.detail(id, false));
    }

    @PreAuthorize("@ss.hasPermi('acq:ad:secret')")
    @GetMapping("/{id}/secrets")
    public AjaxResult secrets(@PathVariable("id") Long id, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return success(service.detail(id, true));
    }

    @PreAuthorize("@ss.hasPermi('acq:ad:secret')")
    @PutMapping("/{id}/secrets")
    @Log(title = "投放配置-ad", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public AjaxResult secret(@PathVariable("id") Long id, @RequestBody ConfigCommands.AdSecret c) {
        service.secret(id, c);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('acq:ad:add')")
    @PostMapping
    @Log(title = "投放配置-ad", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public AjaxResult add(@RequestBody ConfigCommands.AdAccount c) {
        return success(Map.of("id", service.save(null, c).toString()));
    }

    @PreAuthorize("@ss.hasPermi('acq:ad:edit')")
    @PutMapping("/{id}")
    @Log(title = "投放配置-ad", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public AjaxResult edit(@PathVariable("id") Long id, @RequestBody ConfigCommands.AdAccount c) {
        service.save(id, c);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('acq:ad:status')")
    @PutMapping("/{id}/status")
    @Log(title = "投放配置-ad", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public AjaxResult status(@PathVariable("id") Long id, @RequestBody StatusCommand c) {
        service.status(id, c.getStatus());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('acq:ad:remove')")
    @DeleteMapping("/{id}")
    @Log(title = "投放配置-ad", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public AjaxResult remove(@PathVariable("id") Long id) {
        service.delete(id);
        return success();
    }
}
