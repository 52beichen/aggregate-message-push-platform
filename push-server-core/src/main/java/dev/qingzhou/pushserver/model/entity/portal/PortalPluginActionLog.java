package dev.qingzhou.pushserver.model.entity.portal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("v2_plugin_action_log")
public class PortalPluginActionLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String pluginKey;
    private String eventId;
    private Integer status;
    private String message;
    private String appId;
    private String appName;
    private String userId;
    private String type;
    private String content;
    private String pluginConfig;
    private Long createdAt;
}
