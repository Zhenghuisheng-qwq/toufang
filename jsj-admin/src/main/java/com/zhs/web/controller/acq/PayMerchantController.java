package com.zhs.web.controller.acq;

import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import com.zhs.service.acq.MerchantAdminService;
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
@RequestMapping("/acq/merchant")
public class PayMerchantController extends BaseController {

    @Resource
    private MerchantAdminService service;

    @PreAuthorize("@ss.hasPermi('acq:merchant:list')")
    @GetMapping("/list")
    public AjaxResult list(ConfigCommands.Query q) {
        return success(service.page(q));
    }

    @PreAuthorize("@ss.hasPermi('acq:merchant:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable("id") Long id) {
        return success(service.detail(id, false));
    }

    @PreAuthorize("@ss.hasPermi('acq:merchant:secret')")
    @GetMapping("/{id}/secrets")
    public AjaxResult secrets(@PathVariable("id") Long id, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return success(service.detail(id, true));
    }

    @PreAuthorize("@ss.hasPermi('acq:merchant:add')")
    @PostMapping
    @Log(title = "投放配置-merchant", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public AjaxResult add(@RequestBody ConfigCommands.Merchant c) {
        return success(Map.of("id", service.save(null, c).toString()));
    }

    @PreAuthorize("@ss.hasPermi('acq:merchant:edit')")
    @PutMapping("/{id}")
    @Log(title = "投放配置-merchant", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public AjaxResult edit(@PathVariable("id") Long id, @RequestBody ConfigCommands.Merchant c) {
        service.save(id, c);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('acq:merchant:status')")
    @PutMapping("/{id}/status")
    @Log(title = "投放配置-merchant", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public AjaxResult status(@PathVariable("id") Long id, @RequestBody StatusCommand c) {
        service.status(id, c.getStatus());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('acq:merchant:remove')")
    @DeleteMapping("/{id}")
    @Log(title = "投放配置-merchant", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public AjaxResult remove(@PathVariable("id") Long id) {
        service.delete(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('acq:merchant:version') and @ss.hasPermi('acq:merchant:secret')")
    @PostMapping("/{id}/version")
    @Log(title = "投放配置-merchant", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public AjaxResult version(@PathVariable("id") Long id, @RequestBody ConfigCommands.MerchantVersion c) {
        return success(Map.of("id", service.saveVersion(id, null, c).toString()));
    }

    @PreAuthorize("@ss.hasPermi('acq:merchant:version') and @ss.hasPermi('acq:merchant:secret')")
    @PutMapping("/{id}/version/{versionId}")
    @Log(title = "投放配置-merchant", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public AjaxResult editVersion(@PathVariable("id") Long id, @PathVariable("versionId") Long versionId, @RequestBody ConfigCommands.MerchantVersion c) {
        service.saveVersion(id, versionId, c);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('acq:merchant:version')")
    @PutMapping("/{id}/version/{versionId}/activate")
    @Log(title = "投放配置-merchant", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public AjaxResult activate(@PathVariable("id") Long id, @PathVariable("versionId") Long versionId) {
        service.activate(id, versionId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('acq:merchant:version')")
    @PutMapping("/{id}/version/deactivate")
    @Log(title = "投放配置-merchant", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    public AjaxResult deactivate(@PathVariable("id") Long id) {
        service.deactivate(id);
        return success();
    }
}
