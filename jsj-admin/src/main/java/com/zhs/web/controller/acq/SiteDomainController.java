package com.zhs.web.controller.acq;

import java.util.Arrays;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhs.common.annotation.Log;
import com.zhs.common.constant.HttpStatus;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import com.zhs.service.acq.SiteDomainAdminService;
import com.zhs.service.acq.dto.AcqPage;
import com.zhs.service.acq.dto.DomainCommands;
import com.zhs.service.acq.dto.StatusCommand;
import jakarta.annotation.Resource;

@RestController
@RequestMapping("/acq/domain")
public class SiteDomainController extends BaseController {

    @Resource
    private SiteDomainAdminService siteDomainAdminService;

    @PreAuthorize("@ss.hasPermi('acq:domain:list')")
    @GetMapping("/list")
    public TableDataInfo list(DomainCommands.Query query) {
        return table(siteDomainAdminService.page(query));
    }

    @PreAuthorize("@ss.hasPermi('acq:domain:link')")
    @GetMapping("/merchant-options")
    public AjaxResult merchantOptions() {
        return success(siteDomainAdminService.merchantOptions());
    }

    @PreAuthorize("@ss.hasPermi('acq:domain:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(siteDomainAdminService.detail(id));
    }

    @PreAuthorize("@ss.hasPermi('acq:domain:add')")
    @Log(title = "投放域名", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DomainCommands.Save command) {
        return success(siteDomainAdminService.create(command));
    }

    @PreAuthorize("@ss.hasPermi('acq:domain:edit')")
    @Log(title = "投放域名", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable("id") Long id, @RequestBody DomainCommands.Save command) {
        return success(siteDomainAdminService.update(id, command));
    }

    @PreAuthorize("@ss.hasPermi('acq:domain:status')")
    @Log(title = "投放域名状态", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody StatusCommand command) {
        siteDomainAdminService.changeStatus(command == null ? null : command.getId(),
                command == null ? null : command.getStatus());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('acq:domain:remove')")
    @Log(title = "投放域名", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        siteDomainAdminService.delete(ids == null ? List.of() : Arrays.asList(ids));
        return success();
    }

    @PreAuthorize("@ss.hasPermi('acq:domain:link')")
    @Log(title = "域名落地页关联", businessType = BusinessType.GRANT)
    @PostMapping("/{id}/landing")
    public AjaxResult linkLanding(@PathVariable("id") Long id, @RequestBody DomainCommands.Link command) {
        siteDomainAdminService.linkLanding(id, command == null ? null : command.getLandingPageId());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('acq:domain:link')")
    @Log(title = "域名落地页关联", businessType = BusinessType.GRANT)
    @DeleteMapping("/{id}/landing/{landingPageId}")
    public AjaxResult unlinkLanding(@PathVariable("id") Long id, @PathVariable("landingPageId") Long landingPageId) {
        siteDomainAdminService.unlinkLanding(id, landingPageId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('acq:domain:link')")
    @Log(title = "域名商户关联", businessType = BusinessType.GRANT)
    @PostMapping("/{id}/merchant")
    public AjaxResult linkMerchant(@PathVariable("id") Long id, @RequestBody DomainCommands.Link command) {
        siteDomainAdminService.linkMerchant(id, command == null ? null : command.getPayMerchantId());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('acq:domain:link')")
    @Log(title = "域名商户关联", businessType = BusinessType.GRANT)
    @DeleteMapping("/{id}/merchant/{payMerchantId}")
    public AjaxResult unlinkMerchant(@PathVariable("id") Long id, @PathVariable("payMerchantId") Long payMerchantId) {
        siteDomainAdminService.unlinkMerchant(id, payMerchantId);
        return success();
    }

    private TableDataInfo table(AcqPage<?> page) {
        TableDataInfo data = new TableDataInfo(page.getRows(), page.getTotal());
        data.setCode(HttpStatus.SUCCESS);
        data.setMsg("查询成功");
        return data;
    }
}
