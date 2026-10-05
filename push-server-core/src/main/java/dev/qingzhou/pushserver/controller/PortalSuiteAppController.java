package dev.qingzhou.pushserver.controller;

import dev.qingzhou.pushserver.common.PortalResponse;
import dev.qingzhou.pushserver.common.PortalSessionSupport;
import dev.qingzhou.pushserver.model.dto.portal.PortalSuiteAppCreateRequest;
import dev.qingzhou.pushserver.model.dto.portal.PortalSuiteAppUpdateRequest;
import dev.qingzhou.pushserver.model.entity.portal.PortalWecomSuiteApp;
import dev.qingzhou.pushserver.model.vo.portal.PortalSuiteAppResponse;
import dev.qingzhou.pushserver.service.PortalWecomSuiteAppService;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/v2/apps/third-party")
public class PortalSuiteAppController {
    private final PortalWecomSuiteAppService service;

    public PortalSuiteAppController(PortalWecomSuiteAppService service) {
        this.service = service;
    }

    @PostMapping
    public PortalResponse<PortalSuiteAppResponse> create(@Valid @RequestBody PortalSuiteAppCreateRequest request,
                                                         HttpSession session, HttpServletRequest servletRequest) {
        Long userId = PortalSessionSupport.requireUserId(session);
        PortalWecomSuiteApp app = service.create(userId, request.getSuiteId(), request.getSuiteSecret(),
                request.getToken(), request.getEncodingAesKey());
        return PortalResponse.ok(toResponse(app, servletRequest));
    }

    @GetMapping
    public PortalResponse<List<PortalSuiteAppResponse>> list(HttpSession session, HttpServletRequest request) {
        Long userId = PortalSessionSupport.requireUserId(session);
        return PortalResponse.ok(service.listByUser(userId).stream().map(app -> toResponse(app, request)).toList());
    }

    @PutMapping("/{suiteAppId}")
    public PortalResponse<PortalSuiteAppResponse> update(@PathVariable Long suiteAppId,
                                                         @RequestBody PortalSuiteAppUpdateRequest request,
                                                         HttpSession session, HttpServletRequest servletRequest) {
        Long userId = PortalSessionSupport.requireUserId(session);
        PortalWecomSuiteApp app = service.update(userId, suiteAppId, request.getSuiteSecret(),
                request.getToken(), request.getEncodingAesKey());
        return PortalResponse.ok(toResponse(app, servletRequest));
    }

    @DeleteMapping("/{suiteAppId}")
    public PortalResponse<Void> delete(@PathVariable Long suiteAppId, HttpSession session) {
        service.delete(PortalSessionSupport.requireUserId(session), suiteAppId);
        return PortalResponse.ok("Deleted", null);
    }

    @PostMapping("/{suiteAppId}/refresh-token")
    public PortalResponse<PortalSuiteAppResponse> refreshToken(@PathVariable Long suiteAppId,
                                                                 HttpSession session,
                                                                 HttpServletRequest request) {
        Long userId = PortalSessionSupport.requireUserId(session);
        service.requireByUser(userId, suiteAppId);
        service.refreshSuiteAccessToken(suiteAppId);
        return PortalResponse.ok(toResponse(service.requireByUser(userId, suiteAppId), request));
    }

    private PortalSuiteAppResponse toResponse(PortalWecomSuiteApp app, HttpServletRequest request) {
        PortalSuiteAppResponse response = new PortalSuiteAppResponse();
        response.setId(app.getId());
        response.setSuiteId(app.getSuiteId());
        response.setHasSuiteTicket(app.getSuiteTicket() != null && !app.getSuiteTicket().isBlank());
        response.setHasSuiteAccessToken(app.getSuiteAccessToken() != null
                && app.getSuiteAccessTokenExpiresAt() != null
                && app.getSuiteAccessTokenExpiresAt() > System.currentTimeMillis());
        response.setCreatedAt(app.getCreatedAt());
        response.setUpdatedAt(app.getUpdatedAt());
        response.setCallbackUrl(ServletUriComponentsBuilder.fromContextPath(request)
                .path("/api/v2/wecom/suite-callback/{id}").buildAndExpand(app.getId()).toUriString());
        return response;
    }
}
