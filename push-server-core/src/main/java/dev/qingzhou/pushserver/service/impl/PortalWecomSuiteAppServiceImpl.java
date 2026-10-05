package dev.qingzhou.pushserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.qingzhou.pushserver.exception.PortalException;
import dev.qingzhou.pushserver.exception.PortalStatus;
import dev.qingzhou.pushserver.manager.wecom.WecomApiClient;
import dev.qingzhou.pushserver.manager.wecom.WecomSuiteToken;
import dev.qingzhou.pushserver.mapper.portal.PortalWecomSuiteAppMapper;
import dev.qingzhou.pushserver.model.entity.portal.PortalProxyConfig;
import dev.qingzhou.pushserver.model.entity.portal.PortalWecomSuiteApp;
import dev.qingzhou.pushserver.service.PortalProxyConfigService;
import dev.qingzhou.pushserver.service.PortalWecomSuiteAppService;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PortalWecomSuiteAppServiceImpl extends ServiceImpl<PortalWecomSuiteAppMapper, PortalWecomSuiteApp>
        implements PortalWecomSuiteAppService {

    private final WecomApiClient wecomApiClient;
    private final PortalProxyConfigService proxyConfigService;

    public PortalWecomSuiteAppServiceImpl(WecomApiClient wecomApiClient, PortalProxyConfigService proxyConfigService) {
        this.wecomApiClient = wecomApiClient;
        this.proxyConfigService = proxyConfigService;
    }

    @Override
    public PortalWecomSuiteApp create(Long userId, String suiteId, String suiteSecret, String token, String aesKey) {
        requireText(suiteId, "SuiteID");
        requireText(suiteSecret, "SuiteSecret");
        requireText(token, "Token");
        validateAesKey(aesKey);

        QueryWrapper<PortalWecomSuiteApp> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("suite_id", suiteId.trim());
        if (count(query) > 0) {
            throw new PortalException(PortalStatus.CONFLICT, "SuiteID already exists");
        }

        long now = System.currentTimeMillis();
        PortalWecomSuiteApp app = new PortalWecomSuiteApp();
        app.setUserId(userId);
        app.setSuiteId(suiteId.trim());
        app.setSuiteSecret(suiteSecret.trim());
        app.setToken(token.trim());
        app.setEncodingAesKey(aesKey.trim());
        app.setCreatedAt(now);
        app.setUpdatedAt(now);
        save(app);
        return app;
    }

    @Override
    public List<PortalWecomSuiteApp> listByUser(Long userId) {
        QueryWrapper<PortalWecomSuiteApp> query = new QueryWrapper<>();
        query.eq("user_id", userId).orderByDesc("created_at");
        return list(query);
    }

    @Override
    public PortalWecomSuiteApp requireByUser(Long userId, Long suiteAppId) {
        PortalWecomSuiteApp app = getById(suiteAppId);
        if (app == null || !userId.equals(app.getUserId())) {
            throw new PortalException(PortalStatus.NOT_FOUND, "Third-party app not found");
        }
        return app;
    }

    @Override
    public PortalWecomSuiteApp update(Long userId, Long suiteAppId, String suiteSecret, String token, String aesKey) {
        PortalWecomSuiteApp app = requireByUser(userId, suiteAppId);
        boolean changed = false;
        if (StringUtils.hasText(suiteSecret) && !suiteSecret.equals(app.getSuiteSecret())) {
            app.setSuiteSecret(suiteSecret.trim());
            app.setSuiteTicket(null);
            app.setSuiteAccessToken(null);
            app.setSuiteAccessTokenExpiresAt(null);
            changed = true;
        }
        if (token != null && !token.equals(app.getToken())) {
            requireText(token, "Token");
            app.setToken(token.trim());
            changed = true;
        }
        if (aesKey != null && !aesKey.equals(app.getEncodingAesKey())) {
            validateAesKey(aesKey);
            app.setEncodingAesKey(aesKey.trim());
            changed = true;
        }
        if (changed) {
            app.setUpdatedAt(System.currentTimeMillis());
            updateById(app);
        }
        return app;
    }

    @Override
    public void delete(Long userId, Long suiteAppId) {
        removeById(requireByUser(userId, suiteAppId).getId());
    }

    @Override
    public void saveSuiteTicket(Long suiteAppId, String suiteId, String suiteTicket) {
        PortalWecomSuiteApp app = getById(suiteAppId);
        if (app == null) throw new PortalException(PortalStatus.NOT_FOUND, "Third-party app not found");
        if (StringUtils.hasText(suiteId) && !app.getSuiteId().equals(suiteId)) {
            throw new PortalException(PortalStatus.BAD_REQUEST, "SuiteID does not match callback");
        }
        requireText(suiteTicket, "suite_ticket");
        app.setSuiteTicket(suiteTicket.trim());
        app.setUpdatedAt(System.currentTimeMillis());
        updateById(app);
    }

    @Override
    public void refreshSuiteAccessToken(Long suiteAppId) {
        PortalWecomSuiteApp app = getById(suiteAppId);
        if (app == null || !StringUtils.hasText(app.getSuiteTicket())) return;
        PortalProxyConfig proxy = proxyConfigService.getByUserId(app.getUserId());
        WecomSuiteToken token = wecomApiClient.getSuiteToken(app.getSuiteId(), app.getSuiteSecret(),
                app.getSuiteTicket(), proxy);
        app.setSuiteAccessToken(token.getSuiteAccessToken());
        long expiresIn = token.getExpiresIn() == null ? 7200L : token.getExpiresIn();
        app.setSuiteAccessTokenExpiresAt(System.currentTimeMillis() + Math.max(60L, expiresIn - 60L) * 1000L);
        app.setUpdatedAt(System.currentTimeMillis());
        updateById(app);
    }

    private void validateAesKey(String key) {
        if (!StringUtils.hasText(key) || key.trim().length() != 43) {
            throw new PortalException(PortalStatus.BAD_REQUEST, "EncodingAESKey must be 43 characters");
        }
        try {
            Base64.getDecoder().decode(key.trim() + "=");
        } catch (IllegalArgumentException ex) {
            throw new PortalException(PortalStatus.BAD_REQUEST, "Invalid EncodingAESKey");
        }
    }

    private void requireText(String value, String field) {
        if (!StringUtils.hasText(value)) throw new PortalException(PortalStatus.BAD_REQUEST, field + " is required");
    }
}
