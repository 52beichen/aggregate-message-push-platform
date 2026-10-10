package dev.qingzhou.pushserver.controller;

import dev.qingzhou.pushserver.common.PortalResponse;
import dev.qingzhou.pushserver.common.PortalSessionSupport;
import dev.qingzhou.pushserver.model.dto.portal.PortalSuiteAppCreateRequest;
import dev.qingzhou.pushserver.model.dto.portal.PortalSuiteAppUpdateRequest;
import dev.qingzhou.pushserver.model.entity.portal.PortalWecomSuiteApp;
import dev.qingzhou.pushserver.model.entity.portal.PortalWecomSuiteAuthorization;
import dev.qingzhou.pushserver.model.vo.portal.PortalSuiteAppResponse;
import dev.qingzhou.pushserver.model.vo.portal.PortalSuiteAuthorizationResponse;
import dev.qingzhou.pushserver.service.PortalWecomSuiteAppService;
import dev.qingzhou.pushserver.service.WecomPortalUrlService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/apps/third-party")
public class PortalSuiteAppController {
    private final PortalWecomSuiteAppService service;
    private final WecomPortalUrlService urlService;

    public PortalSuiteAppController(PortalWecomSuiteAppService service, WecomPortalUrlService urlService) {
        this.service = service;
        this.urlService = urlService;
    }

    @PostMapping
    public PortalResponse<PortalSuiteAppResponse> create(@Valid @RequestBody PortalSuiteAppCreateRequest request,
                                                         HttpSession session) {
        Long userId = PortalSessionSupport.requireUserId(session);
        PortalWecomSuiteApp app = service.create(userId, request.getSuiteId(), request.getSuiteSecret(),
                request.getToken(), request.getEncodingAesKey());
        return PortalResponse.ok(toResponse(app));
    }

    @GetMapping
    public PortalResponse<List<PortalSuiteAppResponse>> list(HttpSession session) {
        Long userId = PortalSessionSupport.requireUserId(session);
        return PortalResponse.ok(service.listByUser(userId).stream().map(this::toResponse).toList());
    }

    @PutMapping("/{suiteAppId}")
    public PortalResponse<PortalSuiteAppResponse> update(@PathVariable Long suiteAppId,
                                                         @RequestBody PortalSuiteAppUpdateRequest request,
                                                         HttpSession session) {
        Long userId = PortalSessionSupport.requireUserId(session);
        PortalWecomSuiteApp app = service.update(userId, suiteAppId, request.getSuiteSecret(),
                request.getToken(), request.getEncodingAesKey());
        return PortalResponse.ok(toResponse(app));
    }

    @DeleteMapping("/{suiteAppId}")
    public PortalResponse<Void> delete(@PathVariable Long suiteAppId, HttpSession session) {
        service.delete(PortalSessionSupport.requireUserId(session), suiteAppId);
        return PortalResponse.ok("Deleted", null);
    }

    @PostMapping("/{suiteAppId}/refresh-token")
    public PortalResponse<PortalSuiteAppResponse> refreshToken(@PathVariable Long suiteAppId,
                                                                 HttpSession session) {
        Long userId = PortalSessionSupport.requireUserId(session);
        service.requireByUser(userId, suiteAppId);
        service.refreshSuiteAccessToken(suiteAppId);
        return PortalResponse.ok(toResponse(service.requireByUser(userId, suiteAppId)));
    }

    @GetMapping("/{suiteAppId}/authorizations")
    public PortalResponse<List<PortalSuiteAuthorizationResponse>> authorizations(@PathVariable Long suiteAppId,
                                                                                 HttpSession session) {
        Long userId = PortalSessionSupport.requireUserId(session);
        return PortalResponse.ok(service.listAuthorizations(userId, suiteAppId).stream()
                .map(this::toAuthorizationResponse)
                .toList());
    }

    private PortalSuiteAppResponse toResponse(PortalWecomSuiteApp app) {
        PortalSuiteAppResponse response = new PortalSuiteAppResponse();
        response.setId(app.getId());
        response.setSuiteId(app.getSuiteId());
        response.setHasSuiteTicket(app.getSuiteTicket() != null && !app.getSuiteTicket().isBlank());
        response.setHasSuiteAccessToken(app.getSuiteAccessToken() != null
                && app.getSuiteAccessTokenExpiresAt() != null
                && app.getSuiteAccessTokenExpiresAt() > System.currentTimeMillis());
        response.setCreatedAt(app.getCreatedAt());
        response.setUpdatedAt(app.getUpdatedAt());
        response.setCallbackUrl(urlService.publicUrl("/api/v2/wecom/suite-callback/" + app.getId()));
        response.setInstallUrl(urlService.publicUrl("/api/v2/wecom/suite-auth/" + app.getId() + "/install"));
        response.setAuthorizationCallbackUrl(urlService.publicUrl(
                "/api/v2/wecom/suite-auth/" + app.getId() + "/complete"));
        response.setAuthorizedCorpCount(service.listAuthorizations(app.getUserId(), app.getId()).stream()
                .filter(authorization -> Integer.valueOf(1).equals(authorization.getStatus()))
                .count());
        return response;
    }

    private PortalSuiteAuthorizationResponse toAuthorizationResponse(PortalWecomSuiteAuthorization authorization) {
        PortalSuiteAuthorizationResponse response = new PortalSuiteAuthorizationResponse();
        response.setId(authorization.getId());
        response.setCorpId(authorization.getCorpId());
        response.setCorpName(authorization.getCorpName());
        response.setCorpFullName(authorization.getCorpFullName());
        response.setCorpType(authorization.getCorpType());
        response.setCorpSquareLogoUrl(authorization.getCorpSquareLogoUrl());
        response.setAgentId(authorization.getAgentId());
        response.setActive(Integer.valueOf(1).equals(authorization.getStatus()));
        response.setAuthorizedAt(authorization.getAuthorizedAt());
        response.setCancelledAt(authorization.getCancelledAt());
        response.setUpdatedAt(authorization.getUpdatedAt());
        return response;
    }
}
