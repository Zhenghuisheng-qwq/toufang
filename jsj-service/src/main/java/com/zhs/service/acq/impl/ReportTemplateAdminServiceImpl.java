package com.zhs.service.acq.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.AcqDuplicate;
import com.zhs.service.acq.AcqTexts;
import com.zhs.service.acq.AcquisitionCodes;
import com.zhs.service.acq.ConfigViews;
import com.zhs.service.acq.ReportTemplateAdminService;
import com.zhs.service.acq.report.PlainTextReportRenderer;
import com.zhs.service.acq.dto.AcqPage;
import com.zhs.service.acq.dto.ReportCommands;
import com.zhs.service.mapper.acq.ReportTemplateMapper;
import com.zhs.service.mapper.acq.ReportTemplateVersionMapper;
import com.zhs.service.model.acq.ReportTemplate;
import com.zhs.service.model.acq.ReportTemplateVersion;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReportTemplateAdminServiceImpl implements ReportTemplateAdminService {
    public List<Map<String, Object>> options(String keyword) {
        List<ReportTemplate> templates = reportTemplateMapper.selectList(new LambdaQueryWrapper<ReportTemplate>()
                .like(keyword != null && !keyword.isBlank(), ReportTemplate::getTemplateName, keyword)
                .orderByDesc(ReportTemplate::getId).last("LIMIT 100"));
        return templates.stream().map(t -> {
            ReportTemplateVersion v = t.getCurrentVersionId() == null ? null : reportTemplateVersionMapper.selectById(t.getCurrentVersionId());
            boolean complete = v != null && t.getId().equals(v.getTemplateId()) && Integer.valueOf(1).equals(v.getStatus());
            if (complete) {
                try {
                    reportRenderer.validate(v.getContentType(), v.getContentText());
                } catch (BusinessException e) {
                    complete = false;
                }
            }
            return ConfigViews.of("id", t.getId(), "templateName", t.getTemplateName(),
                    "status", t.getStatus(), "currentVersionId", t.getCurrentVersionId(),
                    "versionNo", v == null ? null : v.getVersionNo(), "complete", complete);
        }).toList();
    }

    private static final int CONTENT_LIMIT = 20000;

    @Resource
    private ReportTemplateMapper reportTemplateMapper;

    @Resource
    private ReportTemplateVersionMapper reportTemplateVersionMapper;

    @Resource
    private PlainTextReportRenderer reportRenderer;

    @Override
    public ReportCommands.PreviewResult preview(ReportCommands.Preview command) {
        if (command == null) {
            throw new BusinessException("请填写预览内容和输入");
        }
        return new ReportCommands.PreviewResult(reportRenderer.render(AcquisitionCodes.CONTENT_TEXT,
                command.getContentText(), command.getInput()), "开发预览，不代表正式测算结果");
    }

    @Override
    public AcqPage<ReportCommands.Row> page(ReportCommands.Query query) {
        LambdaQueryWrapper<ReportTemplate> wrapper = new LambdaQueryWrapper<>();
        String name = AcqTexts.likePattern(query == null ? null : query.getTemplateName());
        if (name != null) {
            wrapper.apply("template_name LIKE {0} ESCAPE '\\\\'", name);
        }
        if (query != null && query.getStatus() != null) {
            wrapper.eq(ReportTemplate::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(ReportTemplate::getId);
        Page<ReportTemplate> result = reportTemplateMapper.selectPage(
                new Page<>(AcqTexts.pageNum(query == null ? null : query.getPageNum()),
                        AcqTexts.pageSize(query == null ? null : query.getPageSize())),
                wrapper);
        List<ReportCommands.Row> rows = new ArrayList<>();
        for (ReportTemplate template : result.getRecords()) {
            rows.add(toRow(template));
        }
        return new AcqPage<>(result.getTotal(), rows);
    }

    @Override
    public ReportCommands.Detail detail(Long id) {
        ReportTemplate template = requireTemplate(id);
        ReportCommands.Detail detail = new ReportCommands.Detail();
        detail.setId(template.getId());
        detail.setTemplateName(template.getTemplateName());
        detail.setStatus(template.getStatus());
        detail.setRemark(template.getRemark());
        detail.setCurrentVersionId(template.getCurrentVersionId());
        List<ReportTemplateVersion> versions = reportTemplateVersionMapper.selectList(
                new LambdaQueryWrapper<ReportTemplateVersion>()
                        .eq(ReportTemplateVersion::getTemplateId, id)
                        .orderByDesc(ReportTemplateVersion::getVersionNo));
        for (ReportTemplateVersion version : versions) {
            detail.getVersions().add(toVersion(version, template.getCurrentVersionId()));
        }
        return detail;
    }

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public ReportCommands.Detail create(ReportCommands.Save command) {
        ReportTemplate template = new ReportTemplate();
        template.setId(null);
        template.setTemplateName(nameOf(command));
        template.setRemark(remarkOf(command));
        template.setStatus(AcquisitionCodes.TEMPLATE_DRAFT);
        template.setCurrentVersionId(null);
        reportTemplateMapper.insert(template);
        if (template.getId() == null) {
            throw new BusinessException("模板保存后没有取得主键");
        }
        return detail(template.getId());
    }

    @Override
    public ReportCommands.Detail update(Long id, ReportCommands.Save command) {
        requireTemplate(id);
        int rows = reportTemplateMapper.update(null, new LambdaUpdateWrapper<ReportTemplate>()
                .set(ReportTemplate::getTemplateName, nameOf(command))
                .set(ReportTemplate::getRemark, remarkOf(command))
                .eq(ReportTemplate::getId, id));
        if (rows != 1) {
            throw new BusinessException("模板不存在");
        }
        return detail(id);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        requireTemplate(id);
        int target = AcqTexts.requireChoice(status, "模板状态",
                AcquisitionCodes.TEMPLATE_DRAFT, AcquisitionCodes.TEMPLATE_ENABLED, AcquisitionCodes.TEMPLATE_DISABLED);
        int rows = reportTemplateMapper.update(null, new LambdaUpdateWrapper<ReportTemplate>()
                .set(ReportTemplate::getStatus, target)
                .eq(ReportTemplate::getId, id));
        if (rows != 1) {
            throw new BusinessException("模板状态更新失败");
        }
    }

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的模板");
        }
        if (ids.size() > 50) {
            throw new BusinessException("一次最多删除50条");
        }
        for (Long id : ids) {
            deleteOne(id);
        }
    }

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public ReportCommands.Version createDraft(Long templateId, ReportCommands.Draft command) {
        ReportTemplate template = reportTemplateMapper.lockById(templateId);
        if (template == null) {
            throw new BusinessException("报告模板不存在");
        }
        Integer max = reportTemplateVersionMapper.selectMaxVersionNo(templateId);
        ReportTemplateVersion version = new ReportTemplateVersion();
        version.setId(null);
        version.setTemplateId(templateId);
        version.setVersionNo((max == null ? 0 : max) + 1);
        version.setStatus(AcquisitionCodes.VERSION_DRAFT);
        version.setChangeNote(noteOf(command));
        version.setContentType(AcquisitionCodes.CONTENT_TEXT);
        version.setContentText(contentOf(command, false));
        try {
            reportTemplateVersionMapper.insert(version);
        } catch (RuntimeException ex) {
            if (AcqDuplicate.isDuplicate(ex)) {
                throw new BusinessException("版本号冲突，请重试");
            }
            throw ex;
        }
        if (version.getId() == null) {
            throw new BusinessException("版本保存后没有取得主键");
        }
        return toVersion(version, template.getCurrentVersionId());
    }

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public ReportCommands.Version updateDraft(Long versionId, ReportCommands.Draft command) {
        ReportTemplateVersion version = requireVersion(versionId);
        if (reportTemplateMapper.lockById(version.getTemplateId()) == null) {
            throw new BusinessException("报告模板不存在");
        }
        int rows = reportTemplateVersionMapper.update(null, new LambdaUpdateWrapper<ReportTemplateVersion>()
                .set(ReportTemplateVersion::getChangeNote, noteOf(command))
                .set(ReportTemplateVersion::getContentType, AcquisitionCodes.CONTENT_TEXT)
                .set(ReportTemplateVersion::getContentText, contentOf(command, false))
                .eq(ReportTemplateVersion::getId, versionId)
                .eq(ReportTemplateVersion::getStatus, AcquisitionCodes.VERSION_DRAFT));
        if (rows != 1) {
            throw new BusinessException("已发布版本不能直接修改，请创建新版本");
        }
        return toVersion(requireVersion(versionId), currentVersionId(version.getTemplateId()));
    }

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public ReportCommands.Version publish(Long versionId) {
        ReportTemplateVersion version = requireVersion(versionId);
        ReportTemplate template = reportTemplateMapper.lockById(version.getTemplateId());
        if (template == null) {
            throw new BusinessException("报告模板不存在");
        }
        // Use a current locking read; a repeatable-read snapshot may predate the parent lock.
        version = reportTemplateVersionMapper.lockById(versionId);
        if (version == null) {
            throw new BusinessException("版本不存在");
        }
        if (version.getStatus() != null && version.getStatus() == AcquisitionCodes.VERSION_PUBLISHED) {
            throw new BusinessException("该版本已经发布");
        }
        if (version.getContentText() == null || version.getContentText().isBlank()) {
            throw new BusinessException("发布前请先填写模板正文");
        }
        reportRenderer.validate(version.getContentType(), version.getContentText());
        LocalDateTime publishedTime = LocalDateTime.now();
        int rows = reportTemplateVersionMapper.update(null, new LambdaUpdateWrapper<ReportTemplateVersion>()
                .set(ReportTemplateVersion::getStatus, AcquisitionCodes.VERSION_PUBLISHED)
                .set(ReportTemplateVersion::getPublishedTime, publishedTime)
                .eq(ReportTemplateVersion::getId, versionId)
                .eq(ReportTemplateVersion::getStatus, AcquisitionCodes.VERSION_DRAFT));
        if (rows != 1) {
            throw new BusinessException("只有草稿可以发布");
        }
        int pointed = reportTemplateMapper.update(null, new LambdaUpdateWrapper<ReportTemplate>()
                .set(ReportTemplate::getCurrentVersionId, versionId)
                .eq(ReportTemplate::getId, template.getId()));
        if (pointed != 1) {
            throw new BusinessException("发布版本指针更新失败");
        }
        version.setStatus(AcquisitionCodes.VERSION_PUBLISHED);
        version.setPublishedTime(publishedTime);
        return toVersion(version, versionId);
    }

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public void deleteDraft(Long versionId) {
        ReportTemplateVersion version = requireVersion(versionId);
        if (reportTemplateMapper.lockById(version.getTemplateId()) == null) {
            throw new BusinessException("报告模板不存在");
        }
        version = reportTemplateVersionMapper.lockById(versionId);
        if (version == null) {
            throw new BusinessException("版本不存在");
        }
        if (version.getStatus() == null || version.getStatus() != AcquisitionCodes.VERSION_DRAFT) {
            throw new BusinessException("已发布版本不能删除");
        }
        if (reportTemplateVersionMapper.countReportRef(versionId) > 0) {
            throw new BusinessException("版本已被业务使用，不能删除");
        }
        reportTemplateVersionMapper.deleteById(versionId);
    }

    private void deleteOne(Long id) {
        if (id == null || reportTemplateMapper.lockById(id) == null) {
            throw new BusinessException("报告模板不存在");
        }
        if (reportTemplateMapper.countLandingRef(id) > 0) {
            throw new BusinessException("模板仍被落地页关联，不能删除，请改为停用");
        }
        if (reportTemplateMapper.countReportRef(id) > 0) {
            throw new BusinessException("模板已被报告使用，不能删除，请改为停用");
        }
        List<ReportTemplateVersion> versions = reportTemplateVersionMapper.selectList(
                new LambdaQueryWrapper<ReportTemplateVersion>().eq(ReportTemplateVersion::getTemplateId, id));
        for (ReportTemplateVersion version : versions) {
            if (version.getStatus() != null && version.getStatus() == AcquisitionCodes.VERSION_PUBLISHED) {
                throw new BusinessException("模板存在已发布版本，不能删除，请改为停用");
            }
            if (reportTemplateVersionMapper.countReportRef(version.getId()) > 0) {
                throw new BusinessException("版本已被业务使用，不能删除");
            }
        }
        for (ReportTemplateVersion version : versions) {
            reportTemplateVersionMapper.deleteById(version.getId());
        }
        reportTemplateMapper.deleteById(id);
    }

    private String nameOf(ReportCommands.Save command) {
        if (command == null) {
            throw new BusinessException("请填写模板信息");
        }
        return AcqTexts.requireText(command.getTemplateName(), "模板名称", 128);
    }

    private String remarkOf(ReportCommands.Save command) {
        if (command == null) {
            throw new BusinessException("请填写模板信息");
        }
        return AcqTexts.optionalText(command.getRemark(), "说明", 255);
    }

    private String noteOf(ReportCommands.Draft command) {
        if (command == null) {
            return null;
        }
        return AcqTexts.optionalText(command.getChangeNote(), "版本说明", 255);
    }

    private String contentOf(ReportCommands.Draft command, boolean required) {
        String text = command == null ? null : AcqTexts.optionalText(command.getContentText(), "模板正文", CONTENT_LIMIT);
        if (required && (text == null || text.isBlank())) {
            throw new BusinessException("模板正文不能为空");
        }
        return text;
    }

    private ReportTemplate requireTemplate(Long id) {
        if (id == null) {
            throw new BusinessException("报告模板不存在");
        }
        ReportTemplate template = reportTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("报告模板不存在");
        }
        return template;
    }

    private ReportTemplateVersion requireVersion(Long id) {
        if (id == null) {
            throw new BusinessException("版本不存在");
        }
        ReportTemplateVersion version = reportTemplateVersionMapper.selectById(id);
        if (version == null) {
            throw new BusinessException("版本不存在");
        }
        return version;
    }

    private Long currentVersionId(Long templateId) {
        ReportTemplate template = reportTemplateMapper.selectById(templateId);
        return template == null ? null : template.getCurrentVersionId();
    }

    private ReportCommands.Row toRow(ReportTemplate template) {
        ReportCommands.Row row = new ReportCommands.Row();
        row.setId(template.getId());
        row.setTemplateName(template.getTemplateName());
        row.setStatus(template.getStatus());
        row.setRemark(template.getRemark());
        row.setCurrentVersionId(template.getCurrentVersionId());
        return row;
    }

    private ReportCommands.Version toVersion(ReportTemplateVersion version, Long currentVersionId) {
        ReportCommands.Version view = new ReportCommands.Version();
        view.setId(version.getId());
        view.setTemplateId(version.getTemplateId());
        view.setVersionNo(version.getVersionNo());
        view.setStatus(version.getStatus());
        view.setChangeNote(version.getChangeNote());
        view.setContentType(version.getContentType());
        view.setContentText(version.getContentText());
        view.setPublishedTime(version.getPublishedTime());
        view.setCurrent(version.getId() != null && version.getId().equals(currentVersionId));
        return view;
    }
}
