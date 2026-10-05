package dev.qingzhou.pushserver.service;

import dev.qingzhou.pushserver.model.dto.portal.AppPluginConfigSaveRequest;
import dev.qingzhou.pushserver.model.vo.portal.PortalAppPluginConfigVo;
import java.util.List;

public interface PortalAppPluginService {
    
    List<PortalAppPluginConfigVo> listByApp(Long userId, Long appId);
    
    void saveConfig(Long userId, Long appId, AppPluginConfigSaveRequest request);
    
    void deleteConfig(Long userId, Long appId, String pluginKey);
}
