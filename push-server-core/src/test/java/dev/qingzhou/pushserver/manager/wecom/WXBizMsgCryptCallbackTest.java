package dev.qingzhou.pushserver.manager.wecom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WXBizMsgCryptCallbackTest {
    private static final String TOKEN = "callback-token";
    private static final String AES_KEY = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";

    @Test
    void urlVerificationCanAcceptAProviderCorpIdAfterSignatureAndAesValidation() throws Exception {
        String timestamp = "1791640000";
        String nonce = "nonce";
        WXBizMsgCrypt sender = new WXBizMsgCrypt(TOKEN, AES_KEY, "provider-corp-id");
        String encryptedEcho = sender.encrypt("0123456789abcdef", "verified-echo");
        String signature = WXBizMsgCrypt.SHA1.getSHA1(TOKEN, timestamp, nonce, encryptedEcho);
        WXBizMsgCrypt receiver = new WXBizMsgCrypt(TOKEN, AES_KEY, "ww-suite-id");

        assertThrows(AesException.class,
                () -> receiver.VerifyURL(signature, timestamp, nonce, encryptedEcho));
        assertEquals("verified-echo",
                receiver.VerifyURL(signature, timestamp, nonce, encryptedEcho, false));
    }
}
