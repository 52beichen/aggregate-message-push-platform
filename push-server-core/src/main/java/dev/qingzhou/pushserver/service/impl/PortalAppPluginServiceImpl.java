package dev.qingzhou.pushserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.qingzhou.push.api.model.PluginMeta;
import dev.qingzhou.push.api.spi.PushPlugin;
import dev.qingzhou.pushserver.mapper.portal.PortalAppPluginConfigMapper;
import dev.qingzhou.pushserver.model.dto.portal.AppPluginConfigSaveRequest;
import dev.qingzhou.pushserver.model.entity.portal.PortalAppPluginConfig;
import dev.qingzhou.pushserver.model.vo.portal.PortalAppPluginConfigVo;
import dev.qingzhou.pushserver.service.PluginManagerService;
import dev.qingzhou.pushserver.service.PortalAppPluginService;
import dev.qingzhou.pushserver.service.PortalWecomAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalAppPluginServiceImpl implements PortalAppPluginService {

    private final PortalWecomAppService appService;
    private final PluginManagerService pluginManagerService;
    private final PortalAppPluginConfigMapper configMapper;

    @Override
    public List<PortalAppPluginConfigVo> listByApp(Long userId, Long appId) {
        // Ensure user owns the app
        appService.requireByUser(userId, appId);

        // Get all active plugins from memory (Local + Remote)
        List<PushPlugin> allPlugins = pluginManagerService.getAllPlugins();

        // Get stored configs for this app
        List<PortalAppPluginConfig> storedConfigs = configMapper.selectList(
                new LambdaQueryWrapper<PortalAppPluginConfig>()
                        .eq(PortalAppPluginConfig::getAppId, appId)
        );
        Map<String, PortalAppPluginConfig> configMap = storedConfigs.stream()
                .collect(Collectors.toMap(PortalAppPluginConfig::getPluginKey, Function.identity()));

        List<PortalAppPluginConfigVo> result = new ArrayList<>();
        for (PushPlugin plugin : allPlugins) {
            PluginMeta meta = plugin.getMeta();
            if (meta == null) {
                // Skip plugins without meta (shouldn't happen usually for valid plugins)
                continue;
            }
            // Use ID from meta as the key. If ID is missing, fallback to class name or specific logic?
            // Usually meta.id is the unique key. 
            // WAIT: In PluginManagerService, registerRemotePlugin uses a pluginKey.
            // But PushPlugin interface has getMeta().
            // For remote plugins, the proxy should return the meta provided during registration.
            
            String pluginKey = meta.getId(); 
            // Ideally pluginKey should be consistent. Assuming meta.id is the key.

            PortalAppPluginConfig stored = configMap.get(pluginKey);

            PortalAppPluginConfigVo vo = PortalAppPluginConfigVo.builder()
                    .pluginKey(pluginKey)
                    .name(meta.getName())
                    .description(meta.getDescription())
                    .meta(meta)
                    .configJson(stored != null ? stored.getConfigJson() : null)
                    .status(stored != null ? stored.getStatus() : 0) // Default to disabled if not configured
                    .updatedAt(stored != null ? stored.getUpdatedAt() : null)
                    .build();
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public void saveConfig(Long userId, Long appId, AppPluginConfigSaveRequest request) {
        appService.requireByUser(userId, appId);

        String pluginKey = request.getPluginKey();
        
        PortalAppPluginConfig existing = configMapper.selectOne(
                new LambdaQueryWrapper<PortalAppPluginConfig>()
                        .eq(PortalAppPluginConfig::getAppId, appId)
                        .eq(PortalAppPluginConfig::getPluginKey, pluginKey)
        );

        long now = System.currentTimeMillis();

        if (existing == null) {
            existing = new PortalAppPluginConfig();
            existing.setAppId(appId);
            existing.setPluginKey(pluginKey);
            existing.setConfigJson(request.getConfigJson());
            existing.setStatus(request.getStatus() != null ? request.getStatus() : 1);
            existing.setCreatedAt(now);
            existing.setUpdatedAt(now);
            configMapper.insert(existing);
        } else {
            if (request.getConfigJson() != null) {
                existing.setConfigJson(request.getConfigJson());
            }
            if (request.getStatus() != null) {
                existing.setStatus(request.getStatus());
            }
            existing.setUpdatedAt(now);
            configMapper.updateById(existing);
        }
    }

    @Override
    @Transactional
    public void deleteConfig(Long userId, Long appId, String pluginKey) {
        appService.requireByUser(userId, appId);
        
        configMapper.delete(
                new LambdaQueryWrapper<PortalAppPluginConfig>()
                        .eq(PortalAppPluginConfig::getAppId, appId)
                        .eq(PortalAppPluginConfig::getPluginKey, pluginKey)
        );
    }
}
