package dev.qingzhou.pushserver.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "push.portal.wecom")
public class PortalWecomProperties {

    private String baseUrl = "https://qyapi.weixin.qq.com";
    private String publicBaseUrl = "https://juhe.beichenwl.cn";
    private String authorizationCallbackUrl = "https://u.beichenwl.cn/wework_suite_install_return.php";
    private String internalApiKey = "";
    private Map<String, String> clientRedirects = new LinkedHashMap<>(Map.of(
            "u-login", "https://u.beichenwl.cn/wework_suite_install_return.php"));

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getAuthorizationCallbackUrl() {
        return authorizationCallbackUrl;
    }

    public void setAuthorizationCallbackUrl(String authorizationCallbackUrl) {
        this.authorizationCallbackUrl = authorizationCallbackUrl;
    }

    public String getInternalApiKey() {
        return internalApiKey;
    }

    public void setInternalApiKey(String internalApiKey) {
        this.internalApiKey = internalApiKey;
    }

    public Map<String, String> getClientRedirects() {
        return clientRedirects;
    }

    public void setClientRedirects(Map<String, String> clientRedirects) {
        this.clientRedirects = clientRedirects == null ? new LinkedHashMap<>() : clientRedirects;
    }
}
