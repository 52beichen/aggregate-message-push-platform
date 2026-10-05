package dev.qingzhou.pushserver.model.entity.portal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("v2_plugin_heartbeat_log")
public class PortalPluginHeartbeatLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String pluginKey;
    private Integer currentInflight;
    private Integer uptimeSeconds;
    private Long createdAt;
}
