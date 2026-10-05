package dev.qingzhou.pushserver.model.entity.portal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("v2_app_plugin_config")
public class PortalAppPluginConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("app_id")
    private Long appId;

    @TableField("plugin_key")
    private String pluginKey;

    /**
     * JSON string of configuration
     */
    @TableField("config_json")
    private String configJson;

    /**
     * 1: Enabled, 0: Disabled
     */
    private Integer status;

    @TableField("created_at")
    private Long createdAt;

    @TableField("updated_at")
    private Long updatedAt;
}
