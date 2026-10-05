package dev.qingzhou.pushserver.model.vo.portal;

import dev.qingzhou.push.api.model.PluginMeta;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PortalAppPluginConfigVo {
    // 插件的基本信息
    private String pluginKey;
    private String name;
    private String description;
    
    // 插件定义的配置字段
    private PluginMeta meta;
    
    // 当前应用配置的值
    private String configJson;
    
    // 在该应用中是否启用
    private Integer status; // 1: Enabled, 0: Disabled
    
    private Long updatedAt;
}
