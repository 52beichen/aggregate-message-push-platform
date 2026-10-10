package dev.qingzhou.pushserver.service;

import dev.qingzhou.pushserver.config.PortalWecomProperties;
import dev.qingzhou.pushserver.exception.PortalException;
import dev.qingzhou.pushserver.exception.PortalStatus;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class WecomPortalUrlService {
    private final PortalWecomProperties properties;

    public WecomPortalUrlService(PortalWecomProperties properties) {
        this.properties = properties;
    }

    public String publicUrl(String path) {
        String baseUrl = requireHttpsUrl(properties.getPublicBaseUrl(), "WeCom public base URL");
        return UriComponentsBuilder.fromUriString(stripTrailingSlash(baseUrl))
                .path(path.startsWith("/") ? path : "/" + path)
                .build()
                .toUriString();
    }

    public String clientRedirect(String clientId) {
        if (!StringUtils.hasText(clientId)) return null;
        String redirect = properties.getClientRedirects().get(clientId);
        if (!StringUtils.hasText(redirect)) {
            throw new PortalException(PortalStatus.BAD_REQUEST, "Unknown WeCom authorization client");
        }
        return requireHttpsUrl(redirect, "WeCom client redirect URL");
    }

    public String authorizationCallbackUrl() {
        return requireHttpsUrl(properties.getAuthorizationCallbackUrl(),
                "WeCom authorization callback URL");
    }

    private String requireHttpsUrl(String value, String name) {
        if (!StringUtils.hasText(value)) throw new IllegalStateException(name + " is not configured");
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(name + " is invalid", ex);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
            throw new IllegalStateException(name + " must be an absolute HTTPS URL");
        }
        return uri.toString();
    }

    private String stripTrailingSlash(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') end--;
        return value.substring(0, end);
    }
}
