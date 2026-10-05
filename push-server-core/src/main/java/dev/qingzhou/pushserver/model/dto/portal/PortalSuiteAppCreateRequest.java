package dev.qingzhou.pushserver.model.dto.portal;

import jakarta.validation.constraints.NotBlank;

public class PortalSuiteAppCreateRequest {
    @NotBlank private String suiteId;
    @NotBlank private String suiteSecret;
    @NotBlank private String token;
    @NotBlank private String encodingAesKey;

    public String getSuiteId() { return suiteId; }
    public void setSuiteId(String suiteId) { this.suiteId = suiteId; }
    public String getSuiteSecret() { return suiteSecret; }
    public void setSuiteSecret(String suiteSecret) { this.suiteSecret = suiteSecret; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getEncodingAesKey() { return encodingAesKey; }
    public void setEncodingAesKey(String encodingAesKey) { this.encodingAesKey = encodingAesKey; }
}
