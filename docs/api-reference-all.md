# 聚合消息推送平台接口清单

本文档根据当前仓库源码整理，覆盖 HTTP 接口和 gRPC 插件接口。对应代码版本：`dc517d0`。

## 1. 基本约定

- HTTP 默认端口：`8000`
- gRPC 默认端口：`9090`
- 公网示例地址：`https://juhe.beichenwl.cn`
- 所有 `@RestController` 接口统一增加 `/api` 前缀。
- 除特别说明外，请求和响应均使用 `application/json; charset=UTF-8`。
- 时间字段如 `createdAt`、`updatedAt`、`authorizedAt` 均为 Unix 毫秒时间戳。
- 静态资源、Vue SPA 页面路由和域名验证文件不属于 API，不在业务接口表中列出。

### 1.1 标准响应

大部分 V2 管理接口使用以下结构：

```json
{
  "success": true,
  "message": "成功",
  "data": {}
}
```

失败时通常为：

```json
{
  "success": false,
  "message": "错误原因",
  "data": null
}
```

分页数据结构：

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "pageSize": 20
}
```

### 1.2 鉴权类型

| 标识 | 说明 |
| --- | --- |
| 公开 | 不需要登录会话 |
| 会话 | 先调用 `/api/login` 登录，并携带服务端返回的会话 Cookie |
| CSRF | 会话类写接口还需先请求 `/api/v2/auth/csrf`，随后发送 `X-XSRF-TOKEN` 请求头 |
| 应用密钥 | 请求头 `X-API-Key: <应用 API Key>` |
| V1 密钥 | 请求头 `X-API-Key: <push.auth.key>` |
| 内部密钥 | 请求头 `X-Internal-API-Key: <内部密钥>` |
| 企微签名 | 由企业微信提供 `msg_signature`、`timestamp`、`nonce` 等参数并进行 AES 验签 |
| 插件令牌 | gRPC Metadata：`Authorization: Bearer <插件令牌>` |

以下请求不校验 CSRF：`/api/login`、`/api/v1/**`、`/api/v2/auth/register`、`/api/v2/openapi/**`、`/api/v2/wecom/**`。其他会话写接口需要 CSRF。

## 2. 登录、注册和初始化

| 方法 | 路径 | 鉴权 | 请求 | 响应/用途 |
| --- | --- | --- | --- | --- |
| POST | `/api/login` | 公开 | JSON：`account`、`password`、`captcha` | 登录并创建会话；返回 `code`、`msg`、`username` |
| GET | `/api/captcha` | 公开 | 无 | 返回 `{enabled, siteKey}`，用于获取 Turnstile 配置 |
| GET | `/api/public/init-status` | 公开 | 无 | 返回 `{initialized}` |
| POST | `/api/public/init` | 公开、CSRF | 初始化请求体 | 首次创建管理员并保存 Turnstile 配置，仅未初始化时可调用 |
| POST | `/api/v2/auth/register` | 公开 | JSON：`account`、`password` | 自助注册账号 |
| GET | `/api/v2/auth/csrf` | 公开 | 无 | 返回 CSRF Token，并建立相应 Cookie |
| POST | `/api/v2/auth/logout` | 会话、CSRF | 无 | 使当前会话失效 |
| POST | `/logout` | 会话、CSRF | 无 | Spring Security 提供的兼容退出接口 |

登录请求示例：

```json
{
  "account": "admin",
  "password": "password",
  "captcha": "Turnstile 响应令牌；未启用 Turnstile 时可为空"
}
```

初始化请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | string | 是 | 初始管理员账号 |
| `password` | string | 是 | 初始管理员密码 |
| `turnstileEnabled` | boolean | 否 | 是否启用 Turnstile，默认 `false` |
| `turnstileSiteKey` | string | 否 | Turnstile Site Key |
| `turnstileSecretKey` | string | 否 | Turnstile Secret Key |

## 3. 当前账号

| 方法 | 路径 | 鉴权 | 请求 | 响应/用途 |
| --- | --- | --- | --- | --- |
| GET | `/api/v2/me` | 会话 | 无 | 当前用户：`id`、`account`、`createdAt`、`updatedAt` |
| PUT | `/api/v2/me/password` | 会话、CSRF | `oldPassword`、`newPassword` | 修改当前账号密码 |

## 4. 系统配置

| 方法 | 路径 | 鉴权 | 请求 | 响应/用途 |
| --- | --- | --- | --- | --- |
| GET | `/api/system/version` | 公开 | 无 | 返回当前程序版本 `{version}` |
| GET | `/api/system/version/ignore` | 会话 | 无 | 返回被忽略的版本号 |
| POST | `/api/system/version/ignore` | 会话、CSRF | JSON：`version` | 保存忽略版本号 |
| GET | `/api/system/turnstile` | 会话 | 无 | 获取 Turnstile 配置；已有 Secret 以 `******` 返回 |
| PUT | `/api/system/turnstile` | 会话、CSRF | `enabled`、`siteKey`、`secretKey` | 更新 Turnstile 配置；`secretKey=******` 表示保留原值 |

## 5. 仪表盘

| 方法 | 路径 | 鉴权 | 查询参数 | 响应/用途 |
| --- | --- | --- | --- | --- |
| GET | `/api/dashboard/stats` | 会话 | 无 | `todayTotal`、`successRate`、`activeApps`、`lastErrorTime` |
| GET | `/api/dashboard/charts` | 会话 | 无 | 趋势 `trend` 和消息类型分布 `distribution` |
| GET | `/api/dashboard/recent-logs` | 会话 | `limit`，默认 10，范围 1-100 | 最近推送记录 |

## 6. 企业配置

| 方法 | 路径 | 鉴权 | 请求 | 响应/用途 |
| --- | --- | --- | --- | --- |
| GET | `/api/v2/corp` | 会话 | 无 | 获取当前用户的企业微信 `corpId` |
| PUT | `/api/v2/corp` | 会话、CSRF | JSON：`corpId`，必填 | 新增或更新企业 ID |

## 7. 网络代理配置

| 方法 | 路径 | 鉴权 | 请求 | 响应/用途 |
| --- | --- | --- | --- | --- |
| GET | `/api/v2/proxy` | 会话 | 无 | 获取代理配置；未配置时 `data=null` |
| PUT | `/api/v2/proxy` | 会话、CSRF | 代理配置请求体 | 新增或更新代理配置 |
| DELETE | `/api/v2/proxy` | 会话、CSRF | 无 | 删除当前用户的代理配置 |

代理配置请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `host` | string | 是 | 代理服务器地址 |
| `port` | integer | 是 | 端口，范围 1-65535 |
| `username` | string | 否 | 代理账号 |
| `password` | string | 否 | 代理密码 |
| `type` | string | 否 | 代理类型，默认 `HTTP` |
| `exitIp` | string | 否 | 出口 IP 备注 |
| `active` | boolean | 否 | 是否启用，默认 `true` |

## 8. 自建企业微信应用

| 方法 | 路径 | 鉴权 | 请求 | 响应/用途 |
| --- | --- | --- | --- | --- |
| GET | `/api/v2/apps` | 会话 | 无 | 查询当前用户的自建应用列表 |
| POST | `/api/v2/apps` | 会话、CSRF | `agentId`、`secret`，均必填 | 新增自建应用 |
| PUT | `/api/v2/apps/{appId}` | 会话、CSRF | `secret`、`token`、`encodingAesKey`，均可选 | 更新应用凭据和回调配置 |
| DELETE | `/api/v2/apps/{appId}` | 会话、CSRF | 无 | 删除应用及其 API Key |
| POST | `/api/v2/apps/{appId}/sync` | 会话、CSRF | 无 | 从企业微信同步应用名称、头像和描述 |
| GET | `/api/v2/apps/{appId}/api-key` | 会话 | 无 | 查询应用 API Key 和每分钟限流值 |
| POST | `/api/v2/apps/{appId}/api-key` | 会话、CSRF | 无 | 创建 API Key；已有 Key 时会轮换 |
| POST | `/api/v2/apps/{appId}/api-key/reset` | 会话、CSRF | 无 | 重新生成 API Key |
| PUT | `/api/v2/apps/{appId}/api-key` | 会话、CSRF | `rateLimitPerMinute`，最小为 0 | 更新调用限流；`0` 表示不启用每分钟限制 |
| DELETE | `/api/v2/apps/{appId}/api-key` | 会话、CSRF | 无 | 删除 API Key |

应用响应字段：`id`、`agentId`、`name`、`avatarUrl`、`description`、`createdAt`、`updatedAt`。

API Key 响应字段：`appId`、`hasKey`、`apiKey`、`rateLimitPerMinute`、`createdAt`、`updatedAt`。

## 9. 企业微信第三方应用

| 方法 | 路径 | 鉴权 | 请求 | 响应/用途 |
| --- | --- | --- | --- | --- |
| GET | `/api/v2/apps/third-party` | 会话 | 无 | 查询第三方应用列表及回调状态 |
| POST | `/api/v2/apps/third-party` | 会话、CSRF | 第三方应用创建请求体 | 新增第三方应用 |
| PUT | `/api/v2/apps/third-party/{suiteAppId}` | 会话、CSRF | 第三方应用更新请求体 | 更新密钥、Token 或 EncodingAESKey；空字段保留原值 |
| DELETE | `/api/v2/apps/third-party/{suiteAppId}` | 会话、CSRF | 无 | 删除第三方应用 |
| POST | `/api/v2/apps/third-party/{suiteAppId}/refresh-token` | 会话、CSRF | 无 | 使用 SuiteTicket 刷新 SuiteAccessToken |
| GET | `/api/v2/apps/third-party/{suiteAppId}/authorizations` | 会话 | 无 | 查询已授权企业列表 |

创建请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `suiteId` | string | 是 | 企业微信 SuiteID |
| `suiteSecret` | string | 是 | SuiteSecret |
| `token` | string | 是 | 回调 Token |
| `encodingAesKey` | string | 是 | 43 位 EncodingAESKey |

更新请求体包含 `suiteSecret`、`token`、`encodingAesKey`，均为可选字段。

第三方应用响应字段：

- `id`、`suiteId`
- `hasSuiteTicket`、`hasSuiteAccessToken`
- `callbackUrl`、`installUrl`、`authorizationCallbackUrl`
- `authorizedCorpCount`、`createdAt`、`updatedAt`

已授权企业响应字段：`id`、`corpId`、`corpName`、`corpFullName`、`corpType`、`corpSquareLogoUrl`、`agentId`、`active`、`authorizedAt`、`cancelledAt`、`updatedAt`。

## 10. 应用插件配置

| 方法 | 路径 | 鉴权 | 请求 | 响应/用途 |
| --- | --- | --- | --- | --- |
| GET | `/api/v2/apps/{appId}/plugins` | 会话 | 无 | 查询应用的可用插件和当前配置 |
| POST | `/api/v2/apps/{appId}/plugins` | 会话、CSRF | `pluginKey`、`configJson`、`status` | 保存或更新应用插件配置 |
| DELETE | `/api/v2/apps/{appId}/plugins/{pluginKey}` | 会话、CSRF | 无 | 删除应用的指定插件配置 |

`status` 使用 `1` 表示启用、`0` 表示停用；`configJson` 为插件自定义 JSON 字符串。

## 11. 后台消息发送与日志

| 方法 | 路径 | 鉴权 | 请求 | 响应/用途 |
| --- | --- | --- | --- | --- |
| POST | `/api/v2/messages/send` | 会话、CSRF | 消息发送请求体，包含 `appId` | 从后台发送企业微信消息并返回发送日志 |
| GET | `/api/v2/messages/logs` | 会话 | 查询参数见下表 | 查询发送日志 |

日志查询参数：

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `page` | integer | 无 | 传入 `page` 或 `pageSize` 时启用分页 |
| `pageSize` | integer | 20 | 分页大小 |
| `limit` | integer | 20 | 未启用分页时返回的最近记录数 |
| `success` | boolean | 无 | 按成功状态过滤 |
| `appId` | long | 无 | 按应用过滤 |

消息发送请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `appId` | long | 后台接口必填 | 自建应用数字 ID；Open API 中由 API Key 自动确定 |
| `toUser` | string | 条件必填 | 用户 ID，多个值用 `\|` 分隔 |
| `toParty` | string | 条件必填 | 部门 ID，多个值用 `\|` 分隔 |
| `toAll` | boolean | 否 | `true` 时发送给全员 |
| `msgType` | string | 否 | `TEXT`、`MARKDOWN`、`TEXT_CARD`、`NEWS`，默认 `TEXT` |
| `content` | string | 按类型 | `TEXT`、`MARKDOWN` 必填 |
| `title` | string | 按类型 | `TEXT_CARD` 必填 |
| `description` | string | 按类型 | `TEXT_CARD` 必填 |
| `url` | string | 按类型 | `TEXT_CARD` 必填 |
| `btnText` | string | 否 | 文本卡片按钮文字 |
| `articles` | array | 按类型 | `NEWS` 必填，且至少一项 |

`articles` 项字段：`title`（必填）、`url`（必填）、`description`、`picUrl`。

必须满足以下接收者规则之一：`toAll=true`，或至少提供 `toUser`、`toParty` 中的一项。

日志响应字段：`id`、`appId`、`agentId`、`msgType`、`toUser`、`toParty`、`toAll`、`title`、`description`、`url`、`content`、`success`、`errorMessage`、`createdAt`。

## 12. V2 Open API

| 方法 | 路径 | 鉴权 | 请求 | 响应/用途 |
| --- | --- | --- | --- | --- |
| POST | `/api/v2/openapi/messages/send` | 应用密钥 | 消息发送请求体，不传 `appId` | 使用 API Key 对应的应用发送消息 |

请求头：

```http
X-API-Key: <应用管理中生成的 API Key>
Content-Type: application/json
```

该接口额外兼容以下字段：

| 字段 | 说明 |
| --- | --- |
| `target` | `toUser` 的兼容字段；值为 `@all` 时等价于 `toAll=true` |
| `type` | `msgType` 的兼容字段；当前实现可使用 `text`、`markdown`、`text_card`、`news` |

调用示例：

```bash
curl -X POST 'https://juhe.beichenwl.cn/api/v2/openapi/messages/send' \
  -H 'Content-Type: application/json' \
  -H 'X-API-Key: YOUR_APP_API_KEY' \
  -d '{
    "toUser": "zhangsan|lisi",
    "msgType": "TEXT",
    "content": "系统通知"
  }'
```

## 13. 兼容 V1 推送接口

| 方法 | 路径 | 鉴权 | 请求 | 响应/用途 |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/push` | V1 密钥 | V1 推送请求体 | 使用服务端全局企业微信配置发送消息 |

请求体字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `target` | string | 必填，目标用户 |
| `type` | string | `text`、`markdown`、`text-card`、`image`、`news`，默认 `text` |
| `title` | string | Markdown、文本卡片按类型需要 |
| `content` | string | 文本、Markdown、文本卡片按类型需要 |
| `url` | string | 文本卡片必填 |
| `mediaId` | string | 图片消息必填 |
| `articles` | array | 图文消息必填；项包含 `title`、`description`、`url`、`picUrl` |

V1 接口返回核心模块的 `PushResult`，不使用 V2 的 `PortalResponse` 包装。

## 14. 插件管理

| 方法 | 路径 | 鉴权 | 请求 | 响应/用途 |
| --- | --- | --- | --- | --- |
| GET | `/api/v2/plugin/list` | 会话 | 无 | 查询插件列表和在线状态 |
| POST | `/api/v2/plugin/create` | 会话、CSRF | `pluginKey`、`name`、`description` | 创建插件并返回一次连接令牌 |
| POST | `/api/v2/plugin/reset-token` | 会话、CSRF | JSON：`id` | 重新生成插件连接令牌 |
| POST | `/api/v2/plugin/status` | 会话、CSRF | JSON：`id`、`status` | 启用或停用插件，`1` 启用、`0` 停用 |
| POST | `/api/v2/plugin/delete` | 会话、CSRF | JSON：`id` | 删除插件 |
| GET | `/api/v2/plugins/{pluginKey}/actions` | 会话 | `page=1`、`pageSize=20`，最大 200 | 查询插件任务回执日志 |
| GET | `/api/v2/plugins/{pluginKey}/heartbeats` | 会话 | `page=1`、`pageSize=20`，最大 200 | 查询插件心跳日志 |

`pluginKey` 只能包含字母、数字、下划线和连字符。

## 15. 自建应用企业微信回调

| 方法 | 路径 | 鉴权 | 参数/请求体 | 响应/用途 |
| --- | --- | --- | --- | --- |
| GET | `/api/v2/wecom/callback/{appId}` | 企微签名 | 查询参数：`msg_signature`、`timestamp`、`nonce`、`echostr` | 企业微信回调 URL 校验；成功返回解密后的 `echostr` |
| POST | `/api/v2/wecom/callback/{appId}` | 企微签名 | 查询参数：`msg_signature`、`timestamp`、`nonce`；Body 为加密 XML | 接收消息或事件，成功返回纯文本 `success` |

POST 回调解密后会异步分发给已启用插件，支持文本、图片和事件点击等类型。

## 16. 第三方应用企业微信回调

| 方法 | 路径 | 鉴权 | 参数/请求体 | 响应/用途 |
| --- | --- | --- | --- | --- |
| GET | `/api/v2/wecom/suite-callback/{suiteAppId}` | 公开或企微签名 | 验证时传 `msg_signature`、`timestamp`、`nonce`、`echostr` | 参数齐全时执行回调验证；无参数访问返回健康提示 |
| POST | `/api/v2/wecom/suite-callback/{suiteAppId}` | 企微签名 | 签名查询参数；Body 为加密 XML | 接收 SuiteTicket、授权、变更授权和取消授权事件 |
| GET | `/api/v2/wecom/suite-auth/{suiteAppId}/install` | 公开 | `client`、`resume` 均可选 | 创建授权会话并以 302 跳转到企业微信安装授权页 |
| GET | `/api/v2/wecom/suite-auth/{suiteAppId}/complete` | 公开 | `auth_code`、`state`，均必填 | 企业微信授权完成回调；保存授权后返回成功 HTML 或 302 跳回来源站点 |

`install` 参数说明：

- `client`：授权来源标识，必须存在于服务端白名单；当前默认支持 `u-login`。
- `resume`：来源站点恢复状态，最大 512 字符。
- 授权完成地址按应用生成：`https://juhe.beichenwl.cn/api/v2/wecom/suite-auth/{suiteAppId}/complete`。

Suite 回调处理的 `InfoType`：

| InfoType | 行为 |
| --- | --- |
| `suite_ticket` | 保存 SuiteTicket，并尝试刷新 SuiteAccessToken |
| `create_auth` | 使用临时授权码新增企业授权 |
| `change_auth` | 刷新企业授权信息 |
| `cancel_auth` | 标记企业取消授权 |

## 17. 企业授权内部查询

| 方法 | 路径 | 鉴权 | 请求 | 响应/用途 |
| --- | --- | --- | --- | --- |
| GET | `/api/v2/internal/wecom/suite-apps/{suiteAppId}/corps/{corpId}/authorization` | 内部密钥 | 无 | 查询指定企业在第三方应用中的授权状态 |

请求头：

```http
X-Internal-API-Key: <PUSH_PORTAL_WECOM_INTERNAL_API_KEY>
```

响应字段：`corpId`、`active`、`agentId`、`authorizedAt`、`updatedAt`。响应带有 `Cache-Control: private, max-age=30`。

## 18. 错误处理接口

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| ANY | `/api/error` | 会话 | 内部错误分发控制器，不建议业务调用 |

常见 HTTP 状态码：

| 状态码 | 说明 |
| --- | --- |
| `400` | 请求格式、参数校验或企业微信业务错误 |
| `401` | 未登录、会话过期或 API Key 无效 |
| `403` | CSRF 校验失败或无访问权限 |
| `404` | 资源或接口不存在 |
| `409` | 当前资源状态冲突，例如尚未收到 SuiteTicket |
| `429` | API Key 限流或 V1 安全限流 |
| `502` | 调用企业微信上游接口失败 |
| `503` | 内部企业微信查询接口尚未配置密钥 |

## 19. gRPC 插件接口

### 19.1 服务信息

| 项目 | 值 |
| --- | --- |
| 默认地址 | `<服务器>:9090` |
| 服务名 | `dev.qingzhou.push.api.grpc.PluginGateway` |
| RPC | `Connect` |
| 类型 | 双向流：`stream PluginUpstreamPacket` -> `stream PlatformDownstreamPacket` |
| 鉴权 | Metadata：`Authorization: Bearer <插件令牌>` |
| Proto 文件 | `push-server-api/src/main/proto/plugin_gateway.proto` |

Docker 镜像默认只声明 HTTP 端口 `8000`。需要远程插件连接时，还必须在 Docker/宝塔中额外映射 TCP 端口 `9090`。

### 19.2 上行数据包

`PluginUpstreamPacket` 包含公共 `header`，并通过 `oneof payload` 发送以下一种数据：

| Payload | 用途 | 主要字段 |
| --- | --- | --- |
| `register` | 插件注册 | `plugin_key`、`plugin_version`、`commands`、`max_concurrency`、`config_definitions` |
| `heartbeat` | 插件心跳 | `current_inflight`、`uptime_seconds` |
| `action_ack` | 任务处理回执 | `event_id`、`status`、`message` |
| `push_request` | 插件主动请求平台推送 | `request_id`、`app_id`、`target_user_id`、`type`、`content`、`title`、`url`、`media_id`、`articles` |

### 19.3 下行数据包

`PlatformDownstreamPacket` 通过 `oneof payload` 返回以下一种数据：

| Payload | 用途 | 主要字段 |
| --- | --- | --- |
| `register_ack` | 注册确认 | `success`、`reason`、`server_protocol_ver` |
| `action_event` | 向插件下发用户动作 | `event_id`、`app_id`、`user_id`、`user_name`、`type`、`content`、`channel_source`、`plugin_config` |
| `push_response` | 插件主动推送的处理结果 | `success`、`error_code`、`error_msg` |

公共 `PacketHeader` 字段：`trace_id`、`timestamp`、`plugin_key`、`protocol_ver`。

插件创建后，`POST /api/v2/plugin/create` 返回的令牌用于 gRPC Bearer 鉴权。插件必须处于启用状态才能建立连接。

## 20. 接口数量汇总

| 类型 | 数量 |
| --- | ---: |
| HTTP 业务接口 | 59 |
| HTTP 框架/错误兼容接口 | 2 |
| gRPC 双向流 RPC | 1 |
| 合计 | 62 |

HTTP 数量不包含静态文件、前端页面路由和企业微信官方上游 API。
