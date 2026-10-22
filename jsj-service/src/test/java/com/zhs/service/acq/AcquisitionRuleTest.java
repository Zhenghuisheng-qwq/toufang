package com.zhs.service.acq;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.model.acq.WecomRoute;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcquisitionRuleTest {

    @Test
    void landingRouteOverridesGlobalRoute() {
        WecomRoute global = route(AcquisitionCodes.ROUTE_GLOBAL, null, 11L);
        WecomRoute landing = route(AcquisitionCodes.ROUTE_LANDING, 7L, 22L);
        WecomRoute selected = WecomRouteSelector.select(List.of(global, landing), 7L);
        assertEquals(22L, selected.getTeacherRowId());
        assertEquals(global, WecomRouteSelector.select(List.of(global, landing), 8L));
        assertNull(WecomRouteSelector.select(List.of(), 7L));
    }

    @Test
    void activeKeySeparatesGlobalAndLanding() {
        assertEquals("G", WecomRouteSelector.activeKey(AcquisitionCodes.ROUTE_GLOBAL, null));
        assertEquals("L:9", WecomRouteSelector.activeKey(AcquisitionCodes.ROUTE_LANDING, 9L));
        assertThrows(BusinessException.class, () -> WecomRouteSelector.activeKey(AcquisitionCodes.ROUTE_LANDING, null));
    }

    @Test
    void missingClickIdDoesNotInventDedupKey() {
        assertNull(AdClickKeys.dedupKey(AcquisitionCodes.PLATFORM_TENCENT, " "));
        assertEquals("1:gdt-1", AdClickKeys.dedupKey(AcquisitionCodes.PLATFORM_TENCENT, " gdt-1 "));
        assertEquals("2:bd-1", AdClickKeys.dedupKey(AcquisitionCodes.PLATFORM_BAIDU, "bd-1"));
        assertEquals("3:ks-1", AdClickKeys.dedupKey(AcquisitionCodes.PLATFORM_KUAISHOU, "ks-1"));
    }

    @Test
    void secretFieldsAcceptReferenceOnly() {
        SecretRefGuard.requireRef("privateKeyRef", null);
        SecretRefGuard.requireRef("privateKeyRef", "ref:pay/mch-a/v1");
        assertThrows(BusinessException.class,
                () -> SecretRefGuard.requireRef("privateKeyRef", "-----BEGIN PRIVATE KEY-----\nabc"));
        assertThrows(BusinessException.class,
                () -> SecretRefGuard.requireRef("apiV3KeyRef", "plain-api-v3-key"));
    }

    private static WecomRoute route(int scope, Long landingPageId, Long teacherRowId) {
        WecomRoute route = new WecomRoute();
        route.setStatus(AcquisitionCodes.ROUTE_ENABLED);
        route.setScopeType(scope);
        route.setLandingPageId(landingPageId);
        route.setTeacherRowId(teacherRowId);
        return route;
    }
}
