package dev.qingzhou.pushserver.manager.wecom;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class WecomAuthorizationResponse extends WecomResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    @JsonProperty("permanent_code")
    private String permanentCode;

    @JsonProperty("auth_corp_info")
    private AuthCorpInfo authCorpInfo;

    @JsonProperty("auth_info")
    private AuthInfo authInfo;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public Integer getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Integer expiresIn) { this.expiresIn = expiresIn; }
    public String getPermanentCode() { return permanentCode; }
    public void setPermanentCode(String permanentCode) { this.permanentCode = permanentCode; }
    public AuthCorpInfo getAuthCorpInfo() { return authCorpInfo; }
    public void setAuthCorpInfo(AuthCorpInfo authCorpInfo) { this.authCorpInfo = authCorpInfo; }
    public AuthInfo getAuthInfo() { return authInfo; }
    public void setAuthInfo(AuthInfo authInfo) { this.authInfo = authInfo; }

    public static class AuthCorpInfo {
        @JsonProperty("corpid")
        private String corpId;
        @JsonProperty("corp_name")
        private String corpName;
        @JsonProperty("corp_full_name")
        private String corpFullName;
        @JsonProperty("corp_type")
        private String corpType;
        @JsonProperty("corp_square_logo_url")
        private String corpSquareLogoUrl;

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
    }

    public static class AuthInfo {
        private List<Agent> agent;

        public List<Agent> getAgent() { return agent; }
        public void setAgent(List<Agent> agent) { this.agent = agent; }
    }

    public static class Agent {
        @JsonProperty("agentid")
        private Long agentId;

        public Long getAgentId() { return agentId; }
        public void setAgentId(Long agentId) { this.agentId = agentId; }
    }
}
