package dev.qingzhou.pushserver.controller;

import dev.qingzhou.pushserver.common.PortalResponse;
import dev.qingzhou.pushserver.common.PortalSessionSupport;
import dev.qingzhou.pushserver.model.dto.portal.AppPluginConfigSaveRequest;
import dev.qingzhou.pushserver.model.vo.portal.PortalAppPluginConfigVo;
import dev.qingzhou.pushserver.service.PortalAppPluginService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v2/apps/{appId}/plugins")
@RequiredArgsConstructor
public class PortalAppPluginController {

    private final PortalAppPluginService appPluginService;

    @GetMapping
    public PortalResponse<List<PortalAppPluginConfigVo>> list(
            @PathVariable Long appId,
            HttpSession session
    ) {
        Long userId = PortalSessionSupport.requireUserId(session);
        return PortalResponse.ok(appPluginService.listByApp(userId, appId));
    }

    @PostMapping
    public PortalResponse<Void> saveConfig(
            @PathVariable Long appId,
            @Valid @RequestBody AppPluginConfigSaveRequest request,
            HttpSession session
    ) {
        Long userId = PortalSessionSupport.requireUserId(session);
        appPluginService.saveConfig(userId, appId, request);
        return PortalResponse.ok(null);
    }

    @DeleteMapping("/{pluginKey}")
    public PortalResponse<Void> deleteConfig(
            @PathVariable Long appId,
            @PathVariable String pluginKey,
            HttpSession session
    ) {
        Long userId = PortalSessionSupport.requireUserId(session);
        appPluginService.deleteConfig(userId, appId, pluginKey);
        return PortalResponse.ok(null);
    }
}
