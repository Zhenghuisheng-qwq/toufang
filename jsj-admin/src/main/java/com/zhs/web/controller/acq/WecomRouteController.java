package com.zhs.web.controller.acq;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.annotation.Log;
import com.zhs.common.enums.BusinessType;
import com.zhs.service.acq.WecomAdminService;
import com.zhs.service.acq.dto.ConfigCommands;
import com.zhs.service.acq.dto.StatusCommand;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/acq/route")
public class WecomRouteController extends BaseController {
    @Resource private WecomAdminService service;
    @PreAuthorize("@ss.hasPermi('acq:route:add') or @ss.hasPermi('acq:route:edit')")
    @GetMapping("/options")
    public AjaxResult options(ConfigCommands.Query q) { return success(service.corps(q)); }

    @PreAuthorize("@ss.hasPermi('acq:route:add') or @ss.hasPermi('acq:route:edit')")
    @GetMapping("/options/{corpId}")
    public AjaxResult teachers(@PathVariable("corpId") Long corpId) { return success(service.routeOptions(corpId)); }
    @PreAuthorize("@ss.hasPermi('acq:route:list')")
    @GetMapping("/list")
    public AjaxResult list(ConfigCommands.Query q) { return success(service.routes(q)); }

    @PreAuthorize("@ss.hasPermi('acq:route:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable("id") Long id) { return success(service.route(id)); }

    @PreAuthorize("@ss.hasPermi('acq:route:preview')")
    @GetMapping("/preview")
    public AjaxResult preview(@RequestParam(value="landingPageId",required=false) Long pageId) { return success(service.resolve(pageId)); }

    @PreAuthorize("@ss.hasPermi('acq:route:add')")
    @PostMapping
    @Log(title="投放配置-route", businessType=BusinessType.UPDATE, isSaveRequestData=false, isSaveResponseData=false)
    public AjaxResult add(@RequestBody ConfigCommands.Route c) { return success(java.util.Map.of("id",service.saveRoute(null,c).toString())); }

    @PreAuthorize("@ss.hasPermi('acq:route:edit')")
    @PutMapping("/{id}")
    @Log(title="投放配置-route", businessType=BusinessType.UPDATE, isSaveRequestData=false, isSaveResponseData=false)
    public AjaxResult edit(@PathVariable("id") Long id,@RequestBody ConfigCommands.Route c) { service.saveRoute(id,c); return success(); }

    @PreAuthorize("@ss.hasPermi('acq:route:switch')")
    @PutMapping("/{id}/status")
    @Log(title="投放配置-route", businessType=BusinessType.UPDATE, isSaveRequestData=false, isSaveResponseData=false)
    public AjaxResult status(@PathVariable("id") Long id,@RequestBody StatusCommand c) { service.routeStatus(id,c.getStatus()); return success(); }

    @PreAuthorize("@ss.hasPermi('acq:route:remove')")
    @DeleteMapping("/{id}")
    @Log(title="投放配置-route", businessType=BusinessType.UPDATE, isSaveRequestData=false, isSaveResponseData=false)
    public AjaxResult remove(@PathVariable("id") Long id) { service.deleteRoute(id); return success(); }

}

