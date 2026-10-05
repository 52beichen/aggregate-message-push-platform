package dev.qingzhou.pushserver.model.vo.portal;

public class PortalSuiteAppResponse {
    private Long id;
    private String suiteId;
    private boolean hasSuiteTicket;
    private boolean hasSuiteAccessToken;
    private String callbackUrl;
    private Long createdAt;
    private Long updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSuiteId() { return suiteId; }
    public void setSuiteId(String suiteId) { this.suiteId = suiteId; }
    public boolean isHasSuiteTicket() { return hasSuiteTicket; }
    public void setHasSuiteTicket(boolean hasSuiteTicket) { this.hasSuiteTicket = hasSuiteTicket; }
    public boolean isHasSuiteAccessToken() { return hasSuiteAccessToken; }
    public void setHasSuiteAccessToken(boolean hasSuiteAccessToken) { this.hasSuiteAccessToken = hasSuiteAccessToken; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}
