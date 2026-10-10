package dev.qingzhou.pushserver.manager.wecom;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class WecomAuthorizationState {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long MAX_AGE_SECONDS = Duration.ofMinutes(30).toSeconds();

    private WecomAuthorizationState() {
    }

    public static String create(Long suiteAppId, String suiteSecret) {
        byte[] nonce = new byte[12];
        RANDOM.nextBytes(nonce);
        String payload = suiteAppId + "." + (System.currentTimeMillis() / 1000L) + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);
        return payload + "." + sign(payload, suiteSecret);
    }

    public static boolean verify(String state, Long suiteAppId, String suiteSecret) {
        if (state == null || suiteAppId == null || suiteSecret == null) return false;
        String[] parts = state.split("\\.", -1);
        if (parts.length != 4 || !suiteAppId.toString().equals(parts[0])) return false;
        try {
            long issuedAt = Long.parseLong(parts[1]);
            long age = System.currentTimeMillis() / 1000L - issuedAt;
            if (age < -60L || age > MAX_AGE_SECONDS) return false;
            String payload = parts[0] + "." + parts[1] + "." + parts[2];
            byte[] expected = sign(payload, suiteSecret).getBytes(StandardCharsets.US_ASCII);
            byte[] actual = parts[3].getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign WeCom authorization state", ex);
        }
    }
}
