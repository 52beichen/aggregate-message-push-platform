package dev.qingzhou.pushserver.manager.wecom;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;

public final class WecomAuthorizationState {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern VALID_STATE = Pattern.compile("[A-Za-z0-9]{32,128}");

    private WecomAuthorizationState() {
    }

    public static String create() {
        byte[] nonce = new byte[24];
        RANDOM.nextBytes(nonce);
        return HexFormat.of().formatHex(nonce);
    }

    public static boolean isValidFormat(String state) {
        return state != null && VALID_STATE.matcher(state).matches();
    }
}
