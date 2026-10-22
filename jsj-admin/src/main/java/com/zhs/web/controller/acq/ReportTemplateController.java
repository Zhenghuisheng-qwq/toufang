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
import com.zhs.service.acq.ReportTemplateAdminService;
import com.zhs.service.acq.dto.AcqPage;
import com.zhs.service.acq.dto.ReportCommands;
import com.zhs.service.acq.dto.StatusCommand;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/acq/template")
public class ReportTemplateController extends BaseController {

    @Resource
    private ReportTemplateAdminService reportTemplateAdminService;

    @PreAuthorize("@ss.hasPermi('acq:landing:add') or @ss.hasPermi('acq:landing:edit')")
    @GetMapping("/options")
    public AjaxResult options(@org.springframework.web.bind.annotation.RequestParam(value="keyword", required=false) String keyword) {
        return success(reportTemplateAdminService.options(keyword));
    }

    @PreAuthorize("@ss.hasPermi('acq:template:preview')")
    @PostMapping("/preview")
    public AjaxResult preview(@RequestBody ReportCommands.Preview command, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return success(reportTemplateAdminService.preview(command));
    }

    @PreAuthorize("@ss.hasPermi('acq:template:list')")
    @GetMapping("/list")
    public TableDataInfo list(ReportCommands.Query query) {
        return table(reportTemplateAdminService.page(query));
    }

    @PreAuthorize("@ss.hasPermi('acq:template:query') or @ss.hasPermi('acq:template:preview')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(reportTemplateAdminService.detail(id));
    }

    @PreAuthorize("@ss.hasPermi('acq:template:add')")
    @Log(title = "报告模板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ReportCommands.Save command) {
        return success(reportTemplateAdminService.create(command));
    }

    @PreAuthorize("@ss.hasPermi('acq:template:edit')")
    @Log(title = "报告模板", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable("id") Long id, @RequestBody ReportCommands.Save command) {
        return success(reportTemplateAdminService.update(id, command));
    }

    @PreAuthorize("@ss.hasPermi('acq:template:status')")
    @Log(title = "报告模板状态", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody StatusCommand command) {
        reportTemplateAdminService.changeStatus(command == null ? null : command.getId(),
                command == null ? null : command.getStatus());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('acq:template:remove')")
    @Log(title = "报告模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        reportTemplateAdminService.delete(ids == null ? List.of() : Arrays.asList(ids));
        return success();
    }

    @PreAuthorize("@ss.hasPermi('acq:template:edit')")
    @Log(title = "报告模板草稿", businessType = BusinessType.INSERT)
    @PostMapping("/{id}/draft")
    public AjaxResult draft(@PathVariable("id") Long id, @RequestBody ReportCommands.Draft command) {
        return success(reportTemplateAdminService.createDraft(id, command));
    }

    @PreAuthorize("@ss.hasPermi('acq:template:edit')")
    @Log(title = "报告模板草稿", businessType = BusinessType.UPDATE)
    @PutMapping("/version/{versionId}")
    public AjaxResult editDraft(@PathVariable("versionId") Long versionId, @RequestBody ReportCommands.Draft command) {
        return success(reportTemplateAdminService.updateDraft(versionId, command));
    }

    @PreAuthorize("@ss.hasPermi('acq:template:publish')")
    @Log(title = "报告模板发布", businessType = BusinessType.UPDATE)
    @PostMapping("/version/{versionId}/publish")
    public AjaxResult publish(@PathVariable("versionId") Long versionId) {
        return success(reportTemplateAdminService.publish(versionId));
    }

    @PreAuthorize("@ss.hasPermi('acq:template:remove')")
    @Log(title = "报告模板草稿", businessType = BusinessType.DELETE)
    @DeleteMapping("/version/{versionId}")
    public AjaxResult removeDraft(@PathVariable("versionId") Long versionId) {
        reportTemplateAdminService.deleteDraft(versionId);
        return success();
    }

    private TableDataInfo table(AcqPage<?> page) {
        TableDataInfo data = new TableDataInfo(page.getRows(), page.getTotal());
        data.setCode(HttpStatus.SUCCESS);
        data.setMsg("查询成功");
        return data;
    }
}
