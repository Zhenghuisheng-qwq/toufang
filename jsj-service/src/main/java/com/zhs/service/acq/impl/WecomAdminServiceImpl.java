package com.zhs.service.acq.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.AcqDuplicate;
import com.zhs.service.acq.AcqTexts;
import com.zhs.service.acq.ConfigRules;
import com.zhs.service.acq.RouteEligibility;
import com.zhs.service.acq.SecretRefGuard;
import com.zhs.service.acq.WecomAdminService;
import com.zhs.service.acq.WecomRouteSelector;
import com.zhs.service.acq.WecomRouteService;
import com.zhs.service.acq.dto.AcqPage;
import com.zhs.service.acq.dto.ConfigCommands;
import com.zhs.service.mapper.acq.ConfigReferenceMapper;
import com.zhs.service.mapper.acq.LandingPageMapper;
import com.zhs.service.mapper.acq.WecomCorpMapper;
import com.zhs.service.mapper.acq.WecomRouteMapper;
import com.zhs.service.mapper.acq.WecomTagMapper;
import com.zhs.service.mapper.acq.WecomTeacherMapper;
import com.zhs.service.model.acq.WecomCorp;
import com.zhs.service.model.acq.WecomRoute;
import com.zhs.service.model.acq.WecomTag;
import com.zhs.service.model.acq.WecomTeacher;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.zhs.service.acq.ConfigViews.of;

@Service
@Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
public class WecomAdminServiceImpl implements WecomAdminService {

    public Map<String, Object> routeOptions(Long corpId) {
        WecomCorp c = corps.selectById(corpId);
        if (c == null) {
            throw new BusinessException("企业不存在");
        }
        Map<String, Object> result = of("id", c.getId(), "corpName", c.getCorpName(), "corpId", c.getCorpId());
        result.put("teachers", teachers.selectList(new LambdaQueryWrapper<WecomTeacher>()
                .eq(WecomTeacher::getCorpRowId, corpId)).stream()
                .map(t -> of("id", t.getId(), "teacherName", t.getTeacherName(),
                        "teacherUserid", t.getTeacherUserid(), "status", t.getStatus())).toList());
        return result;
    }

    @Resource
    private WecomCorpMapper corps;

    @Resource
    private WecomTeacherMapper teachers;

    @Resource
    private WecomTagMapper tags;

    @Resource
    private WecomRouteMapper routes;

    @Resource
    private LandingPageMapper pages;

    @Resource
    private ConfigReferenceMapper refs;

    @Resource
    private WecomRouteService routeService;

    public AcqPage<Map<String, Object>> corps(ConfigCommands.Query q) {
        LambdaQueryWrapper<WecomCorp> w = new LambdaQueryWrapper<>();
        if (q.getKeyword() != null) {
            w.like(WecomCorp::getCorpName, q.getKeyword());
        }
        if (q.getStatus() != null) {
            w.eq(WecomCorp::getStatus, q.getStatus());
        }
        Page<WecomCorp> p = corps.selectPage(new Page<>(AcqTexts.pageNum(q.getPageNum()), AcqTexts.pageSize(q.getPageSize())), w.orderByDesc(WecomCorp::getId));
        return new AcqPage<>(p.getTotal(), p.getRecords().stream().map(this::corpView).toList());
    }

    public Map<String, Object> corp(Long id, boolean secrets) {
        WecomCorp c = corps.selectById(id);
        if (c == null) {
            throw new BusinessException("企业不存在");
        }
        Map<String, Object> result = corpView(c);
        if (secrets) {
            result.put("contactSecretRef", c.getContactSecretRef());
            result.put("tokenRef", c.getTokenRef());
            result.put("encodingAesKeyRef", c.getEncodingAesKeyRef());
        }
        result.put("teachers", teachers.selectList(new LambdaQueryWrapper<WecomTeacher>().eq(WecomTeacher::getCorpRowId, id)).stream()
                .map(t -> of("id", t.getId(), "corpRowId", t.getCorpRowId(), "teacherUserid", t.getTeacherUserid(), "teacherName", t.getTeacherName(), "status", t.getStatus(), "routeReferences", routeCount(null, t.getId()))).toList());
        result.put("tags", tags.selectList(new LambdaQueryWrapper<WecomTag>().eq(WecomTag::getCorpId, c.getCorpId())).stream()
                .map(t -> of("id", t.getId(), "corpRowId", id, "tagId", t.getTagId(), "tagName", t.getTagName(), "groupId", t.getGroupId(), "groupName", t.getGroupName(), "status", t.getStatus())).toList());
        result.put("routes", routes.selectList(new LambdaQueryWrapper<WecomRoute>().eq(WecomRoute::getCorpRowId, id)).stream().map(this::routeView).toList());
        return result;
    }

