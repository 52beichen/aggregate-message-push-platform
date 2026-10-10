package dev.qingzhou.pushserver.model.entity.portal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("v2_wecom_suite_authorization")
public class PortalWecomSuiteAuthorization {
    @TableId(type = IdType.AUTO) private Long id;
    @TableField("suite_app_id") private Long suiteAppId;
    @TableField("corp_id") private String corpId;
    @TableField("permanent_code") private String permanentCode;
    @TableField("corp_access_token") private String corpAccessToken;
    @TableField("corp_access_token_expires_at") private Long corpAccessTokenExpiresAt;
    @TableField("agent_id") private String agentId;
    @TableField("corp_name") private String corpName;
    @TableField("corp_full_name") private String corpFullName;
    @TableField("corp_type") private String corpType;
    @TableField("corp_square_logo_url") private String corpSquareLogoUrl;
    private Integer status;
    @TableField("authorized_at") private Long authorizedAt;
    @TableField("cancelled_at") private Long cancelledAt;
    @TableField("created_at") private Long createdAt;
    @TableField("updated_at") private Long updatedAt;
}
