package dev.qingzhou.pushserver.model.wecom;

public record WecomAuthorizationSession(
        String state,
        Long suiteAppId,
        String clientId,
        String resumeState,
        Long expiresAt,
        Long usedAt,
        Long createdAt) {
}
