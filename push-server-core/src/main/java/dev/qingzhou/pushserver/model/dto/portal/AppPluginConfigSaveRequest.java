package dev.qingzhou.pushserver.model.dto.portal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppPluginConfigSaveRequest {
    
    @NotNull(message = "Plugin key is required")
    private String pluginKey;
    
    private String configJson;
    
    private Integer status;
}
