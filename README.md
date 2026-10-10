![logo.png](docs/images/logo.png)

# push-server

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0+-green.svg)
![Java](https://img.shields.io/badge/Java-25%2B-blue.svg)
![GraalVM](https://img.shields.io/badge/GraalVM-Native-orange.svg)
![License](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)
![Collaborator](https://img.shields.io/badge/Co--authored--by-Google%20Gemini-blue?logo=google-gemini)

**push-server** 是一个基于 Spring Boot 4 构建的、带管理后台的轻量级企业微信推送服务。它封装了企业微信复杂的 API，对外提供极其简单的 HTTP 接口，支持 Docker 原生镜像部署（启动仅需 0.1s，内存占用 <50MB）。

---

## 📖 项目定位与目标

**push-server** 的核心目标是：

> **通过企业微信（WeCom），将系统消息稳定、合规地推送到用户的微信中接收。**

整体消息流转路径如下：

```mermaid
flowchart LR
  A[业务系统 / 服务] -->|HTTP请求| B[push-server]
  B -->|企业微信 API| C[企业微信服务端]
    C --> D[微信 App]
```

---

## ⚡️ 核心特性

* **轻量级 & 高性能**：基于 Spring Boot 4 + GraalVM Native Image，极致的启动速度和资源占用。
* **Web 管理后台**：内置 Web UI，支持在线完成所有配置（企业微信、应用、API Key），查看推送日志和统计报表。
* **动态 IP 代理支持**：内置 **HTTP/SOCKS5 代理**配置功能，支持带账号密码认证，完美解决因服务器 IP 动态变动导致无法加入企业微信白名单的问题。
* **版本自动检查**：后台仪表盘支持自动比对 GitHub 最新发布版本，确保您能第一时间获取功能更新与安全修复。
* **多应用隔离**：支持管理多个企业微信应用，每个应用拥有独立 API Key 和限流策略。
* **内嵌数据库**：默认使用 SQLite 数据库，无需安装额外的数据库服务。
* **开箱即用**：无需复杂配置，首次运行后通过 Web 页面即可完成初始化。
* **插件扩展支持**：支持 gRPC 双向流插件扩展，可实时接收并处理用户在企业微信端的消息与点击事件。
    *   **消息回调增强**：现已支持**图片消息**回调，插件可获取用户发送的图片 URL 进行二次处理。
* **全平台 CI/CD**：提供全自动化的 GitHub Actions 流，支持多架构（Linux amd64/arm64, macOS, Windows）二进制产物构建。
    *   **多架构 Docker**：支持一键构建 amd64 与 arm64 多架构 Docker 镜像。
    *   **API 自动发布**：Release 时自动将 API 模块发布至 GitHub Packages，方便插件开发者引用。

---

## 🛠️ 技术栈

* **后端**: Spring Boot 4, Java 25, MyBatis Plus
* **前端**: Vue 3, Element Plus
* **数据库**: SQLite (内嵌)
* **运行时**: GraalVM (支持 Native Image 编译)

---

## 📸 界面预览

<p align="center">
  <img src="docs/images/dashboard.png" alt="仪表盘" width="100%">
  <br>
  <em>仪表盘统计</em>
</p>

| 系统初始化 | 系统登录 |
| :---: | :---: |
| <img src="docs/images/init.png" width="100%"> | <img src="docs/images/login.png" width="100%"> |

| 应用管理 | 在线调试 |
| :---: | :---: |
| <img src="docs/images/apps.png" width="100%"> | <img src="docs/images/messages.png" width="100%"> |

| 推送日志 | 密钥管理 |
| :---: | :---: |
| <img src="docs/images/logs.png" width="100%"> | <img src="docs/images/keys.png" width="100%"> |

| 系统设置 | 用户管理 |
| :---: | :---: |
| <img src="docs/images/serrings.png" width="100%"> | <img src="docs/images/user.png" width="100%"> |

|                       代理配置                        |                    版本更新提示                     |
|:-------------------------------------------------:|:---------------------------------------------:|
| <img src="docs/images/proxy.png" width="100%"> | <img src="docs/images/version.png" width="100%"> |

---

## 🚀 快速开始 (Docker)

推荐使用 Docker 运行，无需安装 Java 环境。

```bash
docker run -d \
  --name push-server \
  -p 8000:8000 \
  -e PUSH_PORTAL_WECOM_PUBLIC_BASE_URL=https://juhe.beichenwl.cn \
  -e PUSH_PORTAL_WECOM_AUTH_CALLBACK_URL=https://u.beichenwl.cn/wework_suite_install_return.php \
  -e PUSH_PORTAL_WECOM_INTERNAL_API_KEY=请替换为随机密钥 \
  -e PUSH_PORTAL_WECOM_U_LOGIN_REDIRECT_URL=https://u.beichenwl.cn/wework_suite_install_return.php \
  -v $(pwd)/data:/app/data \
  qingzhoudev/push-server:latest
```
* **数据持久化**: `-v $(pwd)/data:/app/data` 会将应用数据（包括 SQLite 数据库）保存到当前目录下的 `data` 文件夹中。
* **首次运行**: 启动后，访问 `http://localhost:8000`，系统会自动跳转至**初始化页面**。请根据引导完成管理员账号注册和企业微信配置。

### Docker Compose

如果您更习惯使用 Docker Compose，可以使用以下配置：

```yaml
services:
  push-server:
    image: qingzhoudev/push-server:latest
    container_name: push-server
    ports:
      - "8000:8000"
    environment:
      PUSH_PORTAL_WECOM_PUBLIC_BASE_URL: https://juhe.beichenwl.cn
      PUSH_PORTAL_WECOM_AUTH_CALLBACK_URL: https://u.beichenwl.cn/wework_suite_install_return.php
      PUSH_PORTAL_WECOM_INTERNAL_API_KEY: 请替换为随机密钥
      PUSH_PORTAL_WECOM_U_LOGIN_REDIRECT_URL: https://u.beichenwl.cn/wework_suite_install_return.php
    volumes:
      - ./data:/app/data
    restart: unless-stopped
```

启动命令：
```bash
docker-compose up -d
```

### 企业微信统一授权中心

第三方应用的数据回调和安装授权由本服务统一处理。`PUSH_PORTAL_WECOM_PUBLIC_BASE_URL` 必须是反向代理到本服务的公网 HTTPS 地址，不要填写容器内部地址。`PUSH_PORTAL_WECOM_AUTH_CALLBACK_URL` 必须位于企业微信后台填写的“安装完成回调域名”之下；当前通过 `u.beichenwl.cn` 桥接回统一授权中心。`PUSH_PORTAL_WECOM_INTERNAL_API_KEY` 用于其他站点只读查询企业授权状态，建议使用 `openssl rand -hex 32` 生成，并在调用方配置相同的值。

授权来源站点只能跳转到服务端白名单。当前 `u-login` 客户端由 `PUSH_PORTAL_WECOM_U_LOGIN_REDIRECT_URL` 配置；授权完成后只返回授权状态、企业 ID 和原登录恢复状态，不返回 `permanent_code` 或企业访问令牌。

`u.beichenwl.cn` 的企业微信插件需要选择“第三方应用安装授权”模式，并填写以下三项：统一授权中心地址 `https://juhe.beichenwl.cn`、后台第三方应用的数字编号，以及与 `PUSH_PORTAL_WECOM_INTERNAL_API_KEY` 相同的内部密钥。企业微信服务商后台的数据/指令回调配置聚合消息推送平台管理页显示的 Suite 回调地址，“安装完成回调域名”填写 `u.beichenwl.cn`；授权码经 PHP 桥接页立即转交统一授权中心，不在 PHP 项目保存。

---

## 🛡️ 安全配置

为了提高系统安全性，建议在**系统设置**中开启 **Cloudflare Turnstile** 验证。

* **风险**: 未开启验证可能导致登录接口面临暴力破解或恶意攻击风险。
* **配置**: 开启验证需前往 [Cloudflare](https://www.cloudflare.com/products/turnstile/) 获取 Site Key 和 Secret Key，并在系统设置中填入。
* **管理**: 系统初始化后，可通过 [**Turnstile 管理 API**](./docs/turnstile-api.md) 或后台管理页面（开发中）随时修改 Site Key 或开关验证。

---

## 🔌 API 文档

**push-server** 提供 V2 和 V1 两套 API。**强烈推荐使用 V2 API**。

### V2 API (推荐)

V2 API 提供了更强大、更标准的功能。

* **鉴权**: 使用在 **Portal 管理后台** -> **应用管理** 中为每个应用生成的 **API Key**。在请求时，将其放入 `X-API-Key` Header 中。
* **详细文档**: 完整的 API 定义和示例请参考 [**V2 OpenAPI 文档**](./docs/openapi-v2.md)。

**调用示例 (发送文本消息):**
```bash
curl -X POST http://localhost:8000/api/v2/openapi/messages/send \
  -H "X-API-Key: 您在后台生成的App API Key" \
  -H "Content-Type: application/json" \
  -d '{
    "toUser": "ZhangSan|LiSi",
    "msgType": "text",
    "content": "系统通知：您的任务已构建完成。"
  }'
```

### V1 API (兼容)

V1 API 为保持向后兼容而保留。

* **鉴权**: 使用在 `application.yml` (或环境变量 `PUSH_AUTH_KEY`) 中配置的**全局 Token**。
* **URL**: `/api/v1/push`
* **Method**: `POST`
* **Header**: `X-API-Key: <push.auth.key>`

**调用示例 (发送文本消息):**
```bash
curl -X POST http://localhost:8000/api/v1/push \
  -H "X-API-Key: 全局Token" \
  -H "Content-Type: application/json" \
  -d '{
    "target": "ZhangSan|LiSi",
    "type": "TEXT",
    "content": "系统通知：您的任务已构建完成。"
  }'
```

---

## ⚙️ V1 配置说明 (不推荐)

以下配置仅适用于旧版 V1 API。V2 的所有配置均在 Portal 后台在线完成。

```yaml
# application-prod.yml
push:
  auth:
    key: "v1-global-token" # V1 使用的全局 Token
  wecom:
    app-key: "你的企业ID"      # V1 使用
    app-secret: "你的应用Secret" # V1 使用
    agent-id: "你的应用AgentID"   # V1 使用
```
---
## 🤝 鸣谢 (Credits)

本项目是**AI 辅助开发**的实践案例，特别鸣谢：

* **[Google Gemini](https://gemini.google.com/)**：深度参与了本项目的开发全过程，独立完成了**全部前端代码的编写、UI/UX 设计以及样式优化**，展现了卓越的代码生成与设计能力。
* **开源社区**：感谢 Spring Boot, Vue, Element Plus 等优秀开源项目提供的坚实基础。

如果你喜欢这个项目，请不要吝啬你的 Star！🌟
