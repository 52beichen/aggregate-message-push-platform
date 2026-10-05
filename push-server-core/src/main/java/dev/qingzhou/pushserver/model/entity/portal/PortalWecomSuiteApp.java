package dev.qingzhou.pushserver.model.entity.portal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("v2_wecom_suite_app")
public class PortalWecomSuiteApp {
    @TableId(type = IdType.AUTO) private Long id;
    @TableField("user_id") private Long userId;
    @TableField("suite_id") private String suiteId;
    @TableField("suite_secret") private String suiteSecret;
    private String token;
    @TableField("encoding_aes_key") private String encodingAesKey;
    @TableField("suite_ticket") private String suiteTicket;
    @TableField("suite_access_token") private String suiteAccessToken;
    @TableField("suite_access_token_expires_at") private Long suiteAccessTokenExpiresAt;
    @TableField("created_at") private Long createdAt;
    @TableField("updated_at") private Long updatedAt;
}
