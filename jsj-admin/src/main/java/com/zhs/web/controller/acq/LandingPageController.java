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
import com.zhs.service.acq.LandingPageAdminService;
import com.zhs.service.acq.dto.AcqPage;
import com.zhs.service.acq.dto.LandingPageQuery;
import com.zhs.service.acq.dto.LandingPageSaveCommand;
import com.zhs.service.acq.dto.StatusCommand;
import jakarta.annotation.Resource;

@RestController
@RequestMapping("/acq/landing")
public class LandingPageController extends BaseController {

    @Resource
    private LandingPageAdminService landingPageAdminService;

    @PreAuthorize("@ss.hasPermi('acq:landing:list')")
    @GetMapping("/list")
    public TableDataInfo list(LandingPageQuery query) {
        return table(landingPageAdminService.page(query));
    }

    @PreAuthorize("@ss.hasPermi('acq:domain:link')")
    @GetMapping("/options")
    public AjaxResult options() {
        return success(landingPageAdminService.options());
    }

    @PreAuthorize("@ss.hasPermi('acq:landing:query') or @ss.hasPermi('acq:landing:preview')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(landingPageAdminService.detail(id));
    }

    @PreAuthorize("@ss.hasPermi('acq:landing:add')")
    @Log(title = "落地页", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LandingPageSaveCommand command) {
        return success(landingPageAdminService.create(command));
    }

    @PreAuthorize("@ss.hasPermi('acq:landing:edit')")
    @Log(title = "落地页", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable("id") Long id, @RequestBody LandingPageSaveCommand command) {
        return success(landingPageAdminService.update(id, command));
    }

    @PreAuthorize("@ss.hasPermi('acq:landing:status')")
    @Log(title = "落地页状态", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody StatusCommand command) {
        landingPageAdminService.changeStatus(command == null ? null : command.getId(),
                command == null ? null : command.getStatus());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('acq:landing:remove')")
    @Log(title = "落地页", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        landingPageAdminService.delete(ids == null ? List.of() : Arrays.asList(ids));
        return success();
    }

    private TableDataInfo table(AcqPage<?> page) {
        TableDataInfo data = new TableDataInfo(page.getRows(), page.getTotal());
        data.setCode(HttpStatus.SUCCESS);
        data.setMsg("查询成功");
        return data;
    }
}
