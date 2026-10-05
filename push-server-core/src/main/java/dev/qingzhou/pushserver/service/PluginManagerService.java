package dev.qingzhou.pushserver.service;

import dev.qingzhou.push.api.model.ActionContext;
import dev.qingzhou.push.api.model.PluginMeta;
import dev.qingzhou.push.api.spi.PushPlugin;
import java.util.List;

public interface PluginManagerService {
    
    void registerRemotePlugin(String pluginKey, PluginMeta meta);
    
    void unregisterRemotePlugin(String pluginKey);
    
    List<PushPlugin> getAllPlugins();

    List<PushPlugin> getLocalPlugins();
    
    void dispatch(ActionContext context);
}
