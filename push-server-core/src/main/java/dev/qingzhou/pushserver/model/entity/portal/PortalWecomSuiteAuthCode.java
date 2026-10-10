package dev.qingzhou.pushserver.model.entity.portal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("v2_wecom_suite_auth_code")
public class PortalWecomSuiteAuthCode {
    @TableId(type = IdType.AUTO) private Long id;
    @TableField("suite_app_id") private Long suiteAppId;
    @TableField("auth_code_hash") private String authCodeHash;
    private String status;
    @TableField("corp_id") private String corpId;
    @TableField("error_message") private String errorMessage;
    @TableField("created_at") private Long createdAt;
    @TableField("updated_at") private Long updatedAt;
}
