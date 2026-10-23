package com.zhs.service.acq.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.AcquisitionCodes;
import com.zhs.service.acq.WecomRouteSelector;
import com.zhs.service.acq.WecomRouteService;
import com.zhs.service.acq.RouteEligibility;
import com.zhs.service.mapper.acq.ConfigReferenceMapper;
import com.zhs.service.mapper.acq.WecomCorpMapper;
import com.zhs.service.mapper.acq.WecomTeacherMapper;
import com.zhs.service.mapper.acq.LandingPageMapper;
import com.zhs.service.mapper.acq.WecomRouteMapper;
import com.zhs.service.model.acq.WecomRoute;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WecomRouteServiceImpl implements WecomRouteService {

    @Resource
    private WecomRouteMapper wecomRouteMapper;

    @Resource
    private ConfigReferenceMapper references;

    @Resource
    private WecomCorpMapper corps;

    @Resource
    private WecomTeacherMapper teachers;

    @Resource
    private LandingPageMapper pages;

    @Override
    @Transactional(transactionManager = "acqTransactionManager", rollbackFor = Exception.class)
    public void activate(Long routeId) {
        references.lockRoutes();
        WecomRoute route = wecomRouteMapper.selectById(routeId);
        if (route == null) {
            throw new BusinessException("企微路由不存在");
        }
        RouteEligibility.require(route, corps.lockById(route.getCorpRowId()), teachers.lockById(route.getTeacherRowId()),
                route.getLandingPageId() == null ? null : pages.selectById(route.getLandingPageId()));
        String activeKey = WecomRouteSelector.activeKey(route.getScopeType(), route.getLandingPageId());
        wecomRouteMapper.update(null, new LambdaUpdateWrapper<WecomRoute>()
                .set(WecomRoute::getActiveKey, null)
                .set(WecomRoute::getStatus, 0)
                .eq(WecomRoute::getActiveKey, activeKey)
                .ne(WecomRoute::getId, routeId));
        route.setActiveKey(activeKey);
        route.setStatus(AcquisitionCodes.ROUTE_ENABLED);
        if (wecomRouteMapper.updateById(route) != 1) {
            throw new BusinessException("承接切换失败，原配置保持不变");
        }
    }
}
