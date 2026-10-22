package com.zhs.service.acq;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.dto.ConfigCommands;
import com.zhs.service.acq.impl.MerchantAdminServiceImpl;
import com.zhs.service.mapper.acq.*;
import com.zhs.service.model.acq.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Offline rules and mapper fakes only: no Spring boot, JDBC, Redis or external clients. */
class ConfigurationSafetyTest {
    @Test void refsAreReferencesNotPathsOrSecrets() {
        assertDoesNotThrow(() -> SecretRefGuard.requireRef("test", "ref:test/pay-v1"));
        for (String value : new String[]{"ref:", "ref:../key", "file:/key", "https://example.invalid/key", "plain-secret", "ref:a\nb"}) {
            assertThrows(BusinessException.class, () -> SecretRefGuard.requireRef("test", value));
        }
    }
    @Test void historyLocksIdentityButAllowsDisplayEdits() {
        assertDoesNotThrow(() -> ConfigRules.unchanged("a", "a", true, "身份"));
        assertDoesNotThrow(() -> ConfigRules.unchanged("a", "b", false, "身份"));
        assertThrows(BusinessException.class, () -> ConfigRules.unchanged("a", "b", true, "身份"));
    }
    @Test void enterpriseOwnershipIsMandatory() {
        assertThrows(BusinessException.class, () -> ConfigRules.ownership(1L, 2L));
        assertThrows(BusinessException.class, () -> ConfigRules.ownership(null, null));
        assertDoesNotThrow(() -> ConfigRules.ownership(1L, 1L));
    }
    @Test void configCompletenessRequiresDistinctSigningAndVerificationMaterials() {
        PayMerchantVersion v = new PayMerchantVersion();
        v.setApiV3KeyRef("ref:test/api"); v.setPrivateKeyRef("ref:test/private"); v.setVerifyMode("CERTIFICATE");
        v.setPlatformCertRef("ref:test/cert");
        assertFalse(ConfigRules.complete(v));
        v.setMerchantSerialNo("TEST_SERIAL");
        assertTrue(ConfigRules.complete(v));
        v.setVerifyMode("PUBLIC_KEY");
        assertFalse(ConfigRules.complete(v));
        v.setPlatformKeyId("TEST_PUBLIC_KEY_ID"); v.setPlatformPublicKeyRef("ref:test/public");
        assertTrue(ConfigRules.complete(v));
        v.setVerifyMode("UNCONFIRMED"); assertFalse(ConfigRules.complete(v));
    }
    @Test void linkValidationNeverAcceptsExecutableOrCredentialBearingUrls() {
        assertEquals("https://example.invalid/path", ConfigRules.link("https://example.invalid/path"));
        for (String url : new String[]{"javascript:alert(1)", "http://example.invalid", "https://u:p@example.invalid", "https://example.invalid:8443", "https://example.invalid/#x", "https://example.invalid/" + "x".repeat(512)}) {
            assertThrows(BusinessException.class, () -> ConfigRules.link(url));
        }
    }
    @Test void publishedMerchantVersionsAndCrossMerchantEditsAreRejectedBeforeWrite() throws Exception {
        var service = new MerchantAdminServiceImpl();
        var merchant = new PayMerchant(); merchant.setId(1L);
        var version = new PayMerchantVersion(); version.setId(2L); version.setPayMerchantId(1L); version.setStatus(1);
        AtomicInteger writes = new AtomicInteger();
        inject(service, "merchants", fake(PayMerchantMapper.class, (method, args) -> {
            if (method.equals("lockById")) return merchant;
            writes.incrementAndGet(); throw new AssertionError(method);
        }));
        inject(service, "versions", fake(PayMerchantVersionMapper.class, (method, args) -> {
            if (method.equals("selectById")) return version;
            writes.incrementAndGet(); throw new AssertionError(method);
        }));
        var command = new ConfigCommands.MerchantVersion(null,null,null,"UNCONFIRMED",null,null,null,null);
        assertThrows(BusinessException.class, () -> service.saveVersion(1L,2L,command));
        version.setStatus(0); version.setPayMerchantId(9L);
        assertThrows(BusinessException.class, () -> service.saveVersion(1L,2L,command));
        assertEquals(0, writes.get());
    }
    @Test void usedDraftIsAlsoImmutable() throws Exception {
        var service = new MerchantAdminServiceImpl();
        var merchant = new PayMerchant(); merchant.setId(1L);
        var version = new PayMerchantVersion(); version.setId(2L); version.setPayMerchantId(1L); version.setStatus(0);
        inject(service,"merchants",fake(PayMerchantMapper.class,(m,a)->merchant));
        inject(service,"versions",fake(PayMerchantVersionMapper.class,(m,a)->version));
        inject(service,"refs",fake(ConfigReferenceMapper.class,(m,a)->1L));
        assertThrows(BusinessException.class, () -> service.saveVersion(1L,2L,
                new ConfigCommands.MerchantVersion(null,null,null,"UNCONFIRMED",null,null,null,null)));
    }
    @Test void invalidSelectedRouteCannotSilentlyBecomeAnotherEnterprise() {
        var c = new WecomCorp(); c.setId(1L); c.setCorpId("corp-a"); c.setStatus(1);
        var t = new WecomTeacher(); t.setId(2L); t.setCorpRowId(1L); t.setTeacherUserid("teacher-a"); t.setStatus(1);
        var r = new WecomRoute(); r.setCorpRowId(1L); r.setTeacherRowId(2L); r.setCorpId("corp-a"); r.setTeacherUserid("teacher-a"); r.setScopeType(1); r.setEntryType(1);
        assertDoesNotThrow(() -> RouteEligibility.require(r,c,t,null));
        c.setStatus(0);
        assertThrows(BusinessException.class, () -> RouteEligibility.require(r,c,t,null));
        c.setStatus(1); t.setCorpRowId(3L);
        assertThrows(BusinessException.class, () -> RouteEligibility.require(r,c,t,null));
    }
    @Test void idsStayStringsInConfigurationResponses() {
        var view = ConfigViews.of("id", 9007199254740993L, "status", 1);
        assertEquals("9007199254740993", view.get("id"));
        assertEquals(1, view.get("status"));
    }
    @Test void unconfirmedPlatformModeIsExplicit() {
        assertEquals("UNCONFIRMED", ConfigRules.choice("UNCONFIRMED","接口", Set.of("UNCONFIRMED","BAIDU_CONVERSION")));
        assertThrows(BusinessException.class, () -> ConfigRules.choice("TENCENT_MARKETING","接口",Set.of("UNCONFIRMED","BAIDU_CONVERSION")));
    }
    interface Handler { Object invoke(String method, Object[] args) throws Throwable; }
    @SuppressWarnings("unchecked")
    static <T> T fake(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
                (proxy,method,args)->handler.invoke(method.getName(),args));
    }
    static void inject(Object target,String name,Object value) throws Exception {
        var field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
}
