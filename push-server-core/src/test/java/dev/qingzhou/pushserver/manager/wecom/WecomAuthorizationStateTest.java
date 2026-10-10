package dev.qingzhou.pushserver.manager.wecom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WecomAuthorizationStateTest {
    @Test
    void createsEnterpriseWecomCompatibleState() {
        String state = WecomAuthorizationState.create();

        assertTrue(WecomAuthorizationState.isValidFormat(state));
        assertTrue(state.matches("[A-Za-z0-9]+"));
        assertTrue(state.length() <= 128);
    }

    @Test
    void createsUniqueStatesAndRejectsUnsupportedCharacters() {
        String first = WecomAuthorizationState.create();
        String second = WecomAuthorizationState.create();

        assertNotEquals(first, second);
        assertFalse(WecomAuthorizationState.isValidFormat("contains.dot"));
        assertFalse(WecomAuthorizationState.isValidFormat("short"));
    }
}
