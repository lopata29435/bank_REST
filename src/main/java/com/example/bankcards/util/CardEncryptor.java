package com.example.bankcards.util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import java.util.Base64;

@Component
public class CardEncryptor {

    private static final String AES = "AES";
    private static final String AES_CBC = "AES/CBC/PKCS5Padding";
    private static final byte[] FIXED_IV = {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
    };

    private SecretKey secretKey;
    private IvParameterSpec ivSpec;

    @Value("${card.encryption.key}")
    private String keyBase64;

    @PostConstruct
    private void init() {
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        this.secretKey = new SecretKeySpec(keyBytes, AES);
        this.ivSpec = new IvParameterSpec(FIXED_IV);
    }

    public String encrypt(String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_CBC);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decrypt(String ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_CBC);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        byte[] encrypted = Base64.getDecoder().decode(ciphertext);
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted);
    }
}