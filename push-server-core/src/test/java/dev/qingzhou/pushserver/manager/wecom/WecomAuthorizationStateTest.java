package dev.qingzhou.pushserver.manager.wecom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WecomAuthorizationStateTest {
    @Test
    void createsAndVerifiesStateForTheExpectedSuiteApp() {
        String state = WecomAuthorizationState.create(12L, "suite-secret");

        assertTrue(WecomAuthorizationState.verify(state, 12L, "suite-secret"));
        assertFalse(WecomAuthorizationState.verify(state, 13L, "suite-secret"));
        assertFalse(WecomAuthorizationState.verify(state, 12L, "different-secret"));
    }

    @Test
    void rejectsTamperedState() {
        String state = WecomAuthorizationState.create(12L, "suite-secret");

        assertFalse(WecomAuthorizationState.verify(state + "x", 12L, "suite-secret"));
        assertFalse(WecomAuthorizationState.verify("invalid", 12L, "suite-secret"));
    }
}
