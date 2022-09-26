package com.donphds.crypto.utils;

import cn.hutool.core.util.ByteUtil;
import com.donphds.crypto.excpetion.CryptoException;
import com.donphds.crypto.secret.KeyManager;
import com.donphds.crypto.vo.SecretKey;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@UtilityClass
public class CryptUtil {

    public static final String AES_CTR_NO_PADDING = "AES/CTR/NoPadding";
    public static final int VERSION_LEN = 4;
    private static final int IV_LEN = 16;
    private static final String AES_ALGORITHM = "AES";
    private static final SecureRandom secureRandom = new SecureRandom();
    private static KeyManager keyManager;

    public static void SetManager(KeyManager keyManager) {
        CryptUtil.keyManager = keyManager;
    }

    public static String encryptToString(String data) {
        return StringUtils.isBlank(data)
                ? data
                : Base64.getEncoder()
                        .encodeToString(encrypt(data.getBytes(StandardCharsets.UTF_8)));
    }

    public static String decryptToString(String cipher) {
        return StringUtils.isBlank(cipher)
                ? cipher
                : new String(decrypt(Base64.getDecoder().decode(cipher)), StandardCharsets.UTF_8);
    }

    private static byte[] decrypt(byte[] cipher) {
        if (cipher.length <= IV_LEN + VERSION_LEN) {
            throw new CryptoException("invalid cipher length");
        }
        return disassembly(cipher);
    }

    private static byte[] disassembly(byte[] cipher) {
        int cipherLen = cipher.length - VERSION_LEN - IV_LEN;
        byte[] versionByte = new byte[VERSION_LEN];
        byte[] iv = new byte[IV_LEN];
        byte[] ciphertext = new byte[cipherLen];
        System.arraycopy(cipher, 0, versionByte, 0, VERSION_LEN);
        int version = ByteUtil.bytesToInt(versionByte);
        SecretKey key = keyManager.getKey(version);
        System.arraycopy(cipher, VERSION_LEN, iv, 0, IV_LEN);
        System.arraycopy(cipher, VERSION_LEN + IV_LEN, ciphertext, 0, cipherLen);
        return process(
                ciphertext, Base64.getDecoder().decode(key.getAseKey()), iv, Cipher.DECRYPT_MODE);
    }

    private static byte[] encrypt(byte[] plaintext) {
        SecretKey key = keyManager.getLatestKey();
        byte[] iv = generateIV();
        byte[] cipherText =
                process(
                        plaintext,
                        Base64.getDecoder().decode(key.getAseKey()),
                        iv,
                        Cipher.ENCRYPT_MODE);
        return assemble(cipherText, iv, key.getVersion());
    }

    private static byte[] assemble(byte[] cipherText, byte[] iv, int version) {
        byte[] res = new byte[VERSION_LEN + iv.length + cipherText.length];
        byte[] versionByte = ByteUtil.intToBytes(version, ByteOrder.LITTLE_ENDIAN);
        System.arraycopy(versionByte, 0, res, 0, VERSION_LEN);
        System.arraycopy(iv, 0, res, VERSION_LEN, iv.length);
        System.arraycopy(cipherText, 0, res, VERSION_LEN + iv.length, cipherText.length);
        return res;
    }

    public static byte[] generateIV() {
        byte[] bytes = new byte[IV_LEN];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    public static byte[] process(byte[] plaintext, byte[] aesKey, byte[] iv, int mode) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, AES_ALGORITHM);
            Cipher cipher = Cipher.getInstance(AES_CTR_NO_PADDING);
            cipher.init(mode, secretKeySpec, new IvParameterSpec(iv));
            return cipher.doFinal(plaintext);
        } catch (NoSuchAlgorithmException
                | NoSuchPaddingException
                | InvalidKeyException
                | IllegalBlockSizeException
                | BadPaddingException
                | InvalidAlgorithmParameterException e) {
            throw new CryptoException(e);
        }
    }
}
