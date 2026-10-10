package dev.qingzhou.pushserver.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PortalWecomSuiteAppServiceImplTest {
    @Test
    void encodesTheAuthorizationCallbackAsOneQueryParameter() {
        String url = PortalWecomSuiteAppServiceImpl.buildInstallUrl(
                "ww-suite", "pre_auth-code", "https://u.beichenwl.cn/callback?a=1", "state value");

        assertEquals("https://open.work.weixin.qq.com/3rdapp/install"
                + "?suite_id=ww-suite"
                + "&pre_auth_code=pre_auth-code"
                + "&redirect_uri=https%3A%2F%2Fu.beichenwl.cn%2Fcallback%3Fa%3D1"
                + "&state=state%20value", url);
    }
}
