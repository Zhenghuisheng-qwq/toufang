package com.zhs.service.acq;

import com.zhs.common.exception.BusinessException;
import com.zhs.service.auth.impl.AuthServiceImpl;
import com.zhs.service.support.AuthTokenSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthDeliverySafetyTest {

    @Test
    void noSmsProviderCannotAcknowledgeProductionDelivery() throws Exception {
        AuthServiceImpl service = new AuthServiceImpl();
        ConfigurationSafetyTest.inject(service, "authTokenSupport",
                ConfigurationSafetyTest.fake(AuthTokenSupport.class, (method, args) -> {
                    assertEquals("isDevProfile", method, "Must not write limits or verification codes when delivery is unavailable");
                    return false;
                }));
        assertThrows(BusinessException.class, () -> service.sendPhoneCode("13800000000"));
    }
}
