package dev.qingzhou.push.api.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ConfigField {
    private String name;
    private String label;
    private ConfigType type;
    private String defaultValue;
    private boolean required;
    private String description;
    private List<SelectOption> options;
}
