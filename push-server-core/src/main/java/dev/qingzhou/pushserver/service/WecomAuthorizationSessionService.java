package dev.qingzhou.pushserver.service;

import dev.qingzhou.pushserver.exception.PortalException;
import dev.qingzhou.pushserver.exception.PortalStatus;
import dev.qingzhou.pushserver.manager.wecom.WecomAuthorizationState;
import dev.qingzhou.pushserver.model.wecom.WecomAuthorizationSession;
import java.time.Duration;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WecomAuthorizationSessionService {
    private static final long SESSION_TTL_MILLIS = Duration.ofMinutes(30).toMillis();
    private final JdbcTemplate jdbcTemplate;

    public WecomAuthorizationSessionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public WecomAuthorizationSession create(Long suiteAppId, String clientId, String resumeState) {
        long now = System.currentTimeMillis();
        jdbcTemplate.update("DELETE FROM v2_wecom_authorization_session WHERE expires_at < ?", now - 86_400_000L);
        for (int attempt = 0; attempt < 5; attempt++) {
            String state = WecomAuthorizationState.create();
            try {
                jdbcTemplate.update("""
                                INSERT INTO v2_wecom_authorization_session
                                (state, suite_app_id, client_id, resume_state, expires_at, created_at)
                                VALUES (?, ?, ?, ?, ?, ?)
                                """,
                        state, suiteAppId, emptyToNull(clientId), emptyToNull(resumeState),
                        now + SESSION_TTL_MILLIS, now);
                return new WecomAuthorizationSession(state, suiteAppId, emptyToNull(clientId),
                        emptyToNull(resumeState), now + SESSION_TTL_MILLIS, null, now);
            } catch (DuplicateKeyException ignored) {
                // Generate a new cryptographically random state on the extremely unlikely collision.
            }
        }
        throw new IllegalStateException("Unable to create a unique WeCom authorization state");
    }

    public WecomAuthorizationSession requireValid(String state, Long suiteAppId) {
        if (!WecomAuthorizationState.isValidFormat(state)) {
            throw invalidState();
        }
        List<WecomAuthorizationSession> sessions = jdbcTemplate.query("""
                        SELECT state, suite_app_id, client_id, resume_state, expires_at, used_at, created_at
                        FROM v2_wecom_authorization_session
                        WHERE state = ? AND suite_app_id = ?
                        """,
                (rs, rowNum) -> {
                    Object usedAt = rs.getObject("used_at");
                    return new WecomAuthorizationSession(
                            rs.getString("state"),
                            rs.getLong("suite_app_id"),
                            rs.getString("client_id"),
                            rs.getString("resume_state"),
                            rs.getLong("expires_at"),
                            usedAt == null ? null : ((Number) usedAt).longValue(),
                            rs.getLong("created_at"));
                },
                state, suiteAppId);
        if (sessions.isEmpty()) throw invalidState();
        WecomAuthorizationSession session = sessions.get(0);
        if (session.usedAt() != null || session.expiresAt() < System.currentTimeMillis()) throw invalidState();
        return session;
    }

    @Transactional
    public void markUsed(String state, Long suiteAppId) {
        long now = System.currentTimeMillis();
        int updated = jdbcTemplate.update("""
                        UPDATE v2_wecom_authorization_session
                        SET used_at = ?
                        WHERE state = ? AND suite_app_id = ? AND used_at IS NULL AND expires_at >= ?
                        """,
                now, state, suiteAppId, now);
        if (updated != 1) throw invalidState();
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private PortalException invalidState() {
        return new PortalException(PortalStatus.BAD_REQUEST, "Invalid, expired, or already used WeCom authorization state");
    }
}
