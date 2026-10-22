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
@RequestMapping("/acq/wecom")
public class WecomConfigController extends BaseController {
    @Resource private WecomAdminService service;
    @PreAuthorize("@ss.hasPermi('acq:wecom:list')")
    @GetMapping("/list")
    public AjaxResult list(ConfigCommands.Query q) { return success(service.corps(q)); }

    @PreAuthorize("@ss.hasPermi('acq:wecom:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable("id") Long id) { return success(service.corp(id,false)); }

    @PreAuthorize("@ss.hasPermi('acq:wecom:secret')")
    @GetMapping("/{id}/secrets")
    public AjaxResult secrets(@PathVariable("id") Long id, jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return success(service.corp(id,true));
    }

    @PreAuthorize("@ss.hasPermi('acq:wecom:secret')")
    @PutMapping("/{id}/secrets")
    @Log(title="投放配置-wecom", businessType=BusinessType.UPDATE, isSaveRequestData=false, isSaveResponseData=false)
    public AjaxResult secret(@PathVariable("id") Long id,@RequestBody ConfigCommands.CorpSecrets c) { service.secrets(id,c); return success(); }

    @PreAuthorize("@ss.hasPermi('acq:wecom:add')")
    @PostMapping
    @Log(title="投放配置-wecom", businessType=BusinessType.UPDATE, isSaveRequestData=false, isSaveResponseData=false)
    public AjaxResult add(@RequestBody ConfigCommands.Corp c) { return success(java.util.Map.of("id",service.saveCorp(null,c).toString())); }

    @PreAuthorize("@ss.hasPermi('acq:wecom:edit')")
    @PutMapping("/{id}")
    @Log(title="投放配置-wecom", businessType=BusinessType.UPDATE, isSaveRequestData=false, isSaveResponseData=false)
    public AjaxResult edit(@PathVariable("id") Long id,@RequestBody ConfigCommands.Corp c) { service.saveCorp(id,c); return success(); }

    @PreAuthorize("@ss.hasPermi('acq:wecom:status')")
    @PutMapping("/{id}/status")
    @Log(title="投放配置-wecom", businessType=BusinessType.UPDATE, isSaveRequestData=false, isSaveResponseData=false)
    public AjaxResult status(@PathVariable("id") Long id,@RequestBody StatusCommand c) { service.corpStatus(id,c.getStatus()); return success(); }

    @PreAuthorize("@ss.hasPermi('acq:wecom:remove')")
    @DeleteMapping("/{id}")
    @Log(title="投放配置-wecom", businessType=BusinessType.UPDATE, isSaveRequestData=false, isSaveResponseData=false)
    public AjaxResult remove(@PathVariable("id") Long id) { service.deleteCorp(id); return success(); }

    @PreAuthorize("@ss.hasPermi('acq:wecom:add')")
    @PostMapping("/teacher")
    @Log(title="投放配置-wecom", businessType=BusinessType.UPDATE, isSaveRequestData=false, isSaveResponseData=false)
    public AjaxResult addTeacher(@RequestBody ConfigCommands.Teacher c) { return success(java.util.Map.of("id",service.saveTeacher(null,c).toString())); }

    @PreAuthorize("@ss.hasPermi('acq:wecom:edit')")
    @PutMapping("/teacher/{id}")
    @Log(title="投放配置-wecom", businessType=BusinessType.UPDATE, isSaveRequestData=false, isSaveResponseData=false)
    public AjaxResult editTeacher(@PathVariable("id") Long id,@RequestBody ConfigCommands.Teacher c) { service.saveTeacher(id,c); return success(); }

    @PreAuthorize("@ss.hasPermi('acq:wecom:status')")
    @PutMapping("/teacher/{id}/status")
    @Log(title="投放配置-wecom", businessType=BusinessType.UPDATE, isSaveRequestData=false, isSaveResponseData=false)
    public AjaxResult statusTeacher(@PathVariable("id") Long id,@RequestBody StatusCommand c) { service.teacherStatus(id,c.getStatus()); return success(); }

    @PreAuthorize("@ss.hasPermi('acq:wecom:remove')")
    @DeleteMapping("/teacher/{id}")
    @Log(title="投放配置-wecom", businessType=BusinessType.UPDATE, isSaveRequestData=false, isSaveResponseData=false)
    public AjaxResult removeTeacher(@PathVariable("id") Long id) { service.deleteTeacher(id); return success(); }

    @PreAuthorize("@ss.hasPermi('acq:wecom:add')")
    @PostMapping("/tag")
    @Log(title="投放配置-wecom", businessType=BusinessType.UPDATE, isSaveRequestData=false, isSaveResponseData=false)
    public AjaxResult addTag(@RequestBody ConfigCommands.Tag c) { return success(java.util.Map.of("id",service.saveTag(null,c).toString())); }

    @PreAuthorize("@ss.hasPermi('acq:wecom:edit')")
    @PutMapping("/tag/{id}")
    @Log(title="投放配置-wecom", businessType=BusinessType.UPDATE, isSaveRequestData=false, isSaveResponseData=false)
    public AjaxResult editTag(@PathVariable("id") Long id,@RequestBody ConfigCommands.Tag c) { service.saveTag(id,c); return success(); }

    @PreAuthorize("@ss.hasPermi('acq:wecom:status')")
    @PutMapping("/tag/{id}/status")
    @Log(title="投放配置-wecom", businessType=BusinessType.UPDATE, isSaveRequestData=false, isSaveResponseData=false)
    public AjaxResult statusTag(@PathVariable("id") Long id,@RequestBody StatusCommand c) { service.tagStatus(id,c.getStatus()); return success(); }

    @PreAuthorize("@ss.hasPermi('acq:wecom:remove')")
    @DeleteMapping("/tag/{id}")
    @Log(title="投放配置-wecom", businessType=BusinessType.UPDATE, isSaveRequestData=false, isSaveResponseData=false)
    public AjaxResult removeTag(@PathVariable("id") Long id) { service.deleteTag(id); return success(); }

}

