package dev.qingzhou.pushserver.controller.wecom;

import dev.qingzhou.pushserver.exception.PortalException;
import dev.qingzhou.pushserver.exception.PortalStatus;
import dev.qingzhou.pushserver.model.entity.portal.PortalWecomSuiteApp;
import dev.qingzhou.pushserver.model.entity.portal.PortalWecomSuiteAuthorization;
import dev.qingzhou.pushserver.model.wecom.WecomAuthorizationSession;
import dev.qingzhou.pushserver.service.PortalWecomSuiteAppService;
import dev.qingzhou.pushserver.service.WecomAuthorizationSessionService;
import dev.qingzhou.pushserver.service.WecomPortalUrlService;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/v2/wecom/suite-auth/{suiteAppId}")
public class WecomSuiteAuthorizationController {
    private final PortalWecomSuiteAppService service;
    private final WecomAuthorizationSessionService sessionService;
    private final WecomPortalUrlService urlService;

    public WecomSuiteAuthorizationController(PortalWecomSuiteAppService service,
                                             WecomAuthorizationSessionService sessionService,
                                             WecomPortalUrlService urlService) {
        this.service = service;
        this.sessionService = sessionService;
        this.urlService = urlService;
    }

    @GetMapping("/install")
    public ResponseEntity<Void> install(@PathVariable Long suiteAppId,
                                        @RequestParam(name = "client", required = false) String client,
                                        @RequestParam(name = "resume", required = false) String resume) {
        requireApp(suiteAppId);
        if (StringUtils.hasText(client)) urlService.clientRedirect(client);
        if (resume != null && resume.length() > 512) {
            throw new PortalException(PortalStatus.BAD_REQUEST, "WeCom resume state is too long");
        }
        WecomAuthorizationSession session = sessionService.create(suiteAppId, client, resume);
        String redirectUri = urlService.publicUrl("/api/v2/wecom/suite-auth/" + suiteAppId + "/complete");
        String installUrl = service.createInstallUrl(suiteAppId, redirectUri, session.state());
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(installUrl))
                .cacheControl(CacheControl.noStore())
                .build();
    }

    @GetMapping(value = "/complete", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<?> complete(@PathVariable Long suiteAppId,
                                      @RequestParam("auth_code") String authCode,
                                      @RequestParam("state") String state) {
        requireApp(suiteAppId);
        WecomAuthorizationSession session = sessionService.requireValid(state, suiteAppId);
        PortalWecomSuiteAuthorization authorization = service.completeAuthorization(suiteAppId, authCode);
        sessionService.markUsed(state, suiteAppId);

        String clientRedirect = urlService.clientRedirect(session.clientId());
        if (clientRedirect != null) {
            UriComponentsBuilder redirect = UriComponentsBuilder.fromUriString(clientRedirect)
                    .queryParam("authorization", "success")
                    .queryParam("corp_id", authorization.getCorpId());
            if (StringUtils.hasText(session.resumeState())) {
                redirect.queryParam("state", session.resumeState());
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(redirect.build().encode().toUri())
                    .cacheControl(CacheControl.noStore())
                    .build();
        }

        String corpName = authorization.getCorpName();
        if (!StringUtils.hasText(corpName)) corpName = authorization.getCorpId();
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
}
