package com.zhs.service.acq.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhs.service.model.acq.AdAccount;
import com.zhs.service.model.acq.AdConversion;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Endpoint and response schema follow Baidu's official oCPC uploadConvertData specification. */
@Component
public class AdDeliveryAdapter {

    public record Outcome(int status, String code) {
    }

    @Resource
    private ObjectMapper json;

    @Resource
    private PlatformSecrets secrets;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    protected HttpResponse<String> send(HttpRequest request) throws java.io.IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public Outcome deliver(AdConversion event, AdAccount account) {
        if (account == null || !Objects.equals(account.getStatus(), 1)) {
            return new Outcome(5, "ACCOUNT_DISABLED");
        }
        try {
            JsonNode snapshot = json.readTree(event.getDeliverySnapshot());
            String mode = snapshot.path("interfaceMode").asText();
            if (!Objects.equals(account.getExternalAccountId(), snapshot.path("externalAccountId").asText())) {
                return new Outcome(5, "ACCOUNT_IDENTITY_CHANGED");
            }
            if ("TENCENT_MARKETING".equals(mode) && Objects.equals(event.getPlatform(), 1)) {
                return deliverTencent(event, account, snapshot);
            }
            if ("KUAISHOU_CONVERSION".equals(mode) && Objects.equals(event.getPlatform(), 3)) {
                return deliverKuaishou(event, snapshot);
            }
            if (!"BAIDU_CONVERSION".equals(mode) || !Objects.equals(event.getPlatform(), 2)) {
                return new Outcome(5, "PLATFORM_PROTOCOL_NOT_VERIFIED");
            }
            return deliverBaidu(event, account, snapshot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Outcome(4, "RESULT_UNKNOWN");
        } catch (Exception e) {
            return new Outcome(4, "RESULT_UNKNOWN");
        }
    }

    private Outcome deliverTencent(AdConversion event, AdAccount account, JsonNode snapshot) throws Exception {
        String mapping = snapshot.path("event").asText("");
        String click = snapshot.path("tencentClickId").asText("");
        String accountId = snapshot.path("externalAccountId").asText("");
        String source = snapshot.path("dataSourceId").asText("");
        if (!mapping.matches("[A-Z_]{1,64}") || "CUSTOM".equals(mapping)) {
            return new Outcome(5, "EVENT_MAPPING_REQUIRED");
        }
        if (!accountId.matches("[1-9][0-9]{0,17}") || !source.matches("[1-9][0-9]{0,17}")) {
            return new Outcome(5, "ACCOUNT_AND_DATA_SOURCE_REQUIRED");
        }
        if (click.isBlank() || click.length() > 64) {
            return new Outcome(2, "NO_CLICK");
        }
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("action_type", mapping);
        action.put("action_time", event.getEventTime().atZone(ZoneId.of("Asia/Shanghai")).toEpochSecond());
        action.put("external_action_id", FlowSecurity.hash(event.getEventKey()));
        action.put("trace", Map.of("click_id", click));
        action.put("url", snapshot.path("url").asText());
        if (event.getAmountFen() != null) {
            if (!"FEN".equals(snapshot.path("amountUnit").asText())) {
                return new Outcome(5, "AMOUNT_UNIT_REQUIRED");
            }
            action.put("action_param", Map.of("value", event.getAmountFen()));
        }
        String token;
        try {
            token = secrets.resolve(account.getCredentialRef());
        } catch (RuntimeException e) {
            return new Outcome(5, "CREDENTIAL_REQUIRED");
        }
        String query = "access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&timestamp=" + Instant.now().getEpochSecond()
                + "&nonce=" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String body = json.writeValueAsString(Map.of(
                "account_id", Long.parseLong(accountId),
                "user_action_set_id", Long.parseLong(source),
                "actions", List.of(action)));
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.e.qq.com/v1.3/user_actions/add?" + query))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            return new Outcome(4, "HTTP_RESULT_UNCONFIRMED");
        }
        long code = json.readTree(response.body()).path("code").asLong(-1);
        if (code == 0) {
            return new Outcome(1, "ACCEPTED");
        }
        if (code < 0) {
            return new Outcome(4, "INVALID_RESPONSE");
        }
        return new Outcome(3, "TENCENT_CODE_" + code);
    }

    private Outcome deliverKuaishou(AdConversion event, JsonNode snapshot) throws Exception {
        String mapping = snapshot.path("event").asText("");
        String callback = snapshot.path("kuaishouCallback").asText("");
        if (!mapping.matches("[1-9][0-9]{0,5}")) {
            return new Outcome(5, "EVENT_MAPPING_REQUIRED");
        }
        if (callback.isBlank() || callback.length() > 1024) {
            return new Outcome(2, "NO_CLICK");
        }
        String query = "callback=" + URLEncoder.encode(callback, StandardCharsets.UTF_8)
                + "&event_type=" + mapping
                + "&event_time=" + event.getEventTime().atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
        if (event.getAmountFen() != null) {
            if (!"YUAN".equals(snapshot.path("amountUnit").asText())) {
                return new Outcome(5, "AMOUNT_UNIT_REQUIRED");
            }
            query += "&purchase_amount=" + BigDecimal.valueOf(event.getAmountFen(), 2).toPlainString();
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://ad.partner.gifshow.com/track/activate?" + query))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            return new Outcome(4, "HTTP_RESULT_UNCONFIRMED");
        }
        int result = json.readTree(response.body()).path("result").asInt(-1);
        if (result == 1) {
            return new Outcome(1, "ACCEPTED");
        }
        if (result < 0) {
            return new Outcome(4, "INVALID_RESPONSE");
        }
        return new Outcome(3, "KUAISHOU_REJECTED");
    }

    private Outcome deliverBaidu(AdConversion event, AdAccount account, JsonNode snapshot) throws Exception {
        String mapping = snapshot.path("event").asText("");
        String click = snapshot.path("baiduLogidUrl").asText("");
        if (!mapping.matches("[1-9][0-9]{0,5}")) {
            return new Outcome(5, "EVENT_MAPPING_REQUIRED");
        }
        if (click.isBlank() || click.length() > 1024 || !click.contains("bd_vid=")) {
            return new Outcome(2, "NO_CLICK");
        }
        Map<String, Object> conversion = new LinkedHashMap<>();
        conversion.put("logidUrl", click);
        conversion.put("newType", Integer.parseInt(mapping));
        conversion.put("convertTime", event.getEventTime().atZone(ZoneId.of("Asia/Shanghai")).toEpochSecond());
        if (event.getAmountFen() != null) {
            if (!"FEN".equals(snapshot.path("amountUnit").asText())) {
                return new Outcome(5, "AMOUNT_UNIT_REQUIRED");
            }
            conversion.put("convertValue", event.getAmountFen());
        }
        String token;
        try {
            token = secrets.resolve(account.getCredentialRef());
        } catch (RuntimeException e) {
            return new Outcome(5, "CREDENTIAL_REQUIRED");
        }
        String body = json.writeValueAsString(Map.of("token", token, "conversionTypes", List.of(conversion)));
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://ocpc.baidu.com/ocpcapi/api/uploadConvertData"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            return new Outcome(4, "HTTP_RESULT_UNCONFIRMED");
        }
        int status = json.readTree(response.body()).path("header").path("status").asInt(-1);
        if (status == 0) {
            return new Outcome(1, "ACCEPTED");
        }
        if (status < 0) {
            return new Outcome(4, "INVALID_RESPONSE");
        }
        return new Outcome(status == 3 ? 5 : 3, "BAIDU_STATUS_" + status);
    }
}
