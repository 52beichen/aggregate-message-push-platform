package dev.qingzhou.pushserver.controller.wecom;

import dev.qingzhou.pushserver.exception.PortalException;
import dev.qingzhou.pushserver.exception.PortalStatus;
import dev.qingzhou.pushserver.manager.wecom.WecomAuthorizationState;
import dev.qingzhou.pushserver.model.entity.portal.PortalWecomSuiteApp;
import dev.qingzhou.pushserver.model.entity.portal.PortalWecomSuiteAuthorization;
import dev.qingzhou.pushserver.service.PortalWecomSuiteAppService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/v2/wecom/suite-auth/{suiteAppId}")
public class WecomSuiteAuthorizationController {
    private final PortalWecomSuiteAppService service;

    public WecomSuiteAuthorizationController(PortalWecomSuiteAppService service) {
        this.service = service;
    }

    @GetMapping("/install")
    public ResponseEntity<Void> install(@PathVariable Long suiteAppId, HttpServletRequest request) {
        PortalWecomSuiteApp app = requireApp(suiteAppId);
        String state = WecomAuthorizationState.create(suiteAppId, app.getSuiteSecret());
        String redirectUri = callbackUrl(suiteAppId, request);
        String installUrl = service.createInstallUrl(suiteAppId, redirectUri, state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(installUrl))
                .cacheControl(CacheControl.noStore())
                .build();
    }

    @GetMapping(value = "/complete", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> complete(@PathVariable Long suiteAppId,
                                           @RequestParam("auth_code") String authCode,
                                           @RequestParam("state") String state) {
        PortalWecomSuiteApp app = requireApp(suiteAppId);
        if (!WecomAuthorizationState.verify(state, suiteAppId, app.getSuiteSecret())) {
            throw new PortalException(PortalStatus.BAD_REQUEST, "Invalid or expired WeCom authorization state");
        }
        PortalWecomSuiteAuthorization authorization = service.completeAuthorization(suiteAppId, authCode);
        String corpName = authorization.getCorpName();
        if (corpName == null || corpName.isBlank()) corpName = authorization.getCorpId();
        String html = "<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>企业微信授权成功</title></head><body>"
                + "<main><h1>授权成功</h1><p>企业 " + HtmlUtils.htmlEscape(corpName)
                + " 已完成应用授权，可以关闭此页面。</p></main></body></html>";
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private PortalWecomSuiteApp requireApp(Long suiteAppId) {
        PortalWecomSuiteApp app = service.getById(suiteAppId);
        if (app == null) throw new PortalException(PortalStatus.NOT_FOUND, "Third-party app not found");
        return app;
    }

    private String callbackUrl(Long suiteAppId, HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromContextPath(request)
                .path("/api/v2/wecom/suite-auth/{id}/complete")
                .buildAndExpand(suiteAppId)
                .toUriString();
    }
}
