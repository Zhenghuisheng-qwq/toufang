package com.zhs.service.acq;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.model.acq.WecomRoute;

import java.util.List;

/**
 * 落地页指定路由优先，否则使用全局当前路由。
 */
public final class WecomRouteSelector {

    private WecomRouteSelector() {
    }

    public static WecomRoute select(List<WecomRoute> routes, Long landingPageId) {
        WecomRoute landing = null;
        WecomRoute global = null;
        if (routes == null) {
            return null;
        }
        for (WecomRoute route : routes) {
            if (route == null || route.getStatus() == null || route.getStatus() != AcquisitionCodes.ROUTE_ENABLED) {
                continue;
            }
            if (route.getScopeType() != null
                    && route.getScopeType() == AcquisitionCodes.ROUTE_LANDING
                    && landingPageId != null
                    && landingPageId.equals(route.getLandingPageId())) {
                landing = route;
            } else if (route.getScopeType() != null && route.getScopeType() == AcquisitionCodes.ROUTE_GLOBAL) {
                global = route;
            }
        }
        return landing != null ? landing : global;
    }

    public static String activeKey(Integer scopeType, Long landingPageId) {
        if (scopeType == null) {
            throw new BusinessException("路由范围不能为空");
        }
        if (scopeType == AcquisitionCodes.ROUTE_GLOBAL) {
            return "G";
        }
        if (scopeType == AcquisitionCodes.ROUTE_LANDING) {
            if (landingPageId == null) {
                throw new BusinessException("落地页路由必须指定落地页");
            }
            return "L:" + landingPageId;
        }
        throw new BusinessException("未知路由范围");
    }
}
