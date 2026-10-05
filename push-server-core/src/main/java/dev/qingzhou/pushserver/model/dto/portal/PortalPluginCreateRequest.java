package dev.qingzhou.pushserver.model.dto.portal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PortalPluginCreateRequest {
    
    @NotBlank(message = "Plugin Key cannot be empty")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Plugin Key can only contain letters, numbers, underscores and hyphens")
    private String pluginKey;

    @NotBlank(message = "Name cannot be empty")
    private String name;

    private String description;
}
