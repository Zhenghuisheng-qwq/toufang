package com.zhs.service.acq.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhs.service.model.acq.AdAccount;
import com.zhs.service.model.acq.AdConversion;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdDeliveryAdapterTest {

    static class Adapter extends AdDeliveryAdapter {

        HttpRequest sent;

        String body = "{}";

        int http = 200;

        @Override
        protected HttpResponse<String> send(HttpRequest request) {
            sent = request;
            return new HttpResponse<>() {
                public int statusCode() {
                    return http;
                }

                public HttpRequest request() {
                    return request;
                }

                public Optional<HttpResponse<String>> previousResponse() {
                    return Optional.empty();
                }

                public HttpHeaders headers() {
                    return HttpHeaders.of(Map.of(), (a, b) -> true);
                }

                public String body() {
                    return body;
                }

                public Optional<SSLSession> sslSession() {
                    return Optional.empty();
                }

                public URI uri() {
                    return request.uri();
                }

                public HttpClient.Version version() {
                    return HttpClient.Version.HTTP_1_1;
                }
            };
        }
    }

    @Test
    void threeAdaptersRequireBusinessAcknowledgementNotHttp200() throws Exception {
        String[] modes = {"TENCENT_MARKETING", "BAIDU_CONVERSION", "KUAISHOU_CONVERSION"};
        String[] responses = {"{\"code\":0}", "{\"header\":{\"status\":0}}", "{\"result\":1}"};
        String[] hosts = {"api.e.qq.com", "ocpc.baidu.com", "ad.partner.gifshow.com"};
        for (int i = 0; i < 3; i++) {
            Adapter adapter = new Adapter();
            inject(adapter, "json", new ObjectMapper());
            inject(adapter, "secrets", new PlatformSecrets() {
                @Override
                public String resolve(String ref) {
                    return "synthetic-token";
                }
            });
            AdConversion event = new AdConversion();
            event.setPlatform(i + 1);
            event.setEventTime(LocalDateTime.of(2026, 10, 23, 0, 0));
            event.setAmountFen(123);
            event.setEventKey("ORDER:1:1");
            HashMap<String, Object> map = new HashMap<>();
            map.put("interfaceMode", modes[i]);
            map.put("externalAccountId", "123");
            map.put("dataSourceId", "456");
            map.put("event", i == 0 ? "PURCHASE" : "3");
            map.put("amountUnit", i == 2 ? "YUAN" : "FEN");
            map.put("tencentClickId", "valid-click");
            map.put("baiduLogidUrl", "https://fixture.test/?bd_vid=valid");
            map.put("kuaishouCallback", "callback+/&=?");
            map.put("url", "https://fixture.test/pages/1");
            event.setDeliverySnapshot(new ObjectMapper().writeValueAsString(map));
            AdAccount account = new AdAccount();
            account.setStatus(1);
            account.setExternalAccountId("123");
            account.setCredentialRef("ref:test");
            assertEquals(4, adapter.deliver(event, account).status(), "empty response must stay unknown");
            adapter.body = responses[i];
            assertEquals(1, adapter.deliver(event, account).status());
            assertEquals(hosts[i], adapter.sent.uri().getHost());
            assertEquals("https", adapter.sent.uri().getScheme());
            if (i == 2) {
                assertTrue(adapter.sent.uri().getRawQuery().contains("purchase_amount=1.23"));
                assertTrue(adapter.sent.uri().getRawQuery().contains("callback%2B%2F%26%3D%3F"));
            }
            adapter.http = 500;
            assertEquals(4, adapter.deliver(event, account).status());
            adapter.sent = null;
            account.setStatus(0);
            assertEquals(5, adapter.deliver(event, account).status());
            assertNull(adapter.sent);
        }
    }

    private void inject(Object target, String name, Object value) throws Exception {
        Field field = AdDeliveryAdapter.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
