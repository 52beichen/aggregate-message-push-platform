package dev.qingzhou.pushserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.qingzhou.push.api.model.ActionContext;
import dev.qingzhou.push.api.model.ConfigField;
import dev.qingzhou.push.api.model.PluginMeta;
import dev.qingzhou.push.api.spi.PushPlugin;
import dev.qingzhou.push.api.spi.PushSender;
import dev.qingzhou.pushserver.grpc.PluginPacketDispatcher;
import dev.qingzhou.pushserver.mapper.portal.PortalWecomAppMapper;
import dev.qingzhou.pushserver.model.entity.portal.PortalWecomApp;
import dev.qingzhou.pushserver.mapper.portal.PortalPluginActionLogMapper;
import dev.qingzhou.pushserver.model.entity.portal.PortalPluginActionLog;
import dev.qingzhou.pushserver.mapper.portal.PortalAppPluginConfigMapper;
import dev.qingzhou.pushserver.model.entity.portal.PortalAppPluginConfig;
import dev.qingzhou.pushserver.plugin.GrpcPluginProxy;
import dev.qingzhou.pushserver.service.PluginManagerService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PluginManagerServiceImpl implements PluginManagerService {

    private final List<PushPlugin> localPlugins;
    private final PluginPacketDispatcher packetDispatcher;
    private final PushSender pushSender;
    private final PortalAppPluginConfigMapper appPluginConfigMapper;
    private final ObjectMapper objectMapper;
    private final PortalPluginActionLogMapper actionLogMapper;
    private final PortalWecomAppMapper wecomAppMapper;

    private final Map<String, GrpcPluginProxy> remotePlugins = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Found {} local plugins", localPlugins.size());
        for (PushPlugin plugin : localPlugins) {
            try {
                plugin.init(pushSender);
                log.info("Initialized local plugin: {}", plugin.getClass().getName());
            } catch (Exception e) {
                log.error("Failed to init local plugin {}", plugin.getClass().getName(), e);
            }
        }
    }

    @Override
    public void registerRemotePlugin(String pluginKey, PluginMeta meta) {
        log.info("Registering remote plugin: {}", pluginKey);
        GrpcPluginProxy proxy = new GrpcPluginProxy(pluginKey, meta, packetDispatcher);
        remotePlugins.put(pluginKey, proxy);
    }

    @Override
    public void unregisterRemotePlugin(String pluginKey) {
        log.info("Unregistering remote plugin: {}", pluginKey);
        remotePlugins.remove(pluginKey);
    }

    @Override
    public List<PushPlugin> getAllPlugins() {
        List<PushPlugin> all = new ArrayList<>(localPlugins);
        all.addAll(remotePlugins.values());
        return all;
    }

    @Override
    public List<PushPlugin> getLocalPlugins() {
        return localPlugins;
    }

    /**
     * Dispatch a user action to the plugins that are both
     * 1) registered (local or active remote), and
     * 2) enabled for the given app with their own configuration.
     */
    @Override
    public void dispatch(ActionContext context) {
        if (context == null) {
            log.warn("Dispatch skipped because context is null");
            return;
        }

        Long appId = parseAppId(context.getAppId());
        Map<String, PortalAppPluginConfig> activeConfigs = Collections.emptyMap();

        if (appId != null) {
            activeConfigs = appPluginConfigMapper.selectList(
                    new LambdaQueryWrapper<PortalAppPluginConfig>()
                            .eq(PortalAppPluginConfig::getAppId, appId)
                            .eq(PortalAppPluginConfig::getStatus, 1)
            ).stream().collect(Collectors.toMap(
                    PortalAppPluginConfig::getPluginKey,
                    Function.identity(),
                    (existing, replacement) -> replacement
            ));
        }

        boolean handled = false;
        for (PushPlugin plugin : getAllPlugins()) {
            PluginMeta meta = plugin.getMeta();
            String pluginKey = meta != null && StringUtils.hasText(meta.getId())
                    ? meta.getId()
                    : plugin.getClass().getSimpleName();

            if (appId != null && !activeConfigs.containsKey(pluginKey)) {
                log.debug("Skip plugin {} for app {} (not configured or disabled)", pluginKey, appId);
                continue;
            }

            Map<String, String> pluginConfig = buildPluginConfig(
                    meta,
                    activeConfigs.get(pluginKey),
                    context.getPluginConfig()
            );

            ActionContext pluginContext = cloneContextWithConfig(context, pluginConfig);

            // 记录接收 + 请求快照（若已存在则更新）
            upsertActionLog(pluginKey, pluginContext, 0, "RECEIVED");

            try {
                if (plugin.supports(pluginContext)) {
                    String name = meta != null && StringUtils.hasText(meta.getName())
                            ? meta.getName()
                            : plugin.getClass().getSimpleName();

                    log.info("Dispatching event {} (app {}) to plugin {}", pluginContext.getEventId(), appId, name);
                    plugin.handle(pluginContext);

                    // 仅对本地插件记录最终状态；远程插件由其 ActionAck 写日志，避免重复
                    if (!(plugin instanceof GrpcPluginProxy)) {
                        upsertActionLog(pluginKey, pluginContext, 2, "SUCCESS");
                    }
                    handled = true;
                }
            } catch (Exception e) {
                log.error("Plugin {} failed to handle event {}", pluginKey, pluginContext.getEventId(), e);
                if (!(plugin instanceof GrpcPluginProxy)) {
                    upsertActionLog(pluginKey, pluginContext, 3, "FAILED: " + e.getMessage());
                }
            }
        }

        if (!handled) {
            log.warn("No plugin handled event {} (app {})", context.getEventId(), appId);
        }
    }

    private ActionContext cloneContextWithConfig(ActionContext source, Map<String, String> pluginConfig) {
        return ActionContext.builder()
                .eventId(source.getEventId())
                .appId(source.getAppId())
                .userId(source.getUserId())
                .userName(source.getUserName())
                .type(source.getType())
                .content(source.getContent())
                .pluginConfig(pluginConfig)
                .build();
    }

    private Map<String, String> buildPluginConfig(PluginMeta meta,
                                                  PortalAppPluginConfig storedConfig,
                                                  Map<String, String> runtimeOverrides) {
        Map<String, String> result = new HashMap<>();

        if (meta != null && meta.getConfigFields() != null) {
            for (ConfigField field : meta.getConfigFields()) {
                if (StringUtils.hasText(field.getName()) && field.getDefaultValue() != null) {
                    result.put(field.getName(), field.getDefaultValue());
                }
            }
        }

        if (storedConfig != null && StringUtils.hasText(storedConfig.getConfigJson())) {
            result.putAll(parseConfigJson(storedConfig.getConfigJson()));
        }

        if (runtimeOverrides != null && !runtimeOverrides.isEmpty()) {
            result.putAll(runtimeOverrides);
        }

        return result;
    }

    private Map<String, String> parseConfigJson(String json) {
        try {
            Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            Map<String, String> parsed = new HashMap<>();
            if (raw != null) {
                raw.forEach((k, v) -> parsed.put(k, v == null ? null : String.valueOf(v)));
            }
            return parsed;
        } catch (Exception e) {
            log.warn("Failed to parse plugin config json: {}", json, e);
            return Collections.emptyMap();
        }
    }

    private Long parseAppId(String appId) {
        if (!StringUtils.hasText(appId)) {
            return null;
        }
        try {
            return Long.parseLong(appId.trim());
        } catch (NumberFormatException ex) {
            log.warn("Invalid appId on ActionContext: {}", appId);
            return null;
        }
    }

    private void upsertActionLog(String pluginKey, ActionContext ctx, int status, String message) {
        String eventId = ctx.getEventId();
        PortalPluginActionLog entity = new PortalPluginActionLog();
        entity.setPluginKey(pluginKey);
        entity.setEventId(eventId);
        entity.setStatus(status);
        entity.setMessage(message);
        entity.setAppId(ctx.getAppId());
        entity.setAppName(resolveAppName(ctx.getAppId()));
        entity.setUserId(ctx.getUserId());
        entity.setType(ctx.getType());
        entity.setContent(ctx.getContent());
        entity.setPluginConfig(writeJson(ctx.getPluginConfig()));
        entity.setCreatedAt(System.currentTimeMillis());
        try {
            int updated = actionLogMapper.update(entity,
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PortalPluginActionLog>()
                            .eq(PortalPluginActionLog::getPluginKey, pluginKey)
                            .eq(PortalPluginActionLog::getEventId, eventId));
            if (updated == 0) {
                actionLogMapper.insert(entity);
            }
        } catch (Exception ex) {
            log.warn("Failed to upsert plugin action log: pluginKey={}, eventId={}, status={}, msg={}", pluginKey, eventId, status, message, ex);
        }
    }

    private String writeJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveAppName(String appId) {
        if (!StringUtils.hasText(appId)) {
            return null;
        }
        try {
            PortalWecomApp app = wecomAppMapper.selectById(Long.parseLong(appId));
            return app != null ? app.getName() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
