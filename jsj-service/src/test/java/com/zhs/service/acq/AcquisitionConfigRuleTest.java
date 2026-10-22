package com.zhs.service.acq;

import com.zhs.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcquisitionConfigRuleTest {

    @Test
    void priceUsesFenWithoutFloatRounding() {
        assertEquals(990, MoneyAmounts.yuanToFen("9.90"));
        assertEquals(1, MoneyAmounts.yuanToFen("0.01"));
        assertEquals("9.90", MoneyAmounts.fenToYuan(990));
        assertThrows(BusinessException.class, () -> MoneyAmounts.yuanToFen("0"));
        assertThrows(BusinessException.class, () -> MoneyAmounts.yuanToFen("1.001"));
        assertThrows(BusinessException.class, () -> MoneyAmounts.yuanToFen("9.9元"));
    }

    @Test
    void domainRejectsUrlAndNormalizesCase() {
        assertEquals("a.example.com", DomainNames.canonicalize(" A.Example.COM "));
        assertThrows(BusinessException.class, () -> DomainNames.canonicalize("https://a.example.com/"));
        assertThrows(BusinessException.class, () -> DomainNames.canonicalize("https://a.example.com/path"));
        assertThrows(BusinessException.class, () -> DomainNames.canonicalize("a.example.com:443"));
        assertThrows(BusinessException.class, () -> AssetUrls.normalize("https://cdn.example.com/a.png"));
        assertEquals("/profile/upload/2026/10/22/a.png", AssetUrls.normalize("/profile/upload/2026/10/22/a.png"));
    }
}
