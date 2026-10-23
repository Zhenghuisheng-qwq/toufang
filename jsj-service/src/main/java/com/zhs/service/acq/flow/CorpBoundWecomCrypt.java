package com.zhs.service.acq.flow;

import com.zhs.common.exception.BusinessException;
import me.chanjar.weixin.cp.config.WxCpConfigStorage;
import me.chanjar.weixin.cp.util.crypto.WxCpCryptUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/** WxJava 4.8.0 verifies signatures and decrypts, but omits the envelope receiver check. */
public final class CorpBoundWecomCrypt extends WxCpCryptUtil {

    public CorpBoundWecomCrypt(WxCpConfigStorage config) {
        super(config);
    }

    @Override
    public String decrypt(String encrypted) {
        String plain = super.decrypt(encrypted);
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(Arrays.copyOf(aesKey, 16)));
            byte[] bytes = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            int padding = Byte.toUnsignedInt(bytes[bytes.length - 1]);
            if (padding < 1 || padding > 32 || bytes.length - padding < 20) {
                throw new IllegalArgumentException();
            }
            for (int i = bytes.length - padding; i < bytes.length; i++) {
                if (Byte.toUnsignedInt(bytes[i]) != padding) {
                    throw new IllegalArgumentException();
                }
            }
            int length = ByteBuffer.wrap(bytes, 16, 4).getInt();
            int end = bytes.length - padding;
            if (length < 0 || length > end - 20) {
                throw new IllegalArgumentException();
            }
            byte[] receiver = Arrays.copyOfRange(bytes, 20 + length, end);
            if (!MessageDigest.isEqual(receiver, appidOrCorpid.getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException();
            }
            return plain;
        } catch (Exception e) {
            throw new BusinessException("企微密文接收企业不匹配或报文不合法");
        }
    }
}
