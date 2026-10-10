package dev.qingzhou.pushserver.model.vo.portal;

public class WecomInternalAuthorizationResponse {
    private String corpId;
    private boolean active;
    private String agentId;
    private Long authorizedAt;
    private Long updatedAt;

    public String getCorpId() { return corpId; }
    public void setCorpId(String corpId) { this.corpId = corpId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public Long getAuthorizedAt() { return authorizedAt; }
    public void setAuthorizedAt(Long authorizedAt) { this.authorizedAt = authorizedAt; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}
