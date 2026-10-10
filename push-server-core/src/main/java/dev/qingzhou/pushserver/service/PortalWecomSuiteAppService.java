package dev.qingzhou.pushserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.qingzhou.pushserver.model.entity.portal.PortalWecomSuiteAuthorization;
import dev.qingzhou.pushserver.model.entity.portal.PortalWecomSuiteApp;
import java.util.List;

public interface PortalWecomSuiteAppService extends IService<PortalWecomSuiteApp> {
    PortalWecomSuiteApp create(Long userId, String suiteId, String suiteSecret, String token, String encodingAesKey);
    List<PortalWecomSuiteApp> listByUser(Long userId);
    PortalWecomSuiteApp requireByUser(Long userId, Long suiteAppId);
    PortalWecomSuiteApp update(Long userId, Long suiteAppId, String suiteSecret, String token, String encodingAesKey);
    void delete(Long userId, Long suiteAppId);
    void saveSuiteTicket(Long suiteAppId, String suiteId, String suiteTicket);
    void refreshSuiteAccessToken(Long suiteAppId);
    String createInstallUrl(Long suiteAppId, String redirectUri, String state);
    PortalWecomSuiteAuthorization completeAuthorization(Long suiteAppId, String authCode);
    void refreshAuthorization(Long suiteAppId, String corpId);
    void cancelAuthorization(Long suiteAppId, String corpId);
    List<PortalWecomSuiteAuthorization> listAuthorizations(Long userId, Long suiteAppId);
}
