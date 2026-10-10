package dev.qingzhou.pushserver.model.vo.portal;

public class PortalSuiteAuthorizationResponse {
    private Long id;
    private String corpId;
    private String corpName;
    private String corpFullName;
    private String corpType;
    private String corpSquareLogoUrl;
    private String agentId;
    private boolean active;
    private Long authorizedAt;
    private Long cancelledAt;
    private Long updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCorpId() { return corpId; }
    public void setCorpId(String corpId) { this.corpId = corpId; }
    public String getCorpName() { return corpName; }
    public void setCorpName(String corpName) { this.corpName = corpName; }
    public String getCorpFullName() { return corpFullName; }
    public void setCorpFullName(String corpFullName) { this.corpFullName = corpFullName; }
    public String getCorpType() { return corpType; }
    public void setCorpType(String corpType) { this.corpType = corpType; }
    public String getCorpSquareLogoUrl() { return corpSquareLogoUrl; }
    public void setCorpSquareLogoUrl(String corpSquareLogoUrl) { this.corpSquareLogoUrl = corpSquareLogoUrl; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Long getAuthorizedAt() { return authorizedAt; }
    public void setAuthorizedAt(Long authorizedAt) { this.authorizedAt = authorizedAt; }
    public Long getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Long cancelledAt) { this.cancelledAt = cancelledAt; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}
