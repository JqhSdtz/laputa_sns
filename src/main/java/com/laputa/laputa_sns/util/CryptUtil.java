package com.laputa.laputa_sns.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.DigestUtils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 加密工具类
 * @author JQH
 * @since 上午 11:17 20/03/21
 */

@Slf4j
public class CryptUtil {

    /**
     * 默认的AES加密算法
     */
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    private static final String HMAC_MD5_NAME = "HmacMD5";

    private static final Queue<SecureRandom> RANDOMS = new ConcurrentLinkedQueue<>();

    private static final Random NOT_SEC_RAND = new Random();

    private static final char[] CHAR_SET = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
            'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
            's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
            'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '-', '_'};

    private static int[] revPosSet = new int[128];

    static {
        for (int i = 0; i < CHAR_SET.length; ++i) {
            revPosSet[CHAR_SET[i]] = i;
        }
    }

    @Nullable
    public static String aesDecrypt(String key, String content, String iv) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Base64.decodeBase64(key), "AES"), new IvParameterSpec(Base64.decodeBase64(iv)));
            byte[] result = cipher.doFinal(Base64.decodeBase64(content));
            return new String(result, "utf-8");
        } catch (Exception e) {}
        return null;
    }

    private static String messageDigest(@NotNull String str, String alg) {
        byte[] secretBytes = null;
        try {
            MessageDigest md = MessageDigest.getInstance(alg);
            md.update(str.getBytes());
            secretBytes = md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return new BigInteger(1, secretBytes).toString(16);
    }

    public static String md5(@NotNull String str) {
        return DigestUtils.md5DigestAsHex(str.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha1(@NotNull String str) {
        return messageDigest(str, "SHA");
    }

    @Nullable
    public static String hmac(@NotNull String key, @NotNull String content) {
        try {
            SecretKeySpec sk = new SecretKeySpec(key.getBytes("UTF-8"), HMAC_MD5_NAME);
            Mac mac = Mac.getInstance(HMAC_MD5_NAME);
            mac.init(sk);
            byte[] result = mac.doFinal(content.getBytes());
            return Base64.encodeBase64String(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @NotNull
    private static SecureRandom getSecureRandom() {
        //参考org.apache.catalina.util.SessionIdGeneratorBase.getRandomBytes
        //对tomcat中产生随机数代码的拙劣模仿...
        SecureRandom random = RANDOMS.poll();
        if (random == null) {
            try {
                random = SecureRandom.getInstance("SHA1PRNG");
            } catch (NoSuchAlgorithmException e) {
                log.warn("SecureRandom实例获取失败，采用默认实例");
                random = new SecureRandom();
            }
            random.nextInt();
        }
        RANDOMS.add(random);
        return random;
    }

    @NotNull
    public static String randUrlSafeStr(int length, boolean secure) {
        Random random = secure ? getSecureRandom() : NOT_SEC_RAND;
        byte[] randBytes = new byte[length];
        random.nextBytes(randBytes);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; ++i) {
            builder.append(CHAR_SET[randBytes[i] & 0x3f]);
        }
        return builder.toString();
    }

    @NotNull
    public static String longToStr(long l) {
        StringBuilder stringBuilder = new StringBuilder();
        while(l >= 64) {
            stringBuilder.append(CHAR_SET[(int) (l % 64)]);
            l >>= 6;
        }
        if (l > 0) {
            stringBuilder.append(CHAR_SET[(int) l]);
        }
        return stringBuilder.toString();
    }

    public static long strToLong(@NotNull String str) {
        long res = 0;
        for (int i = str.length() - 1; i >= 0; --i) {
            res = (res << 6) + revPosSet[str.charAt(i)];
        }
        return res;
    }

}
