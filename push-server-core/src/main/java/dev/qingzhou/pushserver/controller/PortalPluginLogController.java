package dev.qingzhou.pushserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.qingzhou.pushserver.common.PortalResponse;
import dev.qingzhou.pushserver.model.entity.portal.PortalPluginActionLog;
import dev.qingzhou.pushserver.model.entity.portal.PortalPluginHeartbeatLog;
import dev.qingzhou.pushserver.model.vo.portal.PortalPageResponse;
import dev.qingzhou.pushserver.mapper.portal.PortalPluginActionLogMapper;
import dev.qingzhou.pushserver.mapper.portal.PortalPluginHeartbeatLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 插件观测日志（ActionAck / Heartbeat）查询
 */
@RestController
@RequestMapping("/v2/plugins")
@RequiredArgsConstructor
public class PortalPluginLogController {

    private final PortalPluginActionLogMapper actionLogMapper;
    private final PortalPluginHeartbeatLogMapper heartbeatLogMapper;

    @GetMapping("/{pluginKey}/actions")
    public PortalResponse<PortalPageResponse<PortalPluginActionLog>> listActions(
            @PathVariable String pluginKey,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(Math.min(pageSize, 200), 1);

        Page<PortalPluginActionLog> p = actionLogMapper.selectPage(
                Page.of(safePage, safePageSize),
                new LambdaQueryWrapper<PortalPluginActionLog>()
                        .eq(PortalPluginActionLog::getPluginKey, pluginKey)
                        .orderByDesc(PortalPluginActionLog::getId)
        );

        PortalPageResponse<PortalPluginActionLog> resp = PortalPageResponse.of(
                p.getRecords(), p.getTotal(), (int) p.getCurrent(), (int) p.getSize());
        return PortalResponse.ok(resp);
    }

    @GetMapping("/{pluginKey}/heartbeats")
    public PortalResponse<PortalPageResponse<PortalPluginHeartbeatLog>> listHeartbeats(
            @PathVariable String pluginKey,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(Math.min(pageSize, 200), 1);

        Page<PortalPluginHeartbeatLog> p = heartbeatLogMapper.selectPage(
                Page.of(safePage, safePageSize),
                new LambdaQueryWrapper<PortalPluginHeartbeatLog>()
                        .eq(PortalPluginHeartbeatLog::getPluginKey, pluginKey)
                        .orderByDesc(PortalPluginHeartbeatLog::getId)
        );

        PortalPageResponse<PortalPluginHeartbeatLog> resp = PortalPageResponse.of(
                p.getRecords(), p.getTotal(), (int) p.getCurrent(), (int) p.getSize());
        return PortalResponse.ok(resp);
    }
}
