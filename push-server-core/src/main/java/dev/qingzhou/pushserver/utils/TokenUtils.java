package dev.qingzhou.pushserver.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class TokenUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 生成带前缀的随机 Token (例如: sk_live_abc123...)
     */
    public static String generateToken(String prefix) {
        byte[] randomBytes = new byte[24];
        SECURE_RANDOM.nextBytes(randomBytes);
        String randomStr = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return prefix + randomStr;
    }
}
