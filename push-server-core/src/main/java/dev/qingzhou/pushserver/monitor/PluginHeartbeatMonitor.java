package dev.qingzhou.pushserver.monitor;

import dev.qingzhou.pushserver.grpc.PluginConnectionManager;
import dev.qingzhou.pushserver.service.PluginManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 定期检查远程插件心跳；超时则强制下线。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PluginHeartbeatMonitor {

    private static final long TIMEOUT_MS = 90_000; // 90 秒无心跳视为离线

    private final PluginConnectionManager connectionManager;
    private final PluginManagerService pluginManagerService;

    @Scheduled(fixedDelay = 30_000)
    public void checkHeartbeats() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : connectionManager.snapshotHeartbeats().entrySet()) {
            String pluginKey = entry.getKey();
            Long last = entry.getValue();
            if (last == null) {
                continue;
            }
            if (now - last > TIMEOUT_MS) {
                log.warn("Plugin {} heartbeat timeout ({} ms > {}) - forcing disconnect", pluginKey, now - last, TIMEOUT_MS);
                connectionManager.unregister(pluginKey);
                pluginManagerService.unregisterRemotePlugin(pluginKey);
            }
        }
    }
}
