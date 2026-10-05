package dev.qingzhou.pushserver.controller;

import dev.qingzhou.pushserver.common.PortalResponse;
import dev.qingzhou.pushserver.model.dto.portal.PortalPluginCreateRequest;
import dev.qingzhou.pushserver.model.vo.portal.PortalPluginVo;
import dev.qingzhou.pushserver.service.PortalPluginService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/plugin")
@RequiredArgsConstructor
public class PortalPluginController {

    private final PortalPluginService pluginService;

    @PostMapping("/create")
    public PortalResponse<String> create(@Valid @RequestBody PortalPluginCreateRequest request) {
        String token = pluginService.createPlugin(request);
        return PortalResponse.ok(token);
    }

    @PostMapping("/reset-token")
    public PortalResponse<String> resetToken(@RequestBody Map<String, Integer> payload) {
        Integer id = payload.get("id");
        if (id == null) {
            return PortalResponse.fail("ID is required");
        }
        String newToken = pluginService.resetToken(id);
        return PortalResponse.ok(newToken);
    }

    @PostMapping("/status")
    public PortalResponse<Void> switchStatus(@RequestBody Map<String, Integer> payload) {
        Integer id = payload.get("id");
        Integer status = payload.get("status");
        if (id == null || status == null) {
            return PortalResponse.fail("ID and status are required");
        }
        pluginService.switchStatus(id, status);
        return PortalResponse.ok(null);
    }

    @GetMapping("/list")
    public PortalResponse<List<PortalPluginVo>> list() {
        return PortalResponse.ok(pluginService.listPlugins());
    }

    @PostMapping("/delete")
    public PortalResponse<Void> delete(@RequestBody Map<String, Integer> payload) {
        Integer id = payload.get("id");
        if (id == null) {
            return PortalResponse.fail("ID is required");
        }
        pluginService.deletePlugin(id);
        return PortalResponse.ok(null);
    }
}