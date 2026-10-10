package dev.qingzhou.pushserver.controller.wecom;

import dev.qingzhou.pushserver.common.PortalResponse;
import dev.qingzhou.pushserver.config.PortalWecomProperties;
import dev.qingzhou.pushserver.model.entity.portal.PortalWecomSuiteAuthorization;
import dev.qingzhou.pushserver.model.vo.portal.WecomInternalAuthorizationResponse;
import dev.qingzhou.pushserver.service.PortalWecomSuiteAppService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/internal/wecom/suite-apps/{suiteAppId}/corps/{corpId}/authorization")
public class WecomInternalAuthorizationController {
    private final PortalWecomSuiteAppService service;
    private final PortalWecomProperties properties;

    public WecomInternalAuthorizationController(PortalWecomSuiteAppService service,
                                                 PortalWecomProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping
    public ResponseEntity<PortalResponse<WecomInternalAuthorizationResponse>> getAuthorization(
            @PathVariable Long suiteAppId,
            @PathVariable String corpId,
            @RequestHeader(name = "X-Internal-API-Key", required = false) String apiKey) {
        if (!StringUtils.hasText(properties.getInternalApiKey())) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(PortalResponse.fail("Internal WeCom API is not configured"));
        }
        if (!constantTimeEquals(properties.getInternalApiKey(), apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(PortalResponse.fail("Invalid internal API key"));
        }
        if (service.getById(suiteAppId) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(PortalResponse.fail("Third-party app not found"));
        }

        PortalWecomSuiteAuthorization authorization = service.getAuthorization(suiteAppId, corpId);
        WecomInternalAuthorizationResponse response = new WecomInternalAuthorizationResponse();
        response.setCorpId(corpId);
        if (authorization != null) {
            response.setActive(Integer.valueOf(1).equals(authorization.getStatus()));
            response.setAgentId(authorization.getAgentId());
            response.setAuthorizedAt(authorization.getAuthorizedAt());
            response.setUpdatedAt(authorization.getUpdatedAt());
        }
        return ResponseEntity.ok()
                .header("Cache-Control", "private, max-age=30")
                .body(PortalResponse.ok(response));
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
