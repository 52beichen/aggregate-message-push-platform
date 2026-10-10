package dev.qingzhou.pushserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.qingzhou.pushserver.exception.PortalException;
import dev.qingzhou.pushserver.model.wecom.WecomAuthorizationSession;
import dev.qingzhou.pushserver.service.WecomAuthorizationSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class PushServerApplicationTests {

    @Autowired
    private WecomAuthorizationSessionService authorizationSessionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void authorizationStateCanOnlyBeConsumedOnce() {
        WecomAuthorizationSession session = authorizationSessionService.create(987654321L, "u-login", "resume-token");
        try {
            WecomAuthorizationSession loaded = authorizationSessionService.requireValid(session.state(), 987654321L);
            assertEquals("u-login", loaded.clientId());
            assertEquals("resume-token", loaded.resumeState());

            authorizationSessionService.markUsed(session.state(), 987654321L);
            assertThrows(PortalException.class,
                    () -> authorizationSessionService.requireValid(session.state(), 987654321L));
        } finally {
            jdbcTemplate.update("DELETE FROM v2_wecom_authorization_session WHERE state = ?", session.state());
        }
    }

}