    public Long saveCorp(Long id, ConfigCommands.Corp c) {
        refs.lockRoutes();
        if (c == null) {
            throw new BusinessException("请填写企业");
        }
        WecomCorp row = id == null ? new WecomCorp() : requireCorp(id);
        String corpId = ConfigRules.id(c.corpId(), "CorpID");
        String sid = ConfigRules.id(c.sid(), "回调标识");
        boolean used = id != null && refs.corp(id, row.getCorpId()) > 0;
        ConfigRules.unchanged(row.getCorpId(), corpId, used, "CorpID");
        ConfigRules.unchanged(row.getSid(), sid, used, "回调标识");
        ConfigRules.unchanged(row.getAuthMode(), c.authMode(), used, "授权模式");
        row.setCorpId(corpId);
        row.setSid(sid);
        row.setCorpName(AcqTexts.requireText(c.corpName(), "企业名称", 128));
        row.setAuthMode(ConfigRules.choice(c.authMode(), "授权模式", Set.of("UNCONFIRMED", "SELF_BUILT", "THIRD_PARTY")));
        row.setRemark(AcqTexts.optionalText(c.remark(), "备注", 255));
        try {
            if (id == null) {
                row.setStatus(0);
                corps.insert(row);
            } else {
                corps.update(null, new LambdaUpdateWrapper<WecomCorp>()
                        .eq(WecomCorp::getId, id)
                        .set(WecomCorp::getCorpName, row.getCorpName())
                        .set(WecomCorp::getCorpId, corpId)
                        .set(WecomCorp::getSid, sid)
                        .set(WecomCorp::getAuthMode, row.getAuthMode())
                        .set(WecomCorp::getRemark, row.getRemark()));
            }
        } catch (RuntimeException e) {
            duplicate(e, "CorpID或回调标识已存在，包括历史记录");
        }
        return row.getId();
    }

    public void secrets(Long id, ConfigCommands.CorpSecrets c) {
        requireCorp(id);
        if (c == null) {
            throw new BusinessException("请填写引用");
        }
        corps.update(null, new LambdaUpdateWrapper<WecomCorp>()
                .eq(WecomCorp::getId, id)
                .set(WecomCorp::getContactSecretRef, reference(c.contactSecretRef()))
                .set(WecomCorp::getTokenRef, reference(c.tokenRef()))
                .set(WecomCorp::getEncodingAesKeyRef, reference(c.encodingAesKeyRef())));
    }

    public void corpStatus(Long id, Integer status) {
        refs.lockRoutes();
        requireCorp(id);
        int target = AcqTexts.requireFlag(status, "状态");
        if (target == 0 && routeCount(id, null) > 0) {
            throw new BusinessException("企业被当前承接路由使用，请先停用或切换路由");
        }
        corps.update(null, new LambdaUpdateWrapper<WecomCorp>()
                .eq(WecomCorp::getId, id)
                .set(WecomCorp::getStatus, target));
    }

    public void deleteCorp(Long id) {
        refs.lockRoutes();
        WecomCorp c = requireCorp(id);
        if (refs.corp(id, c.getCorpId()) > 0) {
            throw new BusinessException("企业已有老师、标签、路由或历史业务引用，请停用");
        }
        corps.deleteById(id);
    }

