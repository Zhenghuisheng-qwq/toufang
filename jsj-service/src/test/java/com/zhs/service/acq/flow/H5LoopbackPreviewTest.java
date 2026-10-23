package com.zhs.service.acq.flow;

import com.zhs.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class H5LoopbackPreviewTest {

    @AfterEach
    void close() {
        H5LoopbackPreview.close();
    }

    @Test
    void onlyLoopbackRemoteAndHostCanOpen() {
        assertTrue(H5LoopbackPreview.eligible("127.0.0.1", "127.0.0.1"));
        assertTrue(H5LoopbackPreview.eligible("127.0.0.1", "localhost"));
        assertTrue(H5LoopbackPreview.eligible("0:0:0:0:0:0:0:1", "::1"));
        assertFalse(H5LoopbackPreview.eligible("192.168.1.8", "127.0.0.1"));
        assertFalse(H5LoopbackPreview.eligible("127.0.0.1", "page.example.com"));
        assertFalse(H5LoopbackPreview.active());
    }

    @Test
    void staysClosedUnlessExplicitlyEnabled() {
        H5LoopbackPreview.openIf(false, "127.0.0.1", "127.0.0.1");
        assertFalse(H5LoopbackPreview.active());
        assertThrows(IllegalStateException.class, () -> H5LoopbackPreview.hostKey("127.0.0.1"));

        H5LoopbackPreview.openIf(true, "10.0.0.8", "127.0.0.1");
        assertFalse(H5LoopbackPreview.active());

        H5LoopbackPreview.openIf(true, "127.0.0.1", "127.0.0.1");
        assertTrue(H5LoopbackPreview.active());
        assertEquals("127.0.0.1", H5LoopbackPreview.hostKey(" 127.0.0.1 "));
        assertEquals("localhost", H5LoopbackPreview.hostKey("LocalHost"));
        assertThrows(BusinessException.class, () -> H5LoopbackPreview.hostKey("page.example.com"));
    }
}
