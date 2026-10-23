package com.zhs.web.controller.acq;

import com.zhs.framework.web.service.PermissionService;
import com.zhs.service.acq.AdAccountAdminService;
import com.zhs.service.acq.MerchantAdminService;
import com.zhs.service.acq.WecomAdminService;
import com.zhs.service.acq.dto.ConfigCommands;
import com.zhs.service.acq.report.ReportAdminService;
import com.zhs.system.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Actual Spring method-security proxies, with in-memory service fakes; no application startup. */
class ConfigurationPermissionTest {

    AnnotationConfigApplicationContext context;

    static final AtomicInteger calls = new AtomicInteger();

    @BeforeEach
    void setup() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        calls.set(0);
        context = new AnnotationConfigApplicationContext(Config.class);
        login();
    }

    @AfterEach
    void close() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        context.close();
    }

    void login(String... permissions) {
        LoginUser user = new LoginUser();
        user.setPermissions(new HashSet<>(List.of(permissions)));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @Test
    void unauthorizedCallsNeverReachBusinessServices() {
        assertThrows(AccessDeniedException.class, () -> context.getBean(PayMerchantController.class).remove(1L));
        assertThrows(AccessDeniedException.class, () -> context.getBean(AdAccountController.class).remove(1L));
        assertThrows(AccessDeniedException.class, () -> context.getBean(WecomConfigController.class).remove(1L));
        assertThrows(AccessDeniedException.class, () -> context.getBean(WecomRouteController.class).remove(1L));
        assertEquals(0, calls.get());
    }

    @Test
    void editingDoesNotGrantSensitiveVersionWrites() {
        login("acq:merchant:edit", "acq:merchant:version");
        PayMerchantController controller = context.getBean(PayMerchantController.class);
        assertThrows(AccessDeniedException.class, () -> controller.version(1L, new ConfigCommands.MerchantVersion(null, null, null, "UNCONFIRMED", null, null, null, null)));
        assertEquals(0, calls.get());
    }

    @Test
    void permittedCallReachesService() {
        login("acq:merchant:remove");
        assertDoesNotThrow(() -> context.getBean(PayMerchantController.class).remove(1L));
        assertEquals(1, calls.get());
    }

    @Test
    void reportPermissionsAreIndependent() {
        ReportAdminController controller = context.getBean(ReportAdminController.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        login("acq:report:query");
        assertDoesNotThrow(() -> controller.status("TEST", response));
        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertThrows(AccessDeniedException.class, () -> controller.content("TEST", response));
        assertThrows(AccessDeniedException.class, () -> controller.retry("TEST", response));
        assertEquals(1, calls.get());
        login("acq:report:retry");
        assertDoesNotThrow(() -> controller.retry("TEST", response));
        assertThrows(AccessDeniedException.class, () -> controller.content("TEST", response));
        assertEquals(2, calls.get());
    }

    @SuppressWarnings("unchecked")
    static <T> T service(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return method.getName().equals("toString") ? "test fake" : System.identityHashCode(proxy);
            }
            calls.incrementAndGet();
            return null;
        });
    }

    @Configuration
    @EnableMethodSecurity
    static class Config {

        @Bean
        ReportAdminService reports() {
            return service(ReportAdminService.class);
        }

        @Bean
        ReportAdminController reportController() {
            return new ReportAdminController();
        }

        @Bean(name = "ss")
        PermissionService permissions() {
            return new PermissionService();
        }

        @Bean
        MerchantAdminService merchants() {
            return service(MerchantAdminService.class);
        }

        @Bean
        AdAccountAdminService ads() {
            return service(AdAccountAdminService.class);
        }

        @Bean
        WecomAdminService wecom() {
            return service(WecomAdminService.class);
        }

        @Bean
        PayMerchantController merchantController() {
            return new PayMerchantController();
        }

        @Bean
        AdAccountController adController() {
            return new AdAccountController();
        }

        @Bean
        WecomConfigController wecomController() {
            return new WecomConfigController();
        }

        @Bean
        WecomRouteController routeController() {
            return new WecomRouteController();
        }
    }
}
