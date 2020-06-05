package com.laputa.laputa_sns.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author JQH
 * @since 上午 11:17 20/03/21
 */

@Slf4j
public class CryptUtil {

    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";//默认的AES加密算法

    private static final String HMAC_MD5_NAME = "HmacMD5";

    private static final Queue<SecureRandom> randoms = new ConcurrentLinkedQueue();

    private static final Random notSecRand = new Random();

    private static final char[] charSet = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
            'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
            's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
            'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '-', '_'};

    private static int[] revPosSet = new int[128];

    static {
        for (int i = 0; i < charSet.length; ++i)
            revPosSet[charSet[i]] = i;
    }

    @Nullable
    public static String aesEncrypt(String key, String content) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            byte[] byteContent = content.getBytes("utf-8");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes("UTF-8"), "AES"));// 初始化为加密模式的密码器
            byte[] result = cipher.doFinal(byteContent);
            return Base64.encodeBase64String(result);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Nullable
    public static String aesDecrypt(String key, String content) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes("UTF-8"), "AES"));
            byte[] base64 = Base64.decodeBase64(content);
            byte[] result = cipher.doFinal(base64);
            return new String(result, "utf-8");
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return null;
    }

    public static String md5(@NotNull String str) {
        byte[] secretBytes = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            secretBytes = md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String md5code = new BigInteger(1, secretBytes).toString(16);
        for (int i = 0; i < 32 - md5code.length(); i++)
            md5code = "0" + md5code;
        return md5code;
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
        SecureRandom random = randoms.poll();
        if (random == null) {
            try {
                random = SecureRandom.getInstance("SHA1PRNG");
            } catch (NoSuchAlgorithmException e) {
                log.warn("SecureRandom实例获取失败，采用默认实例");
                random = new SecureRandom();
            }
            random.nextInt();
        }
        randoms.add(random);
        return random;
    }

    @NotNull
    public static String randUrlSafeStr(int length, boolean secure) {
        Random random = secure ? getSecureRandom() : notSecRand;
        byte randBytes[] = new byte[length];
        random.nextBytes(randBytes);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; ++i)
            builder.append(charSet[randBytes[i] & 0x3f]);
        return builder.toString();
    }

    @NotNull
    public static String longToStr(long l) {
        StringBuilder stringBuilder = new StringBuilder();
        while(l >= 64) {
            stringBuilder.append(charSet[(int) (l % 64)]);
            l >>= 6;
        }
        if (l > 0)
            stringBuilder.append(charSet[(int) l]);
        return stringBuilder.toString();
    }

    public static long strToLong(@NotNull String str) {
        long res = 0;
        for (int i = str.length() - 1; i >= 0; --i)
            res = (res << 6) + revPosSet[str.charAt(i)];
        return res;
    }

}
