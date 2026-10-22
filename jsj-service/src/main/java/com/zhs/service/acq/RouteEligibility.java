package com.zhs.service.acq;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.model.acq.*;
import java.util.Objects;
public final class RouteEligibility {
    private RouteEligibility() { }
    public static void require(WecomRoute route,WecomCorp corp,WecomTeacher teacher,LandingPage page){
        if(route==null||corp==null||teacher==null||!Integer.valueOf(1).equals(corp.getStatus())||!Integer.valueOf(1).equals(teacher.getStatus()))throw new BusinessException("指定承接企业或老师不可用，不会自动更换企业");
        ConfigRules.ownership(corp.getId(),teacher.getCorpRowId());
        if (!Objects.equals(route.getCorpRowId(), corp.getId()) || !Objects.equals(route.getTeacherRowId(), teacher.getId()))
            throw new BusinessException("承接关联ID不一致");
        if(!Objects.equals(route.getCorpId(),corp.getCorpId())||!Objects.equals(route.getTeacherUserid(),teacher.getTeacherUserid()))throw new BusinessException("承接身份与登记配置不一致");
        WecomRouteSelector.activeKey(route.getScopeType(),route.getLandingPageId());
        if(Integer.valueOf(2).equals(route.getScopeType())&&page==null)throw new BusinessException("指定落地页不存在");
        if (page != null && !Objects.equals(page.getId(), route.getLandingPageId())) throw new BusinessException("落地页关联不一致");
        if(!Integer.valueOf(1).equals(route.getEntryType())&&!Integer.valueOf(2).equals(route.getEntryType()))throw new BusinessException("请明确获客入口类型");
        if(Integer.valueOf(2).equals(route.getEntryType())&&(route.getLinkId()==null||route.getLinkId().isBlank())&&(route.getLinkUrl()==null||route.getLinkUrl().isBlank()))throw new BusinessException("获客助手需要登记链接或链接ID");
    }
}
