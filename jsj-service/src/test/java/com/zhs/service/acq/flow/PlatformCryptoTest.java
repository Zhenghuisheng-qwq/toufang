package com.zhs.service.acq.flow;

import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RSAPublicKeyNotificationConfig;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import me.chanjar.weixin.cp.util.crypto.WxCpCryptUtil;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Generated local keys exercise the real SDK cryptography; no platform requests. */
class PlatformCryptoTest {

    @Test
    void officialPaymentParserRejectsModifiedRawBodyAndSignature() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        String apiKey = "01234567890123456789012345678901";
        String nonce = "012345678901";
        String aad = "transaction";
        String plain = "{\"out_trade_no\":\"test-order\",\"trade_state\":\"SUCCESS\",\"mchid\":\"test-mch\",\"appid\":\"test-app\",\"amount\":{\"total\":10,\"currency\":\"CNY\"}}";
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8), "AES"), new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
        cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
        String encrypted = Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        String body = "{\"id\":\"fixture-notify\",\"resource\":{\"algorithm\":\"AEAD_AES_256_GCM\",\"ciphertext\":\"" + encrypted + "\",\"nonce\":\"" + nonce + "\",\"associated_data\":\"" + aad + "\"}}";
        String time = String.valueOf(Instant.now().getEpochSecond());
        String requestNonce = "fixture-nonce";
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());
        signer.update((time + "\n" + requestNonce + "\n" + body + "\n").getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signer.sign());
        RSAPublicKeyNotificationConfig config = new RSAPublicKeyNotificationConfig.Builder()
                .publicKey(pair.getPublic())
                .publicKeyId("PUB_KEY_ID_FIXTURE")
                .apiV3Key(apiKey)
                .build();
        NotificationParser parser = new NotificationParser(config);
        RequestParam request = new RequestParam.Builder()
                .serialNumber("PUB_KEY_ID_FIXTURE")
                .timestamp(time)
                .nonce(requestNonce)
                .signature(signature)
                .body(body)
                .build();
        assertEquals("test-order", parser.parse(request, Transaction.class).getOutTradeNo());
        RequestParam modified = new RequestParam.Builder()
                .serialNumber("PUB_KEY_ID_FIXTURE")
                .timestamp(time)
                .nonce(requestNonce)
                .signature(signature)
                .body(body + " ")
                .build();
        assertThrows(RuntimeException.class, () -> parser.parse(modified, Transaction.class));
    }

    @Test
    void wecomCipherRejectsWrongCorpAndTampering() throws Exception {
        WxCpDefaultConfigImpl config = new WxCpDefaultConfigImpl();
        config.setCorpId("fixture-corp");
        config.setToken("fixture-token");
        config.setAesKey(Base64.getEncoder().withoutPadding().encodeToString(new byte[32]));
        CorpBoundWecomCrypt cipher = new CorpBoundWecomCrypt(config);
        String encrypted = new WxCpCryptUtil(config).encrypt("<xml><ToUserName>fixture-corp</ToUserName></xml>");
        assertNotNull(encrypted);
        assertFalse(encrypted.contains("<ToUserName>"));
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(encrypted)));
        String signature = doc.getElementsByTagName("MsgSignature").item(0).getTextContent();
        String time = doc.getElementsByTagName("TimeStamp").item(0).getTextContent();
        String nonce = doc.getElementsByTagName("Nonce").item(0).getTextContent();
        assertEquals("<xml><ToUserName>fixture-corp</ToUserName></xml>", cipher.decrypt(signature, time, nonce, encrypted));
        WxCpDefaultConfigImpl wrong = new WxCpDefaultConfigImpl();
        wrong.setCorpId("other-corp");
        wrong.setToken(config.getToken());
        wrong.setAesKey(config.getAesKey());
        assertThrows(RuntimeException.class, () -> new CorpBoundWecomCrypt(wrong).decrypt(signature, time, nonce, encrypted));
        assertThrows(RuntimeException.class, () -> cipher.decrypt("wrong", "1", "nonce", encrypted));
    }
}
