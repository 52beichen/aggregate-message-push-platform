package dev.qingzhou.push.api.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PluginMeta {
    private String id;
    private String version;
    private String name;
    private String description;
    private int maxConcurrency;
    private List<ConfigField> configFields;
}
