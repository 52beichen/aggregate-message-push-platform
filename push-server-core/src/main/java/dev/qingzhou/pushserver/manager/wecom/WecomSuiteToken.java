package dev.qingzhou.pushserver.manager.wecom;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WecomSuiteToken extends WecomResponse {
    @JsonProperty("suite_access_token")
    private String suiteAccessToken;
    @JsonProperty("expires_in")
    private Integer expiresIn;

    public String getSuiteAccessToken() { return suiteAccessToken; }
    public void setSuiteAccessToken(String suiteAccessToken) { this.suiteAccessToken = suiteAccessToken; }
    public Integer getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Integer expiresIn) { this.expiresIn = expiresIn; }

    @Override
    public boolean isSuccess() {
        return getErrcode() == null || super.isSuccess();
    }
}
