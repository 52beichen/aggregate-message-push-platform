package dev.qingzhou.pushserver.model.vo.portal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PortalPluginVo {
    private Integer id;
    private String pluginKey;
    private String name;
    private String description;
    private Integer status; // 1: Enabled, 0: Disabled
    private Long createdAt;
    // 注意：Token 通常不在此处返回，或者只返回脱敏后的版本
    private Boolean isConnected; // 预留字段：当前是否在线
    private Boolean isBuiltin;
}
