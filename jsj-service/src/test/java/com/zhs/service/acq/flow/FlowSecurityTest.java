package com.zhs.service.acq.flow;
import com.zhs.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;
class FlowSecurityTest {
    @Test void credentialsHaveIndependentEntropyAndOnlyHashesPersist(){
        var values=new HashSet<String>();for(int i=0;i<1000;i++){String token=FlowSecurity.token();assertEquals(43,token.length());assertTrue(values.add(token));String hash=FlowSecurity.credentialHash(token);assertEquals(64,hash.length());assertNotEquals(token,hash);}
    }
    @Test void malformedCredentialsAndReplayKeysAreRejected(){
        for(String token:new String[]{"","123","order123","<script>","a".repeat(42)})assertThrows(BusinessException.class,()->FlowSecurity.credentialHash(token));
        assertThrows(BusinessException.class,()->FlowSecurity.credentialHash(null));
        assertThrows(BusinessException.class,()->FlowSecurity.key(""));assertThrows(BusinessException.class,()->FlowSecurity.key("a".repeat(65)));
        assertEquals("a".repeat(16),FlowSecurity.key("a".repeat(16)));
    }
}