    public Long saveTeacher(Long id, ConfigCommands.Teacher c) {
        refs.lockRoutes();
        if (c == null) {
            throw new BusinessException("请填写老师");
        }
        WecomCorp corp = requireCorp(c.corpRowId());
        WecomTeacher row = id == null ? new WecomTeacher() : requireTeacher(id);
        String userid = ConfigRules.id(c.teacherUserid(), "老师UserID");
        boolean used = id != null && refs.teacher(id, row.getCorpId(), row.getTeacherUserid()) > 0;
        // A member's enterprise is immutable even before first use; move by creating a new record.
        ConfigRules.unchanged(row.getCorpRowId(), corp.getId(), id != null, "所属企业");
        ConfigRules.unchanged(row.getTeacherUserid(), userid, used, "老师UserID");
        row.setCorpRowId(corp.getId());
        row.setCorpId(corp.getCorpId());
        row.setTeacherUserid(userid);
        row.setTeacherName(AcqTexts.requireText(c.teacherName(), "老师名称", 64));
        try {
            if (id == null) {
                row.setStatus(0);
                teachers.insert(row);
            } else {
                teachers.updateById(row);
            }
        } catch (RuntimeException e) {
            duplicate(e, "企业内老师UserID已存在，包括历史记录");
        }
        return row.getId();
    }

    public void teacherStatus(Long id, Integer status) {
        refs.lockRoutes();
        WecomTeacher t = requireTeacher(id);
        requireCorp(t.getCorpRowId());
        int target = AcqTexts.requireFlag(status, "状态");
        if (target == 0 && routeCount(null, id) > 0) {
            throw new BusinessException("老师被当前承接使用，请先解除路由");
        }
        teachers.update(null, new LambdaUpdateWrapper<WecomTeacher>()
                .eq(WecomTeacher::getId, id)
                .set(WecomTeacher::getStatus, target));
    }

    public void deleteTeacher(Long id) {
        refs.lockRoutes();
        WecomTeacher t = requireTeacher(id);
        if (refs.teacher(id, t.getCorpId(), t.getTeacherUserid()) > 0) {
            throw new BusinessException("老师已有路由或业务引用，请停用");
        }
        teachers.deleteById(id);
    }

    public Long saveTag(Long id, ConfigCommands.Tag c) {
        if (c == null) {
            throw new BusinessException("请填写标签");
        }
        WecomCorp corp = requireCorp(c.corpRowId());
        WecomTag row = id == null ? new WecomTag() : requireTag(id);
        String tagId = ConfigRules.id(c.tagId(), "官方标签ID");
        ConfigRules.unchanged(row.getCorpId(), corp.getCorpId(), id != null, "标签企业");
        ConfigRules.unchanged(row.getTagId(), tagId, id != null, "官方标签ID");
        row.setCorpId(corp.getCorpId());
        row.setTagId(tagId);
        row.setTagName(AcqTexts.requireText(c.tagName(), "标签名称", 64));
        row.setGroupId(AcqTexts.optionalText(c.groupId(), "标签组ID", 64));
        row.setGroupName(AcqTexts.optionalText(c.groupName(), "标签组名称", 64));
        try {
            if (id == null) {
                row.setStatus(0);
                tags.insert(row);
            } else {
                tags.update(null, new LambdaUpdateWrapper<WecomTag>()
                        .eq(WecomTag::getId, id)
                        .set(WecomTag::getTagName, row.getTagName())
                        .set(WecomTag::getGroupId, row.getGroupId())
                        .set(WecomTag::getGroupName, row.getGroupName()));
            }
        } catch (RuntimeException e) {
            duplicate(e, "企业内标签ID已存在，包括历史记录");
        }
        return row.getId();
    }

    public void tagStatus(Long id, Integer status) {
        requireTag(id);
        tags.update(null, new LambdaUpdateWrapper<WecomTag>()
                .eq(WecomTag::getId, id)
                .set(WecomTag::getStatus, AcqTexts.requireFlag(status, "状态")));
    }

    public void deleteTag(Long id) {
        WecomTag t = requireTag(id);
        if (Integer.valueOf(1).equals(t.getStatus())) {
            throw new BusinessException("请先停用标签");
        }
        tags.deleteById(id);
    }

