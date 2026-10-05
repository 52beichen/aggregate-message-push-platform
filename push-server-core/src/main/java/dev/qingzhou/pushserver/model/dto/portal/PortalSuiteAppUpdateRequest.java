package dev.qingzhou.pushserver.model.dto.portal;

public class PortalSuiteAppUpdateRequest {
    private String suiteSecret;
    private String token;
    private String encodingAesKey;

    public String getSuiteSecret() { return suiteSecret; }
    public void setSuiteSecret(String suiteSecret) { this.suiteSecret = suiteSecret; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getEncodingAesKey() { return encodingAesKey; }
    public void setEncodingAesKey(String encodingAesKey) { this.encodingAesKey = encodingAesKey; }
}
