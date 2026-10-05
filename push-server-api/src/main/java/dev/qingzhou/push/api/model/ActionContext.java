package dev.qingzhou.push.api.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ActionContext {
    private String eventId;
    private String appId;
    private String userId;
    private String userName;

    private String type;     // TEXT, CLICK
    private String content;  // 具体内容

    // 运行时配置 (从平台透传而来，不要持久化)
    private Map<String, String> pluginConfig;

    public String getConfig(String key) {
        return pluginConfig != null ? pluginConfig.get(key) : null;
    }
}