    public AcqPage<Map<String, Object>> routes(ConfigCommands.Query q) {
        LambdaQueryWrapper<WecomRoute> w = new LambdaQueryWrapper<>();
        if (q.getCorpRowId() != null) {
            w.eq(WecomRoute::getCorpRowId, q.getCorpRowId());
        }
        if (q.getStatus() != null) {
            w.eq(WecomRoute::getStatus, q.getStatus());
        }
        Page<WecomRoute> p = routes.selectPage(new Page<>(AcqTexts.pageNum(q.getPageNum()), AcqTexts.pageSize(q.getPageSize())), w.orderByDesc(WecomRoute::getId));
        return new AcqPage<>(p.getTotal(), p.getRecords().stream().map(this::routeView).toList());
    }

    public Map<String, Object> route(Long id) {
        return routeView(requireRoute(id));
    }

    public Long saveRoute(Long id, ConfigCommands.Route c) {
        refs.lockRoutes();
        if (c == null) {
            throw new BusinessException("请填写承接规则");
        }
        WecomCorp corp = requireCorp(c.corpRowId());
        WecomTeacher teacher = requireTeacher(c.teacherRowId());
        ConfigRules.ownership(corp.getId(), teacher.getCorpRowId());
        WecomRoute row = id == null ? new WecomRoute() : requireRoute(id);
        if (id != null && (Integer.valueOf(1).equals(row.getStatus()) || refs.route(id) > 0)) {
            throw new BusinessException("当前或已有订单引用的路由不能覆盖，请新建后切换");
        }
        int scope = AcqTexts.requireChoice(c.scopeType(), "范围", 1, 2);
        Long pageId = scope == 1 ? null : c.landingPageId();
        WecomRouteSelector.activeKey(scope, pageId);
        if (scope == 2 && pages.selectById(pageId) == null) {
            throw new BusinessException("落地页不存在");
        }
        int entry = AcqTexts.requireChoice(c.entryType(), "获客类型", 1, 2);
        String link = ConfigRules.link(c.linkUrl());
        String linkId = AcqTexts.optionalText(c.linkId(), "链接ID", 128);
        if (entry == 1 && (link != null || linkId != null)) {
            throw new BusinessException("联系我二维码配置不能混入获客助手链接");
        }
        row.setScopeType(scope);
        row.setLandingPageId(pageId);
        row.setCorpRowId(corp.getId());
        row.setTeacherRowId(teacher.getId());
        row.setCorpId(corp.getCorpId());
        row.setTeacherUserid(teacher.getTeacherUserid());
        row.setEntryType(entry);
        row.setLinkId(linkId);
        row.setLinkUrl(link);
        row.setCapabilityNote(AcqTexts.optionalText(c.capabilityNote(), "权限额度登记", 255));
        row.setRemark(AcqTexts.optionalText(c.remark(), "备注", 255));
        row.setSortNo(c.sortNo() == null ? 0 : c.sortNo());
        row.setStatus(0);
        row.setActiveKey(null);
        if (id == null) {
            routes.insert(row);
        } else {
            routes.update(null, new LambdaUpdateWrapper<WecomRoute>()
                    .eq(WecomRoute::getId, id)
                    .set(WecomRoute::getScopeType, scope)
                    .set(WecomRoute::getLandingPageId, pageId)
                    .set(WecomRoute::getCorpRowId, corp.getId())
                    .set(WecomRoute::getTeacherRowId, teacher.getId())
                    .set(WecomRoute::getCorpId, corp.getCorpId())
                    .set(WecomRoute::getTeacherUserid, teacher.getTeacherUserid())
                    .set(WecomRoute::getEntryType, entry)
                    .set(WecomRoute::getLinkId, linkId)
                    .set(WecomRoute::getLinkUrl, link)
                    .set(WecomRoute::getCapabilityNote, row.getCapabilityNote())
                    .set(WecomRoute::getRemark, row.getRemark())
                    .set(WecomRoute::getSortNo, row.getSortNo()));
        }
        return row.getId();
    }

