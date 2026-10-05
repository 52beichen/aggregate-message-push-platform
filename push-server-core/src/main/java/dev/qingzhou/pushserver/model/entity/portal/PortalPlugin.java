package dev.qingzhou.pushserver.model.entity.portal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("v2_plugin")
public class PortalPlugin {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private String pluginKey;
    private String name;
    private String description;
    private String token;
    
    /**
     * 1: Enabled, 0: Disabled
     */
    private Integer status;
    
    private Long createdAt;
    private Long updatedAt;
}
