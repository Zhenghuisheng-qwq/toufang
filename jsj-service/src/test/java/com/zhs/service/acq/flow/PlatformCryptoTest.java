package com.zhs.service.acq.flow;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import com.wechat.pay.java.core.notification.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import me.chanjar.weixin.cp.util.crypto.WxCpCryptUtil;

/** Generated local keys exercise the real SDK cryptography; no platform requests. */
class PlatformCryptoTest {
    @Test void officialPaymentParserRejectsModifiedRawBodyAndSignature()throws Exception{
        var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);var pair=generator.generateKeyPair();
        String apiKey="01234567890123456789012345678901",nonce="012345678901",aad="transaction";
        String plain="{\"out_trade_no\":\"test-order\",\"trade_state\":\"SUCCESS\",\"mchid\":\"test-mch\",\"appid\":\"test-app\",\"amount\":{\"total\":10,\"currency\":\"CNY\"}}";
        var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8),"AES"),new GCMParameterSpec(128,nonce.getBytes(StandardCharsets.UTF_8)));cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));String encrypted=Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        String body="{\"id\":\"fixture-notify\",\"resource\":{\"algorithm\":\"AEAD_AES_256_GCM\",\"ciphertext\":\""+encrypted+"\",\"nonce\":\""+nonce+"\",\"associated_data\":\""+aad+"\"}}";
        String time=String.valueOf(java.time.Instant.now().getEpochSecond()),requestNonce="fixture-nonce";var signer=Signature.getInstance("SHA256withRSA");signer.initSign(pair.getPrivate());signer.update((time+"\n"+requestNonce+"\n"+body+"\n").getBytes(StandardCharsets.UTF_8));String signature=Base64.getEncoder().encodeToString(signer.sign());
        var config=new RSAPublicKeyNotificationConfig.Builder().publicKey(pair.getPublic()).publicKeyId("PUB_KEY_ID_FIXTURE").apiV3Key(apiKey).build();var parser=new NotificationParser(config);
        var request=new RequestParam.Builder().serialNumber("PUB_KEY_ID_FIXTURE").timestamp(time).nonce(requestNonce).signature(signature).body(body).build();assertEquals("test-order",parser.parse(request,Transaction.class).getOutTradeNo());
        var modified=new RequestParam.Builder().serialNumber("PUB_KEY_ID_FIXTURE").timestamp(time).nonce(requestNonce).signature(signature).body(body+" ").build();assertThrows(RuntimeException.class,()->parser.parse(modified,Transaction.class));
    }
    @Test void wecomCipherRejectsWrongCorpAndTampering() throws Exception {
        var config=new WxCpDefaultConfigImpl();config.setCorpId("fixture-corp");config.setToken("fixture-token");config.setAesKey(Base64.getEncoder().withoutPadding().encodeToString(new byte[32]));
        var cipher=new CorpBoundWecomCrypt(config);String encrypted=new WxCpCryptUtil(config).encrypt("<xml><ToUserName>fixture-corp</ToUserName></xml>");
        assertNotNull(encrypted);assertFalse(encrypted.contains("<ToUserName>"));
        var doc=javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(encrypted)));
        String signature=doc.getElementsByTagName("MsgSignature").item(0).getTextContent(),time=doc.getElementsByTagName("TimeStamp").item(0).getTextContent(),nonce=doc.getElementsByTagName("Nonce").item(0).getTextContent();
        assertEquals("<xml><ToUserName>fixture-corp</ToUserName></xml>",cipher.decrypt(signature,time,nonce,encrypted));
        var wrong=new WxCpDefaultConfigImpl();wrong.setCorpId("other-corp");wrong.setToken(config.getToken());wrong.setAesKey(config.getAesKey());
        assertThrows(RuntimeException.class,()->new CorpBoundWecomCrypt(wrong).decrypt(signature,time,nonce,encrypted));
        assertThrows(RuntimeException.class,()->cipher.decrypt("wrong","1","nonce",encrypted));
    }
}