    public void routeStatus(Long id, Integer status) {
        refs.lockRoutes();
        requireRoute(id);
        if (AcqTexts.requireFlag(status, "状态") == 1) {
            routeService.activate(id);
        } else {
            routes.update(null, new LambdaUpdateWrapper<WecomRoute>()
                    .eq(WecomRoute::getId, id)
                    .set(WecomRoute::getStatus, 0)
                    .set(WecomRoute::getActiveKey, null));
        }
    }

    public void deleteRoute(Long id) {
        refs.lockRoutes();
        WecomRoute r = requireRoute(id);
        if (Integer.valueOf(1).equals(r.getStatus()) || refs.route(id) > 0) {
            throw new BusinessException("当前或已有订单引用的路由不能删除");
        }
        routes.deleteById(id);
    }

    public Map<String, Object> resolve(Long pageId) {
        if (pageId != null && pages.selectById(pageId) == null) {
            throw new BusinessException("落地页不存在");
        }
        List<WecomRoute> list = routes.selectList(new LambdaQueryWrapper<WecomRoute>()
                .eq(WecomRoute::getStatus, 1)
                .and(w -> w.eq(WecomRoute::getScopeType, 1).or(pageId != null, o -> o.eq(WecomRoute::getLandingPageId, pageId))));
        WecomRoute r = WecomRouteSelector.select(list, pageId);
        if (r == null) {
            throw new BusinessException("没有当前承接配置");
        }
        RouteEligibility.require(r, corps.selectById(r.getCorpRowId()), teachers.selectById(r.getTeacherRowId()), r.getLandingPageId() == null ? null : pages.selectById(r.getLandingPageId()));
        return routeView(r);
    }

    private long routeCount(Long corpId, Long teacherId) {
        return routes.selectCount(new LambdaQueryWrapper<WecomRoute>()
                .eq(WecomRoute::getStatus, 1)
                .eq(corpId != null, WecomRoute::getCorpRowId, corpId)
                .eq(teacherId != null, WecomRoute::getTeacherRowId, teacherId));
    }

    private WecomCorp requireCorp(Long id) {
        WecomCorp c = id == null ? null : corps.lockById(id);
        if (c == null) {
            throw new BusinessException("企业不存在");
        }
        return c;
    }

    private WecomTeacher requireTeacher(Long id) {
        WecomTeacher t = id == null ? null : teachers.lockById(id);
        if (t == null) {
            throw new BusinessException("老师不存在");
        }
        return t;
    }

    private WecomTag requireTag(Long id) {
        WecomTag t = id == null ? null : tags.lockById(id);
        if (t == null) {
            throw new BusinessException("标签不存在");
        }
        return t;
    }

    private WecomRoute requireRoute(Long id) {
        WecomRoute r = id == null ? null : routes.selectById(id);
        if (r == null) {
            throw new BusinessException("路由不存在");
        }
        return r;
    }

    private String reference(String raw) {
        String ref = AcqTexts.optionalText(raw, "凭据引用", 128);
        SecretRefGuard.requireRef("凭据引用", ref);
        return ref;
    }

    private void duplicate(RuntimeException e, String message) {
        if (AcqDuplicate.isDuplicate(e)) {
            throw new BusinessException(message);
        }
        throw e;
    }

    private Map<String, Object> corpView(WecomCorp c) {
        return of("id", c.getId(), "corpName", c.getCorpName(), "corpId", c.getCorpId(), "sid", c.getSid(),
                "authMode", c.getAuthMode(), "remark", c.getRemark(), "status", c.getStatus());
    }

    private Map<String, Object> routeView(WecomRoute r) {
        return of("id", r.getId(), "scopeType", r.getScopeType(), "landingPageId", r.getLandingPageId(),
                "corpRowId", r.getCorpRowId(), "teacherRowId", r.getTeacherRowId(), "corpId", r.getCorpId(),
                "teacherUserid", r.getTeacherUserid(), "entryType", r.getEntryType(), "linkId", r.getLinkId(),
                "linkUrl", r.getLinkUrl(), "capabilityNote", r.getCapabilityNote(), "remark", r.getRemark(),
                "sortNo", r.getSortNo(), "status", r.getStatus());
    }
}
