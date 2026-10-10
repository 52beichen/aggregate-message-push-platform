package dev.qingzhou.pushserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.qingzhou.pushserver.exception.PortalException;
import dev.qingzhou.pushserver.exception.PortalStatus;
import dev.qingzhou.pushserver.manager.wecom.WecomApiClient;
import dev.qingzhou.pushserver.manager.wecom.WecomAuthorizationResponse;
import dev.qingzhou.pushserver.manager.wecom.WecomPreAuthCode;
import dev.qingzhou.pushserver.manager.wecom.WecomSuiteToken;
import dev.qingzhou.pushserver.mapper.portal.PortalWecomSuiteAuthCodeMapper;
import dev.qingzhou.pushserver.mapper.portal.PortalWecomSuiteAuthorizationMapper;
import dev.qingzhou.pushserver.mapper.portal.PortalWecomSuiteAppMapper;
import dev.qingzhou.pushserver.model.entity.portal.PortalProxyConfig;
import dev.qingzhou.pushserver.model.entity.portal.PortalWecomSuiteAuthCode;
import dev.qingzhou.pushserver.model.entity.portal.PortalWecomSuiteAuthorization;
import dev.qingzhou.pushserver.model.entity.portal.PortalWecomSuiteApp;
import dev.qingzhou.pushserver.service.PortalProxyConfigService;
import dev.qingzhou.pushserver.service.PortalWecomSuiteAppService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PortalWecomSuiteAppServiceImpl extends ServiceImpl<PortalWecomSuiteAppMapper, PortalWecomSuiteApp>
        implements PortalWecomSuiteAppService {

    private final WecomApiClient wecomApiClient;
    private final PortalProxyConfigService proxyConfigService;
    private final PortalWecomSuiteAuthorizationMapper authorizationMapper;
    private final PortalWecomSuiteAuthCodeMapper authCodeMapper;
    private final ConcurrentHashMap<String, Object> authorizationLocks = new ConcurrentHashMap<>();

    public PortalWecomSuiteAppServiceImpl(WecomApiClient wecomApiClient,
                                          PortalProxyConfigService proxyConfigService,
                                          PortalWecomSuiteAuthorizationMapper authorizationMapper,
                                          PortalWecomSuiteAuthCodeMapper authCodeMapper) {
        this.wecomApiClient = wecomApiClient;
        this.proxyConfigService = proxyConfigService;
        this.authorizationMapper = authorizationMapper;
        this.authCodeMapper = authCodeMapper;
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
        Long id = requireByUser(userId, suiteAppId).getId();
        authorizationMapper.delete(new QueryWrapper<PortalWecomSuiteAuthorization>().eq("suite_app_id", id));
        authCodeMapper.delete(new QueryWrapper<PortalWecomSuiteAuthCode>().eq("suite_app_id", id));
        removeById(id);
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

    @Override
    public String createInstallUrl(Long suiteAppId, String redirectUri, String state) {
        requireText(redirectUri, "redirect_uri");
        requireText(state, "state");
        PortalWecomSuiteApp app = requireSuiteApp(suiteAppId);
        String suiteAccessToken = requireSuiteAccessToken(app);
        PortalProxyConfig proxy = proxyConfigService.getByUserId(app.getUserId());
        WecomPreAuthCode preAuthCode = wecomApiClient.getPreAuthCode(suiteAccessToken, proxy);
        requireText(preAuthCode.getPreAuthCode(), "pre_auth_code");
        return UriComponentsBuilder.fromUriString("https://open.work.weixin.qq.com/3rdapp/install")
                .queryParam("suite_id", app.getSuiteId())
                .queryParam("pre_auth_code", preAuthCode.getPreAuthCode())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    @Override
    public PortalWecomSuiteAuthorization completeAuthorization(Long suiteAppId, String authCode) {
        requireText(authCode, "auth_code");
        String codeHash = hashAuthCode(authCode.trim());
        String lockKey = suiteAppId + ":" + codeHash;
        Object lock = authorizationLocks.computeIfAbsent(lockKey, ignored -> new Object());
        try {
            synchronized (lock) {
                PortalWecomSuiteAuthCode event = findAuthCode(suiteAppId, codeHash);
                if (event != null && "SUCCESS".equals(event.getStatus()) && StringUtils.hasText(event.getCorpId())) {
                    PortalWecomSuiteAuthorization existing = findAuthorization(suiteAppId, event.getCorpId());
                    if (existing != null) return existing;
                }

                long now = System.currentTimeMillis();
                if (event == null) {
                    event = new PortalWecomSuiteAuthCode();
                    event.setSuiteAppId(suiteAppId);
                    event.setAuthCodeHash(codeHash);
                    event.setCreatedAt(now);
                    event.setUpdatedAt(now);
                    event.setStatus("PROCESSING");
                    authCodeMapper.insert(event);
                } else {
                    event.setStatus("PROCESSING");
                    event.setErrorMessage(null);
                    event.setUpdatedAt(now);
                    authCodeMapper.updateById(event);
                }

                try {
                    PortalWecomSuiteApp app = requireSuiteApp(suiteAppId);
                    String suiteAccessToken = requireSuiteAccessToken(app);
                    PortalProxyConfig proxy = proxyConfigService.getByUserId(app.getUserId());
                    WecomAuthorizationResponse response = wecomApiClient.getPermanentCode(
                            suiteAccessToken, authCode.trim(), proxy);
                    if (response.getAuthCorpInfo() == null
                            || !StringUtils.hasText(response.getAuthCorpInfo().getCorpId())) {
                        throw new PortalException(PortalStatus.BAD_GATEWAY,
                                "WeCom authorization response does not contain auth_corp_info.corpid");
                    }
                    requireText(response.getPermanentCode(), "permanent_code");
                    PortalWecomSuiteAuthorization authorization = upsertAuthorization(app, response, now);
                    event.setStatus("SUCCESS");
                    event.setCorpId(authorization.getCorpId());
                    event.setErrorMessage(null);
                    event.setUpdatedAt(System.currentTimeMillis());
                    authCodeMapper.updateById(event);
                    return authorization;
                } catch (RuntimeException ex) {
                    event.setStatus("FAILED");
                    event.setErrorMessage(truncate(ex.getMessage(), 1000));
                    event.setUpdatedAt(System.currentTimeMillis());
                    authCodeMapper.updateById(event);
                    throw ex;
                }
            }
        } finally {
            authorizationLocks.remove(lockKey, lock);
        }
    }

    @Override
    public void refreshAuthorization(Long suiteAppId, String corpId) {
        if (!StringUtils.hasText(corpId)) return;
        PortalWecomSuiteAuthorization authorization = findAuthorization(suiteAppId, corpId.trim());
        if (authorization == null || !StringUtils.hasText(authorization.getPermanentCode())) return;
        PortalWecomSuiteApp app = requireSuiteApp(suiteAppId);
        PortalProxyConfig proxy = proxyConfigService.getByUserId(app.getUserId());
        WecomAuthorizationResponse response = wecomApiClient.getAuthorizationInfo(
                requireSuiteAccessToken(app), corpId.trim(), authorization.getPermanentCode(), proxy);
        applyAuthorizationInfo(authorization, response, System.currentTimeMillis());
        authorization.setStatus(1);
        authorization.setCancelledAt(null);
        authorizationMapper.updateById(authorization);
    }

    @Override
    public void cancelAuthorization(Long suiteAppId, String corpId) {
        if (!StringUtils.hasText(corpId)) return;
        PortalWecomSuiteAuthorization authorization = findAuthorization(suiteAppId, corpId.trim());
        if (authorization == null) return;
        long now = System.currentTimeMillis();
        authorization.setStatus(0);
        authorization.setCorpAccessToken(null);
        authorization.setCorpAccessTokenExpiresAt(null);
        authorization.setCancelledAt(now);
        authorization.setUpdatedAt(now);
        authorizationMapper.updateById(authorization);
    }

    @Override
    public PortalWecomSuiteAuthorization getAuthorization(Long suiteAppId, String corpId) {
        if (suiteAppId == null || !StringUtils.hasText(corpId)) return null;
        return findAuthorization(suiteAppId, corpId.trim());
    }

    @Override
    public List<PortalWecomSuiteAuthorization> listAuthorizations(Long userId, Long suiteAppId) {
        requireByUser(userId, suiteAppId);
        return authorizationMapper.selectList(new QueryWrapper<PortalWecomSuiteAuthorization>()
                .eq("suite_app_id", suiteAppId)
                .orderByDesc("updated_at"));
    }

    private PortalWecomSuiteAuthorization upsertAuthorization(PortalWecomSuiteApp app,
                                                               WecomAuthorizationResponse response,
                                                               long now) {
        String corpId = response.getAuthCorpInfo().getCorpId();
        PortalWecomSuiteAuthorization authorization = findAuthorization(app.getId(), corpId);
        boolean created = authorization == null;
        if (created) {
            authorization = new PortalWecomSuiteAuthorization();
            authorization.setSuiteAppId(app.getId());
            authorization.setCorpId(corpId);
            authorization.setCreatedAt(now);
        }
        authorization.setPermanentCode(response.getPermanentCode());
        authorization.setCorpAccessToken(response.getAccessToken());
        if (StringUtils.hasText(response.getAccessToken())) {
            long expiresIn = response.getExpiresIn() == null ? 7200L : response.getExpiresIn();
            authorization.setCorpAccessTokenExpiresAt(now + Math.max(60L, expiresIn - 60L) * 1000L);
        }
        applyAuthorizationInfo(authorization, response, now);
        authorization.setStatus(1);
        authorization.setAuthorizedAt(now);
        authorization.setCancelledAt(null);
        if (created) authorizationMapper.insert(authorization);
        else authorizationMapper.updateById(authorization);
        return authorization;
    }

    private void applyAuthorizationInfo(PortalWecomSuiteAuthorization authorization,
                                        WecomAuthorizationResponse response,
                                        long now) {
        WecomAuthorizationResponse.AuthCorpInfo corp = response.getAuthCorpInfo();
        if (corp != null) {
            if (StringUtils.hasText(corp.getCorpName())) authorization.setCorpName(corp.getCorpName());
            if (StringUtils.hasText(corp.getCorpFullName())) authorization.setCorpFullName(corp.getCorpFullName());
            if (StringUtils.hasText(corp.getCorpType())) authorization.setCorpType(corp.getCorpType());
            if (StringUtils.hasText(corp.getCorpSquareLogoUrl())) {
                authorization.setCorpSquareLogoUrl(corp.getCorpSquareLogoUrl());
            }
        }
        if (response.getAuthInfo() != null && response.getAuthInfo().getAgent() != null
                && !response.getAuthInfo().getAgent().isEmpty()
                && response.getAuthInfo().getAgent().get(0).getAgentId() != null) {
            authorization.setAgentId(response.getAuthInfo().getAgent().get(0).getAgentId().toString());
        }
        authorization.setUpdatedAt(now);
    }

    private String requireSuiteAccessToken(PortalWecomSuiteApp app) {
        long now = System.currentTimeMillis();
        if (!StringUtils.hasText(app.getSuiteAccessToken())
                || app.getSuiteAccessTokenExpiresAt() == null
                || app.getSuiteAccessTokenExpiresAt() <= now) {
            if (!StringUtils.hasText(app.getSuiteTicket())) {
                throw new PortalException(PortalStatus.CONFLICT,
                        "SuiteTicket has not been received. Check the WeCom data callback configuration first");
            }
            refreshSuiteAccessToken(app.getId());
            app = requireSuiteApp(app.getId());
        }
        if (!StringUtils.hasText(app.getSuiteAccessToken())) {
            throw new PortalException(PortalStatus.BAD_GATEWAY, "Unable to obtain suite_access_token");
        }
        return app.getSuiteAccessToken();
    }

    private PortalWecomSuiteApp requireSuiteApp(Long suiteAppId) {
        PortalWecomSuiteApp app = getById(suiteAppId);
        if (app == null) throw new PortalException(PortalStatus.NOT_FOUND, "Third-party app not found");
        return app;
    }

    private PortalWecomSuiteAuthorization findAuthorization(Long suiteAppId, String corpId) {
        return authorizationMapper.selectOne(new QueryWrapper<PortalWecomSuiteAuthorization>()
                .eq("suite_app_id", suiteAppId)
                .eq("corp_id", corpId), false);
    }

    private PortalWecomSuiteAuthCode findAuthCode(Long suiteAppId, String codeHash) {
        return authCodeMapper.selectOne(new QueryWrapper<PortalWecomSuiteAuthCode>()
                .eq("suite_app_id", suiteAppId)
                .eq("auth_code_hash", codeHash), false);
    }

    private String hashAuthCode(String authCode) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(authCode.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash WeCom auth_code", ex);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
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
