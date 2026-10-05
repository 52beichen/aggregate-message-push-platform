package dev.qingzhou.pushserver.service;

import dev.qingzhou.pushserver.model.dto.portal.PortalPluginCreateRequest;
import dev.qingzhou.pushserver.model.vo.portal.PortalPluginVo;
import java.util.List;

public interface PortalPluginService {
    
    // 1. 注册插件，返回包含 Token 的明文
    String createPlugin(PortalPluginCreateRequest request);

    // 2. 重置 Token，返回新 Token
    String resetToken(Integer id);

    // 3. 切换状态 (启用/禁用)
    void switchStatus(Integer id, Integer status);

    // 4. 插件列表
    List<PortalPluginVo> listPlugins();

    // 5. 删除插件
    void deletePlugin(Integer id);
}
