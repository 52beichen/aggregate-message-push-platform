package dev.qingzhou.pushserver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.qingzhou.pushserver.config.PortalWecomProperties;
import dev.qingzhou.pushserver.exception.PortalException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WecomPortalUrlServiceTest {
    @Test
    void buildsCallbackUrlsFromTheConfiguredPublicHttpsOrigin() {
        PortalWecomProperties properties = new PortalWecomProperties();
        properties.setPublicBaseUrl("https://juhe.beichenwl.cn/");
        WecomPortalUrlService service = new WecomPortalUrlService(properties);

        assertEquals("https://juhe.beichenwl.cn/api/v2/wecom/suite-callback/1",
                service.publicUrl("/api/v2/wecom/suite-callback/1"));
    }

    @Test
    void onlyAllowsConfiguredHttpsClientRedirects() {
        PortalWecomProperties properties = new PortalWecomProperties();
        properties.setClientRedirects(Map.of(
                "u-login", "https://u.beichenwl.cn/wework_suite_install_return.php"));
        WecomPortalUrlService service = new WecomPortalUrlService(properties);

        assertEquals("https://u.beichenwl.cn/wework_suite_install_return.php",
                service.clientRedirect("u-login"));
        assertThrows(PortalException.class, () -> service.clientRedirect("unknown"));
    }

    @Test
    void returnsTheConfiguredAuthorizationCallbackBridge() {
        PortalWecomProperties properties = new PortalWecomProperties();
        properties.setAuthorizationCallbackUrl(
                "https://u.beichenwl.cn/wework_suite_install_return.php");
        WecomPortalUrlService service = new WecomPortalUrlService(properties);

        assertEquals("https://u.beichenwl.cn/wework_suite_install_return.php",
                service.authorizationCallbackUrl());

        properties.setAuthorizationCallbackUrl("http://u.beichenwl.cn/callback");
        assertThrows(IllegalStateException.class, service::authorizationCallbackUrl);
    }

    @Test
    void rejectsNonHttpsPublicOrigins() {
        PortalWecomProperties properties = new PortalWecomProperties();
        properties.setPublicBaseUrl("http://juhe.beichenwl.cn");
        WecomPortalUrlService service = new WecomPortalUrlService(properties);

        assertThrows(IllegalStateException.class,
                () -> service.publicUrl("/api/v2/wecom/suite-callback/1"));
    }
}
