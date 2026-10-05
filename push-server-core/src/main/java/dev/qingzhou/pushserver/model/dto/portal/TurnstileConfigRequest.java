package dev.qingzhou.pushserver.model.dto.portal;

import lombok.Data;

@Data
public class TurnstileConfigRequest {
    private boolean enabled;
    private String siteKey;
    private String secretKey;
}
